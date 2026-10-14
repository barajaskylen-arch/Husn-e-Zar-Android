package com.example

import com.example.data.*
import com.example.data.remote.*
import com.example.ui.BackupPayload
import com.example.ui.formatMoney
import com.example.ui.generateOrderReceipt
import com.example.ui.generatePaymentReceipt
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGenerateOrderReceipt_containsAllRequiredFieldsAndNoNetProfit() {
        val order = Order(
            id = "O1-test",
            orderDate = "2026-06-08",
            customerId = "C1",
            customerName = "Ayesha Khan",
            jarsQuantity = 5,
            totalAmount = 7500.0,
            paidAmount = 3000.0,
            pendingAmount = 4500.0,
            dueDate = "2026-06-20",
            baseCostPriceAtTime = 600.0,
            netProfit = 4500.0, // Secret profit
            description = "Wedding batch",
            paymentHistory = "[]"
        )
        val customer = Customer(
            id = "C1",
            name = "Ayesha Khan",
            phone = "0300-1234567",
            address = "Lahore",
            joiningDate = "2026-06-01",
            pendingBalance = 4500.0
        )

        val receipt = generateOrderReceipt(order, customer)

        assertTrue(receipt.contains("✨ Husn e Zar Beauty Cream ✨"))
        assertTrue(receipt.contains("Order ID  : O1-test"))
        assertTrue(receipt.contains("Date      : 2026-06-08"))
        assertTrue(receipt.contains("Customer  : Ayesha Khan"))
        assertTrue(receipt.contains("Phone     : 0300-1234567"))
        assertTrue(receipt.contains("Jars Qty  : 5"))
        assertTrue(receipt.contains("Total Bill: ${formatMoney(order.totalAmount)}"))
        assertTrue(receipt.contains("Paid Now  : ${formatMoney(order.paidAmount)}"))
        assertTrue(receipt.contains("Balance   : ${formatMoney(order.pendingAmount)}"))
        assertTrue(receipt.contains("Due Date  : 2026-06-20"))
        assertTrue(receipt.contains("Shukriya! Husn e Zar 🌿"))

        // Strictly verify that netProfit is NEVER included in customer receipts
        assertFalse(receipt.contains("netProfit"))
        assertFalse(receipt.contains("Profit"))
        assertFalse(receipt.contains("baseCostPriceAtTime"))
    }

    @Test
    fun testGeneratePaymentReceipt_formatsHistoryCorrectly() {
        val order = Order(
            id = "O2-test",
            orderDate = "2026-06-01",
            customerId = "C2",
            customerName = "Fatima Ali",
            jarsQuantity = 2,
            totalAmount = 3000.0,
            paidAmount = 2500.0,
            pendingAmount = 500.0,
            dueDate = "2026-06-15",
            baseCostPriceAtTime = 600.0,
            netProfit = 1800.0,
            description = "",
            paymentHistory = "[]"
        )
        val history = listOf(
            PaymentInfo(date = "2026-06-01", amount = 1000.0),
            PaymentInfo(date = "2026-06-08", amount = 1500.0)
        )

        val paymentReceipt = generatePaymentReceipt(order, "Fatima Ali", history)

        assertTrue(paymentReceipt.contains("✨ Husn e Zar Beauty Cream ✨"))
        assertTrue(paymentReceipt.contains("Payment Receipt"))
        assertTrue(paymentReceipt.contains("Order ID  : O2-test"))
        assertTrue(paymentReceipt.contains("Customer  : Fatima Ali"))
        assertTrue(paymentReceipt.contains("Total Bill: ${formatMoney(order.totalAmount)}"))
        assertTrue(paymentReceipt.contains("1. 2026-06-01 — ${formatMoney(1000.0)}"))
        assertTrue(paymentReceipt.contains("2. 2026-06-08 — ${formatMoney(1500.0)} (Aaj)"))
        assertTrue(paymentReceipt.contains("Total Paid: ${formatMoney(order.paidAmount)}"))
        assertTrue(paymentReceipt.contains("Remaining : ${formatMoney(order.pendingAmount)}"))
        assertTrue(paymentReceipt.contains("Shukriya! Husn e Zar 🌿"))
    }

    @Test
    fun testBackupJsonSerializationAndValidation() {
        val backup = BackupPayload(
            version = "1.0",
            exportDate = "2026-06-08",
            customers = listOf(
                Customer("C1", "Ayesha", "0300-1111111", "Karachi", "2026-01-01", 0.0)
            ),
            orders = listOf(
                Order("O1", "2026-06-08", "C1", "Ayesha", 3, 4500.0, 4500.0, 0.0, "2026-06-08", 600.0, 2700.0, "", "[]")
            ),
            payments = listOf(
                CustomerPayment(customerId = "C1", amount = 4500.0, date = "2026-06-08", description = "Full payment")
            ),
            inventoryBatches = listOf(
                InventoryBatch("B1", "2026-06-01", 5000.0, 10, 50.0, 5500.0, 10, 550.0)
            ),
            expenses = listOf(
                Expense("E1", "Raw Materials", 5000.0, "2026-06-01", "Raw Material", "")
            ),
            tasks = listOf(
                DailyTask("T1", "2026-06-08", "Deliver order O1", true, "")
            )
        )

        val gson = GsonBuilder().create()
        val jsonString = gson.toJson(backup)
        val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)

        assertTrue(jsonObject.has("version"))
        assertTrue(jsonObject.has("customers"))
        assertTrue(jsonObject.has("orders"))
        assertTrue(jsonObject.has("payments"))
        assertTrue(jsonObject.has("inventoryBatches"))
        assertTrue(jsonObject.has("expenses"))
        assertTrue(jsonObject.has("tasks"))

        val restoredBackup = gson.fromJson(jsonString, BackupPayload::class.java)
        assertEquals("1.0", restoredBackup.version)
        assertEquals(1, restoredBackup.customers.size)
        assertEquals("Ayesha", restoredBackup.customers[0].name)
        assertEquals(1, restoredBackup.orders.size)
        assertEquals(4500.0, restoredBackup.orders[0].totalAmount, 0.001)
    }

    @Test
    fun testSupabaseDtoMappings() {
        val customer = Customer(
            id = "c1",
            name = "Zainab",
            phone = "0321-1234567",
            address = "Islamabad",
            joiningDate = "2026-01-10",
            pendingBalance = 1500.0
        )
        val dto = customer.toDto()
        assertEquals("c1", dto.id)
        assertEquals("Zainab", dto.name)
        assertEquals("2026-01-10", dto.joiningDate)
        assertEquals(1500.0, dto.pendingBalance, 0.001)

        val backToEntity = dto.toEntity()
        assertEquals(customer, backToEntity)
    }
}
