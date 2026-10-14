package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class AppRepository(private val dao: AppDao) {
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
    }

    suspend fun updateCustomer(customer: Customer) {
        dao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(id: String) {
        dao.deleteCustomerById(id)
    }

    suspend fun updateOrder(oldOrder: Order, newOrder: Order) {
        dao.updateOrder(newOrder)
        
        // Correct the customer balance
        val diff = newOrder.pendingAmount - oldOrder.pendingAmount
        if (diff != 0.0) {
            val customer = dao.getCustomerById(newOrder.customerId)
            if (customer != null) {
                val updated = customer.copy(pendingBalance = customer.pendingBalance + diff)
                dao.updateCustomer(updated)
            }
        }
    }

    suspend fun addInventoryBatch(batch: InventoryBatch) {
        dao.insertInventoryBatch(batch)
    }

    suspend fun getLatestInventoryBatch(): InventoryBatch? {
        // Return latest
        return dao.getAllInventoryBatches().firstOrNull()?.firstOrNull()
    }

    suspend fun addOrder(order: Order) {
        // Also update customer pending balance?
        dao.insertOrder(order)
        val customer = dao.getCustomerById(order.customerId)
        if (customer != null) {
            val updated = customer.copy(pendingBalance = customer.pendingBalance + order.pendingAmount)
            dao.updateCustomer(updated)
        }
    }

    suspend fun recordCustomerPayment(payment: CustomerPayment, updateCustomerBalance: Boolean = true) {
        dao.insertCustomerPayment(payment)
        // Update customer balance
        if (updateCustomerBalance) {
            val customer = dao.getCustomerById(payment.customerId)
            if (customer != null) {
                val updated = customer.copy(
                    pendingBalance = maxOf(0.0, customer.pendingBalance - payment.amount)
                )
                dao.updateCustomer(updated)
            }
        }
        
        // Let's append this to their open orders if required by schema, 
        // to simplify, we just update the pending balance globally as requested by spec:
        // "saving this entry must reduce that specific customer's pendingBalance across the app and record it into the financial registry"
    }

    suspend fun addExpense(expense: Expense) {
        dao.insertExpense(expense)
    }

    suspend fun addDailyTask(task: DailyTask) {
        dao.insertDailyTask(task)
    }

    suspend fun updateDailyTask(task: DailyTask) {
        dao.updateDailyTask(task)
    }

    suspend fun deleteDailyTask(task: DailyTask) {
        dao.deleteDailyTask(task)
    }

    suspend fun deleteDailyTaskById(id: String) {
        dao.deleteDailyTaskById(id)
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
    }

    fun parsePaymentHistory(json: String): List<PaymentInfo> {
        return try {
            paymentAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
