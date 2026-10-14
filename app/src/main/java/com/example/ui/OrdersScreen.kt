package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Customer
import com.example.data.Order
import com.example.data.PaymentInfo
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    appViewModel: AppViewModel,
    ordersViewModel: OrdersViewModel
) {
    val searchQuery by ordersViewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredOrders by ordersViewModel.filteredOrders.collectAsStateWithLifecycle()
    val customers by appViewModel.customers.collectAsStateWithLifecycle()
    val currentStock by appViewModel.currentAvailableStock.collectAsStateWithLifecycle()
    val currentBaseUnitCost by appViewModel.currentBaseUnitCost.collectAsStateWithLifecycle()

    var showAddOrderModal by remember { mutableStateOf(false) }
    var showPaymentModal by remember { mutableStateOf<Order?>(null) }

    var showReceiptDialog by remember { mutableStateOf(false) }
    var receiptDialogTitle by remember { mutableStateOf("Order Receipt") }
    var receiptText by remember { mutableStateOf("") }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master Order Registry", fontWeight = FontWeight.Bold, color = Gold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Gold,
                    actionIconContentColor = White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOrderModal = true },
                containerColor = Gold,
                contentColor = NavyDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Order")
            }
        },
        containerColor = NavyDark
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyDark)
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = ordersViewModel::updateSearchQuery,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search Customer or Order ID...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Gold) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = NavyBorder,
                            focusedContainerColor = NavyMedium,
                            unfocusedContainerColor = NavyMedium,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            cursorColor = Gold
                        )
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No orders found.", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredOrders) { order ->
                    OrderRow(
                        order = order,
                        onAddPayment = { showPaymentModal = order },
                        onShareReceipt = {
                            val cust = customers.find { it.id == order.customerId }
                                ?: Customer(
                                    id = order.customerId,
                                    name = order.customerName,
                                    phone = "-",
                                    address = "-",
                                    joiningDate = "",
                                    pendingBalance = 0.0
                                )
                            receiptDialogTitle = "Order Receipt"
                            receiptText = generateOrderReceipt(order, cust)
                            showReceiptDialog = true
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddOrderModal) {
        AddOrderModal(
            customers = customers,
            currentStock = currentStock,
            currentBaseCost = currentBaseUnitCost,
            onDismiss = { showAddOrderModal = false },
            onSave = { custId, custName, orderDate, jars, totalAmt, paidAmt, dueDate, desc ->
                if (jars > currentStock) {
                    Toast.makeText(context, "Not enough stock! Only $currentStock jars available.", Toast.LENGTH_LONG).show()
                } else {
                    appViewModel.addOrder(
                        date = orderDate,
                        customerId = custId,
                        customerName = custName,
                        jarsQuantity = jars,
                        totalAmount = totalAmt,
                        paidAmount = paidAmt,
                        dueDate = dueDate,
                        description = desc,
                        baseCostPerJar = currentBaseUnitCost,
                        onOrderCreated = { createdOrder ->
                            val cust = customers.find { it.id == custId }
                                ?: Customer(
                                    id = custId,
                                    name = custName,
                                    phone = "-",
                                    address = "-",
                                    joiningDate = "",
                                    pendingBalance = 0.0
                                )
                            receiptDialogTitle = "Order Receipt"
                            receiptText = generateOrderReceipt(createdOrder, cust)
                            showReceiptDialog = true
                        }
                    )
                    showAddOrderModal = false
                }
            }
        )
    }

    if (showPaymentModal != null) {
        val ord = showPaymentModal!!
        AddPaymentModal(
            order = ord,
            onDismiss = { showPaymentModal = null },
            onSave = { amount, date, desc ->
                appViewModel.addOrderPayment(
                    order = ord,
                    amount = amount,
                    date = date,
                    description = desc,
                    onPaymentRecorded = { updatedOrder, history ->
                        receiptDialogTitle = "Payment Receipt"
                        receiptText = generatePaymentReceipt(updatedOrder, ord.customerName, history)
                        showReceiptDialog = true
                    }
                )
                showPaymentModal = null
            }
        )
    }

    // Receipt AlertDialog
    if (showReceiptDialog) {
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            containerColor = NavyMedium,
            title = {
                Text(
                    text = receiptDialogTitle,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                SelectionContainer {
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = White,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, receiptText)
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(
                                Intent.createChooser(
                                    intent.apply { setPackage(null) },
                                    "Receipt Share Karo"
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Share on WhatsApp", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReceiptDialog = false },
                    border = BorderStroke(1.dp, NavyBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

fun generateOrderReceipt(order: Order, customer: Customer): String {
    return """
━━━━━━━━━━━━━━━━━━━━━━━
  ✨ Husn e Zar Beauty Cream ✨
━━━━━━━━━━━━━━━━━━━━━━━
Order ID  : ${order.id}
Date      : ${order.orderDate}
Customer  : ${customer.name}
Phone     : ${customer.phone}
━━━━━━━━━━━━━━━━━━━━━━━
Jars Qty  : ${order.jarsQuantity}
Total Bill: ${formatMoney(order.totalAmount)}
Paid Now  : ${formatMoney(order.paidAmount)}
Balance   : ${formatMoney(order.pendingAmount)}
Due Date  : ${order.dueDate}
━━━━━━━━━━━━━━━━━━━━━━━
Shukriya! Husn e Zar 🌿
━━━━━━━━━━━━━━━━━━━━━━━
    """.trimIndent()
}

fun generatePaymentReceipt(order: Order, customerName: String, payments: List<PaymentInfo>): String {
    val historyText = if (payments.isEmpty()) {
        "1. ${order.orderDate} — ${formatMoney(order.paidAmount)} (Aaj)"
    } else {
        payments.mapIndexed { idx, p ->
            val tag = if (idx == payments.lastIndex) " (Aaj)" else ""
            "${idx + 1}. ${p.date} — ${formatMoney(p.amount)}$tag"
        }.joinToString("\n")
    }

    return """
━━━━━━━━━━━━━━━━━━━━━━━
  ✨ Husn e Zar Beauty Cream ✨
  Payment Receipt
━━━━━━━━━━━━━━━━━━━━━━━
Order ID  : ${order.id}
Customer  : $customerName
Total Bill: ${formatMoney(order.totalAmount)}
━━━━━━━━━━━━━━━━━━━━━━━
Payment History:
$historyText
━━━━━━━━━━━━━━━━━━━━━━━
Total Paid: ${formatMoney(order.paidAmount)}
Remaining : ${formatMoney(order.pendingAmount)}
━━━━━━━━━━━━━━━━━━━━━━━
Shukriya! Husn e Zar 🌿
━━━━━━━━━━━━━━━━━━━━━━━
    """.trimIndent()
}

@Composable
fun OrderRow(
    order: Order,
    onAddPayment: () -> Unit,
    onShareReceipt: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyMedium)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.id} - ${order.customerName}",
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(order.orderDate, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Jars: ${order.jarsQuantity}", color = TextSecondary)
                Text("Total: ${formatMoney(order.totalAmount)}", fontWeight = FontWeight.Bold, color = White)
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Paid: ${formatMoney(order.paidAmount)}", color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                Text(
                    "Pending: ${formatMoney(order.pendingAmount)}",
                    color = if (order.pendingAmount > 0) ExpenseRed else ProfitGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (order.pendingAmount > 0) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Due: ${order.dueDate}",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Surface(
                        color = AlertRedBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ExpenseRed)
                    ) {
                        Text(
                            text = "Udhar / Credit",
                            color = ExpenseRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = AlertGreenBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ProfitGreen)
                ) {
                    Text(
                        text = "Paid / Clear",
                        color = ProfitGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = NavyBorder)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (order.pendingAmount > 0) {
                    Button(
                        onClick = onAddPayment,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyLight,
                            contentColor = Gold
                        ),
                        border = BorderStroke(1.dp, Gold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp), tint = Gold)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Payment", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onShareReceipt) {
                    Icon(Icons.Default.Share, contentDescription = "Share Receipt", tint = Gold)
                }
            }
        }
    }
}

// 2. Add Order ModalBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderModal(
    customers: List<Customer>,
    currentStock: Int,
    currentBaseCost: Double,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Double, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var orderDate by remember { mutableStateOf(getTodayDateString()) }
    var jarsText by remember { mutableStateOf("") }
    var totalAmountText by remember { mutableStateOf("") }
    var paidAmountText by remember { mutableStateOf("0") }
    var paymentType by remember { mutableStateOf("paid") } // "paid" or "credit"
    var dueDate by remember { mutableStateOf(getTodayDateString()) }
    var description by remember { mutableStateOf("") }

    val jars = jarsText.toIntOrNull() ?: 0
    val total = totalAmountText.toDoubleOrNull() ?: 0.0
    val paid = paidAmountText.toDoubleOrNull() ?: 0.0
    val pending = maxOf(0.0, total - paid)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BottomSheetHeader(title = "Naya Order", onDismiss = onDismiss)

            // Stock & Cost Info Banner
            Surface(
                color = AlertBlueBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stock Available: $currentStock jars", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Base Cost: ${formatMoneyDetailed(currentBaseCost)}", color = Gold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))

            // Customer Selector
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "Customer chunein...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = standardTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(NavyLight)
                ) {
                    customers.forEach { cust ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(cust.name, color = White, fontWeight = FontWeight.Bold)
                                    Text("Phone: ${cust.phone} | Address: ${cust.address}", color = TextSecondary, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                selectedCustomer = cust
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Order Date Picker
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = orderDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Order Date *") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePickerDialog(context, orderDate) { orderDate = it }
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = Gold)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePickerDialog(context, orderDate) { orderDate = it }
                        },
                    colors = standardTextFieldColors()
                )
            }
            Spacer(Modifier.height(12.dp))

            // Jars Quantity
            OutlinedTextField(
                value = jarsText,
                onValueChange = { jarsText = it },
                label = { Text("Jars Quantity *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            // Total Amount
            OutlinedTextField(
                value = totalAmountText,
                onValueChange = {
                    totalAmountText = it
                    if (paymentType == "paid") {
                        paidAmountText = it
                    }
                },
                label = { Text("Total Amount * Rs.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            // Payment Type Segmented Choice
            Text("Payment Type", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = paymentType == "paid",
                    onClick = {
                        paymentType = "paid"
                        paidAmountText = totalAmountText
                    },
                    label = { Text("Paid / Direct", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold,
                        selectedLabelColor = NavyDark,
                        containerColor = NavyLight,
                        labelColor = White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (paymentType == "paid") Gold else NavyBorder,
                        selectedBorderColor = Gold,
                        enabled = true,
                        selected = paymentType == "paid"
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = paymentType == "credit",
                    onClick = {
                        paymentType = "credit"
                    },
                    label = { Text("Udhar / Credit", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRed,
                        selectedLabelColor = White,
                        containerColor = NavyLight,
                        labelColor = White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (paymentType == "credit") ExpenseRed else NavyBorder,
                        selectedBorderColor = ExpenseRed,
                        enabled = true,
                        selected = paymentType == "credit"
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))

            // Paid Amount
            OutlinedTextField(
                value = paidAmountText,
                onValueChange = { paidAmountText = it },
                label = { Text("Paid Amount Rs.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            // Due Date field (only visible for credit / Udhar)
            if (paymentType == "credit" || pending > 0) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (dueDate.isNotBlank()) dueDate else "Tareekh chunein",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Due Date * (Udhar Wapsi)") },
                        trailingIcon = {
                            IconButton(onClick = {
                                showDatePickerDialog(context, dueDate) { dueDate = it }
                            }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Due Date", tint = Gold)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePickerDialog(context, dueDate) { dueDate = it }
                            },
                        colors = standardTextFieldColors()
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(16.dp))

            // Live Calculation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = if (pending > 0) AlertYellowBg else AlertGreenBg),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Order Balance Summary", fontWeight = FontWeight.Bold, color = if (pending > 0) GoldLight else ProfitGreen, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("Pending Balance: ${formatMoney(pending)}", color = if (pending > 0) ExpenseRed else ProfitGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Status: ${if (pending > 0) "Udhar / Partial" else "Full Paid"}", color = WhiteSoft, fontSize = 13.sp)
                    if (jars > currentStock) {
                        Spacer(Modifier.height(4.dp))
                        Text("⚠️ Stock Insufficient! ($currentStock Available)", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, NavyBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val custId = selectedCustomer?.id
                        val cName = selectedCustomer?.name
                        val finalDueDate = if (paymentType == "credit") dueDate else orderDate
                        if (custId != null && cName != null && jars > 0 && total > 0 && jars <= currentStock) {
                            onSave(custId, cName, orderDate, jars, total, paid, finalDueDate, description)
                            onDismiss()
                        }
                    },
                    enabled = jars <= currentStock && selectedCustomer != null && jars > 0 && total > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Karo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 6. Add Payment to Order ModalBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentModal(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf(order.pendingAmount.toString()) }
    var date by remember { mutableStateOf(getTodayDateString()) }
    var description by remember { mutableStateOf("Installment for Order ${order.id}") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BottomSheetHeader(title = "Payment Add Karo", onDismiss = onDismiss)

            // Read-only Top Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyLight),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Order: ${order.id} | Customer: ${order.customerName}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Total: ${formatMoney(order.totalAmount)}", color = White, fontSize = 13.sp)
                    Text("Paid: ${formatMoney(order.paidAmount)}", color = ProfitGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Remaining: ${formatMoney(order.pendingAmount)}", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount * (Max: ${formatMoney(order.pendingAmount)})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePickerDialog(context, date) { date = it }
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = Gold)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePickerDialog(context, date) { date = it }
                        },
                    colors = standardTextFieldColors()
                )
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, NavyBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            onSave(amount, date, description)
                            onDismiss()
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Karo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
