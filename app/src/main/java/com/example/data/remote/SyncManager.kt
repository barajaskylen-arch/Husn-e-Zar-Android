package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

data class PendingAction(
    val id: String = UUID.randomUUID().toString(),
    val table: String,
    val actionType: String, // "UPSERT", "DELETE", "DELETE_ALL"
    val entityId: String = "",
    val jsonPayload: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class SyncManager(
    private val context: Context,
    private val dao: AppDao,
    private val supabaseAuth: SupabaseAuth
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = Mutex()
    private val queueMutex = Mutex()

    private val prefs = context.getSharedPreferences("supabase_sync_prefs", Context.MODE_PRIVATE)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(prefs.getLong(KEY_LAST_SYNCED, 0L))
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val pendingQueueAdapter = moshi.adapter<List<PendingAction>>(
        Types.newParameterizedType(List::class.java, PendingAction::class.java)
    )

    private val customerAdapter = moshi.adapter(CustomerDto::class.java)
    private val orderAdapter = moshi.adapter(OrderDto::class.java)
    private val batchAdapter = moshi.adapter(InventoryBatchDto::class.java)
    private val expenseAdapter = moshi.adapter(ExpenseDto::class.java)
    private val taskAdapter = moshi.adapter(DailyTaskDto::class.java)
    private val paymentAdapter = moshi.adapter(CustomerPaymentDto::class.java)

    private val queueFile = File(context.filesDir, "pending_sync_queue.json")

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = supabaseAuth.getValidAccessToken()
                val authHeader = if (!token.isNullOrBlank()) {
                    "Bearer $token"
                } else {
                    "Bearer ${SupabaseConfig.anonKey}"
                }
                val request = original.newBuilder()
                    .header("apikey", SupabaseConfig.anonKey)
                    .header("Authorization", authHeader)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.getSafeBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)
    }

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // -------------------------------------------------------------
    // Pending Queue Storage
    // -------------------------------------------------------------

    private suspend fun readQueue(): MutableList<PendingAction> = queueMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (queueFile.exists()) {
                    val content = queueFile.readText()
                    if (content.isNotBlank()) {
                        return@withContext pendingQueueAdapter.fromJson(content)?.toMutableList() ?: mutableListOf()
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncManager", "Error reading queue", e)
            }
            mutableListOf()
        }
    }

    private suspend fun writeQueue(queue: List<PendingAction>) = queueMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val json = pendingQueueAdapter.toJson(queue)
                queueFile.writeText(json)
            } catch (e: Exception) {
                Log.e("SyncManager", "Error writing queue", e)
            }
        }
    }

    private fun enqueueAction(action: PendingAction) {
        scope.launch {
            val list = readQueue()
            list.add(action)
            writeQueue(list)
            if (isOnline() && supabaseAuth.isLoggedIn()) {
                flushPendingQueue()
            }
        }
    }

    // -------------------------------------------------------------
    // Flush Pending Writes
    // -------------------------------------------------------------

    suspend fun flushPendingQueue() = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || !supabaseAuth.isLoggedIn() || !isOnline()) return@withContext

        val currentQueue = readQueue()
        if (currentQueue.isEmpty()) return@withContext

        val remaining = mutableListOf<PendingAction>()

        for (action in currentQueue) {
            val success = try {
                executePendingAction(action)
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed action ${action.id}", e)
                false
            }
            if (!success) {
                remaining.add(action)
            }
        }

        writeQueue(remaining)
    }

    private suspend fun executePendingAction(action: PendingAction): Boolean {
        return when (action.actionType) {
            "UPSERT" -> {
                when (action.table) {
                    "customers" -> {
                        val dto = customerAdapter.fromJson(action.jsonPayload) ?: return true
                        val res = api.upsertCustomers(customers = listOf(dto))
                        res.isSuccessful
                    }
                    "orders" -> {
                        val dto = orderAdapter.fromJson(action.jsonPayload) ?: return true
                        val res = api.upsertOrders(orders = listOf(dto))
                        res.isSuccessful
                    }
                    "inventory_batches" -> {
                        val dto = batchAdapter.fromJson(action.jsonPayload) ?: return true
                        val res = api.upsertInventoryBatches(batches = listOf(dto))
                        res.isSuccessful
                    }
                    "expenses" -> {
                        val dto = expenseAdapter.fromJson(action.jsonPayload) ?: return true
                        val res = api.upsertExpenses(expenses = listOf(dto))
                        res.isSuccessful
                    }
                    "daily_tasks" -> {
                        val dto = taskAdapter.fromJson(action.jsonPayload) ?: return true
                        val res = api.upsertDailyTasks(tasks = listOf(dto))
                        res.isSuccessful
                    }
                    "customer_payments" -> {
                        val dto = paymentAdapter.fromJson(action.jsonPayload) ?: return true
                        val res = api.upsertCustomerPayments(payments = listOf(dto))
                        res.isSuccessful
                    }
                    else -> true
                }
            }
            "DELETE" -> {
                val filter = "eq.${action.entityId}"
                when (action.table) {
                    "customers" -> api.deleteCustomerById(filter).isSuccessful
                    "orders" -> api.deleteOrderById(filter).isSuccessful
                    "inventory_batches" -> api.deleteInventoryBatchById(filter).isSuccessful
                    "expenses" -> api.deleteExpenseById(filter).isSuccessful
                    "daily_tasks" -> api.deleteDailyTaskById(filter).isSuccessful
                    "customer_payments" -> api.deleteCustomerPaymentById(filter).isSuccessful
                    else -> true
                }
            }
            "DELETE_ALL" -> {
                val filter = "not.is.null"
                when (action.table) {
                    "customers" -> api.deleteAllCustomers(filter).isSuccessful
                    "orders" -> api.deleteAllOrders(filter).isSuccessful
                    "inventory_batches" -> api.deleteAllInventoryBatches(filter).isSuccessful
                    "expenses" -> api.deleteAllExpenses(filter).isSuccessful
                    "daily_tasks" -> api.deleteAllDailyTasks(filter).isSuccessful
                    "customer_payments" -> api.deleteAllCustomerPayments(filter).isSuccessful
                    else -> true
                }
            }
            else -> true
        }
    }

    // -------------------------------------------------------------
    // Full Sync: Reads & One-time Migration
    // -------------------------------------------------------------

    suspend fun syncAll(): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            val msg = "Supabase URL aur Key missing hain. AI Studio Secrets panel mein add karein."
            _syncStatusMessage.value = msg
            return@withContext Result.failure(IllegalStateException(msg))
        }

        if (!supabaseAuth.isLoggedIn()) {
            val msg = "Pehle login karein."
            _syncStatusMessage.value = msg
            return@withContext Result.failure(IllegalStateException(msg))
        }

        if (!isOnline()) {
            val msg = "Internet connection nahi hai."
            _syncStatusMessage.value = msg
            return@withContext Result.failure(IllegalStateException(msg))
        }

        if (_isSyncing.value) {
            return@withContext Result.success("Sync pehle se chal raha hai")
        }

        syncMutex.withLock {
            _isSyncing.value = true
            try {
                // 1. Flush any pending local writes
                flushPendingQueue()

                // 2. Fetch all 6 remote tables
                val remoteCustomers = api.getCustomers()
                val remoteOrders = api.getOrders()
                val remoteBatches = api.getInventoryBatches()
                val remoteExpenses = api.getExpenses()
                val remoteTasks = api.getDailyTasks()
                val remotePayments = api.getCustomerPayments()

                val isRemoteCompletelyEmpty = remoteCustomers.isEmpty() &&
                        remoteOrders.isEmpty() &&
                        remoteBatches.isEmpty() &&
                        remoteExpenses.isEmpty() &&
                        remoteTasks.isEmpty() &&
                        remotePayments.isEmpty()

                val localCustomers = dao.getAllCustomersOnce()
                val localOrders = dao.getAllOrdersOnce()
                val localBatches = dao.getAllBatchesOnce()
                val localExpenses = dao.getAllExpensesOnce()
                val localTasks = dao.getAllTasksOnce()
                val localPayments = dao.getAllPaymentsOnce()

                val hasLocalData = localCustomers.isNotEmpty() ||
                        localOrders.isNotEmpty() ||
                        localBatches.isNotEmpty() ||
                        localExpenses.isNotEmpty() ||
                        localTasks.isNotEmpty() ||
                        localPayments.isNotEmpty()

                // 3. One-time Migration: If Supabase is empty and local Room has data
                if (isRemoteCompletelyEmpty && hasLocalData) {
                    if (localCustomers.isNotEmpty()) {
                        api.upsertCustomers(customers = localCustomers.map { it.toDto() })
                    }
                    if (localOrders.isNotEmpty()) {
                        api.upsertOrders(orders = localOrders.map { it.toDto() })
                    }
                    if (localBatches.isNotEmpty()) {
                        api.upsertInventoryBatches(batches = localBatches.map { it.toDto() })
                    }
                    if (localExpenses.isNotEmpty()) {
                        api.upsertExpenses(expenses = localExpenses.map { it.toDto() })
                    }
                    if (localTasks.isNotEmpty()) {
                        api.upsertDailyTasks(tasks = localTasks.map { it.toDto() })
                    }
                    if (localPayments.isNotEmpty()) {
                        api.upsertCustomerPayments(payments = localPayments.map { it.toDto() })
                    }
                } else {
                    // 4. Update local Room from Supabase remote (Supabase is source of truth)
                    if (remoteCustomers.isNotEmpty()) {
                        dao.insertCustomers(remoteCustomers.map { it.toEntity() })
                    }
                    if (remoteOrders.isNotEmpty()) {
                        dao.insertOrders(remoteOrders.map { it.toEntity() })
                    }
                    if (remoteBatches.isNotEmpty()) {
                        dao.insertInventoryBatches(remoteBatches.map { it.toEntity() })
                    }
                    if (remoteExpenses.isNotEmpty()) {
                        dao.insertExpenses(remoteExpenses.map { it.toEntity() })
                    }
                    if (remoteTasks.isNotEmpty()) {
                        dao.insertDailyTasks(remoteTasks.map { it.toEntity() })
                    }
                    if (remotePayments.isNotEmpty()) {
                        dao.insertCustomerPayments(remotePayments.map { it.toEntity() })
                    }
                }

                val now = System.currentTimeMillis()
                prefs.edit().putLong(KEY_LAST_SYNCED, now).apply()
                _lastSyncedTime.value = now
                _syncStatusMessage.value = "Sync kamyab raha"

                Result.success("Sync kamyab raha")
            } catch (e: Exception) {
                Log.e("SyncManager", "Sync error", e)
                val msg = "Sync mein masla aaya: ${e.localizedMessage ?: "Unknown error"}"
                _syncStatusMessage.value = msg
                Result.failure(e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // -------------------------------------------------------------
    // Repository Sync Helpers (Invoked after Room writes)
    // -------------------------------------------------------------

    fun syncUpsertCustomer(customer: Customer) {
        enqueueAction(
            PendingAction(
                table = "customers",
                actionType = "UPSERT",
                entityId = customer.id,
                jsonPayload = customerAdapter.toJson(customer.toDto())
            )
        )
    }

    fun syncDeleteCustomer(id: String) {
        enqueueAction(
            PendingAction(
                table = "customers",
                actionType = "DELETE",
                entityId = id
            )
        )
    }

    fun syncUpsertOrder(order: Order) {
        enqueueAction(
            PendingAction(
                table = "orders",
                actionType = "UPSERT",
                entityId = order.id,
                jsonPayload = orderAdapter.toJson(order.toDto())
            )
        )
    }

    fun syncDeleteOrder(id: String) {
        enqueueAction(
            PendingAction(
                table = "orders",
                actionType = "DELETE",
                entityId = id
            )
        )
    }

    fun syncUpsertBatch(batch: InventoryBatch) {
        enqueueAction(
            PendingAction(
                table = "inventory_batches",
                actionType = "UPSERT",
                entityId = batch.id,
                jsonPayload = batchAdapter.toJson(batch.toDto())
            )
        )
    }

    fun syncUpsertExpense(expense: Expense) {
        enqueueAction(
            PendingAction(
                table = "expenses",
                actionType = "UPSERT",
                entityId = expense.id,
                jsonPayload = expenseAdapter.toJson(expense.toDto())
            )
        )
    }

    fun syncUpsertDailyTask(task: DailyTask) {
        enqueueAction(
            PendingAction(
                table = "daily_tasks",
                actionType = "UPSERT",
                entityId = task.id,
                jsonPayload = taskAdapter.toJson(task.toDto())
            )
        )
    }

    fun syncDeleteDailyTask(id: String) {
        enqueueAction(
            PendingAction(
                table = "daily_tasks",
                actionType = "DELETE",
                entityId = id
            )
        )
    }

    fun syncUpsertCustomerPayment(payment: CustomerPayment) {
        enqueueAction(
            PendingAction(
                table = "customer_payments",
                actionType = "UPSERT",
                entityId = payment.id,
                jsonPayload = paymentAdapter.toJson(payment.toDto())
            )
        )
    }

    fun syncClearAll() {
        val tables = listOf("customers", "orders", "customer_payments", "inventory_batches", "expenses", "daily_tasks")
        for (tbl in tables) {
            enqueueAction(
                PendingAction(
                    table = tbl,
                    actionType = "DELETE_ALL"
                )
            )
        }
    }

    fun syncRestoreAll(
        customersList: List<Customer>,
        ordersList: List<Order>,
        paymentsList: List<CustomerPayment>,
        batchesList: List<InventoryBatch>,
        expensesList: List<Expense>,
        tasksList: List<DailyTask>
    ) {
        syncClearAll()
        customersList.forEach { syncUpsertCustomer(it) }
        ordersList.forEach { syncUpsertOrder(it) }
        paymentsList.forEach { syncUpsertCustomerPayment(it) }
        batchesList.forEach { syncUpsertBatch(it) }
        expensesList.forEach { syncUpsertExpense(it) }
        tasksList.forEach { syncUpsertDailyTask(it) }
    }

    companion object {
        private const val KEY_LAST_SYNCED = "last_synced_timestamp"
    }
}
