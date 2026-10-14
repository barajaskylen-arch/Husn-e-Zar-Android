package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Customer
import com.example.data.CustomerPayment
import com.example.data.Order
import kotlinx.coroutines.flow.*

class CustomerDetailsViewModel(
    private val customerId: String,
    private val appViewModel: AppViewModel
) : ViewModel() {

    val customer: StateFlow<Customer?> = appViewModel.customers
        .map { list -> list.find { it.id == customerId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val orders: StateFlow<List<Order>> = appViewModel.orders
        .map { list -> list.filter { it.customerId == customerId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<CustomerPayment>> = appViewModel.customerPayments
        .map { list -> list.filter { it.customerId == customerId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCustomer(customer: Customer) {
        appViewModel.updateCustomer(customer)
    }

    fun updateOrder(oldOrder: Order, newOrder: Order) {
        appViewModel.updateOrder(oldOrder, newOrder)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val customerId: String, private val appViewModel: AppViewModel) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomerDetailsViewModel(customerId, appViewModel) as T
        }
    }
}
