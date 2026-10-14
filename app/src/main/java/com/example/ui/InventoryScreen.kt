package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.InventoryBatch
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    appViewModel: AppViewModel
) {
    val batches by appViewModel.inventoryBatches.collectAsStateWithLifecycle()
    val availableStock by appViewModel.currentAvailableStock.collectAsStateWithLifecycle()
    val baseUnitCost by appViewModel.currentBaseUnitCost.collectAsStateWithLifecycle()

    var showAddBatchModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory & Batch Production", fontWeight = FontWeight.Bold, color = Gold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Gold,
                    actionIconContentColor = White
                ),
                actions = {
                    IconButton(onClick = { showAddBatchModal = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Batch", tint = Gold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBatchModal = true },
                containerColor = Gold,
                contentColor = NavyDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Batch")
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
            item {
                Card(
                    border = BorderStroke(1.dp, CardBorder),
                    colors = CardDefaults.cardColors(containerColor = NavyMedium),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Real-Time Stock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Available Jars", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "$availableStock Units",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (availableStock <= 15) ExpenseRed else ProfitGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Base Unit Cost", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatMoneyDetailed(baseUnitCost),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        }

                        if (availableStock <= 15) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = AlertRedBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ExpenseRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "Low Stock", tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Low stock warning! Please create a new batch.", color = ExpenseRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Production History Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }

            if (batches.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No batches recorded yet.", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(batches) { batch ->
                    Card(
                        border = BorderStroke(1.dp, CardBorder),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Batch #${batch.id}", fontWeight = FontWeight.Bold, color = Gold)
                                Text(batch.date, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Raw Material: ${formatMoney(batch.rawMaterialCost)} | Jars: ${batch.emptyJarsCount} (${formatMoneyDetailed(batch.jarUnitCost)}/jar)", color = WhiteSoft, fontSize = 13.sp)
                            Text("Total Batch Cost: ${formatMoney(batch.totalCalculatedCost)}", color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ready Jars: ${batch.jarsProduced}", color = GoldLight, fontWeight = FontWeight.Bold)
                                Text(
                                    "Cost: ${formatMoneyDetailed(batch.baseCostPerJar)} / jar",
                                    color = ProfitGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddBatchModal) {
        AddBatchModal(
            onDismiss = { showAddBatchModal = false },
            onSave = { rawMat, jarsQty, costPerJar, totalReady, date, _ ->
                appViewModel.addInventoryBatch(
                    date = date,
                    rawMaterialCost = rawMat,
                    emptyJarsCount = jarsQty,
                    jarUnitCost = costPerJar,
                    jarsProduced = totalReady
                )
                showAddBatchModal = false
            }
        )
    }
}
