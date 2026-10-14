package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        enableEdgeToEdge()
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
                        color = NavyDark
                    ) {
                        var showCustomerModal by remember { mutableStateOf(false) }
                    var showBalanceModal by remember { mutableStateOf(false) }
                    var showExpenseModal by remember { mutableStateOf(false) }

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

                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            Column(Modifier.fillMaxSize().background(NavyDark)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DashboardScreen(
                                        appViewModel = appViewModel,
                                        dashboardViewModel = dashboardViewModel,
                                        onNavigateToPricing = { navController.navigate("pricing") },
                                        onAddCustomer = { showCustomerModal = true },
                                        onAddBalance = { showBalanceModal = true },
                                        onAddExpense = { showExpenseModal = true }
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
                            Column(Modifier.fillMaxSize().background(NavyDark)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OrdersScreen(appViewModel, ordersViewModel)
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("inventory") {
                            Column(Modifier.fillMaxSize().background(NavyDark)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    InventoryScreen(appViewModel = appViewModel)
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("customers") {
                            Column(Modifier.fillMaxSize().background(NavyDark)) {
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
                            Column(Modifier.fillMaxSize().background(NavyDark)) {
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
                            Column(Modifier.fillMaxSize().background(NavyDark)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ScheduleScreen(appViewModel = appViewModel)
                                }
                                AppBottomNavigation(currentRoute = currentRoute, navController = navController)
                            }
                        }

                        composable("settings") {
                            Column(Modifier.fillMaxSize().background(NavyDark)) {
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
                }
            }
        }
    }
}
}

@Composable
fun AppBottomNavigation(currentRoute: String, navController: NavController) {
    NavigationBar(
        containerColor = NavyDark
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 10.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == "dashboard",
            onClick = {
                navController.navigate("dashboard") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Gold,
                selectedTextColor = Gold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = NavyLight
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Orders") },
            label = { Text("Orders", fontSize = 10.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == "orders",
            onClick = {
                navController.navigate("orders") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Gold,
                selectedTextColor = Gold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = NavyLight
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Build, contentDescription = "Inventory") },
            label = { Text("Inventory", fontSize = 10.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == "inventory",
            onClick = {
                navController.navigate("inventory") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Gold,
                selectedTextColor = Gold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = NavyLight
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
            label = { Text("Customers", fontSize = 10.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == "customers",
            onClick = {
                navController.navigate("customers") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Gold,
                selectedTextColor = Gold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = NavyLight
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Expenses") },
            label = { Text("Expenses", fontSize = 10.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == "expenses",
            onClick = {
                navController.navigate("expenses") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Gold,
                selectedTextColor = Gold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = NavyLight
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
            label = { Text("Schedule", fontSize = 10.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == "schedule",
            onClick = {
                navController.navigate("schedule") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Gold,
                selectedTextColor = Gold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = NavyLight
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp, maxLines = 1, softWrap = false) },
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Gold,
                selectedTextColor = Gold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = NavyLight
            )
        )
    }
}
