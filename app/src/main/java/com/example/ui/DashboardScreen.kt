package com.example.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    appViewModel: AppViewModel,
    dashboardViewModel: DashboardViewModel,
    onNavigateToPricing: () -> Unit = {},
    onAddCustomer: () -> Unit,
    onAddBalance: () -> Unit,
    onAddExpense: () -> Unit,
    onNewOrder: () -> Unit
) {
    val state by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing by appViewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncedTime by appViewModel.lastSyncedTime.collectAsStateWithLifecycle()
    val pendingUploads by appViewModel.pendingUploadsCount.collectAsStateWithLifecycle()
    val isOnline = remember(isSyncing, lastSyncedTime, pendingUploads) { appViewModel.syncManager.isOnline() }

    // First two months expanded by default (current month & previous month), remaining 4 compact
    var expandedSet by rememberSaveable { mutableStateOf(setOf(0, 1)) }

    Scaffold(
        containerColor = Color.White
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingVals),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. DASHBOARD HERO HEADER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(DarkBlueDeep, DarkBlue)
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            // Top Row: Greetings & Business Title + Sync Status Chip & Price Calculator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Assalam o Alaikum",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Yellow
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Husn e Zar",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Sync Status Chip
                                    val (dotColor, syncLabel) = formatRelativeSyncTime(
                                        lastSyncedTime = lastSyncedTime,
                                        isOnline = isOnline,
                                        pendingUploads = pendingUploads,
                                        isSyncing = isSyncing
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(dotColor)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = syncLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    IconButton(
                                        onClick = onNavigateToPricing,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Calculate,
                                            contentDescription = "Price Calculator",
                                            tint = Yellow,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Date
                            Text(
                                text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.75f)
                            )

                            Spacer(Modifier.height(14.dp))

                            // Aaj ki Received Income Label & Big Yellow Number
                            Text(
                                text = "Aaj ki Received Income",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatMoney(state.todayReceivedIncome),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Yellow,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Thin yellow progress indicator when sync is active
                        if (isSyncing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.BottomCenter),
                                color = Yellow,
                                trackColor = DarkBlueDeep
                            )
                        }
                    }
                }
            }

            // B. QUICK ACTIONS ROW
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    QuickActionButton(
                        icon = Icons.Default.PersonAdd,
                        label = "Add Customer",
                        onClick = onAddCustomer
                    )
                    QuickActionButton(
                        icon = Icons.Default.ShoppingCart,
                        label = "New Order",
                        onClick = onNewOrder
                    )
                    QuickActionButton(
                        icon = Icons.Default.Receipt,
                        label = "Add Expense",
                        onClick = onAddExpense
                    )
                    QuickActionButton(
                        icon = Icons.Default.Payments,
                        label = "Record Payment",
                        onClick = onAddBalance
                    )
                }
            }

            // C. ALERTS SECTION: RED if any alert, GREEN if none
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
            }

            // D. THIS MONTH OVERVIEW
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
            }

            // E. TODAY'S SUMMARY
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
            }

            // F. 6 MONTHS REPORT: CANVAS BAR CHART & CARDS
            if (state.historicalTrends.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Last 6 Months Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue
                        )
                        Spacer(Modifier.height(10.dp))
                        // Canvas Financial Comparison Bar Chart with animated bars
                        FinancialComparisonBarChart(reports = state.historicalTrends)
                    }
                }

                // Month Cards: First 2 expanded by default, remaining compact with click-to-expand
                items(state.historicalTrends.size) { index ->
                    val report = state.historicalTrends[index]
                    val isExpanded = index in expandedSet
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (isExpanded) {
                            MonthReportCard(
                                report = report,
                                index = index,
                                isCurrentMonth = index == 0,
                                isExpanded = true,
                                onToggleExpand = {
                                    expandedSet = expandedSet - index
                                }
                            )
                        } else {
                            CompactMonthReportCard(
                                report = report,
                                isCurrentMonth = index == 0,
                                onToggleExpand = {
                                    expandedSet = expandedSet + index
                                }
                            )
                        }
                    }
                }
            }

            // G. WEEKLY CREDIT ALERTS
            if (state.weeklyCreditAlerts.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Weekly Credit Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkBlue)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(state.weeklyCreditAlerts) { alert ->
                    val isOverdue = alert.isOverdue
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// -------------------------------------------------------------
// Quick Action Button with Smooth Touch Scale Animation
// -------------------------------------------------------------

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.92f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "quickActionScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(Yellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DarkBlue,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBlue,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2
        )
    }
}

// -------------------------------------------------------------
// Canvas Financial Comparison Bar Chart with Animated Bars
// -------------------------------------------------------------

