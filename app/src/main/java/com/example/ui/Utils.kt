package com.example.ui

import java.text.NumberFormat
import java.util.Locale

fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "PK"))
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 0
    return "Rs. ${formatter.format(amount)}"
}

fun formatMoneyDetailed(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "PK"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "Rs. ${formatter.format(amount)}"
}
