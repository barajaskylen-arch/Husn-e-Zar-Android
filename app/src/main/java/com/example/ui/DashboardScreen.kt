package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    appViewModel: AppViewModel,
    dashboardViewModel: DashboardViewModel,
    onNavigateToPricing: () -> Unit = {},
    onAddCustomer: () -> Unit,
    onAddBalance: () -> Unit,
    onAddExpense: () -> Unit
) {
    val state by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Husn e Zar", fontWeight = FontWeight.Bold, color = Gold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Gold,
                    actionIconContentColor = White
                ),
                actions = {
                    IconButton(onClick = onNavigateToPricing) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = "Price Calculator",
                            tint = Gold
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onAddCustomer,
                    containerColor = Gold,
                    contentColor = NavyDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                }
                SmallFloatingActionButton(
                    onClick = onAddBalance,
                    containerColor = Gold,
                    contentColor = NavyDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = "Add Payment")
                }
                FloatingActionButton(
                    onClick = onAddExpense,
                    containerColor = Gold,
                    contentColor = NavyDark
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = "Add Expense")
                }
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
            // A. Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    Text(
                        text = state.currentTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            // B. Today's Advanced Notification Container
            item {
                if (state.lowStockWarning) {
                    AlertCard(
                        title = "Low Stock Warning",
                        message = "Total remaining available jars in stock is 5 or less.",
                        containerColor = AlertRedBg,
                        borderColor = ExpenseRed,
                        iconColor = ExpenseRed,
                        icon = Icons.Default.Warning
                    )
                }
                state.overdueDebtors.forEach { msg ->
                    AlertCard(
                        title = "Overdue Debtor Alert",
                        message = msg,
                        containerColor = AlertRedBg,
                        borderColor = ExpenseRed,
                        iconColor = ExpenseRed,
                        icon = Icons.Default.MoneyOff
                    )
                }
                if (state.uncheckedTasksReminder) {
                    AlertCard(
                        title = "Daily Progress Reminder",
                        message = "You have unchecked tasks in your Daily Progress list today.",
                        containerColor = AlertYellowBg,
                        borderColor = GoldLight,
                        iconColor = GoldLight,
                        icon = Icons.Default.Checklist
                    )
                }
            }

            // C. This Month Overview
            item {
                Text("This Month Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Total Sales", formatMoney(state.currentMonthTotalSales), Modifier.weight(1f), valueColor = White)
                    MetricCard("Total Expense", formatMoney(state.currentMonthTotalExpense), Modifier.weight(1f), valueColor = ExpenseRed)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Jars Sold", "${state.currentMonthJarsSold}", Modifier.weight(1f), valueColor = GoldLight)
                    MetricCard("Net Profit", formatMoney(state.currentMonthNetProfit), Modifier.weight(1f), valueColor = if (state.currentMonthNetProfit >= 0) ProfitGreen else ExpenseRed)
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyMedium),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Credit Exposure Metrics", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Gold)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Unpaid Customers:", color = TextSecondary)
                            Text("${state.currentMonthUnpaidCustomersCount}", color = White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Balance Owed:", color = TextSecondary)
                            Text(formatMoney(state.currentMonthRemainingBalanceOwed), color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("New Customers Joined:", color = TextSecondary)
                            Text("${state.currentMonthNewCustomers}", color = ProfitGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // D. Today's Summary
            item {
                Text("Today's Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavyMedium),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Income Received:", color = TextSecondary)
                            Text(formatMoney(state.todayReceivedIncome), color = ProfitGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Jars Sold:", color = TextSecondary)
                            Text("${state.todayJarsSoldAmount}", color = GoldLight, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expenses Outgoing:", color = TextSecondary)
                            Text(formatMoney(state.todayOutgoingExpense), color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expected Remaining Balance:", color = TextSecondary)
                            Text(formatMoney(state.todayExpectedRemainingBalance), color = White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // F. Historical 3-Months Trend Comparison Cards
            if (state.historicalTrends.isNotEmpty()) {
                item {
                    Text("Historical Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.historicalTrends) { trend ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(trend.monthName, fontWeight = FontWeight.Bold, color = Gold)
                            Spacer(Modifier.height(4.dp))
                            Text("Sale: ${formatMoney(trend.totalSale)} | Profit: ${formatMoney(trend.netProfit)} | Jars: ${trend.jarsSold}", color = WhiteSoft, fontSize = 13.sp)
                        }
                    }
                }
            }

            // G. Weekly Credit Alert List
            if (state.weeklyCreditAlerts.isNotEmpty()) {
                item {
                    Text("Weekly Credit Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.weeklyCreditAlerts) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, if (alert.isOverdue) ExpenseRed else CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(alert.customerName, fontWeight = FontWeight.Bold, color = White)
                                Text("Due: ${alert.dueDate}", style = MaterialTheme.typography.bodySmall, color = if (alert.isOverdue) ExpenseRed else TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatMoney(alert.pendingAmount), fontWeight = FontWeight.Bold, color = ExpenseRed)
                                Spacer(Modifier.height(2.dp))
                                Surface(
                                    color = if (alert.isOverdue) AlertRedBg else AlertYellowBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (alert.isOverdue) ExpenseRed else GoldLight)
                                ) {
                                    Text(
                                        if (alert.isOverdue) "Overdue" else "Pending",
                                        color = if (alert.isOverdue) ExpenseRed else GoldLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AlertCard(
    title: String,
    message: String,
    containerColor: Color,
    borderColor: Color,
    iconColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = iconColor, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(message, color = WhiteSoft, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = White
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavyMedium),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}
