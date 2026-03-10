package com.bookash.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Goal - Metas de gastos do usuário
 * 
 * Permite definir limites de gastos por período.
 */
@Parcelize
data class Goal(
    val id: String = "",
    val userId: String = "",
    val type: String = "monthly", // daily, weekly, monthly, yearly
    val targetAmount: Double = 0.0,
    val isEnabled: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
) : Parcelable {
    
    fun getDisplayName(): String {
        return when (type) {
            "daily" -> "Diária"
            "weekly" -> "Semanal"
            "monthly" -> "Mensal"
            "yearly" -> "Anual"
            else -> type
        }
    }
    
    fun getTypeKey(): String {
        return type
    }
}
