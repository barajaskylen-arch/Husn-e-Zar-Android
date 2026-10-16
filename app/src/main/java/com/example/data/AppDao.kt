package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Customer
    @Query("SELECT * FROM customers ORDER BY joiningDate DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    // Order
    @Query("SELECT * FROM orders ORDER BY date DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE date BETWEEN :startDate AND :endDate")
    fun getOrdersBetween(startDate: String, endDate: String): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    // InventoryBatch
    @Query("SELECT * FROM inventory_batches ORDER BY date DESC")
    fun getAllInventoryBatches(): Flow<List<InventoryBatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryBatch(batch: InventoryBatch)

    // Expense
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    // DailyTask
    @Query("SELECT * FROM daily_tasks ORDER BY date DESC")
    fun getAllDailyTasks(): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks WHERE date = :date")
    fun getDailyTasksForDate(date: String): Flow<List<DailyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTask(task: DailyTask)

    @Update
    suspend fun updateDailyTask(task: DailyTask)

    @Delete
    suspend fun deleteDailyTask(task: DailyTask)

    @Query("DELETE FROM daily_tasks WHERE id = :id")
    suspend fun deleteDailyTaskById(id: String)

    // Customer Payment
    @Query("SELECT * FROM customer_payments ORDER BY date DESC")
    fun getAllCustomerPayments(): Flow<List<CustomerPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerPayment(payment: CustomerPayment)

    @Update
    suspend fun updateOrder(order: Order)

    @Update
    suspend fun updateInventoryBatch(batch: InventoryBatch)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Update
    suspend fun updateCustomerPayment(payment: CustomerPayment)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderById(id: String)

    @Query("DELETE FROM customer_payments WHERE id = :id")
    suspend fun deleteCustomerPaymentById(id: String)

    @Query("DELETE FROM inventory_batches WHERE id = :id")
    suspend fun deleteInventoryBatchById(id: String)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    // Helper queries
    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): Customer?

    // Single-shot list queries for backup & export
    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersOnce(): List<Customer>

    @Query("SELECT * FROM orders")
    suspend fun getAllOrdersOnce(): List<Order>

    @Query("SELECT * FROM customer_payments")
    suspend fun getAllPaymentsOnce(): List<CustomerPayment>

    @Query("SELECT * FROM inventory_batches")
    suspend fun getAllBatchesOnce(): List<InventoryBatch>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesOnce(): List<Expense>

    @Query("SELECT * FROM daily_tasks")
    suspend fun getAllTasksOnce(): List<DailyTask>

    // Batch insertion for import & restore
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<Order>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerPayments(payments: List<CustomerPayment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryBatches(batches: List<InventoryBatch>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTasks(tasks: List<DailyTask>)

    // Delete all table queries
    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()

    @Query("DELETE FROM customer_payments")
    suspend fun deleteAllCustomerPayments()

    @Query("DELETE FROM inventory_batches")
    suspend fun deleteAllInventoryBatches()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM daily_tasks")
    suspend fun deleteAllDailyTasks()
}
