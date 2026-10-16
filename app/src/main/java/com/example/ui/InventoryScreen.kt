package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
                title = { Text("Inventory & Batches (${batches.size})", fontWeight = FontWeight.Bold, color = Yellow) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    actionIconContentColor = Yellow
                ),
                actions = {
                    IconButton(
                        onClick = { showAddBatchModal = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Batch", tint = Yellow)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBatchModal = true },
                containerColor = Yellow,
                contentColor = DarkBlue,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Batch")
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
            // Large Stock Level Gauge / Progress Card (Turns RED when stock <= 5)
            item {
                val stockColor = when {
                    availableStock <= 5 -> AlertRed
                    availableStock <= 15 -> Yellow
                    else -> SuccessGreen
                }
                val stockFraction = (availableStock.toFloat() / 50f).coerceIn(0f, 1f)

                Card(
                    border = BorderStroke(1.5.dp, if (availableStock <= 5) AlertRed else CardBorder),
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Live Stock Level",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Yellow
                            )

                            Surface(
                                color = if (availableStock <= 5) AlertRedBg else if (availableStock <= 15) AlertYellowBg else AlertGreenBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, stockColor)
                            ) {
                                Text(
                                    text = if (availableStock <= 5) "Critical Stock" else if (availableStock <= 15) "Low Stock" else "Healthy Stock",
                                    color = stockColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "$availableStock",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = stockColor,
                                    lineHeight = 46.sp
                                )
                                Text(
                                    text = "Ready Jars Available",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Base Unit Cost",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    formatMoneyDetailed(baseUnitCost),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "per ready jar",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Large Gauge Bar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { stockFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = stockColor,
                                trackColor = DarkBlueDeep
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0 Jars", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Alert (5)", color = AlertRed.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Target (50+)", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Danger Banner when stock <= 5
                        if (availableStock <= 5) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                color = AlertRedBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, AlertRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Stock Alert",
                                        tint = AlertRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Khatra: Stock 5 ya is se kam hai! Fauran naya batch tayar karein.",
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
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
                    color = DarkBlue
                )
            }

            if (batches.isEmpty()) {
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
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Yellow,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Koi Batch Nahi Hai",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Pehla batch banayein taake inventory stock shuru ho.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = { showAddBatchModal = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Yellow,
                                    contentColor = DarkBlue
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = DarkBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pehla Batch Banayein", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkBlue)
                            }
                        }
                    }
                }
            } else {
                items(batches, key = { it.id }) { batch ->
                    Box(modifier = Modifier.animateItem()) {
                        Card(
                            border = BorderStroke(1.dp, CardBorder),
                            colors = CardDefaults.cardColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Batch #${batch.id}", fontWeight = FontWeight.Bold, color = Yellow, fontSize = 15.sp)
                                    Text(batch.date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Raw Material: ${formatMoney(batch.rawMaterialCost)} | Jars: ${batch.emptyJarsCount} (${formatMoneyDetailed(batch.jarUnitCost)}/jar)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Total Batch Cost: ${formatMoney(batch.totalCalculatedCost)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                                Spacer(Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ready Jars: ${batch.jarsProduced}", color = Yellow, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Cost: ${formatMoneyDetailed(batch.baseCostPerJar)} / jar",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
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
