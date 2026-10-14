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
                title = { Text("Husn e Zar", fontWeight = FontWeight.Bold, color = Yellow) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    actionIconContentColor = Yellow
                ),
                actions = {
                    IconButton(onClick = onNavigateToPricing) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = "Price Calculator",
                            tint = Yellow
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onAddCustomer,
                    containerColor = Yellow,
                    contentColor = DarkBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                }
                SmallFloatingActionButton(
                    onClick = onAddBalance,
                    containerColor = Yellow,
                    contentColor = DarkBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = "Add Payment")
                }
                FloatingActionButton(
                    onClick = onAddExpense,
                    containerColor = Yellow,
                    contentColor = DarkBlue
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = "Add Expense")
                }
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
                        color = DarkBlue
                    )
                    Text(
                        text = state.currentTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            // B. Alerts Section: RED if any alert, GREEN if none
            item {
                val hasAlerts = state.lowStockWarning || state.overdueDebtors.isNotEmpty() || state.uncheckedTasksReminder

                if (hasAlerts) {
                    if (state.lowStockWarning) {
                        AlertCard(
                            title = "Low Stock Warning",
                            message = "Total remaining available jars in stock is 5 or less.",
                            containerColor = AlertRedBg,
                            borderColor = AlertRed,
                            iconColor = AlertRed,
                            icon = Icons.Default.Warning
                        )
                    }
                    state.overdueDebtors.forEach { msg ->
                        AlertCard(
                            title = "Overdue Debtor Alert",
                            message = msg,
                            containerColor = AlertRedBg,
                            borderColor = AlertRed,
                            iconColor = AlertRed,
                            icon = Icons.Default.MoneyOff
                        )
                    }
                    if (state.uncheckedTasksReminder) {
                        AlertCard(
                            title = "Daily Progress Reminder",
                            message = "You have unchecked tasks in your Daily Progress list today.",
                            containerColor = AlertRedBg,
                            borderColor = AlertRed,
                            iconColor = AlertRed,
                            icon = Icons.Default.Checklist
                        )
                    }
                } else {
                    AlertCard(
                        title = "No Alert - Sab theek hai",
                        message = "Tamam operations, inventory stock aur payments theek hain.",
                        containerColor = AlertGreenBg,
                        borderColor = SuccessGreen,
                        iconColor = SuccessGreen,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            // C. This Month Overview
            item {
                Text(
                    "This Month Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Total Sales", formatMoney(state.currentMonthTotalSales), Modifier.weight(1f), valueColor = Color.White)
                    MetricCard("Total Expense", formatMoney(state.currentMonthTotalExpense), Modifier.weight(1f), valueColor = AlertRed)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Jars Sold", "${state.currentMonthJarsSold}", Modifier.weight(1f), valueColor = Yellow)
                    MetricCard("Net Profit", formatMoney(state.currentMonthNetProfit), Modifier.weight(1f), valueColor = if (state.currentMonthNetProfit >= 0) SuccessGreen else AlertRed)
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Credit Exposure Metrics", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Yellow)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Unpaid Customers:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text("${state.currentMonthUnpaidCustomersCount}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Balance Owed:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text(formatMoney(state.currentMonthRemainingBalanceOwed), color = AlertRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("New Customers Joined:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text("${state.currentMonthNewCustomers}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // D. Today's Summary
            item {
                Text("Today's Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkBlue)
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Income Received:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text(formatMoney(state.todayReceivedIncome), color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Jars Sold:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text("${state.todayJarsSoldAmount}", color = Yellow, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expenses Outgoing:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text(formatMoney(state.todayOutgoingExpense), color = AlertRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expected Remaining Balance:", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Text(formatMoney(state.todayExpectedRemainingBalance), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // F. Last 6 Months Report
            if (state.historicalTrends.isNotEmpty()) {
                item {
                    Text("Last 6 Months Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.historicalTrends) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        border = BorderStroke(1.dp, Yellow.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(report.monthLabel, fontWeight = FontWeight.Bold, color = Yellow, fontSize = 15.sp)
                                Surface(
                                    color = Yellow.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "${report.jarsCreated} Jars Created",
                                        color = Yellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Sale", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(formatMoney(report.totalSale), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column {
                                    Text("Total Expense", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(formatMoney(report.totalExpense), color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Revenue", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(formatMoney(report.totalRevenue), color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // G. Weekly Credit Alert List
            if (state.weeklyCreditAlerts.isNotEmpty()) {
                item {
                    Text("Weekly Credit Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkBlue)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.weeklyCreditAlerts) { alert ->
                    val isOverdue = alert.isOverdue
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        border = BorderStroke(1.dp, if (isOverdue) AlertRed else SuccessGreen),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alert.customerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Due: ${alert.dueDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverdue) AlertRed else Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatMoney(alert.pendingAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverdue) AlertRed else SuccessGreen,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = if (isOverdue) AlertRedBg else AlertGreenBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (isOverdue) AlertRed else SuccessGreen)
                                ) {
                                    Text(
                                        if (isOverdue) "Overdue" else "Pending",
                                        color = if (isOverdue) AlertRed else SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = iconColor, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(message, color = iconColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkBlue),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}
