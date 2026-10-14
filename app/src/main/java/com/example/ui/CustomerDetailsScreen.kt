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
import androidx.compose.material.icons.filled.Edit
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
        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            Text("Customer not found.", color = DarkBlue, fontWeight = FontWeight.Bold)
        }
        return
    }

    val currentCustomer = customer!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile", fontWeight = FontWeight.Bold, color = Yellow) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Yellow)
                    }
                },
                actions = {
                    Button(
                        onClick = { showStatementDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow,
                            contentColor = DarkBlue
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Statement",
                            modifier = Modifier.size(16.dp),
                            tint = DarkBlue
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Statement", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    navigationIconContentColor = Yellow
                )
            )
        },
        containerColor = Color.White
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Area I: Customer Profile Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentCustomer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Yellow)
                                Text("ID: ${currentCustomer.id}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Phone: ${currentCustomer.phone}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Address: ${currentCustomer.address}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                        Text("Joined: ${currentCustomer.joiningDate}", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentCustomer.pendingBalance > 0) AlertRedBg else AlertGreenBg
                            ),
                            border = BorderStroke(1.5.dp, if (currentCustomer.pendingBalance > 0) AlertRed else SuccessGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                "Pending Balance: ${formatMoney(currentCustomer.pendingBalance)}",
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                fontWeight = FontWeight.Bold,
                                color = if (currentCustomer.pendingBalance > 0) AlertRed else SuccessGreen,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Area II: Order History Ledger
            item {
                Text("Order History Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkBlue)
            }
            if (orders.isEmpty()) {
                item {
                    Text("No orders found.", color = DarkBlue.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            } else {
                items(orders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showEditOrder = order },
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order ${order.id}", fontWeight = FontWeight.Bold, color = Yellow)
                                Text(order.orderDate, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Jars: ${order.jarsQuantity} | Total: ${formatMoney(order.totalAmount)}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Paid: ${formatMoney(order.paidAmount)} | Pending: ${formatMoney(order.pendingAmount)}", color = if (order.pendingAmount > 0) AlertRed else SuccessGreen, fontWeight = FontWeight.Bold)

                            if (order.pendingAmount > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Due: ${order.dueDate}", color = AlertRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)

                                    // WhatsApp Payment Reminder button
                                    Button(
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
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Yellow,
                                            contentColor = DarkBlue
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Remind", tint = DarkBlue, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Area III: Payment History Registry
            item {
                Text("Payment History Registry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkBlue)
            }
            if (payments.isEmpty()) {
                item {
                    Text("No payments found.", color = DarkBlue.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            } else {
                items(payments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(formatMoney(payment.amount), fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 15.sp)
                                if (payment.description.isNotBlank()) {
                                    Text(payment.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                            Text(payment.date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Statement AlertDialog (Dark Blue background, Yellow header, White monospace text)
    if (showStatementDialog) {
        val statementText = remember(currentCustomer, orders, payments) {
            generateCustomerStatement(currentCustomer, orders, payments)
        }

        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            containerColor = DarkBlue,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Customer Statement", fontWeight = FontWeight.Bold, color = Yellow)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = statementText,
                        color = DarkBlue,
                        fontWeight = FontWeight.Bold,
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
                        containerColor = Yellow,
                        contentColor = DarkBlue
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = DarkBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp Share", fontWeight = FontWeight.Bold, color = DarkBlue)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showStatementDialog = false },
                    border = BorderStroke(2.dp, Yellow),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Yellow),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Yellow)
                }
            }
        )
    }

    if (showEditOrder != null) {
        val order = showEditOrder!!
        EditOrderDialog(
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

@Composable
fun EditOrderDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (Int, Double, String) -> Unit
) {
    val context = LocalContext.current
    var jars by remember { mutableStateOf(order.jarsQuantity.toString()) }
    var paid by remember { mutableStateOf(order.paidAmount.toString()) }
    var dueDate by remember { mutableStateOf(order.dueDate) }

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Edit Order ${order.id}",
        icon = Icons.Default.Edit,
        saveText = "Save Order Changes",
        saveEnabled = (jars.toIntOrNull() ?: 0) > 0 && (paid.toDoubleOrNull() ?: -1.0) >= 0.0,
        onSave = {
            val j = jars.toIntOrNull()
            val p = paid.toDoubleOrNull()
            if (j != null && p != null && j > 0) {
                onSave(j, p, dueDate)
            }
        }
    ) {
        Text("Note: Changing jars recalculates total based on unit price.", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DarkBlue.copy(alpha = 0.7f))

        OutlinedTextField(
            value = jars,
            onValueChange = { jars = it },
            label = { Text("Jars Quantity *", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = paid,
            onValueChange = { paid = it },
            label = { Text("Paid Amount Rs. *", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dueDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Due Date *", fontWeight = FontWeight.Bold) },
                trailingIcon = {
                    IconButton(onClick = {
                        showDatePickerDialog(context, dueDate) { dueDate = it }
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = DarkBlue)
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
}
