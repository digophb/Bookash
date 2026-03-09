package com.bookash.app

/**
 * Tag - Modelo para tags de transações
 * 
 * Tags permitem organizar e filtrar transações com mais flexibilidade
 * que categorias. Uma transação pode ter múltiplas tags.
 */

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: String = "",
    val name: String,
    val color: String = "#357266",
    val userId: String = "",
    val createdAt: String = ""
) : Parcelable
