package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class DashboardState(
    val currentTime: String = "",
    val lowStockWarning: Boolean = false,
    val overdueDebtors: List<String> = emptyList(), // Messages
    val uncheckedTasksReminder: Boolean = false,
    
    // This month
    val currentMonthTotalSales: Double = 0.0,
    val currentMonthTotalExpense: Double = 0.0,
    val currentMonthJarsSold: Int = 0,
    val currentMonthNetProfit: Double = 0.0,
    val currentMonthUnpaidCustomersCount: Int = 0,
    val currentMonthRemainingBalanceOwed: Double = 0.0,
    val currentMonthNewCustomers: Int = 0,

    // Today summary
    val todayReceivedIncome: Double = 0.0,
    val todayJarsSoldAmount: Int = 0,
    val todayOutgoingExpense: Double = 0.0,
    val todayExpectedRemainingBalance: Double = 0.0,

    // Last 6 Months report
    val historicalTrends: List<MonthReport> = emptyList(),

    // Weekly Credit Alert List
    val weeklyCreditAlerts: List<CreditAlert> = emptyList()
)

data class MonthReport(
    val monthLabel: String,
    val jarsCreated: Int,
    val totalExpense: Double,
    val totalSale: Double,
    val totalRevenue: Double
)

data class CreditAlert(
    val dueDate: String,
    val customerName: String,
    val pendingAmount: Double,
    val isOverdue: Boolean
)

class DashboardViewModel(private val appViewModel: AppViewModel) : ViewModel() {
    
