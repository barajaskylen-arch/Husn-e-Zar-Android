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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
                title = { Text("Master Order Registry (${filteredOrders.size})", fontWeight = FontWeight.Bold, color = Yellow) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    actionIconContentColor = Yellow
                ),
                actions = {
                    IconButton(
                        onClick = { showAddOrderModal = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Order", tint = Yellow)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOrderModal = true },
                containerColor = Yellow,
                contentColor = DarkBlue,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Order")
            }
        },
        containerColor = Color.White
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
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
                        placeholder = { Text("Search Customer or Order ID...", color = DarkBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DarkBlue) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { ordersViewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = DarkBlue)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = standardTextFieldColors()
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkBlueDeep),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Yellow,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "Koi Order Nahi Hai" else "Koi Nateeja Nahi Mila",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "Pehla order banayein taake sale aur inventory record shuru hon." else "Di gayi search query ke mutabiq koi order nahi mila.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isEmpty()) {
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = { showAddOrderModal = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Yellow,
                                        contentColor = DarkBlue
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pehla Order Banayein", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBlue)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredOrders, key = { it.id }) { order ->
                    Box(modifier = Modifier.animateItem()) {
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

    // Receipt AlertDialog (White container with Dark Blue header and text)
    if (showReceiptDialog) {
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = receiptDialogTitle,
                    color = DarkBlue,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .background(Color(0xFFF5F7FA), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = receiptText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue,
                            lineHeight = 18.sp
                        )
                    }
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
                        containerColor = Yellow,
                        contentColor = DarkBlue
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Share on WhatsApp", fontWeight = FontWeight.Bold, color = DarkBlue)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReceiptDialog = false },
                    border = BorderStroke(2.dp, DarkBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = DarkBlue)
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
    val isPaid = order.paidAmount >= order.totalAmount && order.totalAmount > 0
    val isPartial = order.paidAmount > 0 && order.paidAmount < order.totalAmount
    val statusLabel = if (isPaid) "Paid" else if (isPartial) "Partial" else "Pending"
    val statusColor = if (isPaid) SuccessGreen else if (isPartial) Yellow else AlertRed
    val statusBg = if (isPaid) AlertGreenBg else if (isPartial) AlertYellowBg else AlertRedBg
    val paidRatio = if (order.totalAmount > 0) (order.paidAmount / order.totalAmount).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Customer Name & Order ID + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.id} • ${order.customerName}",
                        fontWeight = FontWeight.Bold,
                        color = Yellow,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Status Chip: Paid (green), Partial (yellow), Pending (red)
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Date and Jars Quantity with Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = Yellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = order.orderDate,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Jars",
                        tint = Yellow,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${order.jarsQuantity} Jars",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Financial Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ${formatMoney(order.totalAmount)}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text("Paid: ${formatMoney(order.paidAmount)}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Baaqi: ${formatMoney(order.pendingAmount)}",
                    color = if (order.pendingAmount > 0) AlertRed else SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Paid / Total Thin Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { paidRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = DarkBlueDeep
                )
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${(paidRatio * 100).toInt()}% paid",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    if (order.pendingAmount > 0 && order.dueDate.isNotBlank()) {
                        Text(
                            text = "Due: ${order.dueDate}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertRed
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (order.pendingAmount > 0) {
                    Button(
                        onClick = onAddPayment,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow,
                            contentColor = DarkBlue
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Payment", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onShareReceipt,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Receipt", tint = Yellow)
                }
            }
        }
    }
}