@Composable
fun FinancialComparisonBarChart(reports: List<MonthReport>) {
    if (reports.isEmpty()) return

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(reports) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val maxVal = remember(reports) {
        maxOf(1.0, reports.maxOfOrNull { maxOf(it.totalSale, it.totalExpense) } ?: 1.0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with title & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Comparison",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(DarkBlue, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("Sale", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBlue)

                    Spacer(Modifier.width(10.dp))

                    Box(modifier = Modifier.size(10.dp).background(Yellow, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 4.dp.toPx()
                val itemCount = reports.size
                val groupWidth = canvasWidth / itemCount
                val barWidth = minOf(12.dp.toPx(), groupWidth * 0.28f)
                val spacing = 3.dp.toPx()

                reports.forEachIndexed { i, report ->
                    val groupCenterX = i * groupWidth + (groupWidth / 2)
                    val saleHeight = (report.totalSale / maxVal * canvasHeight * animProgress.value).toFloat()
                    val expenseHeight = (report.totalExpense / maxVal * canvasHeight * animProgress.value).toFloat()

                    // Draw Sale bar (Dark Blue)
                    val saleLeft = groupCenterX - barWidth - (spacing / 2)
                    drawRoundRect(
                        color = DarkBlue,
                        topLeft = Offset(saleLeft, canvasHeight - saleHeight),
                        size = Size(barWidth, maxOf(saleHeight, 2.dp.toPx())),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw Expense bar (Yellow)
                    val expenseLeft = groupCenterX + (spacing / 2)
                    drawRoundRect(
                        color = Yellow,
                        topLeft = Offset(expenseLeft, canvasHeight - expenseHeight),
                        size = Size(barWidth, maxOf(expenseHeight, 2.dp.toPx())),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Month Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                reports.forEach { report ->
                    val shortName = report.monthLabel.split(" ").firstOrNull()?.take(3) ?: report.monthLabel.take(3)
                    Text(
                        text = shortName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Compact Month Card (Click to expand)
// -------------------------------------------------------------

@Composable
fun CompactMonthReportCard(
    report: MonthReport,
    isCurrentMonth: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onToggleExpand),
        colors = CardDefaults.cardColors(containerColor = DarkBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = report.monthLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                if (isCurrentMonth) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Yellow,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Current",
                            color = DarkBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Revenue: ${formatMoney(report.totalRevenue)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (report.totalRevenue >= 0) SuccessGreen else AlertRed
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Yellow,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Expanded Month Report Card
// -------------------------------------------------------------

@Composable
fun MonthReportCard(
    report: MonthReport,
    index: Int,
    isCurrentMonth: Boolean,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null
) {
    val isAllZero = report.jarsCreated == 0 && report.totalExpense == 0.0 && report.totalSale == 0.0 && report.totalRevenue == 0.0
    val hasAnimated = rememberSaveable(report.monthLabel) { mutableStateOf(false) }
    val animProgress = remember { Animatable(if (hasAnimated.value) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!hasAnimated.value) {
            delay(index * 70L)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
            hasAnimated.value = true
        }
    }

    val progressValue = animProgress.value
    val displayedJars = (report.jarsCreated * progressValue).toInt()
    val displayedSale = report.totalSale * progressValue
    val displayedExpense = report.totalExpense * progressValue
    val displayedRevenue = report.totalRevenue * progressValue

    val expenseRatio = if (report.totalSale > 0.0) {
        (report.totalExpense / report.totalSale).toFloat()
    } else {
        0f
    }
    val clampedRatio = expenseRatio.coerceIn(0f, 1f)
    val progressColor = if (report.totalExpense <= report.totalSale) SuccessGreen else AlertRed
    val percentageLabel = if (report.totalSale > 0.0) {
        "Expense = ${(expenseRatio * 100).toInt()}% of Sale"
    } else {
        "Expense = 0% of Sale"
    }

    val miniBoxBg = Color(0xFF143870)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .graphicsLayer {
                val currentP = if (hasAnimated.value) 1f else progressValue
                alpha = if (isAllZero) 0.7f * currentP else currentP
                translationY = if (hasAnimated.value) 0f else (1f - currentP) * 35f
            },
        colors = CardDefaults.cardColors(containerColor = DarkBlue),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onToggleExpand != null) Modifier.clickable { onToggleExpand() } else Modifier),
                color = Yellow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = report.monthLabel,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue,
                            fontSize = 16.sp
                        )
                        if (isCurrentMonth) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = DarkBlue,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Current",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (onToggleExpand != null) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Collapse",
                            tint = DarkBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = miniBoxBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(
                                color = Yellow.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Yellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Jars Created",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$displayedJars",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Yellow
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = miniBoxBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Total Sale",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = formatMoney(displayedSale),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = miniBoxBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(
                                color = AlertRed.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = AlertRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Total Expense",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = formatMoney(displayedExpense),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertRed
                            )
                        }
                    }

                    val isRevenuePositive = report.totalRevenue >= 0.0
                    val revenueColor = if (isRevenuePositive) SuccessGreen else AlertRed
                    val revenueBorder = if (isRevenuePositive) SuccessGreen.copy(alpha = 0.6f) else AlertRed.copy(alpha = 0.6f)
                    val revenueIcon = if (isRevenuePositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = miniBoxBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, revenueBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(
                                color = revenueColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = revenueIcon,
                                        contentDescription = null,
                                        tint = revenueColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Total Revenue",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = revenueColor
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = formatMoney(displayedRevenue),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = revenueColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = percentageLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { clampedRatio * (if (hasAnimated.value) 1f else progressValue) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Alert and Metric Components
// -------------------------------------------------------------

@Composable
fun AlertCard(
    title: String,
    message: String,
    containerColor: Color,
    borderColor: Color,
    iconColor: Color,
    icon: ImageVector
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

// -------------------------------------------------------------
// Relative Sync Time Formatter
// -------------------------------------------------------------

fun formatRelativeSyncTime(
    lastSyncedTime: Long,
    isOnline: Boolean,
    pendingUploads: Int,
    isSyncing: Boolean
): Pair<Color, String> {
    if (isSyncing) {
        return Pair(Yellow, "Syncing...")
    }
    if (!isOnline) {
        return Pair(Yellow, "Offline - $pendingUploads pending")
    }
    if (pendingUploads > 0) {
        return Pair(Yellow, "$pendingUploads pending")
    }
    if (lastSyncedTime == 0L) {
        return Pair(SuccessGreen, "Ready to sync")
    }
    val diffSec = (System.currentTimeMillis() - lastSyncedTime) / 1000
    val text = when {
        diffSec < 60 -> "Synced just now"
        diffSec < 3600 -> "Synced ${diffSec / 60}m ago"
        diffSec < 86400 -> "Synced ${diffSec / 3600}h ago"
        else -> "Synced"
    }
    return Pair(SuccessGreen, text)
}
