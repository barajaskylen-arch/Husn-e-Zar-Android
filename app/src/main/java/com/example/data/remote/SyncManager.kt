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
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

data class PendingAction(
    val id: String = UUID.randomUUID().toString(),
    val table: String,
    val actionType: String, // "UPSERT", "DELETE", "DELETE_ALL", "BATCH_UPSERT"
    val entityId: String = "",
    val jsonPayload: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)

sealed class ActionOutcome {
    object Success : ActionOutcome()
    data class ClientError(val code: Int, val message: String) : ActionOutcome()
    data class TransientError(val code: Int?, val message: String) : ActionOutcome()
}

class SyncManager(
    private val context: Context,
    private val dao: AppDao,
    private val supabaseAuth: SupabaseAuth
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = Mutex()
    private val queueMutex = Mutex()
    private val flushMutex = Mutex()
    private var needsAnotherFlush = false

    private val prefs = context.getSharedPreferences("supabase_sync_prefs", Context.MODE_PRIVATE)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(prefs.getLong(KEY_LAST_SYNCED, 0L))
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val _pendingUploadsCount = MutableStateFlow(0)
    val pendingUploadsCount: StateFlow<Int> = _pendingUploadsCount.asStateFlow()

    var onDefaultTasksNeeded: (() -> Unit)? = null

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val pendingQueueAdapter = moshi.adapter<List<PendingAction>>(
        Types.newParameterizedType(List::class.java, PendingAction::class.java)
    )

    // Single entity adapters
    private val customerAdapter = moshi.adapter(CustomerDto::class.java)
    private val orderAdapter = moshi.adapter(OrderDto::class.java)
    private val batchAdapter = moshi.adapter(InventoryBatchDto::class.java)
    private val expenseAdapter = moshi.adapter(ExpenseDto::class.java)
    private val taskAdapter = moshi.adapter(DailyTaskDto::class.java)
    private val paymentAdapter = moshi.adapter(CustomerPaymentDto::class.java)

    // Batch entity adapters (for 200-row chunks in restore / migration)
    private val customersListAdapter = moshi.adapter<List<CustomerDto>>(
        Types.newParameterizedType(List::class.java, CustomerDto::class.java)
    )
    private val ordersListAdapter = moshi.adapter<List<OrderDto>>(
        Types.newParameterizedType(List::class.java, OrderDto::class.java)
    )
    private val batchesListAdapter = moshi.adapter<List<InventoryBatchDto>>(
        Types.newParameterizedType(List::class.java, InventoryBatchDto::class.java)
    )
    private val expensesListAdapter = moshi.adapter<List<ExpenseDto>>(
        Types.newParameterizedType(List::class.java, ExpenseDto::class.java)
    )
    private val tasksListAdapter = moshi.adapter<List<DailyTaskDto>>(
        Types.newParameterizedType(List::class.java, DailyTaskDto::class.java)
    )
    private val paymentsListAdapter = moshi.adapter<List<CustomerPaymentDto>>(
        Types.newParameterizedType(List::class.java, CustomerPaymentDto::class.java)
    )

    private val queueFile = File(context.filesDir, "pending_sync_queue.json")

    init {
        scope.launch {
            queueMutex.withLock {
                _pendingUploadsCount.value = readFileQueue().size
            }
        }
    }

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
    // Low-level Queue File I/O
    // -------------------------------------------------------------

    private fun readFileQueue(): MutableList<PendingAction> {
        try {
            if (queueFile.exists()) {
                val content = queueFile.readText()
                if (content.isNotBlank()) {
                    return pendingQueueAdapter.fromJson(content)?.toMutableList() ?: mutableListOf()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error reading queue", e)
        }
        return mutableListOf()
    }

    private fun writeFileQueue(queue: List<PendingAction>) {
        try {
            val json = pendingQueueAdapter.toJson(queue)
            queueFile.writeText(json)
        } catch (e: Exception) {
            Log.e("SyncManager", "Error writing queue", e)
        }
    }

    // -------------------------------------------------------------
    // Queue Modification with Lock & Coalescing
    // -------------------------------------------------------------

    private fun applyActionWithCoalescing(list: MutableList<PendingAction>, action: PendingAction) {
        when (action.actionType) {
            "UPSERT" -> {
                if (action.entityId.isNotBlank()) {
                    val existingIndex = list.indexOfFirst {
                        it.table == action.table && it.entityId == action.entityId && it.actionType == "UPSERT"
                    }
                    if (existingIndex != -1) {
                        list[existingIndex] = action
                        return
                    }
                }
                list.add(action)
            }
            "DELETE" -> {
                if (action.entityId.isNotBlank()) {
                    list.removeAll {
                        it.table == action.table && it.entityId == action.entityId && it.actionType == "UPSERT"
                    }
                    val existingDelIndex = list.indexOfFirst {
                        it.table == action.table && it.entityId == action.entityId && it.actionType == "DELETE"
                    }
                    if (existingDelIndex != -1) {
                        list[existingDelIndex] = action
                        return
                    }
                }
                list.add(action)
            }
            "DELETE_ALL" -> {
                list.removeAll { it.table == action.table }
                list.add(action)
            }
            "BATCH_UPSERT" -> {
                list.add(action)
            }
            else -> {
                list.add(action)
            }
        }
    }

    private fun enqueueAction(action: PendingAction) {
        enqueueAll(listOf(action))
    }

    fun enqueueAll(actions: List<PendingAction>) {
        if (actions.isEmpty()) return
        scope.launch {
            queueMutex.withLock {
                val list = readFileQueue()
                for (action in actions) {
                    applyActionWithCoalescing(list, action)
                }
                writeFileQueue(list)
                _pendingUploadsCount.value = list.size
            }
            triggerFlush()
        }
    }

    // -------------------------------------------------------------
    // Flush Coalescing & Execution
    // -------------------------------------------------------------

    private fun triggerFlush() {
        scope.launch {
            if (!isOnline() || !supabaseAuth.isLoggedIn()) return@launch
            if (flushMutex.isLocked) {
                needsAnotherFlush = true
                return@launch
            }
            flushPendingQueue()
        }
    }

    suspend fun flushPendingQueue() = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || !supabaseAuth.isLoggedIn() || !isOnline()) return@withContext
        flushMutex.withLock {
            do {
                needsAnotherFlush = false
                doFlushIteration()
            } while (needsAnotherFlush && isOnline() && supabaseAuth.isLoggedIn())
        }
    }

    private suspend fun doFlushIteration() {
        val snapshot = queueMutex.withLock {
            readFileQueue().toList()
        }
        if (snapshot.isEmpty()) return

        for (action in snapshot) {
            val result = executePendingActionWithRetry(action)
            when (result) {
                is ActionOutcome.Success -> {
                    // Success: remove ONLY this action by id under lock
                    queueMutex.withLock {
                        val current = readFileQueue()
                        current.removeAll { it.id == action.id }
                        writeFileQueue(current)
                        _pendingUploadsCount.value = current.size
                    }
                }
                is ActionOutcome.ClientError -> {
                    // 4xx error (not 401, 408, 429)
                    val newAttempts = action.attempts + 1
                    if (newAttempts >= 5) {
                        Log.e(
                            "SyncManager",
                            "Dropping action ${action.id} on table ${action.table} after 5 failed attempts: code ${result.code}, msg: ${result.message}"
                        )
                        queueMutex.withLock {
                            val current = readFileQueue()
                            current.removeAll { it.id == action.id }
                            writeFileQueue(current)
                            _pendingUploadsCount.value = current.size
                        }
                    } else {
                        queueMutex.withLock {
                            val current = readFileQueue()
                            val idx = current.indexOfFirst { it.id == action.id }
                            if (idx != -1) {
                                current[idx] = current[idx].copy(attempts = newAttempts)
                                writeFileQueue(current)
                            }
                        }
                    }
                }
                is ActionOutcome.TransientError -> {
                    // Network error, 5xx, 401, 408, 429: leave in queue
                    Log.w(
                        "SyncManager",
                        "Transient failure for action ${action.id} on table ${action.table}: code ${result.code}, msg: ${result.message}"
                    )
                    if (!isOnline() || !supabaseAuth.isLoggedIn()) {
                        break
                    }
                }
            }
        }
    }

    private suspend fun executePendingActionWithRetry(action: PendingAction): ActionOutcome {
        val firstAttempt = executePendingAction(action)
        if (firstAttempt is ActionOutcome.TransientError && firstAttempt.code == 401) {
            // Refresh token once and retry
            val newToken = supabaseAuth.forceRefreshToken()
            if (!newToken.isNullOrBlank()) {
                return executePendingAction(action)
            }
        }
        return firstAttempt
    }

    private suspend fun executePendingAction(action: PendingAction): ActionOutcome {
        return try {
            when (action.actionType) {
                "UPSERT" -> {
                    val res = when (action.table) {
                        "customers" -> {
                            val dto = customerAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertCustomers(customers = listOf(dto))
                        }
                        "orders" -> {
                            val dto = orderAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertOrders(orders = listOf(dto))
                        }
                        "inventory_batches" -> {
                            val dto = batchAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertInventoryBatches(batches = listOf(dto))
                        }
                        "expenses" -> {
                            val dto = expenseAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertExpenses(expenses = listOf(dto))
                        }
                        "daily_tasks" -> {
                            val dto = taskAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertDailyTasks(tasks = listOf(dto))
                        }
                        "customer_payments" -> {
                            val dto = paymentAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertCustomerPayments(payments = listOf(dto))
                        }
                        else -> return ActionOutcome.Success
                    }
                    handleResponse(res)
                }
                "BATCH_UPSERT" -> {
                    val res = when (action.table) {
                        "customers" -> {
                            val dtos = customersListAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertCustomers(customers = dtos)
                        }
                        "orders" -> {
                            val dtos = ordersListAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertOrders(orders = dtos)
                        }
                        "inventory_batches" -> {
                            val dtos = batchesListAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertInventoryBatches(batches = dtos)
                        }
                        "expenses" -> {
                            val dtos = expensesListAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertExpenses(expenses = dtos)
                        }
                        "daily_tasks" -> {
                            val dtos = tasksListAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertDailyTasks(tasks = dtos)
                        }
                        "customer_payments" -> {
                            val dtos = paymentsListAdapter.fromJson(action.jsonPayload) ?: return ActionOutcome.Success
                            api.upsertCustomerPayments(payments = dtos)
                        }
                        else -> return ActionOutcome.Success
                    }
                    handleResponse(res)
                }
                "DELETE" -> {
                    val filter = "eq.${action.entityId}"
                    val res = when (action.table) {
                        "customers" -> api.deleteCustomerById(filter)
                        "orders" -> api.deleteOrderById(filter)
                        "inventory_batches" -> api.deleteInventoryBatchById(filter)
                        "expenses" -> api.deleteExpenseById(filter)
                        "daily_tasks" -> api.deleteDailyTaskById(filter)
                        "customer_payments" -> api.deleteCustomerPaymentById(filter)
                        else -> return ActionOutcome.Success
                    }
                    handleResponse(res)
                }
                "DELETE_ALL" -> {
                    val filter = "not.is.null"
                    val res = when (action.table) {
                        "customers" -> api.deleteAllCustomers(filter)
                        "orders" -> api.deleteAllOrders(filter)
                        "inventory_batches" -> api.deleteAllInventoryBatches(filter)
                        "expenses" -> api.deleteAllExpenses(filter)
                        "daily_tasks" -> api.deleteAllDailyTasks(filter)
                        "customer_payments" -> api.deleteAllCustomerPayments(filter)
                        else -> return ActionOutcome.Success
                    }
                    handleResponse(res)
                }
                else -> ActionOutcome.Success
            }
        } catch (e: Exception) {
            ActionOutcome.TransientError(null, e.localizedMessage ?: "Network error")
        }
    }

    private fun handleResponse(res: Response<ResponseBody>): ActionOutcome {
        return if (res.isSuccessful) {
            ActionOutcome.Success
        } else {
            val code = res.code()
            val errorMsg = try {
                res.errorBody()?.string() ?: "HTTP error $code"
            } catch (e: Exception) {
                "HTTP error $code"
            }
            if (code in 400..499 && code != 401 && code != 408 && code != 429) {
                ActionOutcome.ClientError(code, errorMsg)
            } else {
                ActionOutcome.TransientError(code, errorMsg)
            }
        }
    }

    // -------------------------------------------------------------
    // Safe Pull Helpers with Pagination & 401 Retry
    // -------------------------------------------------------------

    private suspend fun <T> callWith401Retry(call: suspend () -> T): T {
        return try {
            call()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                val refreshed = supabaseAuth.forceRefreshToken()
                if (!refreshed.isNullOrBlank()) {
                    call()
                } else {
                    throw e
                }
            } else {
                throw e
            }
        }
    }

    private suspend fun fetchAllCustomers(): List<CustomerDto> {
        val result = mutableListOf<CustomerDto>()
        var offset = 0
        val limit = 1000
        while (true) {
            val page = callWith401Retry { api.getCustomers(limit = limit, offset = offset) }
            result.addAll(page)
            if (page.size < limit) break
            offset += page.size
        }
        return result
    }

    private suspend fun fetchAllOrders(): List<OrderDto> {
        val result = mutableListOf<OrderDto>()
        var offset = 0
        val limit = 1000
        while (true) {
            val page = callWith401Retry { api.getOrders(limit = limit, offset = offset) }
            result.addAll(page)
            if (page.size < limit) break
            offset += page.size
        }
        return result
    }

    private suspend fun fetchAllBatches(): List<InventoryBatchDto> {
        val result = mutableListOf<InventoryBatchDto>()
        var offset = 0
        val limit = 1000
        while (true) {
            val page = callWith401Retry { api.getInventoryBatches(limit = limit, offset = offset) }
            result.addAll(page)
            if (page.size < limit) break
            offset += page.size
        }
        return result
    }

    private suspend fun fetchAllExpenses(): List<ExpenseDto> {
        val result = mutableListOf<ExpenseDto>()
        var offset = 0
        val limit = 1000
        while (true) {
            val page = callWith401Retry { api.getExpenses(limit = limit, offset = offset) }
            result.addAll(page)
            if (page.size < limit) break
            offset += page.size
        }
        return result
    }

    private suspend fun fetchAllTasks(): List<DailyTaskDto> {
        val result = mutableListOf<DailyTaskDto>()
        var offset = 0
        val limit = 1000
        while (true) {
            val page = callWith401Retry { api.getDailyTasks(limit = limit, offset = offset) }
            result.addAll(page)
            if (page.size < limit) break
            offset += page.size
        }
        return result
    }

    private suspend fun fetchAllPayments(): List<CustomerPaymentDto> {
        val result = mutableListOf<CustomerPaymentDto>()
        var offset = 0
        val limit = 1000
        while (true) {
            val page = callWith401Retry { api.getCustomerPayments(limit = limit, offset = offset) }
            result.addAll(page)
            if (page.size < limit) break
            offset += page.size
        }
        return result
    }

    // -------------------------------------------------------------
    // Full Sync: Reads & Safe Remote-to-Local Pull
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

                // Check if queue is completely empty after flush
                val remainingQueueCount = queueMutex.withLock { readFileQueue().size }
                if (remainingQueueCount > 0) {
                    val msg = "Kuch changes abhi upload nahi hue, baad mein dobara try karein"
                    _syncStatusMessage.value = msg
                    return@withContext Result.failure(IllegalStateException(msg))
                }

                // 2. Fetch all 6 remote tables with pagination & 401 retry
                val remoteCustomers = fetchAllCustomers()
                val remoteOrders = fetchAllOrders()
                val remoteBatches = fetchAllBatches()
                val remoteExpenses = fetchAllExpenses()
                val remoteTasks = fetchAllTasks()
                val remotePayments = fetchAllPayments()

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
                        localCustomers.chunked(200).forEach { chunk ->
                            api.upsertCustomers(customers = chunk.map { it.toDto() })
                        }
                    }
                    if (localOrders.isNotEmpty()) {
                        localOrders.chunked(200).forEach { chunk ->
                            api.upsertOrders(orders = chunk.map { it.toDto() })
                        }
                    }
                    if (localBatches.isNotEmpty()) {
                        localBatches.chunked(200).forEach { chunk ->
                            api.upsertInventoryBatches(batches = chunk.map { it.toDto() })
                        }
                    }
                    if (localExpenses.isNotEmpty()) {
                        localExpenses.chunked(200).forEach { chunk ->
                            api.upsertExpenses(expenses = chunk.map { it.toDto() })
                        }
                    }
                    if (localTasks.isNotEmpty()) {
                        localTasks.chunked(200).forEach { chunk ->
                            api.upsertDailyTasks(tasks = chunk.map { it.toDto() })
                        }
                    }
                    if (localPayments.isNotEmpty()) {
                        localPayments.chunked(200).forEach { chunk ->
                            api.upsertCustomerPayments(payments = chunk.map { it.toDto() })
                        }
                    }
                } else {
                    // 4. Update local Room from Supabase remote (Supabase is source of truth)
                    // 4a. Upsert remote rows
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

                    // 4b. Apply remote deletes to local: delete local rows missing in remote
                    val remoteCustIds = remoteCustomers.map { it.id }.toSet()
                    localCustomers.filter { it.id !in remoteCustIds }.forEach { dao.deleteCustomerById(it.id) }

                    val remoteOrderIds = remoteOrders.map { it.id }.toSet()
                    localOrders.filter { it.id !in remoteOrderIds }.forEach { dao.deleteOrderById(it.id) }

                    val remoteBatchIds = remoteBatches.map { it.id }.toSet()
                    localBatches.filter { it.id !in remoteBatchIds }.forEach { dao.deleteInventoryBatchById(it.id) }

                    val remoteExpenseIds = remoteExpenses.map { it.id }.toSet()
                    localExpenses.filter { it.id !in remoteExpenseIds }.forEach { dao.deleteExpenseById(it.id) }

                    val remoteTaskIds = remoteTasks.map { it.id }.toSet()
                    localTasks.filter { it.id !in remoteTaskIds }.forEach { dao.deleteDailyTaskById(it.id) }

                    val remotePaymentIds = remotePayments.map { it.id }.toSet()
                    localPayments.filter { it.id !in remotePaymentIds }.forEach { dao.deleteCustomerPaymentById(it.id) }
                }

                // 5. Default tasks creation check: if both local and remote tasks were empty
                if (remoteTasks.isEmpty() && localTasks.isEmpty()) {
                    onDefaultTasksNeeded?.invoke()
                }

                val now = System.currentTimeMillis()
                prefs.edit().putLong(KEY_LAST_SYNCED, now).apply()
                _lastSyncedTime.value = now
                _syncStatusMessage.value = "Sync kamyab raha"

                Result.success("Sync kamyab raha")
            } catch (e: Exception) {
                Log.e("SyncManager", "Sync error", e)
                val msg = "Sync mein masla aaya: ${e.localizedMessage ?: "Network masla"}"
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

    fun syncDeleteBatch(id: String) {
        enqueueAction(
            PendingAction(
                table = "inventory_batches",
                actionType = "DELETE",
                entityId = id
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

    fun syncDeleteExpense(id: String) {
        enqueueAction(
            PendingAction(
                table = "expenses",
                actionType = "DELETE",
                entityId = id
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

    fun syncDeleteCustomerPayment(id: String) {
        enqueueAction(
            PendingAction(
                table = "customer_payments",
                actionType = "DELETE",
                entityId = id
            )
        )
    }

    fun syncClearAll() {
        val tables = listOf("customers", "orders", "customer_payments", "inventory_batches", "expenses", "daily_tasks")
        val actions = tables.map { tbl ->
            PendingAction(
                table = tbl,
                actionType = "DELETE_ALL"
            )
        }
        enqueueAll(actions)
    }

    fun syncRestoreAll(
        customersList: List<Customer>,
        ordersList: List<Order>,
        paymentsList: List<CustomerPayment>,
        batchesList: List<InventoryBatch>,
        expensesList: List<Expense>,
        tasksList: List<DailyTask>
    ) {
        val actions = mutableListOf<PendingAction>()
        val tables = listOf("customers", "orders", "customer_payments", "inventory_batches", "expenses", "daily_tasks")
        for (tbl in tables) {
            actions.add(PendingAction(table = tbl, actionType = "DELETE_ALL"))
        }

        customersList.chunked(200).forEach { chunk ->
            actions.add(
                PendingAction(
                    table = "customers",
                    actionType = "BATCH_UPSERT",
                    jsonPayload = customersListAdapter.toJson(chunk.map { it.toDto() })
                )
            )
        }
        ordersList.chunked(200).forEach { chunk ->
            actions.add(
                PendingAction(
                    table = "orders",
                    actionType = "BATCH_UPSERT",
                    jsonPayload = ordersListAdapter.toJson(chunk.map { it.toDto() })
                )
            )
        }
        paymentsList.chunked(200).forEach { chunk ->
            actions.add(
                PendingAction(
                    table = "customer_payments",
                    actionType = "BATCH_UPSERT",
                    jsonPayload = paymentsListAdapter.toJson(chunk.map { it.toDto() })
                )
            )
        }
        batchesList.chunked(200).forEach { chunk ->
            actions.add(
                PendingAction(
                    table = "inventory_batches",
                    actionType = "BATCH_UPSERT",
                    jsonPayload = batchesListAdapter.toJson(chunk.map { it.toDto() })
                )
            )
        }
        expensesList.chunked(200).forEach { chunk ->
            actions.add(
                PendingAction(
                    table = "expenses",
                    actionType = "BATCH_UPSERT",
                    jsonPayload = expensesListAdapter.toJson(chunk.map { it.toDto() })
                )
            )
        }
        tasksList.chunked(200).forEach { chunk ->
            actions.add(
                PendingAction(
                    table = "daily_tasks",
                    actionType = "BATCH_UPSERT",
                    jsonPayload = tasksListAdapter.toJson(chunk.map { it.toDto() })
                )
            )
        }

        enqueueAll(actions)
    }

    companion object {
        private const val KEY_LAST_SYNCED = "last_synced_timestamp"
    }
}
