package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class BackupPayload(
    val version: String = "1.0",
    val exportDate: String,
    val customers: List<Customer>,
    val orders: List<Order>,
    val payments: List<CustomerPayment>,
    val inventoryBatches: List<InventoryBatch>,
    val expenses: List<Expense>,
    val tasks: List<DailyTask>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appViewModel: AppViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val customers by appViewModel.customers.collectAsStateWithLifecycle()
    val orders by appViewModel.orders.collectAsStateWithLifecycle()
    val batches by appViewModel.inventoryBatches.collectAsStateWithLifecycle()
    val expenses by appViewModel.expenses.collectAsStateWithLifecycle()
    val isSyncing by appViewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncedTime by appViewModel.lastSyncedTime.collectAsStateWithLifecycle()

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    var snackbarColor by remember { mutableStateOf(ProfitGreen) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                }
                if (!jsonString.isNullOrBlank()) {
                    pendingImportJson = jsonString
                    showImportConfirmDialog = true
                } else {
                    scope.launch {
                        snackbarColor = ExpenseRed
                        snackbarHostState.showSnackbar("❌ File khali ya invalid hai")
                    }
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarColor = ExpenseRed
                    snackbarHostState.showSnackbar("❌ File read nahi ho saki")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Gold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Gold
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = snackbarColor,
                    contentColor = White,
                    snackbarData = data
                )
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
            // APP INFO CARD
            item {
                Card(
                    border = BorderStroke(1.dp, CardBorder),
                    colors = CardDefaults.cardColors(containerColor = NavyMedium),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "App Info",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        InfoRow(label = "App Name", value = "Husn e Zar Beauty Cream Manager")
                        Spacer(Modifier.height(6.dp))
                        InfoRow(label = "Version", value = "1.0.0")
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = NavyBorder
                        )
                        InfoRow(label = "Total Customers", value = "${customers.size}")
                        Spacer(Modifier.height(6.dp))
                        InfoRow(label = "Total Orders", value = "${orders.size}")
                        Spacer(Modifier.height(6.dp))
                        InfoRow(label = "Total Batches", value = "${batches.size}")
                        Spacer(Modifier.height(6.dp))
                        InfoRow(label = "Total Expenses", value = "${expenses.size}")
                    }
                }
            }

            // CLOUD SYNC & ACCOUNT CARD
            item {
                Card(
                    border = BorderStroke(1.dp, CardBorder),
                    colors = CardDefaults.cardColors(containerColor = NavyMedium),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cloud Sync (Supabase)",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = Gold,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoRow(
                            label = "Admin Account",
                            value = appViewModel.supabaseAuth.getUserEmail().ifEmpty { "Connected" }
                        )
                        Spacer(Modifier.height(6.dp))
                        val lastSyncedStr = if (lastSyncedTime == 0L) {
                            "Kabhi nahi"
                        } else {
                            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(lastSyncedTime))
                        }
                        InfoRow(label = "Last Synced", value = lastSyncedStr)

                        Spacer(Modifier.height(16.dp))

                        // Button 1: Sync Now
                        Button(
                            onClick = {
                                scope.launch {
                                    appViewModel.syncNow { result ->
                                        result.fold(
                                            onSuccess = {
                                                snackbarColor = ProfitGreen
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("✅ Supabase sync mukammal ho gaya")
                                                }
                                            },
                                            onFailure = { ex ->
                                                snackbarColor = ExpenseRed
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("❌ Sync fail: ${ex.localizedMessage ?: "Network masla"}")
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = NavyDark,
                                disabledContainerColor = Gold.copy(alpha = 0.4f),
                                disabledContentColor = NavyDark.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = NavyDark,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Sync ho raha hai...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("🔄 Sync Now", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Button 2: Logout
                        OutlinedButton(
                            onClick = { showLogoutConfirmDialog = true },
                            border = BorderStroke(1.dp, ExpenseRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ExpenseRed
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🚪 Logout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // BACKUP & RESTORE SECTION CARD
            item {
                Card(
                    border = BorderStroke(1.dp, CardBorder),
                    colors = CardDefaults.cardColors(containerColor = NavyMedium),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Backup & Restore",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(16.dp))

                        // Button 1: Export Data
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val custs = appViewModel.getAllCustomersOnce()
                                        val ords = appViewModel.getAllOrdersOnce()
                                        val pays = appViewModel.getAllPaymentsOnce()
                                        val btchs = appViewModel.getAllBatchesOnce()
                                        val exps = appViewModel.getAllExpensesOnce()
                                        val tsks = appViewModel.getAllTasksOnce()

                                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        val backup = BackupPayload(
                                            version = "1.0",
                                            exportDate = today,
                                            customers = custs,
                                            orders = ords,
                                            payments = pays,
                                            inventoryBatches = btchs,
                                            expenses = exps,
                                            tasks = tsks
                                        )

                                        val gson = GsonBuilder().setPrettyPrinting().create()
                                        val jsonString = gson.toJson(backup)
                                        val filename = "HusnEZar_Backup_$today.json"

                                        val success = saveJsonToDownloads(context, filename, jsonString)
                                        if (success) {
                                            snackbarColor = ProfitGreen
                                            snackbarHostState.showSnackbar("✅ Backup save ho gaya Downloads mein!")
                                        } else {
                                            snackbarColor = ExpenseRed
                                            snackbarHostState.showSnackbar("❌ Backup save karne mein error aaya")
                                        }
                                    } catch (e: Exception) {
                                        snackbarColor = ExpenseRed
                                        snackbarHostState.showSnackbar("❌ Error: ${e.localizedMessage}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = NavyDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📤 Data Export Karo", fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(10.dp))

                        // Button 2: Import Data
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            border = BorderStroke(1.dp, Gold),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Gold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📥 Data Import Karo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // DANGER ZONE CARD
            item {
                Card(
                    border = BorderStroke(1.dp, ExpenseRed),
                    colors = CardDefaults.cardColors(containerColor = AlertRedBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Danger Zone",
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                deleteConfirmText = ""
                                showDeleteConfirmDialog = true
                            },
                            border = BorderStroke(1.dp, ExpenseRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ExpenseRed
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🗑️ Saara Data Delete Karo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }

        // Overwrite Warning Dialog for Import
        if (showImportConfirmDialog && pendingImportJson != null) {
            AlertDialog(
                onDismissRequest = {
                    showImportConfirmDialog = false
                    pendingImportJson = null
                },
                containerColor = NavyMedium,
                title = { Text("⚠️ Data Overwrite Hoga", fontWeight = FontWeight.Bold, color = ExpenseRed) },
                text = {
                    Text("Import karne se aapka maujuda data delete ho jayega. Kya sure hain?", color = White)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val json = pendingImportJson
                            showImportConfirmDialog = false
                            pendingImportJson = null
                            if (json != null) {
                                scope.launch {
                                    try {
                                        val gson = Gson()
                                        val jsonObject = gson.fromJson(json, JsonObject::class.java)

                                        val hasVersion = jsonObject.has("version")
                                        val hasCustomers = jsonObject.has("customers")
                                        val hasOrders = jsonObject.has("orders")

                                        if (!hasVersion || !hasCustomers || !hasOrders) {
                                            snackbarColor = ExpenseRed
                                            snackbarHostState.showSnackbar("❌ File invalid hai")
                                            return@launch
                                        }

                                        val customerListType = object : TypeToken<List<Customer>>() {}.type
                                        val orderListType = object : TypeToken<List<Order>>() {}.type
                                        val paymentListType = object : TypeToken<List<CustomerPayment>>() {}.type
                                        val batchListType = object : TypeToken<List<InventoryBatch>>() {}.type
                                        val expenseListType = object : TypeToken<List<Expense>>() {}.type
                                        val taskListType = object : TypeToken<List<DailyTask>>() {}.type

                                        val customersList: List<Customer> = gson.fromJson(jsonObject.get("customers"), customerListType) ?: emptyList()
                                        val ordersList: List<Order> = gson.fromJson(jsonObject.get("orders"), orderListType) ?: emptyList()
                                        val paymentsList: List<CustomerPayment> = if (jsonObject.has("payments")) gson.fromJson(jsonObject.get("payments"), paymentListType) ?: emptyList() else emptyList()
                                        val batchesList: List<InventoryBatch> = if (jsonObject.has("inventoryBatches")) gson.fromJson(jsonObject.get("inventoryBatches"), batchListType) ?: emptyList() else emptyList()
                                        val expensesList: List<Expense> = if (jsonObject.has("expenses")) gson.fromJson(jsonObject.get("expenses"), expenseListType) ?: emptyList() else emptyList()
                                        val tasksList: List<DailyTask> = if (jsonObject.has("tasks")) gson.fromJson(jsonObject.get("tasks"), taskListType) ?: emptyList() else emptyList()

                                        appViewModel.restoreAllData(
                                            customersList,
                                            ordersList,
                                            paymentsList,
                                            batchesList,
                                            expensesList,
                                            tasksList
                                        )

                                        snackbarColor = ProfitGreen
                                        snackbarHostState.showSnackbar("✅ Data import ho gaya!")
                                    } catch (e: Exception) {
                                        snackbarColor = ExpenseRed
                                        snackbarHostState.showSnackbar("❌ File invalid hai")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Import Karo")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showImportConfirmDialog = false
                            pendingImportJson = null
                        },
                        border = BorderStroke(1.dp, NavyBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete All Data Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                containerColor = NavyMedium,
                title = { Text("⚠️ Saara Data Delete Hoga", fontWeight = FontWeight.Bold, color = ExpenseRed) },
                text = {
                    Column {
                        Text("Yeh action undo nahi ho sakta. Saare customers, orders, payments, inventory aur expenses delete honge.", color = White)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = deleteConfirmText,
                            onValueChange = { deleteConfirmText = it },
                            placeholder = { Text("DELETE likho confirm k liye", color = TextSecondary) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = standardTextFieldColors()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            scope.launch {
                                appViewModel.clearAllTables()
                                snackbarColor = ProfitGreen
                                snackbarHostState.showSnackbar("✅ Saara data delete ho gaya")
                            }
                        },
                        enabled = deleteConfirmText == "DELETE",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = White,
                            disabledContainerColor = AlertRedBg,
                            disabledContentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Haan, Delete Karo")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = false },
                        border = BorderStroke(1.dp, NavyBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                containerColor = NavyMedium,
                title = { Text("Logout Karein?", fontWeight = FontWeight.Bold, color = Gold) },
                text = {
                    Text("Aap session se logout ho jayenge. Local data cache mein mehfooz rahega.", color = White)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            appViewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutConfirmDialog = false },
                        border = BorderStroke(1.dp, NavyBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun saveJsonToDownloads(context: Context, filename: String, jsonContent: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
                true
            } else {
                false
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, filename)
            file.writeText(jsonContent, Charsets.UTF_8)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, filename)
            file.writeText(jsonContent, Charsets.UTF_8)
            true
        } catch (ex: Exception) {
            false
        }
    }
}
