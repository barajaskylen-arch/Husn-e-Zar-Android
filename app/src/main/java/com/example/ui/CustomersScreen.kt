package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.Customer
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    appViewModel: AppViewModel,
    customersViewModel: CustomersViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onAddCustomer: () -> Unit
) {
    val searchQuery by customersViewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredCustomers by customersViewModel.filteredCustomers.collectAsStateWithLifecycle()

    var showEditCustomerModal by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Directory", fontWeight = FontWeight.Bold, color = Yellow) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Yellow,
                    actionIconContentColor = Yellow
                ),
                actions = {
                    IconButton(onClick = onAddCustomer) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = Yellow)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                containerColor = Yellow,
                contentColor = DarkBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = customersViewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name, phone or address...", color = DarkBlue.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DarkBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = standardTextFieldColors()
                )
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Box(Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No customers found.", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filteredCustomers) { customer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetails(customer.id) },
                        border = BorderStroke(1.dp, CardBorder),
                        colors = CardDefaults.cardColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    customer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Yellow
                                )
                                IconButton(
                                    onClick = { showEditCustomerModal = customer },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = Yellow)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Phone: ${customer.phone}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Address: ${customer.address}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (customer.pendingBalance > 0) AlertRedBg else AlertGreenBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (customer.pendingBalance > 0) AlertRed else SuccessGreen)
                                ) {
                                    Text(
                                        text = if (customer.pendingBalance > 0) "Pending: ${formatMoney(customer.pendingBalance)}" else "No Pending Balance",
                                        color = if (customer.pendingBalance > 0) AlertRed else SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text("Joined: ${customer.joiningDate}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showEditCustomerModal != null) {
        val cust = showEditCustomerModal!!
        EditCustomerDialog(
            customer = cust,
            onDismiss = { showEditCustomerModal = null },
            onSave = { updatedName, updatedPhone, updatedAddress ->
                appViewModel.updateCustomer(cust.copy(name = updatedName, phone = updatedPhone, address = updatedAddress))
                showEditCustomerModal = null
            }
        )
    }
}

@Composable
fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var address by remember { mutableStateOf(customer.address) }

    AnimatedCenterDialog(
        onDismiss = onDismiss,
        title = "Edit Customer",
        icon = Icons.Default.Edit,
        saveEnabled = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank(),
        onSave = {
            if (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                onSave(name.trim(), phone.trim(), address.trim())
            }
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Customer Name *", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 15) phone = it },
            label = { Text("Phone Number *", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address *", fontWeight = FontWeight.Bold) },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = standardTextFieldColors()
        )
    }
}
