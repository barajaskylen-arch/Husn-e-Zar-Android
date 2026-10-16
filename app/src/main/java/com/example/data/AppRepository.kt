package com.example.data

import androidx.room.withTransaction
import com.example.data.remote.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class AppRepository(
    private val dao: AppDao,
    private val syncManager: SyncManager? = null,
    private val database: AppDatabase? = null
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

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

    suspend fun deleteCustomer(id: String) {
        dao.deleteCustomerById(id)
        syncManager?.syncDeleteCustomer(id)
    }

    suspend fun deleteCustomerCascade(customerId: String) = runInTransaction {
        // Delete all orders of this customer
        val customerOrders = dao.getAllOrdersOnce().filter { it.customerId == customerId }
        for (order in customerOrders) {
            dao.deleteOrderById(order.id)
            syncManager?.syncDeleteOrder(order.id)
        }

        // Delete all payments of this customer
        val customerPayments = dao.getAllPaymentsOnce().filter { it.customerId == customerId }
        for (payment in customerPayments) {
            dao.deleteCustomerPaymentById(payment.id)
            syncManager?.syncDeleteCustomerPayment(payment.id)
        }

        // Finally delete the customer
        dao.deleteCustomerById(customerId)
        syncManager?.syncDeleteCustomer(customerId)
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

    suspend fun deleteOrder(order: Order) = runInTransaction {
        // 1. For each entry in order.paymentHistory, find matching CustomerPayment (same customerId + same date + same amount)
        val history = parsePaymentHistory(order.paymentHistory)
        if (history.isNotEmpty()) {
            val allPayments = dao.getAllPaymentsOnce().filter { it.customerId == order.customerId }.toMutableList()
            for (item in history) {
                val matchIndex = allPayments.indexOfFirst {
                    it.date == item.date && kotlin.math.abs(it.amount - item.amount) < 0.001
                }
                if (matchIndex != -1) {
                    val matchedPayment = allPayments.removeAt(matchIndex)
                    dao.deleteCustomerPaymentById(matchedPayment.id)
                    syncManager?.syncDeleteCustomerPayment(matchedPayment.id)
                }
            }
        }

        // 2. Delete the order
        dao.deleteOrderById(order.id)
        syncManager?.syncDeleteOrder(order.id)

        // 3. Customer's pendingBalance = max(0, pendingBalance - order.pendingAmount)
        val customer = dao.getCustomerById(order.customerId)
        if (customer != null) {
            val newBalance = maxOf(0.0, customer.pendingBalance - order.pendingAmount)
            val updated = customer.copy(pendingBalance = newBalance)
            dao.updateCustomer(updated)
            syncManager?.syncUpsertCustomer(updated)
        }
    }

    suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense)
        syncManager?.syncUpsertExpense(expense)
    }

    suspend fun deleteExpense(id: String) {
        dao.deleteExpenseById(id)
        syncManager?.syncDeleteExpense(id)
    }

    suspend fun updateInventoryBatch(batch: InventoryBatch) {
        val totalCost = batch.rawMaterialCost + (batch.emptyJarsCount * batch.jarUnitCost)
        val unitCost = if (batch.jarsProduced > 0) totalCost / batch.jarsProduced else 0.0
        val computedBatch = batch.copy(
            totalCalculatedCost = totalCost,
            baseCostPerJar = unitCost
        )
        dao.updateInventoryBatch(computedBatch)
        syncManager?.syncUpsertBatch(computedBatch)
    }

    suspend fun deleteInventoryBatch(id: String) {
        dao.deleteInventoryBatchById(id)
        syncManager?.syncDeleteBatch(id)
    }

    suspend fun updateCustomerPayment(oldPayment: CustomerPayment, newPayment: CustomerPayment) = runInTransaction {
        dao.updateCustomerPayment(newPayment)
        syncManager?.syncUpsertCustomerPayment(newPayment)

        val orders = dao.getAllOrdersOnce().filter { it.customerId == oldPayment.customerId }
        var matchedOrder: Order? = null
        var matchedHistoryIndex = -1
        var matchedHistoryList: MutableList<PaymentInfo>? = null

        for (ord in orders) {
            val history = parsePaymentHistory(ord.paymentHistory).toMutableList()
            val idx = history.indexOfFirst {
                it.date == oldPayment.date && kotlin.math.abs(it.amount - oldPayment.amount) < 0.001
            }
            if (idx != -1) {
                matchedOrder = ord
                matchedHistoryIndex = idx
                matchedHistoryList = history
                break
            }
        }

        if (matchedOrder != null && matchedHistoryList != null && matchedHistoryIndex != -1) {
            matchedHistoryList[matchedHistoryIndex] = PaymentInfo(
                date = newPayment.date,
                amount = newPayment.amount,
                description = newPayment.description
            )
            val paidDiff = newPayment.amount - oldPayment.amount
            val newPaid = maxOf(0.0, matchedOrder.paidAmount + paidDiff)
            val newPending = maxOf(0.0, matchedOrder.totalAmount - newPaid)
            val updatedOrder = matchedOrder.copy(
                paidAmount = newPaid,
                pendingAmount = newPending,
                paymentHistory = paymentAdapter.toJson(matchedHistoryList)
            )
            updateOrder(matchedOrder, updatedOrder)
        } else {
            val customer = dao.getCustomerById(newPayment.customerId)
            if (customer != null) {
                val newBal = maxOf(0.0, customer.pendingBalance + oldPayment.amount - newPayment.amount)
                val updatedCust = customer.copy(pendingBalance = newBal)
                dao.updateCustomer(updatedCust)
                syncManager?.syncUpsertCustomer(updatedCust)
            }
        }
    }

    suspend fun deleteCustomerPayment(payment: CustomerPayment) = runInTransaction {
        dao.deleteCustomerPaymentById(payment.id)
        syncManager?.syncDeleteCustomerPayment(payment.id)

        val orders = dao.getAllOrdersOnce().filter { it.customerId == payment.customerId }
        var matchedOrder: Order? = null
        var matchedHistoryIndex = -1
        var matchedHistoryList: MutableList<PaymentInfo>? = null

        for (ord in orders) {
            val history = parsePaymentHistory(ord.paymentHistory).toMutableList()
            val idx = history.indexOfFirst {
                it.date == payment.date && kotlin.math.abs(it.amount - payment.amount) < 0.001
            }
            if (idx != -1) {
                matchedOrder = ord
                matchedHistoryIndex = idx
                matchedHistoryList = history
                break
            }
        }

        if (matchedOrder != null && matchedHistoryList != null && matchedHistoryIndex != -1) {
            matchedHistoryList.removeAt(matchedHistoryIndex)
            val newPaid = maxOf(0.0, matchedOrder.paidAmount - payment.amount)
            val newPending = maxOf(0.0, matchedOrder.totalAmount - newPaid)
            val updatedOrder = matchedOrder.copy(
                paidAmount = newPaid,
                pendingAmount = newPending,
                paymentHistory = paymentAdapter.toJson(matchedHistoryList)
            )
            updateOrder(matchedOrder, updatedOrder)
        } else {
            val customer = dao.getCustomerById(payment.customerId)
            if (customer != null) {
                val newBal = customer.pendingBalance + payment.amount
                val updatedCust = customer.copy(pendingBalance = newBal)
                dao.updateCustomer(updatedCust)
                syncManager?.syncUpsertCustomer(updatedCust)
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
