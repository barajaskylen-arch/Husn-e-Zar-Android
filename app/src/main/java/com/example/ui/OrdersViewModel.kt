package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Order
import kotlinx.coroutines.flow.*

class OrdersViewModel(private val appViewModel: AppViewModel) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredOrders: StateFlow<List<Order>> = combine(
        appViewModel.orders,
        _searchQuery
    ) { orders, query ->
        if (query.isBlank()) {
            orders
        } else {
            orders.filter {
                it.customerName.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
