package com.bookash.app

import android.util.Log
import io.github.jan.tennert.supabase.postgrest.from
import io.github.jan.tennert.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.tennert.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Serviço de comunicação com o Supabase usando SDK oficial.
 * 
 * IMPORTANTE: Todas as operações CRUD possuem logging para:
 * - Debugging em produção
 * - Auditoria (crítico em app financeiro)
 * - Monitoramento de performance
 * - Detecção de erros
 */
object SupabaseService {
    
    private const val TAG = "BookashAPI"
    
    // ============== CATEGORIES ==============
    
    /**
     * Busca categorias do usuário logado.
     * IMPORTANTE: Sempre filtra por user_id para isolamento de dados.
     */
    suspend fun getCategories(userId: String, type: String? = null): List<Category> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = type ?: "all"
        Log.d(TAG, "[CATEGORIES] GET - Iniciando busca (userId: $userId, filtro: $filter)")
        
        try {
            val result = if (type != null) {
                SupabaseClient.postgrest.from("categories")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("type", type)
                        }
                    }
                    .decodeList<CategoryDTO>()
            } else {
                SupabaseClient.postgrest.from("categories")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<CategoryDTO>()
            }
            
            val categories = result.map { it.toCategory() }
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[CATEGORIES] GET - Sucesso: ${categories.size} categorias encontradas (${duration}ms)")
            categories
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    /**
     * Salva uma nova categoria para o usuário logado.
     */
    suspend fun saveCategory(category: Category, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] CREATE - Iniciando: name='${category.name}', type=${category.type}, userId=$userId")
        
        try {
            SupabaseClient.postgrest.from("categories").insert(
                CategoryDTO(
                    name = category.name,
                    type = category.type,
                    color = category.color,
                    icon = category.icon,
                    userId = userId
                )
            )
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[CATEGORIES] CREATE - Sucesso: '${category.name}' criada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] CREATE - Erro ao criar '${category.name}' após ${duration}ms", e)
            false
        }
    }
    
    suspend fun deleteCategory(categoryId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] DELETE - Iniciando: id=$categoryId")
        
        try {
            SupabaseClient.postgrest.from("categories").delete {
                filter { eq("id", categoryId) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[CATEGORIES] DELETE - Sucesso: id=$categoryId (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] DELETE - Erro ao excluir id=$categoryId após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateCategory(category: Category, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] UPDATE - Iniciando: id=${category.id}, name='${category.name}'")
        
        try {
            SupabaseClient.postgrest.from("categories").update(
                CategoryDTO(
                    name = category.name,
                    type = category.type,
                    color = category.color,
                    icon = category.icon
                )
            ) {
                filter { eq("id", category.id) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[CATEGORIES] UPDATE - Sucesso: '${category.name}' atualizada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] UPDATE - Erro ao atualizar id=${category.id} após ${duration}ms", e)
            false
        }
    }
    
    /**
     * Verifica se já existe uma categoria com o mesmo nome e tipo para o usuário.
     */
    suspend fun categoryExists(name: String, type: String, excludeId: String? = null, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] EXISTS - Verificando: name='$name', type=$type, userId=$userId")
        
        try {
            val result = SupabaseClient.postgrest.from("categories")
                .select {
                    filter {
                        ilike("name", name)
                        eq("type", type)
                        if (userId != null) eq("user_id", userId)
                        if (excludeId != null) neq("id", excludeId)
                    }
                }
                .decodeList<CategoryDTO>()
            
            val exists = result.isNotEmpty()
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[CATEGORIES] EXISTS - ${if (exists) "Duplicata encontrada" else "Não encontrada"} (${duration}ms)")
            exists
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] EXISTS - Erro após ${duration}ms", e)
            false
        }
    }
    
    // ============== ACCOUNTS ==============
    
    /**
     * Busca contas do usuário logado.
     */
    suspend fun getAccounts(userId: String, archived: Boolean = false): List<Account> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = if (archived) "arquivadas" else "ativas"
        Log.d(TAG, "[ACCOUNTS] GET - Iniciando busca (userId: $userId, filtro: $filter)")
        
        try {
            val result = SupabaseClient.postgrest.from("accounts")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_archived", archived)
                    }
                    order("created_at", order = io.github.jan.tennert.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<AccountDTO>()
            
            val accounts = result.map { it.toAccount() }
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] GET - Sucesso: ${accounts.size} contas $filter encontradas (${duration}ms)")
            accounts
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    /**
     * Salva uma nova conta para o usuário logado.
     */
    suspend fun saveAccount(account: Account, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] CREATE - Iniciando: name='${account.name}', type=${account.type}, userId=$userId")
        
        try {
            SupabaseClient.postgrest.from("accounts").insert(
                AccountDTO(
                    name = account.name,
                    balance = account.balance,
                    type = account.type,
                    icon = account.icon,
                    userId = userId
                )
            )
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] CREATE - Sucesso: '${account.name}' criada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] CREATE - Erro ao criar '${account.name}' após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateAccount(account: Account, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] UPDATE - Iniciando: id=${account.id}, name='${account.name}'")
        
        try {
            SupabaseClient.postgrest.from("accounts").update(
                AccountDTO(
                    name = account.name,
                    balance = account.balance,
                    type = account.type,
                    icon = account.icon
                )
            ) {
                filter { eq("id", account.id) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] UPDATE - Sucesso: '${account.name}' atualizada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] UPDATE - Erro ao atualizar id=${account.id} após ${duration}ms", e)
            false
        }
    }
    
    suspend fun archiveAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] ARCHIVE - Iniciando: id=$accountId")
        
        try {
            SupabaseClient.postgrest.from("accounts").update(
                mapOf("is_archived" to true)
            ) {
                filter { eq("id", accountId) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] ARCHIVE - Sucesso: id=$accountId arquivada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] ARCHIVE - Erro ao arquivar id=$accountId após ${duration}ms", e)
            false
        }
    }
    
    suspend fun reactivateAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] REACTIVATE - Iniciando: id=$accountId")
        
        try {
            SupabaseClient.postgrest.from("accounts").update(
                mapOf("is_archived" to false)
            ) {
                filter { eq("id", accountId) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] REACTIVATE - Sucesso: id=$accountId reativada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] REACTIVATE - Erro ao reativar id=$accountId após ${duration}ms", e)
            false
        }
    }
    
    suspend fun deleteAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] DELETE - Iniciando: id=$accountId")
        
        try {
            SupabaseClient.postgrest.from("accounts").delete {
                filter { eq("id", accountId) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] DELETE - Sucesso: id=$accountId excluída (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] DELETE - Erro ao excluir id=$accountId após ${duration}ms", e)
            false
        }
    }
    
    // ============== TRANSACTIONS ==============
    
    suspend fun getTransactions(userId: String, limit: Int = 50): List<Transaction> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] GET - Iniciando busca (userId: $userId, limit: $limit)")
        
        try {
            val result = SupabaseClient.postgrest.from("transactions")
                .select {
                    filter { eq("user_id", userId) }
                    order("date", order = io.github.jan.tennert.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<TransactionDTO>()
            
            val transactions = result.map { it.toTransaction() }
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TRANSACTIONS] GET - Sucesso: ${transactions.size} transações encontradas (${duration}ms)")
            transactions
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveTransaction(transaction: Transaction, token: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] CREATE - Iniciando: type=${transaction.type}, category='${transaction.category}'")
        
        try {
            SupabaseClient.postgrest.from("transactions").insert(
                TransactionDTO(
                    userId = transaction.userId,
                    type = transaction.type,
                    amount = transaction.amount,
                    description = transaction.description,
                    category = transaction.category,
                    date = transaction.date,
                    status = transaction.status,
                    accountId = transaction.accountId.ifEmpty { null },
                    isRecurring = transaction.isRecurring,
                    recurrencePeriod = transaction.recurrencePeriod.ifEmpty { null },
                    recurrenceCount = transaction.recurrenceCount
                )
            )
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TRANSACTIONS] CREATE - Sucesso: ${transaction.type} '${transaction.description}' (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] CREATE - Erro ao criar transação após ${duration}ms", e)
            false
        }
    }
    
    // ============== TAGS ==============
    
    suspend fun getTags(userId: String): List<Tag> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] READ - Iniciando busca para userId: $userId")
        
        try {
            val result = SupabaseClient.postgrest.from("tags")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", order = io.github.jan.tennert.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<TagDTO>()
            
            val tags = result.map { it.toTag() }
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TAGS] READ - Sucesso: ${tags.size} tags encontradas (${duration}ms)")
            tags
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] READ - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveTag(tag: Tag, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] CREATE - Iniciando: name='${tag.name}', userId=$userId")
        
        try {
            SupabaseClient.postgrest.from("tags").insert(
                TagDTO(
                    name = tag.name,
                    color = tag.color,
                    userId = userId
                )
            )
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TAGS] CREATE - Sucesso: '${tag.name}' criada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] CREATE - Erro ao criar '${tag.name}' após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateTag(tag: Tag): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] UPDATE - Iniciando: id=${tag.id}, name='${tag.name}'")
        
        try {
            SupabaseClient.postgrest.from("tags").update(
                TagDTO(
                    name = tag.name,
                    color = tag.color
                )
            ) {
                filter { eq("id", tag.id) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TAGS] UPDATE - Sucesso: '${tag.name}' atualizada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] UPDATE - Erro ao atualizar '${tag.name}' após ${duration}ms", e)
            false
        }
    }
    
    suspend fun deleteTag(tagId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] DELETE - Iniciando: id=$tagId")
        
        try {
            SupabaseClient.postgrest.from("tags").delete {
                filter { eq("id", tagId) }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TAGS] DELETE - Sucesso: tag excluída (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] DELETE - Erro após ${duration}ms", e)
            false
        }
    }
    
    suspend fun tagExists(name: String, userId: String, excludeId: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = SupabaseClient.postgrest.from("tags")
                .select {
                    filter {
                        eq("name", name)
                        eq("user_id", userId)
                        if (excludeId != null) neq("id", excludeId)
                    }
                }
                .decodeList<TagDTO>()
            
            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "[TAGS] EXISTS - Erro ao verificar existência", e)
            false
        }
    }
}

