package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.MoreHoriz

// ... rest of class remains unchanged ...
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(appViewModel) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MyApplicationTheme {
                val isLoggedIn by appViewModel.isLoggedIn.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(
                        supabaseAuth = appViewModel.supabaseAuth,
                        onLoginSuccess = {
                            appViewModel.syncNow()
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White
                    ) {
                        var showCustomerModal by remember { mutableStateOf(false) }
                    var showBalanceModal by remember { mutableStateOf(false) }
                    var showExpenseModal by remember { mutableStateOf(false) }
                    var showOrderModal by remember { mutableStateOf(false) }

                    val customers by appViewModel.customers.collectAsState()
                    val customersViewModel: CustomersViewModel by viewModels {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return CustomersViewModel(appViewModel) as T
                            }
                        }
                    }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

                    val ordersViewModel: OrdersViewModel by viewModels {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return OrdersViewModel(appViewModel) as T
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) },
                        exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) },
                        popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) },
                        popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) }
                    ) {
                        composable("dashboard") {
                            Column(Modifier.fillMaxSize().background(Color.White)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DashboardScreen(
                                        appViewModel = appViewModel,
                                        dashboardViewModel = dashboardViewModel,
                                        onNavigateToPricing = { navController.navigate("pricing") },
                                        onAddCustomer = { showCustomerModal = true },
                                        onAddBalance = { showBalanceModal = true },
                                        onAddExpense = { showExpenseModal = true },
                                        onNewOrder = { showOrderModal = true }
                                    )
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("pricing") {
                            PricingScreen(
                                appViewModel = appViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("orders") {
                            Column(Modifier.fillMaxSize().background(Color.White)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OrdersScreen(appViewModel, ordersViewModel)
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("inventory") {
                            Column(Modifier.fillMaxSize().background(Color.White)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    InventoryScreen(appViewModel = appViewModel)
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("customers") {
                            Column(Modifier.fillMaxSize().background(Color.White)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CustomersScreen(
                                        appViewModel = appViewModel,
                                        customersViewModel = customersViewModel,
                                        onNavigateToDetails = { id -> navController.navigate("customer_details/$id") },
                                        onNavigateBack = { navController.popBackStack() },
                                        onAddCustomer = { showCustomerModal = true }
                                    )
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("expenses") {
                            Column(Modifier.fillMaxSize().background(Color.White)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ExpensesScreen(
                                        appViewModel = appViewModel,
                                        onAddExpense = { showExpenseModal = true }
                                    )
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("schedule") {
                            Column(Modifier.fillMaxSize().background(Color.White)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ScheduleScreen(appViewModel = appViewModel)
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("settings") {
                            Column(Modifier.fillMaxSize().background(Color.White)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SettingsScreen(appViewModel = appViewModel)
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("customer_details/{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: return@composable
                            val detailViewModel = androidx.lifecycle.viewmodel.compose.viewModel<CustomerDetailsViewModel>(
                                factory = CustomerDetailsViewModel.Factory(id, appViewModel)
                            )
                            CustomerDetailsScreen(
                                viewModel = detailViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    if (showCustomerModal) {
                        RegisterCustomerModal(
                            onDismiss = { showCustomerModal = false },
                            onSave = { name, phone, address, date ->
                                appViewModel.addCustomer(name, phone, address, date)
                            }
                        )
                    }

                    if (showBalanceModal) {
                        AddBalanceModal(
                            customers = customers,
                            onDismiss = { showBalanceModal = false },
                            onSave = { custId, amt, date, desc ->
                                appViewModel.recordCustomerPayment(custId, amt, date, desc)
                            }
                        )
                    }

                    if (showExpenseModal) {
                        AddExpenseModal(
                            onDismiss = { showExpenseModal = false },
                            onSave = { title, amt, date, cat, desc ->
                                appViewModel.addExpense(title, amt, date, cat, desc)
                            }
                        )
                    }

                    if (showOrderModal) {
                        val currentStock by appViewModel.currentAvailableStock.collectAsState()
                        val currentBaseUnitCost by appViewModel.currentBaseUnitCost.collectAsState()
                        AddOrderModal(
                            customers = customers,
                            currentStock = currentStock,
                            currentBaseCost = currentBaseUnitCost,
                            onDismiss = { showOrderModal = false },
                            onSave = { custId, custName, orderDate, jars, totalAmt, paidAmt, dueDate, desc ->
                                if (jars > currentStock) {
                                    android.widget.Toast.makeText(this@MainActivity, "Stock kam hai! Sirf $currentStock jars available hain.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    appViewModel.addOrder(
                                        date = orderDate,
                                        customerId = custId,
                                        customerName = custName,
                                        jarsQuantity = jars,
                                        totalAmount = totalAmt,
                                        paidAmount = paidAmt,
                                        dueDate = dueDate,
                                        description = desc,
                                        baseCostPerJar = currentBaseUnitCost
                                    )
                                    showOrderModal = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun AppBottomNavigation(currentRoute: String, navController: NavController) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val isMoreSelected = currentRoute in listOf("expenses", "schedule", "settings")

    val unselectedColor = Color(0xFF8A99AD)
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = DarkBlue,
        selectedTextColor = DarkBlue,
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor,
        indicatorColor = Yellow
    )

    Column {
        HorizontalDivider(
            color = Yellow,
            thickness = 1.dp
        )
        NavigationBar(
            containerColor = Color.White
        ) {
            // 1. Dashboard
            val dashSelected = currentRoute == "dashboard"
            val dashScale by animateFloatAsState(if (dashSelected) 1.12f else 1f, animationSpec = tween(200), label = "dashScale")
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Dashboard",
                        modifier = Modifier.graphicsLayer { scaleX = dashScale; scaleY = dashScale }
                    )
                },
                label = { Text("Dashboard", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) },
                selected = dashSelected,
                onClick = {
                    navController.navigate("dashboard") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = itemColors
            )

            // 2. Orders
            val ordersSelected = currentRoute == "orders"
            val ordersScale by animateFloatAsState(if (ordersSelected) 1.12f else 1f, animationSpec = tween(200), label = "ordersScale")
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Orders",
                        modifier = Modifier.graphicsLayer { scaleX = ordersScale; scaleY = ordersScale }
                    )
                },
                label = { Text("Orders", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) },
                selected = ordersSelected,
                onClick = {
                    navController.navigate("orders") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = itemColors
            )

            // 3. Customers
            val custSelected = currentRoute == "customers"
            val custScale by animateFloatAsState(if (custSelected) 1.12f else 1f, animationSpec = tween(200), label = "custScale")
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.People,
                        contentDescription = "Customers",
                        modifier = Modifier.graphicsLayer { scaleX = custScale; scaleY = custScale }
                    )
                },
                label = { Text("Customers", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) },
                selected = custSelected,
                onClick = {
                    navController.navigate("customers") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = itemColors
            )

            // 4. Inventory
            val invSelected = currentRoute == "inventory"
            val invScale by animateFloatAsState(if (invSelected) 1.12f else 1f, animationSpec = tween(200), label = "invScale")
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Inventory",
                        modifier = Modifier.graphicsLayer { scaleX = invScale; scaleY = invScale }
                    )
                },
                label = { Text("Inventory", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) },
                selected = invSelected,
                onClick = {
                    navController.navigate("inventory") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = itemColors
            )

            // 5. More (Popup for Expenses, Schedule, Settings)
            val moreScale by animateFloatAsState(if (isMoreSelected) 1.12f else 1f, animationSpec = tween(200), label = "moreScale")
            NavigationBarItem(
                icon = {
                    Box {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            modifier = Modifier.graphicsLayer { scaleX = moreScale; scaleY = moreScale }
                        )
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Expenses", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showMoreMenu = false
                                    navController.navigate("expenses") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )

                            HorizontalDivider(color = Color(0xFFECEFF1), thickness = 1.dp)

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Schedule", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showMoreMenu = false
                                    navController.navigate("schedule") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )

                            HorizontalDivider(color = Color(0xFFECEFF1), thickness = 1.dp)

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Settings", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showMoreMenu = false
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                },
                label = { Text("More", fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) },
                selected = isMoreSelected,
                onClick = { showMoreMenu = true },
                colors = itemColors
            )
        }
    }
}
