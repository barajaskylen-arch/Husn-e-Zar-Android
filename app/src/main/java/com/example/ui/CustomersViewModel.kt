package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Customer
import kotlinx.coroutines.flow.*

data class TopCustomer(
    val customer: Customer,
    val totalJarsBought: Int
)

class CustomersViewModel(private val appViewModel: AppViewModel) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredCustomers: StateFlow<List<Customer>> = combine(
        appViewModel.customers,
        _searchQuery
    ) { customers, query ->
        if (query.isBlank()) {
            customers
        } else {
            customers.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true) ||
                it.phone.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topCustomers: StateFlow<List<TopCustomer>> = combine(
        appViewModel.customers,
        appViewModel.orders
    ) { customers, orders ->
        val jarCounts = orders.groupBy { it.customerId }
            .mapValues { entry -> entry.value.sumOf { it.jarsQuantity } }
        
        customers.map { TopCustomer(it, jarCounts[it.id] ?: 0) }
            .filter { it.totalJarsBought > 0 }
            .sortedByDescending { it.totalJarsBought }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
