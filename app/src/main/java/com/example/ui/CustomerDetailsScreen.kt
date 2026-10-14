package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
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
import com.example.data.CustomerPayment
import com.example.data.Order
import com.example.ui.theme.*

fun generateCustomerStatement(
    customer: Customer,
    orders: List<Order>,
    payments: List<CustomerPayment>
): String {
    val totalBill = orders.sumOf { it.totalAmount }
    val totalPaid = orders.sumOf { it.paidAmount }
    val totalPending = orders.sumOf { it.pendingAmount }

    return buildString {
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("  ✨ Husn e Zar Beauty Cream ✨")
        appendLine("  Customer Statement")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("Customer : ${customer.name}")
        appendLine("Phone    : ${customer.phone}")
        appendLine("Since    : ${customer.joiningDate}")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("ORDER SUMMARY:")
        orders.forEachIndexed { i, order ->
            appendLine("${i + 1}. ${order.orderDate} — " +
                    "${order.jarsQuantity} jars — " +
                    formatMoney(order.totalAmount))
            if (order.pendingAmount > 0)
                appendLine("   Pending: ${formatMoney(order.pendingAmount)}")
        }
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("Total Bill   : ${formatMoney(totalBill)}")
        appendLine("Total Paid   : ${formatMoney(totalPaid)}")
        appendLine("Balance Due  : ${formatMoney(totalPending)}")
        appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
        appendLine("Shukriya — Husn e Zar 🌿")
    }
}

