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
    val tags: List<String> = emptyList(),
    val reminderDate: String? = null,
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
    val includeInBalance: Boolean = true, // Se TRUE, saldo incluído no total do dashboard
    val userId: String = ""
)

/**
 * Reminder - Lembretes de transações
 *
 * Permite agendar notificações para transações futuras ou recorrentes.
 */
data class Reminder(
    val id: String = "",
    val userId: String = "",
    val transactionId: String? = null,
    val title: String,
    val description: String? = null,
    val amount: Double? = null,
    val reminderDate: String, // ISO 8601 format
    val isRecurring: Boolean = false,
    val recurrenceType: String? = null, // "daily", "weekly", "monthly", "yearly"
    val recurrenceInterval: Int = 1,
    val isActive: Boolean = true,
    val lastTriggeredAt: String? = null,
    val nextTriggerAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

/**
 * AppSettings - Configurações do aplicativo por usuário
 * 
 * Armazena preferências do usuário como tema, idioma e notificações.
 * Cada usuário tem exatamente um registro de configurações.
 */
data class AppSettings(
    val id: String = "",
    val userId: String = "",
    val theme: String = "system", // "light", "dark", "system"
    val language: String = "pt-BR",
    val notificationsEnabled: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = ""
)
