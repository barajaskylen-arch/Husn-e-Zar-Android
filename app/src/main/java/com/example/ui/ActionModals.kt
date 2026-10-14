package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Customer
import com.example.data.Order
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
fun BottomSheetHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(NavyBorder, RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Gold,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = NavyBorder)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun standardTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold,
    unfocusedBorderColor = NavyBorder,
    focusedLabelColor = Gold,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Gold,
    focusedTextColor = White,
    unfocusedTextColor = White,
    focusedContainerColor = NavyLight,
    unfocusedContainerColor = NavyLight
)

// 1. Add Customer ModalBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
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
            BottomSheetHeader(title = "Naya Customer", onDismiss = onDismiss)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name *") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 15) phone = it },
                label = { Text("Phone Number * (e.g. 03001234567)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address *") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = joiningDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Joining Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            showDatePickerDialog(context, joiningDate) { joiningDate = it }
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = Gold)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePickerDialog(context, joiningDate) { joiningDate = it }
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
                        if (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                            onSave(name.trim(), phone.trim(), address.trim(), joiningDate)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank(),
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

// 2. Add Balance / Record Payment for Customer ModalBottomSheet
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

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.let { "${it.name} (Bal: Rs. ${it.pendingBalance})" } ?: "Customer chunein...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Customer *") },
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
                                    Text("Phone: ${cust.phone} | Bal: Rs. ${cust.pendingBalance}", color = TextSecondary, fontSize = 12.sp)
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

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount Received (Rs.) *") },
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
                    label = { Text("Payment Date") },
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
                        val amt = amount.toDoubleOrNull()
                        val custId = selectedCustomer?.id
                        if (amt != null && amt > 0 && custId != null) {
                            onSave(custId, amt, date, description)
                            onDismiss()
                        }
                    },
                    enabled = selectedCustomer != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
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

// 3. Add Expense ModalBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
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
            BottomSheetHeader(title = "Naya Expense", onDismiss = onDismiss)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount * Rs.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(14.dp))

            Text("Category *", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(2).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = NavyDark,
                            containerColor = NavyLight,
                            labelColor = White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (category == cat) Gold else NavyBorder,
                            selectedBorderColor = Gold,
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
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = NavyDark,
                            containerColor = NavyLight,
                            labelColor = White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (category == cat) Gold else NavyBorder,
                            selectedBorderColor = Gold,
                            enabled = true,
                            selected = category == cat
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
                        val amt = amount.toDoubleOrNull()
                        if (title.isNotBlank() && amt != null && amt > 0) {
                            onSave(title.trim(), amt, date, category, description.trim())
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
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

// 4. Add Inventory Batch ModalBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
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
            BottomSheetHeader(title = "Naya Batch", onDismiss = onDismiss)

            OutlinedTextField(
                value = rawCost,
                onValueChange = { rawCost = it },
                label = { Text("Raw Material Cost * Rs.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = emptyJarsData,
                onValueChange = { emptyJarsData = it },
                label = { Text("Empty Jars Quantity *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = jarCost,
                onValueChange = { jarCost = it },
                label = { Text("Cost Per Empty Jar * Rs.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = jarsProducedData,
                onValueChange = { jarsProducedData = it },
                label = { Text("Total Jars Ready *") },
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
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(16.dp))

            // Live preview card
            Card(
                colors = CardDefaults.cardColors(containerColor = AlertBlueBg),
                border = BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Batch Cost Preview", fontWeight = FontWeight.Bold, color = Gold, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("Total Jar Cost: Rs. ${String.format(Locale.getDefault(), "%.2f", totalJarCost)}", color = White, fontSize = 13.sp)
                    Text("Total Batch Cost: Rs. ${String.format(Locale.getDefault(), "%.2f", totalBatchCost)}", color = White, fontSize = 13.sp)
                    Text("Cost Per Jar: Rs. ${String.format(Locale.getDefault(), "%.2f", costPerReadyJar)}", color = ProfitGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("New Stock: $totalReady jars", color = GoldLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                        if (rawMat > 0 && jarsQty > 0 && costPerJar >= 0 && totalReady > 0) {
                            onSave(rawMat, jarsQty, costPerJar, totalReady, date, notes)
                        }
                    },
                    enabled = rawMat > 0 && jarsQty > 0 && totalReady > 0,
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

// 5. Add Task ModalBottomSheet (Schedule)
@OptIn(ExperimentalMaterial3Api::class)
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
            BottomSheetHeader(title = "Naya Task", onDismiss = onDismiss)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(14.dp))

            Text("Category", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(2).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 11.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = NavyDark,
                            containerColor = NavyLight,
                            labelColor = White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (category == cat) Gold else NavyBorder,
                            selectedBorderColor = Gold,
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
                        label = { Text(cat, fontSize = 11.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = NavyDark,
                            containerColor = NavyLight,
                            labelColor = White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (category == cat) Gold else NavyBorder,
                            selectedBorderColor = Gold,
                            enabled = true,
                            selected = category == cat
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
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
            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reminder Time (Optional)") },
                    placeholder = { Text("Waqt chunein...", color = TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = {
                            showTimePickerDialog(context) { reminderTime = it }
                        }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Select Time", tint = Gold)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showTimePickerDialog(context) { reminderTime = it }
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
                        if (title.isNotBlank()) {
                            onSave(title.trim(), category, description.trim(), reminderTime)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
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
