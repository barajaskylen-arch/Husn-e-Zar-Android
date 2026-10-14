package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val supabaseAuth = com.example.data.remote.SupabaseAuth(application)
    val syncManager = com.example.data.remote.SyncManager(application, database.appDao(), supabaseAuth)
    private val repository = AppRepository(database.appDao(), syncManager)
    private val prefs = application.getSharedPreferences("husn_e_zar_prefs", Context.MODE_PRIVATE)

    val isLoggedIn: StateFlow<Boolean> = supabaseAuth.isLoggedInFlow
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val lastSyncedTime: StateFlow<Long> = syncManager.lastSyncedTime

    fun syncNow(onResult: ((Result<String>) -> Unit)? = null) {
        viewModelScope.launch {
            val result = syncManager.syncAll()
            onResult?.invoke(result)
        }
    }

    fun logout() {
        supabaseAuth.logout()
    }

    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val orders = repository.orders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventoryBatches = repository.inventoryBatches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val dailyTasks = repository.dailyTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customerPayments = repository.customerPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAvailableStock = combine(inventoryBatches, orders) { batches, ordersList ->
        batches.sumOf { it.jarsProduced } - ordersList.sumOf { it.jarsQuantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentBaseUnitCost = inventoryBatches.map { batches ->
        batches.maxByOrNull { it.date }?.baseCostPerJar ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _streakCount = MutableStateFlow(prefs.getInt("streak_count", 0))
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()

    private val _bestStreak = MutableStateFlow(prefs.getInt("best_streak", 0))
    val bestStreak: StateFlow<Int> = _bestStreak.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        if (!prefs.getBoolean("default_tasks_loaded", false)) {
            viewModelScope.launch {
                val today = dateFormat.format(Date())
                val defaultTasks = listOf(
                    DailyTask(id = UUID.randomUUID().toString(), date = today, taskName = "Social Media Post", isCompleted = false, description = "Social Media"),
                    DailyTask(id = UUID.randomUUID().toString(), date = today, taskName = "Customer Follow-up", isCompleted = false, description = "Customer Follow-up"),
                    DailyTask(id = UUID.randomUUID().toString(), date = today, taskName = "Order Packing", isCompleted = false, description = "Production"),
                    DailyTask(id = UUID.randomUUID().toString(), date = today, taskName = "Stock Check", isCompleted = false, description = "Admin"),
                    DailyTask(id = UUID.randomUUID().toString(), date = today, taskName = "Payment Collection", isCompleted = false, description = "Admin"),
                    DailyTask(id = UUID.randomUUID().toString(), date = today, taskName = "New Customer Outreach", isCompleted = false, description = "Customer Follow-up")
                )
                defaultTasks.forEach { task: DailyTask ->
                    repository.addDailyTask(task)
                }
                prefs.edit().putBoolean("default_tasks_loaded", true).apply()
                updateStreakStatus()
            }
        } else {
            viewModelScope.launch {
                updateStreakStatus()
            }
        }

        viewModelScope.launch {
            dailyTasks.collect { tasks ->
                updateStreakStatus(tasks)
            }
        }

        // Automatic pull on startup and after login
        viewModelScope.launch {
            supabaseAuth.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    syncManager.syncAll()
                }
            }
        }
    }

    private fun updateStreakStatus(taskList: List<DailyTask>? = null) {
        val todayStr = dateFormat.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(cal.time)

        val tasks = taskList ?: dailyTasks.value
        val todayTasks = tasks.filter { it.date == todayStr }
        val yesterdayTasks = tasks.filter { it.date == yesterdayStr }

        val isTodayComplete = todayTasks.isNotEmpty() && todayTasks.all { it.isCompleted }
        val isYesterdayComplete = yesterdayTasks.isNotEmpty() && yesterdayTasks.all { it.isCompleted }

        val lastCompletedDate = prefs.getString("last_completed_date", null)
        var currentStreak = prefs.getInt("streak_count", 0)
        var bestStreakVal = prefs.getInt("best_streak", 0)

        if (isTodayComplete) {
            if (lastCompletedDate != todayStr) {
                if (lastCompletedDate == yesterdayStr || isYesterdayComplete) {
                    currentStreak += 1
                } else {
                    currentStreak = 1
                }
                prefs.edit().putString("last_completed_date", todayStr).apply()
            }
        } else {
            // Check if streak broke from yesterday
            if (lastCompletedDate != null && lastCompletedDate != todayStr && lastCompletedDate != yesterdayStr) {
                currentStreak = 0
            }
        }

        if (currentStreak > bestStreakVal) {
            bestStreakVal = currentStreak
            prefs.edit().putInt("best_streak", bestStreakVal).apply()
        }

        prefs.edit().putInt("streak_count", currentStreak).apply()
        _streakCount.value = currentStreak
        _bestStreak.value = bestStreakVal
    }

    fun addCustomer(name: String, phone: String, address: String, joiningDate: String) {
        viewModelScope.launch {
            val count = customers.value.size + 1
            val id = "C$count-${UUID.randomUUID().toString().take(4)}"
            repository.addCustomer(
                Customer(
                    id = id,
                    name = name,
                    phone = phone,
                    address = address,
                    joiningDate = joiningDate,
                    pendingBalance = 0.0
                )
            )
        }
    }

    fun recordCustomerPayment(customerId: String, amount: Double, date: String, description: String) {
        viewModelScope.launch {
            repository.recordCustomerPayment(
                CustomerPayment(
                    customerId = customerId,
                    amount = amount,
                    date = date,
                    description = description
                )
            )
        }
    }

    fun addExpense(title: String, amount: Double, date: String, category: String, description: String) {
        viewModelScope.launch {
            val count = expenses.value.size + 1
            val id = "E$count-${UUID.randomUUID().toString().take(4)}"
            repository.addExpense(
                Expense(
                    id = id,
                    title = title,
                    amount = amount,
                    date = date,
                    category = category,
                    description = description
                )
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
        }
    }

    fun updateOrder(oldOrder: Order, newOrder: Order) {
        viewModelScope.launch {
            repository.updateOrder(oldOrder, newOrder)
        }
    }

    fun addInventoryBatch(date: String, rawMaterialCost: Double, emptyJarsCount: Int, jarUnitCost: Double, jarsProduced: Int) {
        viewModelScope.launch {
            val totalCost = rawMaterialCost + (emptyJarsCount * jarUnitCost)
            val baseCostPerJar = if (jarsProduced > 0) totalCost / jarsProduced else 0.0
            val count = inventoryBatches.value.size + 1
            val id = "B$count-${UUID.randomUUID().toString().take(4)}"
            repository.addInventoryBatch(
                InventoryBatch(
                    id = id,
                    date = date,
                    rawMaterialCost = rawMaterialCost,
                    emptyJarsCount = emptyJarsCount,
                    jarUnitCost = jarUnitCost,
                    totalCalculatedCost = totalCost,
                    jarsProduced = jarsProduced,
                    baseCostPerJar = baseCostPerJar
                )
            )
        }
    }

    fun addOrder(
        date: String,
        customerId: String,
        customerName: String,
        jarsQuantity: Int,
        totalAmount: Double,
        paidAmount: Double,
        dueDate: String,
        description: String,
        baseCostPerJar: Double,
        onOrderCreated: ((Order) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val netProfit = totalAmount - (jarsQuantity * baseCostPerJar)
            val pendingAmount = totalAmount - paidAmount
            val count = orders.value.size + 1
            val id = "O$count-${UUID.randomUUID().toString().take(4)}"
            val initialHistory = if (paidAmount > 0) {
                "[{\"date\":\"$date\",\"amount\":$paidAmount}]"
            } else "[]"
            
            val newOrder = Order(
                id = id,
                orderDate = date,
                customerId = customerId,
                customerName = customerName,
                jarsQuantity = jarsQuantity,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                pendingAmount = pendingAmount,
                dueDate = dueDate,
                baseCostPriceAtTime = baseCostPerJar,
                netProfit = netProfit,
                description = description,
                paymentHistory = initialHistory
            )
            repository.addOrder(newOrder)
            onOrderCreated?.invoke(newOrder)
        }
    }

    fun addOrderPayment(
        order: Order,
        amount: Double,
        date: String,
        description: String,
        onPaymentRecorded: ((Order, List<PaymentInfo>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val newPaid = order.paidAmount + amount
            val newPending = maxOf(0.0, order.totalAmount - newPaid)
            val history = repository.parsePaymentHistory(order.paymentHistory).toMutableList()
            history.add(PaymentInfo(date = date, amount = amount))
            
            val gson = com.google.gson.Gson()
            val updatedHistoryJson = gson.toJson(history)
            
            val updatedOrder = order.copy(
                paidAmount = newPaid,
                pendingAmount = newPending,
                paymentHistory = updatedHistoryJson
            )
            repository.updateOrder(order, updatedOrder)
            
            repository.recordCustomerPayment(
                CustomerPayment(
                    customerId = order.customerId,
                    amount = amount,
                    date = date,
                    description = description
                ),
                updateCustomerBalance = false // Order update balances customer debt
            )
            onPaymentRecorded?.invoke(updatedOrder, history)
        }
    }

    fun parsePaymentHistory(json: String): List<PaymentInfo> {
        return repository.parsePaymentHistory(json)
    }

    fun updateDailyTaskCompleted(task: DailyTask, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateDailyTask(task.copy(isCompleted = isCompleted))
        }
    }

    fun deleteDailyTask(task: DailyTask) {
        viewModelScope.launch {
            repository.deleteDailyTask(task)
        }
    }

    fun addDailyTask(taskName: String, date: String, description: String = "") {
        viewModelScope.launch {
            val count = dailyTasks.value.size + 1
            val id = "T$count-${UUID.randomUUID().toString().take(4)}"
            repository.addDailyTask(
                DailyTask(
                    id = id,
                    date = date,
                    taskName = taskName,
                    isCompleted = false,
                    description = description
                )
            )
        }
    }

    suspend fun getAllCustomersOnce(): List<Customer> = repository.getAllCustomersOnce()
    suspend fun getAllOrdersOnce(): List<Order> = repository.getAllOrdersOnce()
    suspend fun getAllPaymentsOnce(): List<CustomerPayment> = repository.getAllPaymentsOnce()
    suspend fun getAllBatchesOnce(): List<InventoryBatch> = repository.getAllBatchesOnce()
    suspend fun getAllExpensesOnce(): List<Expense> = repository.getAllExpensesOnce()
    suspend fun getAllTasksOnce(): List<DailyTask> = repository.getAllTasksOnce()

    suspend fun clearAllTables() {
        repository.clearAllTables()
    }

    suspend fun restoreAllData(
        customersList: List<Customer>,
        ordersList: List<Order>,
        paymentsList: List<CustomerPayment>,
        batchesList: List<InventoryBatch>,
        expensesList: List<Expense>,
        tasksList: List<DailyTask>
    ) {
        repository.restoreAllData(
            customersList,
            ordersList,
            paymentsList,
            batchesList,
            expensesList,
            tasksList
        )
    }
}
