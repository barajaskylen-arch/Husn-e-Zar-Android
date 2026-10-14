package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val joiningDate: String,
    val pendingBalance: Double
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "date")
    @SerializedName("date")
    val orderDate: String,
    val customerId: String,
    val customerName: String,
    val jarsQuantity: Int,
    val totalAmount: Double,
    val paidAmount: Double,
    val pendingAmount: Double,
    val dueDate: String,
    val baseCostPriceAtTime: Double,
    val netProfit: Double,
    val description: String,
    val paymentHistory: String // JSON string of List<PaymentInfo>
)

data class PaymentInfo(
    val date: String,
    val amount: Double
)

@Entity(tableName = "inventory_batches")
data class InventoryBatch(
    @PrimaryKey val id: String,
    val date: String,
    val rawMaterialCost: Double,
    val emptyJarsCount: Int,
    val jarUnitCost: Double,
    val totalCalculatedCost: Double,
    val jarsProduced: Int,
    val baseCostPerJar: Double
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val category: String, // 'Raw Material' | 'Marketing' | 'Logistics' | 'Others'
    val description: String
)

@Entity(tableName = "daily_tasks")
data class DailyTask(
    @PrimaryKey val id: String,
    val date: String, // ISO date string yyyy-MM-dd
    val taskName: String,
    val isCompleted: Boolean,
    val description: String
)

@Entity(tableName = "customer_payments")
data class CustomerPayment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val amount: Double,
    val date: String,
    val description: String
)
