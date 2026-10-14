package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val liveCostPerJar by appViewModel.currentBaseUnitCost.collectAsStateWithLifecycle()
    val availableStock by appViewModel.currentAvailableStock.collectAsStateWithLifecycle()

    var costPerJarInput by remember { mutableStateOf("") }
    var targetPercent by remember { mutableStateOf(40f) }
    var bulkJars by remember { mutableStateOf("") }

    LaunchedEffect(liveCostPerJar) {
        if (costPerJarInput.isEmpty() && liveCostPerJar > 0) {
            costPerJarInput = String.format(java.util.Locale.US, "%.2f", liveCostPerJar)
        }
    }

    val costPerJar = costPerJarInput.toDoubleOrNull() ?: liveCostPerJar
    val profitAmount = costPerJar * (targetPercent / 100)
    val sellingPrice = costPerJar + profitAmount

    val jars = bulkJars.toIntOrNull() ?: 0
    val totalCost = costPerJar * jars
    val totalRevenue = sellingPrice * jars
    val totalProfit = totalRevenue - totalCost
    val profitMargin = if (totalRevenue > 0) ((totalProfit / totalRevenue) * 100).toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Price Calculator", fontWeight = FontWeight.Bold, color = Yellow) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Yellow)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    navigationIconContentColor = Yellow
                )
            )
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
            // Section 1 — Current Cost Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Live Inventory Baseline",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Yellow
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Cost Per Jar", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    formatMoneyDetailed(liveCostPerJar),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Current Stock", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    "$availableStock jars",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (availableStock > 10) SuccessGreen else AlertRed
                                )
                            }
                        }
                    }
                }
            }

            // Section 2 — Profit Calculator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Target Profit Calculate Karo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Yellow
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = costPerJarInput,
                            onValueChange = { costPerJarInput = it },
                            label = { Text("Cost Per Jar (PKR)", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = standardTextFieldColors()
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Profit %", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${targetPercent.toInt()}% Profit",
                                color = Yellow,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Slider(
                            value = targetPercent,
                            onValueChange = { targetPercent = it },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Yellow,
                                activeTrackColor = Yellow,
                                inactiveTrackColor = DarkBlueDeep
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        // Section 3 — Quick Presets Row inside the card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(30f, 40f, 50f).forEach { preset ->
                                val isSelected = targetPercent.toInt() == preset.toInt()
                                OutlinedButton(
                                    onClick = { targetPercent = preset },
                                    border = BorderStroke(1.5.dp, if (isSelected) Yellow else Color.White.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Yellow else Color.Transparent,
                                        contentColor = if (isSelected) DarkBlue else Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${preset.toInt()}%", fontWeight = FontWeight.Bold, color = if (isSelected) DarkBlue else Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // Result card (DarkBlueDeep bg)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkBlueDeep),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cost Per Jar:", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatMoneyDetailed(costPerJar), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Profit Per Jar:", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatMoneyDetailed(profitAmount), color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = DarkBlue
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sell At:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        formatMoney(sellingPrice),
                                        color = Yellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4 — Bulk Calculator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBlue),
                    border = BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Bulk Order Calculation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Yellow
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = bulkJars,
                            onValueChange = { bulkJars = it },
                            label = { Text("Number of Jars", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("e.g. 50, 100", color = DarkBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = standardTextFieldColors()
                        )
                        Spacer(Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkBlueDeep),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Cost:", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatMoney(totalCost), color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Profit:", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatMoney(totalProfit), color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Revenue:", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatMoney(totalRevenue), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Profit Margin:", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("$profitMargin%", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