fun generatePaymentReminder(
    customer: Customer,
    order: Order
): String {
    return """
Assalam o Alaikum ${customer.name} bhai/behen,

Umeed hai aap khairiyat se honge. 😊

Husn e Zar Beauty Cream ki taraf se aapko 
yaad dilana chahte hain ke:

Order ID : ${order.id}
Jars     : ${order.jarsQuantity}
Total    : ${formatMoney(order.totalAmount)}
Paid     : ${formatMoney(order.paidAmount)}
Baki     : ${formatMoney(order.pendingAmount)}
Due Date : ${order.dueDate}

Meherbani farma ke jald ada farma dein.
Shukriya! 🌿

— Husn e Zar Beauty Cream
    """.trimIndent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    viewModel: CustomerDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val customer by viewModel.customer.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()

    var showEditOrder by remember { mutableStateOf<Order?>(null) }
    var showStatementDialog by remember { mutableStateOf(false) }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize().background(NavyDark), contentAlignment = Alignment.Center) {
            Text("Customer not found.", color = TextSecondary)
        }
        return
    }

    val currentCustomer = customer!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile", fontWeight = FontWeight.Bold, color = Gold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    Button(
                        onClick = { showStatementDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Statement",
                            modifier = Modifier.size(16.dp),
                            tint = NavyDark
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Statement", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Gold,
                    navigationIconContentColor = White
                )
            )
        },
        containerColor = NavyDark
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyDark)
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Area I: Customer Profile Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyMedium),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentCustomer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Gold)
                                Text("ID: ${currentCustomer.id}", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Phone: ${currentCustomer.phone}", color = White)
                        Text("Address: ${currentCustomer.address}", color = WhiteSoft)
                        Text("Joined: ${currentCustomer.joiningDate}", color = TextSecondary)
                        Spacer(Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentCustomer.pendingBalance > 0) AlertRedBg else AlertGreenBg
                            ),
                            border = BorderStroke(1.dp, if (currentCustomer.pendingBalance > 0) ExpenseRed else ProfitGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Pending Balance: ${formatMoney(currentCustomer.pendingBalance)}",
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                fontWeight = FontWeight.Bold,
                                color = if (currentCustomer.pendingBalance > 0) ExpenseRed else ProfitGreen,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Area II: Order History Ledger
            item {
                Text("Order History Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
            }
            if (orders.isEmpty()) {
                item {
                    Text("No orders found.", color = TextSecondary)
                }
            } else {
                items(orders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showEditOrder = order },
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order ${order.id}", fontWeight = FontWeight.Bold, color = White)
                                Text(order.orderDate, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Jars: ${order.jarsQuantity} | Total: ${formatMoney(order.totalAmount)}", color = WhiteSoft)
                            Text("Paid: ${formatMoney(order.paidAmount)} | Pending: ${formatMoney(order.pendingAmount)}", color = if (order.pendingAmount > 0) ExpenseRed else ProfitGreen)
                            
                            if (order.pendingAmount > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Due: ${order.dueDate}", color = ExpenseRed, style = MaterialTheme.typography.labelSmall)
                                    
                                    // WhatsApp Payment Reminder button
                                    OutlinedButton(
                                        onClick = {
                                            val reminderText = generatePaymentReminder(currentCustomer, order)
                                            val rawPhone = currentCustomer.phone.replace("-", "").replace(" ", "").replace("+", "")
                                            val cleanPhone = if (rawPhone.startsWith("92")) rawPhone else if (rawPhone.startsWith("0")) "92" + rawPhone.drop(1) else "92$rawPhone"
                                            val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(reminderText)}")
                                            val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                                setPackage("com.whatsapp")
                                            }
                                            try {
                                                context.startActivity(waIntent)
                                            } catch (e: Exception) {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }
                                        },
                                        border = BorderStroke(1.dp, Gold),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Remind", tint = Gold, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Area III: Payment History Registry
            item {
                Text("Payment History Registry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
            }
            if (payments.isEmpty()) {
                item {
                    Text("No payments found.", color = TextSecondary)
                }
            } else {
                items(payments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(formatMoney(payment.amount), fontWeight = FontWeight.Bold, color = ProfitGreen, fontSize = 15.sp)
                                if (payment.description.isNotBlank()) {
                                    Text(payment.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                            Text(payment.date, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Statement AlertDialog
    if (showStatementDialog) {
        val statementText = remember(currentCustomer, orders, payments) {
            generateCustomerStatement(currentCustomer, orders, payments)
        }

        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            containerColor = NavyMedium,
            title = {
                Text("Customer Statement", fontWeight = FontWeight.Bold, color = Gold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .background(NavyDark, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = statementText,
                        color = WhiteSoft,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, statementText)
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, statementText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Customer Statement"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp Share", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStatementDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showEditOrder != null) {
        val order = showEditOrder!!
        EditOrderModal(
            order = order,
            onDismiss = { showEditOrder = null },
            onSave = { jars, paidAmount, dueDate ->
                val unitPrice = if (order.jarsQuantity > 0) order.totalAmount / order.jarsQuantity else 0.0
                val newTotalAmount = jars * unitPrice
                val newPendingAmount = maxOf(0.0, newTotalAmount - paidAmount)
                val updatedOrder = order.copy(
                    jarsQuantity = jars,
                    paidAmount = paidAmount,
                    totalAmount = newTotalAmount,
                    pendingAmount = newPendingAmount,
                    dueDate = dueDate
                )
                viewModel.updateOrder(order, updatedOrder)
                showEditOrder = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderModal(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (Int, Double, String) -> Unit
) {
    val context = LocalContext.current
    var jars by remember { mutableStateOf(order.jarsQuantity.toString()) }
    var paid by remember { mutableStateOf(order.paidAmount.toString()) }
    var dueDate by remember { mutableStateOf(order.dueDate) }

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
            BottomSheetHeader(title = "Edit Order ${order.id}", onDismiss = onDismiss)

            Text("Note: Changing jars recalculates total based on unit price.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = jars,
                onValueChange = { jars = it },
                label = { Text("Jars Quantity *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = paid,
                onValueChange = { paid = it },
                label = { Text("Paid Amount Rs. *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due Date *") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePickerDialog(context, dueDate) { dueDate = it }
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = Gold)
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
                        val j = jars.toIntOrNull()
                        val p = paid.toDoubleOrNull()
                        if (j != null && p != null && j > 0) {
                            onSave(j, p, dueDate)
                        }
                    },
                    enabled = (jars.toIntOrNull() ?: 0) > 0 && (paid.toDoubleOrNull() ?: -1.0) >= 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Order Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
