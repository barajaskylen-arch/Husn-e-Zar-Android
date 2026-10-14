package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.Customer
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

fun getTodayDateString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

fun showDatePickerDialog(
    context: Context,
    currentDate: String = "",
    onDateSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    if (currentDate.isNotBlank()) {
        try {
            val parts = currentDate.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val d = parts[2].toIntOrNull()
                if (y != null && m != null && d != null) {
                    calendar.set(y, m - 1, d)
                }
            }
        } catch (_: Exception) {}
    }
    android.app.DatePickerDialog(
        context,
        R.style.GoldDatePickerTheme,
        { _, year, month, day ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(formatted)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun showTimePickerDialog(
    context: Context,
    onTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    android.app.TimePickerDialog(
        context,
        R.style.GoldDatePickerTheme,
        { _, hour, minute ->
            val amPm = if (hour >= 12) "PM" else "AM"
            val h = if (hour % 12 == 0) 12 else hour % 12
            val formatted = String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
            onTimeSelected(formatted)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}

@Composable
fun standardTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Yellow,
    unfocusedBorderColor = DarkBlue,
    focusedLabelColor = DarkBlue,
    unfocusedLabelColor = DarkBlue.copy(alpha = 0.7f),
    cursorColor = DarkBlue,
    focusedTextColor = DarkBlue,
    unfocusedTextColor = DarkBlue,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedLeadingIconColor = DarkBlue,
    unfocusedLeadingIconColor = DarkBlue.copy(alpha = 0.7f),
    focusedTrailingIconColor = DarkBlue,
    unfocusedTrailingIconColor = DarkBlue.copy(alpha = 0.7f)
)

// Reusable Center Dialog with 300ms Fade-in + Scale (0.92 -> 1.0) and 50% Black Scrim
@Composable
fun AnimatedCenterDialog(
    onDismiss: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    onCancel: () -> Unit = onDismiss,
    onSave: (() -> Unit)? = null,
    saveText: String = "Save Karo",
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                       scaleOut(targetScale = 0.92f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.88f)
                        .imePadding()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* prevent backdrop dismissal when clicking dialog */ },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Header (Dark Blue)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DarkBlue
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Yellow,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                    }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Yellow
                                    )
                                }
                            }
                        }

                        // Scrollable fields
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            content()
                        }

                        // Bottom Actions: Cancel (Dark Blue outline) and Save (Yellow filled, Dark Blue text)
                        if (onSave != null) {
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCancel,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(2.dp, DarkBlue),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = DarkBlue
                                    )
                                ) {
                                    Text(
                                        "Cancel",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DarkBlue
                                    )
                                }

                                Button(
                                    onClick = onSave,
                                    enabled = saveEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Yellow,
                                        contentColor = DarkBlue,
                                        disabledContainerColor = Yellow.copy(alpha = 0.5f),
                                        disabledContentColor = DarkBlue.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        saveText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = DarkBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 1. Add Customer Dialog
@Composable
fun RegisterCustomerModal(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf(getTodayDateString()) }

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Customer",
        icon = Icons.Default.PersonAdd,
        saveEnabled = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank(),
        onSave = {
            if (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                onSave(name.trim(), phone.trim(), address.trim(), joiningDate)
                onDismiss()
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Customer Name *", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 15) phone = it },
            label = { Text("Phone Number * (e.g. 03001234567)", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address *", fontWeight = FontWeight.Bold) },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = joiningDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Joining Date", fontWeight = FontWeight.Bold) },
                trailingIcon = {
                    IconButton(onClick = {
                        showDatePickerDialog(context, joiningDate) { joiningDate = it }
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = DarkBlue)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDatePickerDialog(context, joiningDate) { joiningDate = it }
                    },
                colors = standardTextFieldColors()
            )
        }
    }
}

