package com.example.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

@Composable
fun TappableDateRow(
    label: String,
    dateValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DarkBlue)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = DarkBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = DarkBlue.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateValue.ifBlank { "Tareekh chunein" },
                        fontSize = 14.sp,
                        color = DarkBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = "Change Date",
                tint = DarkBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

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
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp * 0.88f).dp
    val coroutineScope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val dismissWithAnimation: () -> Unit = {
        coroutineScope.launch {
            visible = false
            delay(200)
            onDismiss()
        }
    }

    val cancelWithAnimation: () -> Unit = {
        coroutineScope.launch {
            visible = false
            delay(200)
            onCancel()
        }
    }

    Dialog(
        onDismissRequest = dismissWithAnimation,
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
                    onClick = dismissWithAnimation
                ),
            contentAlignment = Alignment.Center
        ) {
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
                        .heightIn(max = maxDialogHeight)
                        .imePadding()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* prevent backdrop dismissal when clicking dialog */ },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxDialogHeight)
                    ) {
                        // Top Header (Dark Blue) - Fixed
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
                                    onClick = dismissWithAnimation,
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

                        // Scrollable fields (wraps content, scrolls when reaching max height)
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            content()
                        }

                        // Bottom Actions: Cancel (Dark Blue outline) and Save (Yellow filled, Dark Blue text) - Fixed
                        if (onSave != null) {
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = cancelWithAnimation,
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
    val haptic = LocalHapticFeedback.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf(getTodayDateString()) }

    var touchedName by remember { mutableStateOf(false) }
    var touchedPhone by remember { mutableStateOf(false) }
    var touchedAddress by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Customer",
        icon = Icons.Default.PersonAdd,
        saveEnabled = isValid,
        onSave = {
            if (isValid) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Customer save ho gaya! ✨", Toast.LENGTH_SHORT).show()
                onSave(name.trim(), phone.trim(), address.trim(), joiningDate)
                onDismiss()
            }
        }
    ) {
        Column {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; touchedName = true },
                label = { Text("Customer Name *", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DarkBlue) },
                isError = touchedName && name.isBlank(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedName && name.isBlank()) {
                Text(
                    text = "Customer ka naam likhna zaroori hai",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 15) { phone = it; touchedPhone = true } },
                label = { Text("Phone Number *", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = DarkBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = touchedPhone && phone.isBlank(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedPhone && phone.isBlank()) {
                Text(
                    text = "Sahih phone number likhna zaroori hai",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; touchedAddress = true },
                label = { Text("Address *", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = DarkBlue) },
                isError = touchedAddress && address.isBlank(),
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedAddress && address.isBlank()) {
                Text(
                    text = "Address likhna zaroori hai",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        TappableDateRow(
            label = "Joining Date",
            dateValue = joiningDate,
            onClick = {
                showDatePickerDialog(context, joiningDate) { joiningDate = it }
            }
        )
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
    val haptic = LocalHapticFeedback.current
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getTodayDateString()) }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var touchedAmount by remember { mutableStateOf(false) }

    val amtNum = amount.toDoubleOrNull() ?: 0.0
    val isValid = selectedCustomer != null && amtNum > 0.0

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Payment Record Karo",
        icon = Icons.Default.Payments,
        saveEnabled = isValid,
        onSave = {
            val custId = selectedCustomer?.id
            if (amtNum > 0 && custId != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Payment record ho gayi! ✨", Toast.LENGTH_SHORT).show()
                onSave(custId, amtNum, date, description)
                onDismiss()
            }
        }
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCustomer?.let { "${it.name} (Bal: Rs. ${it.pendingBalance})" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Customer *", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Customer chunein...", color = DarkBlue.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DarkBlue) },
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
                                Text("Phone: ${cust.phone} • Bal: Rs. ${cust.pendingBalance}", color = DarkBlue.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

        Column {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; touchedAmount = true },
                label = { Text("Amount Received *", fontWeight = FontWeight.Bold) },
                prefix = { Text("Rs. ", fontWeight = FontWeight.Bold, color = DarkBlue) },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = DarkBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = touchedAmount && amtNum <= 0.0,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedAmount && amtNum <= 0.0) {
                Text(
                    text = "Sahih raqam darj karein",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        TappableDateRow(
            label = "Payment Date",
            dateValue = date,
            onClick = {
                showDatePickerDialog(context, date) { date = it }
            }
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)", fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = DarkBlue) },
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
    val haptic = LocalHapticFeedback.current
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getTodayDateString()) }
    var category by remember { mutableStateOf("Raw Material") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Raw Material", "Marketing", "Logistics", "Others")

    var touchedTitle by remember { mutableStateOf(false) }
    var touchedAmount by remember { mutableStateOf(false) }

    val amtNum = amount.toDoubleOrNull() ?: 0.0
    val isValid = title.isNotBlank() && amtNum > 0.0

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Expense",
        icon = Icons.Default.Receipt,
        saveEnabled = isValid,
        onSave = {
            if (isValid) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Kharcha save ho gaya! ✨", Toast.LENGTH_SHORT).show()
                onSave(title.trim(), amtNum, date, category, description.trim())
                onDismiss()
            }
        }
    ) {
        Column {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; touchedTitle = true },
                label = { Text("Expense Title *", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = DarkBlue) },
                isError = touchedTitle && title.isBlank(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedTitle && title.isBlank()) {
                Text(
                    text = "Kharcha ka naam likhna zaroori hai",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; touchedAmount = true },
                label = { Text("Expense Amount *", fontWeight = FontWeight.Bold) },
                prefix = { Text("Rs. ", fontWeight = FontWeight.Bold, color = DarkBlue) },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = DarkBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = touchedAmount && amtNum <= 0.0,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedAmount && amtNum <= 0.0) {
                Text(
                    text = "Kharcha ki sahih raqam darj karein",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

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

        TappableDateRow(
            label = "Expense Date",
            dateValue = date,
            onClick = {
                showDatePickerDialog(context, date) { date = it }
            }
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)", fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = DarkBlue) },
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
    val haptic = LocalHapticFeedback.current
    var rawCost by remember { mutableStateOf("") }
    var emptyJarsData by remember { mutableStateOf("") }
    var jarCost by remember { mutableStateOf("") }
    var jarsProducedData by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getTodayDateString()) }
    var notes by remember { mutableStateOf("") }

    var touchedRaw by remember { mutableStateOf(false) }
    var touchedEmpty by remember { mutableStateOf(false) }
    var touchedProduced by remember { mutableStateOf(false) }

    val rawMat = rawCost.toDoubleOrNull() ?: 0.0
    val jarsQty = emptyJarsData.toIntOrNull() ?: 0
    val costPerJar = jarCost.toDoubleOrNull() ?: 0.0
    val totalReady = jarsProducedData.toIntOrNull() ?: 0

    val totalJarCost = jarsQty * costPerJar
    val totalBatchCost = rawMat + totalJarCost
    val costPerReadyJar = if (totalReady > 0) totalBatchCost / totalReady else 0.0

    val isValid = rawMat > 0 && jarsQty > 0 && totalReady > 0

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Production Batch",
        icon = Icons.Default.Build,
        saveEnabled = isValid,
        onSave = {
            if (isValid) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Batch save ho gaya! ✨", Toast.LENGTH_SHORT).show()
                onSave(rawMat, jarsQty, costPerJar, totalReady, date, notes)
                onDismiss()
            }
        }
    ) {
        Column {
            OutlinedTextField(
                value = rawCost,
                onValueChange = { rawCost = it; touchedRaw = true },
                label = { Text("Raw Material Cost *", fontWeight = FontWeight.Bold) },
                prefix = { Text("Rs. ", fontWeight = FontWeight.Bold, color = DarkBlue) },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = DarkBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = touchedRaw && rawMat <= 0.0,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedRaw && rawMat <= 0.0) {
                Text(
                    text = "Raw material ki laagat darj karein",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = emptyJarsData,
                onValueChange = { emptyJarsData = it; touchedEmpty = true },
                label = { Text("Empty Jars Quantity *", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = DarkBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = touchedEmpty && jarsQty <= 0,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedEmpty && jarsQty <= 0) {
                Text(
                    text = "Khali jars ki tadad darj karein",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        OutlinedTextField(
            value = jarCost,
            onValueChange = { jarCost = it },
            label = { Text("Cost Per Empty Jar", fontWeight = FontWeight.Bold) },
            prefix = { Text("Rs. ", fontWeight = FontWeight.Bold, color = DarkBlue) },
            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = DarkBlue) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Column {
            OutlinedTextField(
                value = jarsProducedData,
                onValueChange = { jarsProducedData = it; touchedProduced = true },
                label = { Text("Total Jars Ready *", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = DarkBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = touchedProduced && totalReady <= 0,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedProduced && totalReady <= 0) {
                Text(
                    text = "Tayar jars ki tadad darj karein",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        TappableDateRow(
            label = "Production Date",
            dateValue = date,
            onClick = {
                showDatePickerDialog(context, date) { date = it }
            }
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)", fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = DarkBlue) },
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
    val haptic = LocalHapticFeedback.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Social Media") }
    var description by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("") }
    val categories = listOf("Social Media", "Customer Follow-up", "Production", "Admin")

    var touchedTitle by remember { mutableStateOf(false) }

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Naya Task",
        icon = Icons.Default.Checklist,
        saveEnabled = title.isNotBlank(),
        onSave = {
            if (title.isNotBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Task save ho gaya! ✨", Toast.LENGTH_SHORT).show()
                onSave(title.trim(), category, description.trim(), reminderTime)
                onDismiss()
            }
        }
    ) {
        Column {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; touchedTitle = true },
                label = { Text("Task Title *", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = DarkBlue) },
                isError = touchedTitle && title.isBlank(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            if (touchedTitle && title.isBlank()) {
                Text(
                    text = "Task ka naam likhna zaroori hai",
                    color = AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

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
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = DarkBlue) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showTimePickerDialog(context) { reminderTime = it }
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, DarkBlue)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = DarkBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Reminder Time (Optional)",
                            fontSize = 11.sp,
                            color = DarkBlue.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reminderTime.ifBlank { "Waqt chunein..." },
                            fontSize = 14.sp,
                            color = DarkBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Time",
                    tint = DarkBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
