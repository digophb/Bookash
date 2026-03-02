package com.bookash.app

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String,
    val type: String,
    val color: String = "#357266",
    val icon: String = "category",
    val userId: String = ""
)

@Serializable
data class Account(
    val id: String = "",
    val name: String,
    val balance: Double = 0.0,
    val type: String = "corrente",
    val icon: String = "wallet",
    val isArchived: Boolean = false,
    val userId: String = ""
)

@Serializable
data class Tag(
    val id: String = "",
    val name: String,
    val color: String = "#357266",
    val userId: String = ""
)

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val description: String,
    val category: String,
    val amount: Double = 0.0,
    val type: String,
    val date: String,
    val status: String = "paid",
    val accountId: String = "",
    val iconRes: Int = R.drawable.ic_arrow_down,
    val isRecurring: Boolean = false,
    val recurrencePeriod: String = "",
    val recurrenceCount: Int = 1
)
