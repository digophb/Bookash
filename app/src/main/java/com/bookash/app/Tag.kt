package com.bookash.app

/**
 * Tag - Modelo para tags de transações
 * 
 * Tags permitem organizar e filtrar transações com mais flexibilidade
 * que categorias. Uma transação pode ter múltiplas tags.
 */
data class Tag(
    val id: String = "",
    val name: String,
    val color: String = "#357266",
    val userId: String = ""
)
