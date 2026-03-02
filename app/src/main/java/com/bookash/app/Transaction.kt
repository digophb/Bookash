package com.bookash.app

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val description: String,
    val category: String,
    val amount: Double,
    val type: String,
    val date: String,
    val status: String = "paid",
    val accountId: String = "",
    val isRecurring: Boolean = false,
    val recurrencePeriod: String = "",
    val recurrenceCount: Int = 1,
    val iconRes: Int = 0
)

data class Category(
    val id: String = "",
    val name: String,
    val type: String,
    val color: String = "#357266",
    val icon: String = "category",
    val userId: String = "",
    val isDefault: Boolean = false // TRUE = categoria padrão (não pode editar/excluir)
)

data class Account(
    val id: String = "",
    val name: String,
    val balance: Double = 0.0,
    val type: String = "corrente",
    val icon: String = "wallet",
    val isArchived: Boolean = false,
    val userId: String = ""
)
