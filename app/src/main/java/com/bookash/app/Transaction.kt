package com.bookash.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: String = "",
    val userId: String = "",
    val description: String,
    val categoryId: String = "", // UUID da categoria
    val categoryName: String = "", // Nome da categoria (cache para exibicao)
    val amount: Double,
    val type: String, // income, expense, transfer
    val date: String,
    val accountId: String? = null, // Conta associada (receitas/despesas)
    val fromAccountId: String? = null, // Conta de origem (transferencias)
    val toAccountId: String? = null, // Conta de destino (transferencias)
    val fromAccountName: String? = null, // Nome da conta de origem (preenchido ao carregar)
    val toAccountName: String? = null, // Nome da conta de destino (preenchido ao carregar)
    val creditCardId: String? = null, // UUID do cartao de credito
    val notes: String? = null,
    val isRecurring: Boolean = false,
    val recurringType: String? = null, // daily, weekly, monthly, yearly
    val recurringCount: Int? = null, // Quantidade de repetições (null = infinito)
    val recurringUntil: String? = null,
    val recurringId: String? = null, // UUID que agrupa todas as ocorrências da mesma série
    val status: String? = "completed", // pending, completed
    val isDeleted: Boolean = false,
    val tags: List<Tag> = emptyList(), // Tags associadas (do modelo Tag.kt)
    val attachments: List<Attachment> = emptyList(), // Anexos
    val iconRes: Int = 0
) : Parcelable

/**
 * Attachment - Anexos de transacoes
 * 
 * Permite anexar fotos, recibos, documentos a transacoes.
 */
@Parcelize
data class Attachment(
    val id: String = "",
    val transactionId: String = "",
    val url: String,           // URL do arquivo no Storage
    val type: String = "image", // image, document, pdf
    val createdAt: String = ""
) : Parcelable

@Parcelize
data class Category(
    val id: String = "",
    val name: String,
    val type: String,
    val color: String = "#357266",
    val icon: String = "category",
    val userId: String = "",
    val isDefault: Boolean = false // TRUE = categoria padrão (não pode editar/excluir)
) : Parcelable

@Parcelize
data class Account(
    val id: String = "",
    val name: String,
    val balance: Double = 0.0,
    val type: String = "corrente",
    val icon: String = "wallet",
    val isArchived: Boolean = false,
    val includeInBalance: Boolean = true, // Se TRUE, saldo incluído no total do dashboard
    val userId: String = ""
) : Parcelable

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
