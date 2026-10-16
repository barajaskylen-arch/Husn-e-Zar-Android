package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    appViewModel: AppViewModel,
    onAddExpense: () -> Unit
) {
    val allExpenses by appViewModel.expenses.collectAsStateWithLifecycle()
    val allOrders by appViewModel.orders.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf("Monthly") }
    var selectedCategory by remember { mutableStateOf("All") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }

    val todayStr = remember { dateFormat.format(Date()) }
    val currentMonthStr = remember { monthFormat.format(Date()) }

    val weekStartStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        dateFormat.format(cal.time)
    }

    val periodExpenses = remember(allExpenses, selectedPeriod) {
        when (selectedPeriod) {
            "Daily" -> allExpenses.filter { it.date == todayStr }
            "Weekly" -> allExpenses.filter { it.date >= weekStartStr }
            "Monthly" -> allExpenses.filter { it.date.startsWith(currentMonthStr) }
            else -> allExpenses
        }
    }

    val periodOrders = remember(allOrders, selectedPeriod) {
        when (selectedPeriod) {
            "Daily" -> allOrders.filter { it.orderDate == todayStr }
            "Weekly" -> allOrders.filter { it.orderDate >= weekStartStr }
            "Monthly" -> allOrders.filter { it.orderDate.startsWith(currentMonthStr) }
            else -> allOrders
        }
    }

    val filteredExpenses = remember(periodExpenses, selectedCategory) {
        if (selectedCategory == "All") {
            periodExpenses
        } else {
            periodExpenses.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val totalExpensePeriod = periodExpenses.sumOf { it.amount }
    val totalRevenuePeriod = periodOrders.sumOf { it.totalAmount }
    val expenseRatio = if (totalRevenuePeriod > 0) {
        (totalExpensePeriod / totalRevenuePeriod).toFloat().coerceIn(0f, 1f)
    } else if (totalExpensePeriod > 0) 1f else 0f
    val expenseRatioPercent = if (totalRevenuePeriod > 0) {
        ((totalExpensePeriod / totalRevenuePeriod) * 100).toInt()
    } else if (totalExpensePeriod > 0) 100 else 0

    val topCategory = remember(periodExpenses) {
        if (periodExpenses.isEmpty()) "None"
        else {
            periodExpenses.groupBy { it.category }
                .maxByOrNull { entry -> entry.value.sumOf { it.amount } }
                ?.key ?: "None"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses Registry (${filteredExpenses.size})", fontWeight = FontWeight.Bold, color = Yellow) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    actionIconContentColor = Yellow
                ),
                actions = {
                    IconButton(
                        onClick = onAddExpense,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Yellow)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = Yellow,
                contentColor = DarkBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
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
            // Period Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F4F8), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Daily", "Weekly", "Monthly", "All").forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Yellow else Color.Transparent,
                            onClick = { selectedPeriod = period }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = period,
                                    color = if (isSelected) DarkBlue else DarkBlue.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2x2 Summary Grid (Dark Blue Cards, 20dp corners)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Expense Card
                        Card(
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Total Expense", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatMoney(totalExpensePeriod),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                            }
                        }

                        // Total Revenue Card
                        Card(
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Total Revenue", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatMoney(totalRevenuePeriod),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Expense Ratio Card
                        Card(
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Expense Ratio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
                                    Text("$expenseRatioPercent%", fontWeight = FontWeight.Bold, color = if (expenseRatioPercent > 50) AlertRed else Yellow, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { expenseRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (expenseRatioPercent > 50) AlertRed else Yellow,
                                    trackColor = DarkBlueDeep,
                                )
                            }
                        }

                        // Top Category Card
                        Card(
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Top Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    topCategory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Yellow,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                val categories = listOf("All", "Raw Material", "Marketing", "Logistics", "Others")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Yellow,
                                selectedLabelColor = DarkBlue,
                                containerColor = Color(0xFFF0F4F8),
                                labelColor = DarkBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) Yellow else DarkBlue,
                                selectedBorderColor = Yellow,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Expenses List
            if (filteredExpenses.isEmpty()) {
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
                                text = "Koi Kharcha Nahi Hai",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Pehla kharcha darj karein taake expenses record shuru hon.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = onAddExpense,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Yellow,
                                    contentColor = DarkBlue
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pehla Kharcha Darj Karein", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBlue)
                            }
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    Box(modifier = Modifier.animateItem()) {
                        val categoryColor = when (expense.category.lowercase()) {
                            "raw material" -> SuccessGreen
                            "marketing" -> Yellow
                            "logistics" -> Color(0xFF64B5F6)
                            else -> Color.White
                        }
                        val categoryBg = when (expense.category.lowercase()) {
                            "raw material" -> AlertGreenBg
                            "marketing" -> DarkBlueDeep
                            "logistics" -> Color(0xFF0D253F)
                            else -> DarkBlueDeep
                        }
                        val categoryIcon: ImageVector = when (expense.category.lowercase()) {
                            "raw material" -> Icons.Default.Inventory2
                            "marketing" -> Icons.Default.Campaign
                            "logistics" -> Icons.Default.LocalShipping
                            else -> Icons.Default.Category
                        }

                        Card(
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = expense.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Category color and icon chip
                                        Surface(
                                            color = categoryBg,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.7f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = categoryIcon,
                                                    contentDescription = null,
                                                    tint = categoryColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(Modifier.width(5.dp))
                                                Text(
                                                    text = expense.category,
                                                    color = categoryColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = expense.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (expense.description.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = expense.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                // Amount right par bold red
                                Text(
                                    text = formatMoney(expense.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = AlertRed
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