// ============== DTOs para serialização ==============

@Serializable
data class CategoryDTO(
    val id: String? = null,
    val name: String,
    val type: String,
    val color: String = "#357266",
    val icon: String = "category",
    @SerialName("user_id")
    val userId: String? = null
) {
    fun toCategory() = Category(
        id = id ?: "",
        name = name,
        type = type,
        color = color,
        icon = icon,
        userId = userId ?: ""
    )
}

@Serializable
data class AccountDTO(
    val id: String? = null,
    val name: String,
    val balance: Double = 0.0,
    val type: String = "corrente",
    val icon: String = "wallet",
    @SerialName("is_archived")
    val isArchived: Boolean = false,
    @SerialName("user_id")
    val userId: String? = null
) {
    fun toAccount() = Account(
        id = id ?: "",
        name = name,
        balance = balance,
        type = type,
        icon = icon,
        isArchived = isArchived,
        userId = userId ?: ""
    )
}

@Serializable
data class TransactionDTO(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val type: String,
    val amount: Double,
    val description: String,
    val category: String,
    val date: String,
    val status: String = "paid",
    @SerialName("account_id")
    val accountId: String? = null,
    @SerialName("is_recurring")
    val isRecurring: Boolean = false,
    @SerialName("recurrence_period")
    val recurrencePeriod: String? = null,
    @SerialName("recurrence_count")
    val recurrenceCount: Int = 1
) {
    fun toTransaction() = Transaction(
        id = id ?: "",
        userId = userId,
        type = type,
        amount = amount,
        description = description,
        category = category,
        date = date,
        status = status,
        accountId = accountId ?: "",
        isRecurring = isRecurring,
        recurrencePeriod = recurrencePeriod ?: "",
        recurrenceCount = recurrenceCount,
        iconRes = if (type == "income") R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
    )
}

@Serializable
data class TagDTO(
    val id: String? = null,
    val name: String,
    val color: String = "#357266",
    @SerialName("user_id")
    val userId: String? = null
) {
    fun toTag() = Tag(
        id = id ?: "",
        name = name,
        color = color,
        userId = userId ?: ""
    )
}
