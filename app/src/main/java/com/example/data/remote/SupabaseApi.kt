package com.example.data.remote

import com.example.data.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// -------------------------------------------------------------
// DTOs for Supabase Tables (Moshi mapped to snake_case columns)
// -------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class CustomerDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "address") val address: String,
    @Json(name = "joining_date") val joiningDate: String,
    @Json(name = "pending_balance") val pendingBalance: Double
) {
    fun toEntity(): Customer = Customer(
        id = id,
        name = name,
        phone = phone,
        address = address,
        joiningDate = joiningDate,
        pendingBalance = pendingBalance
    )
}

fun Customer.toDto(): CustomerDto = CustomerDto(
    id = id,
    name = name,
    phone = phone,
    address = address,
    joiningDate = joiningDate,
    pendingBalance = pendingBalance
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    @Json(name = "id") val id: String,
    @Json(name = "order_date") val orderDate: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "jars_quantity") val jarsQuantity: Int,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "paid_amount") val paidAmount: Double,
    @Json(name = "pending_amount") val pendingAmount: Double,
    @Json(name = "due_date") val dueDate: String,
    @Json(name = "base_cost_price_at_time") val baseCostPriceAtTime: Double,
    @Json(name = "net_profit") val netProfit: Double,
    @Json(name = "description") val description: String,
    @Json(name = "payment_history") val paymentHistory: String
) {
    fun toEntity(): Order = Order(
        id = id,
        orderDate = orderDate,
        customerId = customerId,
        customerName = customerName,
        jarsQuantity = jarsQuantity,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        pendingAmount = pendingAmount,
        dueDate = dueDate,
        baseCostPriceAtTime = baseCostPriceAtTime,
        netProfit = netProfit,
        description = description,
        paymentHistory = paymentHistory
    )
}

fun Order.toDto(): OrderDto = OrderDto(
    id = id,
    orderDate = orderDate,
    customerId = customerId,
    customerName = customerName,
    jarsQuantity = jarsQuantity,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    pendingAmount = pendingAmount,
    dueDate = dueDate,
    baseCostPriceAtTime = baseCostPriceAtTime,
    netProfit = netProfit,
    description = description,
    paymentHistory = paymentHistory
)

@JsonClass(generateAdapter = true)
data class InventoryBatchDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "raw_material_cost") val rawMaterialCost: Double,
    @Json(name = "empty_jars_count") val emptyJarsCount: Int,
    @Json(name = "jar_unit_cost") val jarUnitCost: Double,
    @Json(name = "total_calculated_cost") val totalCalculatedCost: Double,
    @Json(name = "jars_produced") val jarsProduced: Int,
    @Json(name = "base_cost_per_jar") val baseCostPerJar: Double
) {
    fun toEntity(): InventoryBatch = InventoryBatch(
        id = id,
        date = date,
        rawMaterialCost = rawMaterialCost,
        emptyJarsCount = emptyJarsCount,
        jarUnitCost = jarUnitCost,
        totalCalculatedCost = totalCalculatedCost,
        jarsProduced = jarsProduced,
        baseCostPerJar = baseCostPerJar
    )
}

fun InventoryBatch.toDto(): InventoryBatchDto = InventoryBatchDto(
    id = id,
    date = date,
    rawMaterialCost = rawMaterialCost,
    emptyJarsCount = emptyJarsCount,
    jarUnitCost = jarUnitCost,
    totalCalculatedCost = totalCalculatedCost,
    jarsProduced = jarsProduced,
    baseCostPerJar = baseCostPerJar
)

@JsonClass(generateAdapter = true)
data class ExpenseDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String
) {
    fun toEntity(): Expense = Expense(
        id = id,
        title = title,
        amount = amount,
        date = date,
        category = category,
        description = description
    )
}

fun Expense.toDto(): ExpenseDto = ExpenseDto(
    id = id,
    title = title,
    amount = amount,
    date = date,
    category = category,
    description = description
)

@JsonClass(generateAdapter = true)
data class DailyTaskDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "task_name") val taskName: String,
    @Json(name = "is_completed") val isCompleted: Boolean,
    @Json(name = "description") val description: String
) {
    fun toEntity(): DailyTask = DailyTask(
        id = id,
        date = date,
        taskName = taskName,
        isCompleted = isCompleted,
        description = description
    )
}

fun DailyTask.toDto(): DailyTaskDto = DailyTaskDto(
    id = id,
    date = date,
    taskName = taskName,
    isCompleted = isCompleted,
    description = description
)