// 2. Add Order Center Dialog
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

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Order",
        icon = Icons.Default.ShoppingCart,
        saveEnabled = jars <= currentStock && selectedCustomer != null && jars > 0 && total > 0,
        onSave = {
            val custId = selectedCustomer?.id
            val cName = selectedCustomer?.name
            val finalDueDate = if (paymentType == "credit") dueDate else orderDate
            if (custId != null && cName != null && jars > 0 && total > 0 && jars <= currentStock) {
                onSave(custId, cName, orderDate, jars, total, paid, finalDueDate, description)
                onDismiss()
            }
        }
    ) {
        // Stock & Cost Info Banner in Dark Blue
        Surface(
            color = DarkBlue,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Yellow.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stock Available: $currentStock jars", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Base Cost: ${formatMoneyDetailed(currentBaseCost)}", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Customer Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "Customer chunein...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Customer *", fontWeight = FontWeight.Bold) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                customers.forEach { cust ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(cust.name, color = DarkBlue, fontWeight = FontWeight.Bold)
                                Text("Phone: ${cust.phone} | Address: ${cust.address}", color = DarkBlue.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

        // Order Date Picker
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = orderDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Order Date *", fontWeight = FontWeight.Bold) },
                trailingIcon = {
                    IconButton(onClick = {
                        showDatePickerDialog(context, orderDate) { orderDate = it }
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = DarkBlue)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePickerDialog(context, orderDate) { orderDate = it }
                    },
                colors = standardTextFieldColors()
            )
        }

        // Jars Quantity
        OutlinedTextField(
            value = jarsText,
            onValueChange = { jarsText = it },
            label = { Text("Jars Quantity *", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        // Total Amount
        OutlinedTextField(
            value = totalAmountText,
            onValueChange = {
                totalAmountText = it
                if (paymentType == "paid") {
                    paidAmountText = it
                }
            },
            label = { Text("Total Amount * Rs.", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        // Payment Type Segmented Choice
        Text("Payment Type", color = DarkBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
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
                    selectedContainerColor = Yellow,
                    selectedLabelColor = DarkBlue,
                    containerColor = Color(0xFFF0F4F8),
                    labelColor = DarkBlue
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (paymentType == "paid") Yellow else DarkBlue,
                    selectedBorderColor = Yellow,
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
                    selectedContainerColor = AlertRed,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF0F4F8),
                    labelColor = DarkBlue
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (paymentType == "credit") AlertRed else DarkBlue,
                    selectedBorderColor = AlertRed,
                    enabled = true,
                    selected = paymentType == "credit"
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Paid Amount
        OutlinedTextField(
            value = paidAmountText,
            onValueChange = { paidAmountText = it },
            label = { Text("Paid Amount Rs.", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        // Due Date field (only visible for credit / Udhar)
        if (paymentType == "credit" || pending > 0) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (dueDate.isNotBlank()) dueDate else "Tareekh chunein",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due Date * (Udhar Wapsi)", fontWeight = FontWeight.Bold) },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePickerDialog(context, dueDate) { dueDate = it }
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Due Date", tint = DarkBlue)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePickerDialog(context, dueDate) { dueDate = it }
                        },
                    colors = standardTextFieldColors()
                )
            }
        }

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)", fontWeight = FontWeight.Bold) },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        // Live Calculation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = if (pending > 0) AlertYellowBg else AlertGreenBg),
            border = BorderStroke(1.5.dp, if (pending > 0) Yellow else SuccessGreen),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Order Balance Summary",
                    fontWeight = FontWeight.Bold,
                    color = if (pending > 0) DarkBlue else SuccessGreen,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pending Balance: ${formatMoney(pending)}",
                    color = if (pending > 0) AlertRed else SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "Status: ${if (pending > 0) "Udhar / Partial" else "Full Paid"}",
                    color = DarkBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (jars > currentStock) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⚠️ Stock Insufficient! ($currentStock Available)",
                        color = AlertRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// 6. Add Payment to Order Center Dialog
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

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Payment Add Karo",
        icon = Icons.Default.Payment,
        saveEnabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
        onSave = {
            val amount = amountText.toDoubleOrNull()
            if (amount != null && amount > 0) {
                onSave(amount, date, description)
                onDismiss()
            }
        }
    ) {
        // Read-only Top Info Card in Dark Blue
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkBlue),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Order: ${order.id} | Customer: ${order.customerName}", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text("Total: ${formatMoney(order.totalAmount)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Paid: ${formatMoney(order.paidAmount)}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Remaining: ${formatMoney(order.pendingAmount)}", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount * (Max: ${formatMoney(order.pendingAmount)})", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date *", fontWeight = FontWeight.Bold) },
                trailingIcon = {
                    IconButton(onClick = {
                        showDatePickerDialog(context, date) { date = it }
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = DarkBlue)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePickerDialog(context, date) { date = it }
                    },
                colors = standardTextFieldColors()
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )
    }
}
