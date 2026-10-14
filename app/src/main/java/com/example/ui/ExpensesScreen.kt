package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
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

    var selectedPeriod by remember { mutableStateOf("Monthly") } // "Daily", "Weekly", "Monthly", "All"
    var selectedCategory by remember { mutableStateOf("All") } // "All", "Raw Material", "Marketing", "Logistics", "Others"

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }

    val todayStr = remember { dateFormat.format(Date()) }
    val currentMonthStr = remember { monthFormat.format(Date()) }

    // Week start date (last 7 days)
    val weekStartStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        dateFormat.format(cal.time)
    }

    // Filtered expenses by period & category
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

    // Summary Calculations
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
                title = { Text("Expenses Registry", fontWeight = FontWeight.Bold, color = Gold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Gold,
                    actionIconContentColor = White
                ),
                actions = {
                    IconButton(onClick = onAddExpense) {
                        Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Gold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = Gold,
                contentColor = NavyDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
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
            // Period Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyMedium, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Daily", "Weekly", "Monthly", "All").forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Gold else Color.Transparent,
                            onClick = { selectedPeriod = period }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = period,
                                    color = if (isSelected) NavyDark else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2x2 Summary Grid
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
                            colors = CardDefaults.cardColors(containerColor = NavyMedium),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Total Expense", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatMoney(totalExpensePeriod),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }

                        // Total Revenue Card
                        Card(
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = NavyMedium),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Total Revenue", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatMoney(totalRevenuePeriod),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
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
                            colors = CardDefaults.cardColors(containerColor = NavyMedium),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Expense Ratio", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text("$expenseRatioPercent%", fontWeight = FontWeight.Bold, color = if (expenseRatioPercent > 50) ExpenseRed else GoldLight, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { expenseRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (expenseRatioPercent > 50) ExpenseRed else GoldLight,
                                    trackColor = NavyLight,
                                )
                            }
                        }

                        // Top Category Card
                        Card(
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = NavyMedium),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Top Category", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    topCategory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight,
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
                            label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold,
                                selectedLabelColor = NavyDark,
                                containerColor = NavyMedium,
                                labelColor = White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) Gold else NavyBorder,
                                selectedBorderColor = Gold,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Expenses List
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No expenses found for this selection.", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(filteredExpenses) { expense ->
                    val categoryColor = when (expense.category.lowercase()) {
                        "raw material" -> ProfitGreen
                        "marketing" -> Gold
                        "logistics" -> Color(0xFF64B5F6)
                        else -> WhiteSoft
                    }
                    val categoryBg = when (expense.category.lowercase()) {
                        "raw material" -> AlertGreenBg
                        "marketing" -> AlertYellowBg
                        "logistics" -> AlertBlueBg
                        else -> NavyLight
                    }

                    Card(
                        border = BorderStroke(1.dp, CardBorder),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        shape = RoundedCornerShape(12.dp),
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
                                Text(expense.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = White)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = categoryBg,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = expense.category,
                                            color = categoryColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(expense.date, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                if (expense.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(expense.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                            Text(
                                formatMoney(expense.amount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
