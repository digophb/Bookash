package com.bookash.app

import android.util.Log
import io.github.jan.tennert.supabase.postgrest.postgrest
import io.github.jan.tennert.supabase.postgrest.query.Columns
import io.github.jan.tennert.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

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
    
    suspend fun getCategories(userId: String, type: String? = null): List<Category> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = type ?: "all"
        Log.d(TAG, "[CATEGORIES] GET - Iniciando busca (userId: $userId, filtro: $filter)")
        
        try {
            val query = SupabaseClient.client.postgrest["categories"]
                .select(columns = Columns.ALL) {
                    filter("user_id", FilterOperator.EQ, userId)
                    if (type != null) {
                        filter("type", FilterOperator.EQ, type)
                    }
                }
            
            val result = query.decodeList<Category>()
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[CATEGORIES] GET - Sucesso: ${result.size} categorias encontradas (${duration}ms)")
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveCategory(category: Category, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] CREATE - Iniciando: name='${category.name}', userId=$userId")
        
        try {
            val newCategory = category.copy(userId = userId)
            SupabaseClient.client.postgrest["categories"].insert(newCategory)
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[CATEGORIES] CREATE - Sucesso: '${category.name}' criada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] CREATE - Erro ao criar '${category.name}' após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateCategory(category: Category, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] UPDATE - Iniciando: id=${category.id}, name='${category.name}'")
        
        try {
            SupabaseClient.client.postgrest["categories"].update(category) {
                filter("id", FilterOperator.EQ, category.id)
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
    
    suspend fun deleteCategory(categoryId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] DELETE - Iniciando: id=$categoryId")
        
        try {
            SupabaseClient.client.postgrest["categories"].delete {
                filter("id", FilterOperator.EQ, categoryId)
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
    
    suspend fun categoryExists(name: String, type: String, excludeId: String? = null, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] EXISTS - Verificando: name='$name', type=$type, userId=$userId")
        
        try {
            val categories = SupabaseClient.client.postgrest["categories"]
                .select(columns = Columns.list("id")) {
                    filter("name", FilterOperator.ILIKE, name)
                    filter("type", FilterOperator.EQ, type)
                    if (userId != null) {
                        filter("user_id", FilterOperator.EQ, userId)
                    }
                }.decodeList<CategoryIdResult>()
            
            val duration = System.currentTimeMillis() - startTime
            
            if (categories.isEmpty()) {
                Log.d(TAG, "[CATEGORIES] EXISTS - Não encontrada (${duration}ms)")
                return@withContext false
            }
            
            if (excludeId != null) {
                val foundOther = categories.any { it.id != excludeId }
                if (foundOther) {
                    Log.i(TAG, "[CATEGORIES] EXISTS - Duplicata encontrada (${duration}ms)")
                    return@withContext true
                }
                Log.d(TAG, "[CATEGORIES] EXISTS - Não há duplicata (excluindo self) (${duration}ms)")
                return@withContext false
            }
            
            Log.i(TAG, "[CATEGORIES] EXISTS - Duplicata encontrada (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] EXISTS - Erro após ${duration}ms", e)
            false
        }
    }
    
    // ============== ACCOUNTS ==============
    
    suspend fun getAccounts(userId: String, archived: Boolean = false): List<Account> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = if (archived) "arquivadas" else "ativas"
        Log.d(TAG, "[ACCOUNTS] GET - Iniciando busca (userId: $userId, filtro: $filter)")
        
        try {
            val accounts = SupabaseClient.client.postgrest["accounts"]
                .select(columns = Columns.ALL) {
                    filter("user_id", FilterOperator.EQ, userId)
                    filter("is_archived", FilterOperator.EQ, archived)
                    order("created_at", io.github.jan.tennert.supabase.postgrest.query.Order.ASCENDING, nullsFirst = true)
                }.decodeList<Account>()
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] GET - Sucesso: ${accounts.size} contas $filter encontradas (${duration}ms)")
            accounts
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveAccount(account: Account, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] CREATE - Iniciando: name='${account.name}', userId=$userId")
        
        try {
            val newAccount = account.copy(userId = userId)
            SupabaseClient.client.postgrest["accounts"].insert(newAccount)
            
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
            SupabaseClient.client.postgrest["accounts"].update(account) {
                filter("id", FilterOperator.EQ, account.id)
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
            SupabaseClient.client.postgrest["accounts"].update(
                mapOf("is_archived" to true)
            ) {
                filter("id", FilterOperator.EQ, accountId)
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
            SupabaseClient.client.postgrest["accounts"].update(
                mapOf("is_archived" to false)
            ) {
                filter("id", FilterOperator.EQ, accountId)
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
            SupabaseClient.client.postgrest["accounts"].delete {
                filter("id", FilterOperator.EQ, accountId)
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
            val transactions = SupabaseClient.client.postgrest["transactions"]
                .select(columns = Columns.ALL) {
                    filter("user_id", FilterOperator.EQ, userId)
                    order("date", io.github.jan.tennert.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit)
                }.decodeList<TransactionResult>()
            
            val result = transactions.map { it.toTransaction() }
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TRANSACTIONS] GET - Sucesso: ${result.size} transações encontradas (${duration}ms)")
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveTransaction(transaction: Transaction, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] CREATE - Iniciando: type=${transaction.type}")
        
        try {
            val data = mapOf(
                "user_id" to userId,
                "type" to transaction.type,
                "amount" to transaction.amount,
                "description" to transaction.description,
                "category" to transaction.category,
                "date" to transaction.date,
                "status" to transaction.status,
                "account_id" to transaction.accountId
            )
            
            SupabaseClient.client.postgrest["transactions"].insert(data)
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TRANSACTIONS] CREATE - Sucesso (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] CREATE - Erro após ${duration}ms", e)
            false
        }
    }
    
    // ============== TAGS ==============
    
    suspend fun getTags(userId: String): List<Tag> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] GET - Iniciando busca (userId: $userId)")
        
        try {
            val tags = SupabaseClient.client.postgrest["tags"]
                .select(columns = Columns.ALL) {
                    filter("user_id", FilterOperator.EQ, userId)
                }.decodeList<Tag>()
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TAGS] GET - Sucesso: ${tags.size} tags encontradas (${duration}ms)")
            tags
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveTag(tag: Tag, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] CREATE - Iniciando: name='${tag.name}', userId=$userId")
        
        try {
            val newTag = tag.copy(userId = userId)
            SupabaseClient.client.postgrest["tags"].insert(newTag)
            
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
        Log.d(TAG, "[TAGS] UPDATE - Iniciando: id=${tag.id}")
        
        try {
            SupabaseClient.client.postgrest["tags"].update(tag) {
                filter("id", FilterOperator.EQ, tag.id)
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TAGS] UPDATE - Sucesso (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] UPDATE - Erro após ${duration}ms", e)
            false
        }
    }
    
    suspend fun deleteTag(tagId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] DELETE - Iniciando: id=$tagId")
        
        try {
            SupabaseClient.client.postgrest["tags"].delete {
                filter("id", FilterOperator.EQ, tagId)
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[TAGS] DELETE - Sucesso (${duration}ms)")
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] DELETE - Erro após ${duration}ms", e)
            false
        }
    }
    
    suspend fun tagExists(name: String, excludeId: String? = null, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] EXISTS - Verificando: name='$name'")
        
        try {
            val tags = SupabaseClient.client.postgrest["tags"]
                .select(columns = Columns.list("id")) {
                    filter("name", FilterOperator.ILIKE, name)
                    if (userId != null) {
                        filter("user_id", FilterOperator.EQ, userId)
                    }
                }.decodeList<TagIdResult>()
            
            val duration = System.currentTimeMillis() - startTime
            
            if (tags.isEmpty()) {
                return@withContext false
            }
            
            if (excludeId != null) {
                return@withContext tags.any { it.id != excludeId }
            }
            
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] EXISTS - Erro após ${duration}ms", e)
            false
        }
    }
}

// Data classes para serialização
@Serializable
data class CategoryIdResult(val id: String)

@Serializable
data class TagIdResult(val id: String)

@Serializable
data class TransactionResult(
    val id: String,
    val user_id: String,
    val description: String,
    val category: String,
    val amount: Double,
    val type: String,
    val date: String,
    val status: String = "paid",
    val account_id: String? = null
) {
    fun toTransaction(): Transaction = Transaction(
        id = id,
        userId = user_id,
        description = description,
        category = category,
        amount = amount,
        type = type,
        date = date,
        status = status,
        accountId = account_id ?: "",
        iconRes = if (type == "income") R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
    )
}