// 2. Add Balance / Record Payment for Customer Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBalanceModal(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getTodayDateString()) }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Payment Add Karo",
        icon = Icons.Default.AttachMoney,
        saveEnabled = selectedCustomer != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
        onSave = {
            val amt = amount.toDoubleOrNull()
            val custId = selectedCustomer?.id
            if (amt != null && amt > 0 && custId != null) {
                onSave(custId, amt, date, description)
                onDismiss()
            }
        }
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCustomer?.let { "${it.name} (Bal: Rs. ${it.pendingBalance})" } ?: "Customer chunein...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Customer *", fontWeight = FontWeight.Bold) },
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
                                Text("Phone: ${cust.phone} | Bal: Rs. ${cust.pendingBalance}", color = DarkBlue.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount Received (Rs.) *", fontWeight = FontWeight.Bold) },
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
                label = { Text("Payment Date", fontWeight = FontWeight.Bold) },
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

// 3. Add Expense Dialog
@Composable
fun AddExpenseModal(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getTodayDateString()) }
    var category by remember { mutableStateOf("Raw Material") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Raw Material", "Marketing", "Logistics", "Others")

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Expense",
        icon = Icons.Default.Receipt,
        saveEnabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
        onSave = {
            val amt = amount.toDoubleOrNull()
            if (title.isNotBlank() && amt != null && amt > 0) {
                onSave(title.trim(), amt, date, category, description.trim())
                onDismiss()
            }
        }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount * Rs.", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Text("Category *", color = DarkBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(2).forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Yellow,
                        selectedLabelColor = DarkBlue,
                        containerColor = Color(0xFFF0F4F8),
                        labelColor = DarkBlue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (category == cat) Yellow else DarkBlue,
                        selectedBorderColor = Yellow,
                        enabled = true,
                        selected = category == cat
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.drop(2).forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Yellow,
                        selectedLabelColor = DarkBlue,
                        containerColor = Color(0xFFF0F4F8),
                        labelColor = DarkBlue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (category == cat) Yellow else DarkBlue,
                        selectedBorderColor = Yellow,
                        enabled = true,
                        selected = category == cat
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

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

// 4. Add Inventory Batch Dialog
@Composable
fun AddBatchModal(
    onDismiss: () -> Unit,
    onSave: (Double, Int, Double, Int, String, String) -> Unit
) {
    val context = LocalContext.current
    var rawCost by remember { mutableStateOf("") }
    var emptyJarsData by remember { mutableStateOf("") }
    var jarCost by remember { mutableStateOf("") }
    var jarsProducedData by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getTodayDateString()) }
    var notes by remember { mutableStateOf("") }

    val rawMat = rawCost.toDoubleOrNull() ?: 0.0
    val jarsQty = emptyJarsData.toIntOrNull() ?: 0
    val costPerJar = jarCost.toDoubleOrNull() ?: 0.0
    val totalReady = jarsProducedData.toIntOrNull() ?: 0

    val totalJarCost = jarsQty * costPerJar
    val totalBatchCost = rawMat + totalJarCost
    val costPerReadyJar = if (totalReady > 0) totalBatchCost / totalReady else 0.0

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Batch",
        icon = Icons.Default.Build,
        saveEnabled = rawMat > 0 && jarsQty > 0 && totalReady > 0,
        onSave = {
            if (rawMat > 0 && jarsQty > 0 && costPerJar >= 0 && totalReady > 0) {
                onSave(rawMat, jarsQty, costPerJar, totalReady, date, notes)
                onDismiss()
            }
        }
    ) {
        OutlinedTextField(
            value = rawCost,
            onValueChange = { rawCost = it },
            label = { Text("Raw Material Cost * Rs.", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = emptyJarsData,
            onValueChange = { emptyJarsData = it },
            label = { Text("Empty Jars Quantity *", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = jarCost,
            onValueChange = { jarCost = it },
            label = { Text("Cost Per Empty Jar * Rs.", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = jarsProducedData,
            onValueChange = { jarsProducedData = it },
            label = { Text("Total Jars Ready *", fontWeight = FontWeight.Bold) },
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
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        // Live preview card in Dark Blue
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkBlue),
            border = BorderStroke(1.dp, Yellow.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Batch Cost Preview", fontWeight = FontWeight.Bold, color = Yellow, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text("Total Jar Cost: Rs. ${String.format(Locale.getDefault(), "%.2f", totalJarCost)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Total Batch Cost: Rs. ${String.format(Locale.getDefault(), "%.2f", totalBatchCost)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Cost Per Jar: Rs. ${String.format(Locale.getDefault(), "%.2f", costPerReadyJar)}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("New Stock: $totalReady jars", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// 5. Add Task Dialog (Schedule)
@Composable
fun AddTaskModal(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Social Media") }
    var description by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("") }
    val categories = listOf("Social Media", "Customer Follow-up", "Production", "Admin")

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Task",
        icon = Icons.Default.Checklist,
        saveEnabled = title.isNotBlank(),
        onSave = {
            if (title.isNotBlank()) {
                onSave(title.trim(), category, description.trim(), reminderTime)
                onDismiss()
            }
        }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title *", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Text("Category", color = DarkBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(2).forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Yellow,
                        selectedLabelColor = DarkBlue,
                        containerColor = Color(0xFFF0F4F8),
                        labelColor = DarkBlue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (category == cat) Yellow else DarkBlue,
                        selectedBorderColor = Yellow,
                        enabled = true,
                        selected = category == cat
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.drop(2).forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat, fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Yellow,
                        selectedLabelColor = DarkBlue,
                        containerColor = Color(0xFFF0F4F8),
                        labelColor = DarkBlue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (category == cat) Yellow else DarkBlue,
                        selectedBorderColor = Yellow,
                        enabled = true,
                        selected = category == cat
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = reminderTime,
                onValueChange = {},
                readOnly = true,
                label = { Text("Reminder Time (Optional)", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Waqt chunein...", color = DarkBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) },
                trailingIcon = {
                    IconButton(onClick = {
                        showTimePickerDialog(context) { reminderTime = it }
                    }) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Select Time", tint = DarkBlue)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showTimePickerDialog(context) { reminderTime = it }
                    },
                colors = standardTextFieldColors()
            )
        }
    }
}