    private val timeFormat = SimpleDateFormat("EEEE, MMMM d, yyyy - hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val displayMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    private val _currentTime = MutableStateFlow(timeFormat.format(Date()))
    
    val uiState: StateFlow<DashboardState> = combine(
        _currentTime,
        appViewModel.customers,
        appViewModel.orders,
        combine(
            appViewModel.inventoryBatches,
            appViewModel.expenses,
            appViewModel.dailyTasks,
            appViewModel.customerPayments
        ) { batches, expenses, tasks, payments ->
            Quad(batches, expenses, tasks, payments)
        }
    ) { time, customers, orders, quad ->
        computeDashboardState(time, customers, orders, quad.first, quad.second, quad.third, quad.fourth)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


    init {
        // Simple tick for clock (e.g. every minute)
        /*
        viewModelScope.launch {
            while(isActive) {
                delay(60_000)
                _currentTime.value = timeFormat.format(Date())
            }
        }*/
    }

    private fun computeDashboardState(
        time: String,
        customers: List<Customer>,
        orders: List<Order>,
        batches: List<InventoryBatch>,
        expenses: List<Expense>,
        tasks: List<DailyTask>,
        payments: List<CustomerPayment>
    ): DashboardState {
        val todayStr = dateFormat.format(Date())
        val currentMonthStr = monthFormat.format(Date())
        
        // Low Stock Warning
        val totalJarsProduced = batches.sumOf { it.jarsProduced }
        val totalJarsSold = orders.sumOf { it.jarsQuantity }
        val availableJars = totalJarsProduced - totalJarsSold
        val lowStockWarning = availableJars <= 5

        // Overdue Debtors
        val nowMs = Date().time
        val overdueDebtors = mutableListOf<String>()
        customers.filter { it.pendingBalance > 0 }.forEach { customer ->
            // Find latest unpaid transaction (could simplify to earliest unpaid order)
            val customerOrders = orders.filter { it.customerId == customer.id && it.pendingAmount > 0 }
            val oldestUnpaidOrder = customerOrders.minByOrNull { it.orderDate }
            if (oldestUnpaidOrder != null) {
                val orderDate = dateFormat.parse(oldestUnpaidOrder.orderDate)
                if (orderDate != null) {
                    val diffMs = nowMs - orderDate.time
                    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
                    if (diffDays > 10) {
                        overdueDebtors.add("Customer ${customer.name} balance PKR ${customer.pendingBalance} pending for over 10 days!")
                    }
                }
            }
        }

        // Daily Progress
        val todaysTasks = tasks.filter { it.date == todayStr }
        val uncheckedTasksReminder = todaysTasks.isNotEmpty() && todaysTasks.any { !it.isCompleted }

        // This Month
        val thisMonthOrders = orders.filter { it.orderDate.startsWith(currentMonthStr) }
        val thisMonthExpenses = expenses.filter { it.date.startsWith(currentMonthStr) }
        val currentMonthTotalSales = thisMonthOrders.sumOf { it.totalAmount }
        val currentMonthTotalExpense = thisMonthExpenses.sumOf { it.amount }
        val currentMonthJarsSold = thisMonthOrders.sumOf { it.jarsQuantity }
        val currentMonthNetProfit = thisMonthOrders.sumOf { it.netProfit }
        
        // Credit exposure (customers who bought this month and didn't pay)
        val unpaidThisMonth = thisMonthOrders.filter { it.pendingAmount > 0 }
        val currentMonthUnpaidCustomersCount = unpaidThisMonth.map { it.customerId }.distinct().size
        val currentMonthRemainingBalanceOwed = unpaidThisMonth.sumOf { it.pendingAmount }
        val currentMonthNewCustomers = customers.count { it.joiningDate.startsWith(currentMonthStr) }

        // Today
        val todayOrders = orders.filter { it.orderDate == todayStr }
        val todayPayments = payments.filter { it.date == todayStr }
        val todayExpenses = expenses.filter { it.date == todayStr }
        
        val todayReceivedIncome = todayOrders.sumOf { it.paidAmount } + todayPayments.sumOf { it.amount }
        val todayJarsSoldAmount = todayOrders.sumOf { it.jarsQuantity }
        val todayOutgoingExpense = todayExpenses.sumOf { it.amount }
        val todayExpectedRemainingBalance = todayOrders.sumOf { it.pendingAmount }

        // Last 6 Months Report (current month included, newest on top)
        val reports = mutableListOf<MonthReport>()
        for (i in 0 until 6) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val monthKey = monthFormat.format(cal.time)
            val monthLabel = displayMonthFormat.format(cal.time)
            val jarsCreated = batches.filter { it.date.startsWith(monthKey) }.sumOf { it.jarsProduced }
            val mExpenses = expenses.filter { it.date.startsWith(monthKey) }.sumOf { it.amount }
            val mOrders = orders.filter { it.orderDate.startsWith(monthKey) }
            val mSales = mOrders.sumOf { it.totalAmount }
            val mPayments = payments.filter { it.date.startsWith(monthKey) }.sumOf { it.amount }
            val mRevenue = mOrders.sumOf { it.paidAmount } + mPayments

            reports.add(
                MonthReport(
                    monthLabel = monthLabel,
                    jarsCreated = jarsCreated,
                    totalExpense = mExpenses,
                    totalSale = mSales,
                    totalRevenue = mRevenue
                )
            )
        }

        // Weekly credit alerts (unpaid orders)
        val alerts = orders.filter { it.pendingAmount > 0 }.sortedBy { it.dueDate }.take(10).map {
            val due = dateFormat.parse(it.dueDate)
            val isOverdue = due != null && due.before(Date())
            CreditAlert(it.dueDate, it.customerName, it.pendingAmount, isOverdue)
        }

        return DashboardState(
            currentTime = time,
            lowStockWarning = lowStockWarning,
            overdueDebtors = overdueDebtors,
            uncheckedTasksReminder = uncheckedTasksReminder,
            currentMonthTotalSales = currentMonthTotalSales,
            currentMonthTotalExpense = currentMonthTotalExpense,
            currentMonthJarsSold = currentMonthJarsSold,
            currentMonthNetProfit = currentMonthNetProfit,
            currentMonthUnpaidCustomersCount = currentMonthUnpaidCustomersCount,
            currentMonthRemainingBalanceOwed = currentMonthRemainingBalanceOwed,
            currentMonthNewCustomers = currentMonthNewCustomers,
            todayReceivedIncome = todayReceivedIncome,
            todayJarsSoldAmount = todayJarsSoldAmount,
            todayOutgoingExpense = todayOutgoingExpense,
            todayExpectedRemainingBalance = todayExpectedRemainingBalance,
            historicalTrends = reports,
            weeklyCreditAlerts = alerts
        )
    }
}