@JsonClass(generateAdapter = true)
data class CustomerPaymentDto(
    @Json(name = "id") val id: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String,
    @Json(name = "description") val description: String
) {
    fun toEntity(): CustomerPayment = CustomerPayment(
        id = id,
        customerId = customerId,
        amount = amount,
        date = date,
        description = description
    )
}

fun CustomerPayment.toDto(): CustomerPaymentDto = CustomerPaymentDto(
    id = id,
    customerId = customerId,
    amount = amount,
    date = date,
    description = description
)

// -------------------------------------------------------------
// Auth DTOs (GoTrue)
// -------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class TokenPasswordRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class TokenRefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class AuthTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null
)

// -------------------------------------------------------------
// Supabase Retrofit Interface
// -------------------------------------------------------------

interface SupabaseApi {

    // Auth
    @POST("auth/v1/token")
    suspend fun loginWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body body: TokenPasswordRequest
    ): AuthTokenResponse

    @POST("auth/v1/token")
    suspend fun refreshToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: TokenRefreshRequest
    ): AuthTokenResponse

    // Customers
    @GET("rest/v1/customers")
    suspend fun getCustomers(
        @Query("select") select: String = "*"
    ): List<CustomerDto>

    @POST("rest/v1/customers")
    suspend fun upsertCustomers(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body customers: List<CustomerDto>
    ): Response<ResponseBody>

    @DELETE("rest/v1/customers")
    suspend fun deleteCustomerById(
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @DELETE("rest/v1/customers")
    suspend fun deleteAllCustomers(
        @Query("id") idFilter: String = "not.is.null"
    ): Response<ResponseBody>

    // Orders
    @GET("rest/v1/orders")
    suspend fun getOrders(
        @Query("select") select: String = "*"
    ): List<OrderDto>

    @POST("rest/v1/orders")
    suspend fun upsertOrders(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body orders: List<OrderDto>
    ): Response<ResponseBody>

    @DELETE("rest/v1/orders")
    suspend fun deleteOrderById(
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @DELETE("rest/v1/orders")
    suspend fun deleteAllOrders(
        @Query("id") idFilter: String = "not.is.null"
    ): Response<ResponseBody>

    // Inventory Batches
    @GET("rest/v1/inventory_batches")
    suspend fun getInventoryBatches(
        @Query("select") select: String = "*"
    ): List<InventoryBatchDto>

    @POST("rest/v1/inventory_batches")
    suspend fun upsertInventoryBatches(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body batches: List<InventoryBatchDto>
    ): Response<ResponseBody>

    @DELETE("rest/v1/inventory_batches")
    suspend fun deleteInventoryBatchById(
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @DELETE("rest/v1/inventory_batches")
    suspend fun deleteAllInventoryBatches(
        @Query("id") idFilter: String = "not.is.null"
    ): Response<ResponseBody>

    // Expenses
    @GET("rest/v1/expenses")
    suspend fun getExpenses(
        @Query("select") select: String = "*"
    ): List<ExpenseDto>

    @POST("rest/v1/expenses")
    suspend fun upsertExpenses(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body expenses: List<ExpenseDto>
    ): Response<ResponseBody>

    @DELETE("rest/v1/expenses")
    suspend fun deleteExpenseById(
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @DELETE("rest/v1/expenses")
    suspend fun deleteAllExpenses(
        @Query("id") idFilter: String = "not.is.null"
    ): Response<ResponseBody>

    // Daily Tasks
    @GET("rest/v1/daily_tasks")
    suspend fun getDailyTasks(
        @Query("select") select: String = "*"
    ): List<DailyTaskDto>

    @POST("rest/v1/daily_tasks")
    suspend fun upsertDailyTasks(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body tasks: List<DailyTaskDto>
    ): Response<ResponseBody>

    @DELETE("rest/v1/daily_tasks")
    suspend fun deleteDailyTaskById(
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @DELETE("rest/v1/daily_tasks")
    suspend fun deleteAllDailyTasks(
        @Query("id") idFilter: String = "not.is.null"
    ): Response<ResponseBody>

    // Customer Payments
    @GET("rest/v1/customer_payments")
    suspend fun getCustomerPayments(
        @Query("select") select: String = "*"
    ): List<CustomerPaymentDto>

    @POST("rest/v1/customer_payments")
    suspend fun upsertCustomerPayments(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body payments: List<CustomerPaymentDto>
    ): Response<ResponseBody>

    @DELETE("rest/v1/customer_payments")
    suspend fun deleteCustomerPaymentById(
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @DELETE("rest/v1/customer_payments")
    suspend fun deleteAllCustomerPayments(
        @Query("id") idFilter: String = "not.is.null"
    ): Response<ResponseBody>
}
