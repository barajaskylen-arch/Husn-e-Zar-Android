package com.example.data

import com.example.data.remote.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class AppRepository(
    private val dao: AppDao,
    private val syncManager: SyncManager? = null
) {
    val customers: Flow<List<Customer>> = dao.getAllCustomers()
    val orders: Flow<List<Order>> = dao.getAllOrders()
    val inventoryBatches: Flow<List<InventoryBatch>> = dao.getAllInventoryBatches()
    val expenses: Flow<List<Expense>> = dao.getAllExpenses()
    val dailyTasks: Flow<List<DailyTask>> = dao.getAllDailyTasks()
    val customerPayments: Flow<List<CustomerPayment>> = dao.getAllCustomerPayments()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val paymentListType = Types.newParameterizedType(List::class.java, PaymentInfo::class.java)
    private val paymentAdapter = moshi.adapter<List<PaymentInfo>>(paymentListType)

    suspend fun getCustomerById(id: String): Customer? = dao.getCustomerById(id)

    suspend fun addCustomer(customer: Customer) {
        dao.insertCustomer(customer)
        syncManager?.syncUpsertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) {
        dao.updateCustomer(customer)
        syncManager?.syncUpsertCustomer(customer)
    }

    suspend fun deleteCustomer(id: String) {
        dao.deleteCustomerById(id)
        syncManager?.syncDeleteCustomer(id)
    }

    suspend fun updateOrder(oldOrder: Order, newOrder: Order) {
        dao.updateOrder(newOrder)
        syncManager?.syncUpsertOrder(newOrder)
        
        // Correct the customer balance
        val diff = newOrder.pendingAmount - oldOrder.pendingAmount
        if (diff != 0.0) {
            val customer = dao.getCustomerById(newOrder.customerId)
            if (customer != null) {
                val updated = customer.copy(pendingBalance = customer.pendingBalance + diff)
                dao.updateCustomer(updated)
                syncManager?.syncUpsertCustomer(updated)
            }
        }
    }

    suspend fun addInventoryBatch(batch: InventoryBatch) {
        dao.insertInventoryBatch(batch)
        syncManager?.syncUpsertBatch(batch)
    }

    suspend fun getLatestInventoryBatch(): InventoryBatch? {
        return dao.getAllInventoryBatches().firstOrNull()?.firstOrNull()
    }

    suspend fun addOrder(order: Order) {
        dao.insertOrder(order)
        syncManager?.syncUpsertOrder(order)
        val customer = dao.getCustomerById(order.customerId)
        if (customer != null) {
            val updated = customer.copy(pendingBalance = customer.pendingBalance + order.pendingAmount)
            dao.updateCustomer(updated)
            syncManager?.syncUpsertCustomer(updated)
        }
    }

    suspend fun recordCustomerPayment(payment: CustomerPayment, updateCustomerBalance: Boolean = true) {
        dao.insertCustomerPayment(payment)
        syncManager?.syncUpsertCustomerPayment(payment)
        
        if (updateCustomerBalance) {
            val customer = dao.getCustomerById(payment.customerId)
            if (customer != null) {
                val updated = customer.copy(
                    pendingBalance = maxOf(0.0, customer.pendingBalance - payment.amount)
                )
                dao.updateCustomer(updated)
                syncManager?.syncUpsertCustomer(updated)
            }
        }
    }

    suspend fun addExpense(expense: Expense) {
        dao.insertExpense(expense)
        syncManager?.syncUpsertExpense(expense)
    }

    suspend fun addDailyTask(task: DailyTask) {
        dao.insertDailyTask(task)
        syncManager?.syncUpsertDailyTask(task)
    }

    suspend fun updateDailyTask(task: DailyTask) {
        dao.updateDailyTask(task)
        syncManager?.syncUpsertDailyTask(task)
    }

    suspend fun deleteDailyTask(task: DailyTask) {
        dao.deleteDailyTask(task)
        syncManager?.syncDeleteDailyTask(task.id)
    }

    suspend fun deleteDailyTaskById(id: String) {
        dao.deleteDailyTaskById(id)
        syncManager?.syncDeleteDailyTask(id)
    }

    suspend fun getAllCustomersOnce(): List<Customer> = dao.getAllCustomersOnce()
    suspend fun getAllOrdersOnce(): List<Order> = dao.getAllOrdersOnce()
    suspend fun getAllPaymentsOnce(): List<CustomerPayment> = dao.getAllPaymentsOnce()
    suspend fun getAllBatchesOnce(): List<InventoryBatch> = dao.getAllBatchesOnce()
    suspend fun getAllExpensesOnce(): List<Expense> = dao.getAllExpensesOnce()
    suspend fun getAllTasksOnce(): List<DailyTask> = dao.getAllTasksOnce()

    suspend fun clearAllTables() {
        dao.deleteAllCustomers()
        dao.deleteAllOrders()
        dao.deleteAllCustomerPayments()
        dao.deleteAllInventoryBatches()
        dao.deleteAllExpenses()
        dao.deleteAllDailyTasks()
        syncManager?.syncClearAll()
    }

    suspend fun restoreAllData(
        customersList: List<Customer>,
        ordersList: List<Order>,
        paymentsList: List<CustomerPayment>,
        batchesList: List<InventoryBatch>,
        expensesList: List<Expense>,
        tasksList: List<DailyTask>
    ) {
        clearAllTables()
        if (customersList.isNotEmpty()) dao.insertCustomers(customersList)
        if (ordersList.isNotEmpty()) dao.insertOrders(ordersList)
        if (paymentsList.isNotEmpty()) dao.insertCustomerPayments(paymentsList)
        if (batchesList.isNotEmpty()) dao.insertInventoryBatches(batchesList)
        if (expensesList.isNotEmpty()) dao.insertExpenses(expensesList)
        if (tasksList.isNotEmpty()) dao.insertDailyTasks(tasksList)

        syncManager?.syncRestoreAll(
            customersList = customersList,
            ordersList = ordersList,
            paymentsList = paymentsList,
            batchesList = batchesList,
            expensesList = expensesList,
            tasksList = tasksList
        )
    }

    fun parsePaymentHistory(json: String): List<PaymentInfo> {
        return try {
            paymentAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
