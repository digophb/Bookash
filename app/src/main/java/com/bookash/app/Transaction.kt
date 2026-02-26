package com.bookash.app

data class Transaction(
    val id: String,
    val description: String,
    val category: String,
    val amount: Double,
    val type: String, // "income" ou "expense"
    val date: String,
    val iconRes: Int
)
