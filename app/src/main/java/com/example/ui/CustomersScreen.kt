package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                title = { Text("Customer Directory", fontWeight = FontWeight.Bold, color = Gold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDark,
                    titleContentColor = Gold,
                    actionIconContentColor = White
                ),
                actions = {
                    IconButton(onClick = onAddCustomer) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = Gold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                containerColor = Gold,
                contentColor = NavyDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = customersViewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name, phone or address...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Gold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavyMedium,
                        unfocusedContainerColor = NavyMedium,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = Gold
                    )
                )
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No customers found.", color = TextSecondary)
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
                        colors = CardDefaults.cardColors(containerColor = NavyMedium),
                        shape = RoundedCornerShape(14.dp)
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
                                    color = Gold
                                )
                                IconButton(
                                    onClick = { showEditCustomerModal = customer },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Phone: ${customer.phone}", style = MaterialTheme.typography.bodyMedium, color = White)
                            Text("Address: ${customer.address}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (customer.pendingBalance > 0) AlertRedBg else AlertGreenBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (customer.pendingBalance > 0) ExpenseRed else ProfitGreen)
                                ) {
                                    Text(
                                        text = if (customer.pendingBalance > 0) "Pending: ${formatMoney(customer.pendingBalance)}" else "No Pending Balance",
                                        color = if (customer.pendingBalance > 0) ExpenseRed else ProfitGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text("Joined: ${customer.joiningDate}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
        EditCustomerModalBottomSheet(
            customer = cust,
            onDismiss = { showEditCustomerModal = null },
            onSave = { updatedName, updatedPhone, updatedAddress ->
                appViewModel.updateCustomer(cust.copy(name = updatedName, phone = updatedPhone, address = updatedAddress))
                showEditCustomerModal = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerModalBottomSheet(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var address by remember { mutableStateOf(customer.address) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BottomSheetHeader(title = "Edit Customer", onDismiss = onDismiss)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name *") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 15) phone = it },
                label = { Text("Phone Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address *") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = standardTextFieldColors()
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, NavyBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                            onSave(name.trim(), phone.trim(), address.trim())
                        }
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
