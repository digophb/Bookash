package com.bookash.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Serviço de comunicação com o Supabase.
 * 
 * IMPORTANTE: Todas as operações CRUD possuem logging para:
 * - Debugging em produção
 * - Auditoria (crítico em app financeiro)
 * - Monitoramento de performance
 * - Detecção de erros
 */
object SupabaseService {
    
    private const val TAG = "BookashAPI"
    
    private const val BASE_URL = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
    private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
    
    // ============== CATEGORIES ==============
    
    suspend fun getCategories(type: String? = null): List<Category> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = type ?: "all"
        Log.d(TAG, "[CATEGORIES] GET - Iniciando busca (filtro: $filter)")
        
        try {
            val endpoint = if (type != null) {
                "$BASE_URL/rest/v1/categories?type=eq.$type&select=*"
            } else {
                "$BASE_URL/rest/v1/categories?select=*"
            }
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val categories = parseCategories(JSONArray(response))
                Log.i(TAG, "[CATEGORIES] GET - Sucesso: ${categories.size} categorias encontradas (${duration}ms)")
                categories
            } else {
                Log.w(TAG, "[CATEGORIES] GET - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveCategory(category: Category): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] CREATE - Iniciando: name='${category.name}', type=${category.type}")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/categories").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${category.name}","type":"${category.type}","color":"${category.color}","icon":"${category.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[CATEGORIES] CREATE - Sucesso: '${category.name}' criada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[CATEGORIES] CREATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
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
            val conn = URL("$BASE_URL/rest/v1/categories?id=eq.$categoryId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[CATEGORIES] DELETE - Sucesso: id=$categoryId (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[CATEGORIES] DELETE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] DELETE - Erro ao excluir id=$categoryId após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateCategory(category: Category): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] UPDATE - Iniciando: id=${category.id}, name='${category.name}'")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/categories?id=eq.${category.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${category.name}","type":"${category.type}","color":"${category.color}","icon":"${category.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[CATEGORIES] UPDATE - Sucesso: '${category.name}' atualizada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[CATEGORIES] UPDATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] UPDATE - Erro ao atualizar id=${category.id} após ${duration}ms", e)
            false
        }
    }
    
    /**
     * Verifica se já existe uma categoria com o mesmo nome e tipo.
     * Ignora a categoria com o ID fornecido (útil para edição).
     */
    suspend fun categoryExists(name: String, type: String, excludeId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] EXISTS - Verificando: name='$name', type=$type")
        
        try {
            // Busca categorias com o mesmo nome (case-insensitive) e tipo
            val endpoint = "$BASE_URL/rest/v1/categories?name=ilike.$name&type=eq.$type&select=id"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                
                // Se não há resultados, não existe duplicata
                if (jsonArray.length() == 0) {
                    Log.d(TAG, "[CATEGORIES] EXISTS - Não encontrada (${duration}ms)")
                    return@withContext false
                }
                
                // Se estamos editando, ignorar a própria categoria
                if (excludeId != null) {
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        val foundId = json.optString("id")
                        if (foundId != excludeId) {
                            Log.i(TAG, "[CATEGORIES] EXISTS - Duplicata encontrada: id=$foundId (${duration}ms)")
                            return@withContext true
                        }
                    }
                    Log.d(TAG, "[CATEGORIES] EXISTS - Não há duplicata (excluindo self) (${duration}ms)")
                    return@withContext false
                }
                
                Log.i(TAG, "[CATEGORIES] EXISTS - Duplicata encontrada (${duration}ms)")
                return@withContext true
            }
            
            Log.w(TAG, "[CATEGORIES] EXISTS - Falha na verificação: HTTP $responseCode (${duration}ms)")
            false
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] EXISTS - Erro após ${duration}ms", e)
            false // Em caso de erro, permitir salvar para não bloquear o usuário
        }
    }
    
    private fun parseCategories(jsonArray: JSONArray): List<Category> {
        val list = mutableListOf<Category>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val category = Category(
                id = json.optString("id"),
                name = json.optString("name"),
                type = json.optString("type"),
                color = json.optString("color", "#357266"),
                icon = json.optString("icon", "category")
            )
            list.add(category)
        }
        return list
    }
    
    // ============== ACCOUNTS ==============
    
    suspend fun getAccounts(archived: Boolean = false): List<Account> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = if (archived) "arquivadas" else "ativas"
        Log.d(TAG, "[ACCOUNTS] GET - Iniciando busca (filtro: $filter)")
        
        try {
            val endpoint = "$BASE_URL/rest/v1/accounts?is_archived=eq.$archived&select=*&order=created_at.desc"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val accounts = parseAccounts(JSONArray(response))
                Log.i(TAG, "[ACCOUNTS] GET - Sucesso: ${accounts.size} contas $filter encontradas (${duration}ms)")
                accounts
            } else {
                Log.w(TAG, "[ACCOUNTS] GET - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    suspend fun saveAccount(account: Account): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] CREATE - Iniciando: name='${account.name}', type=${account.type}")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${account.name}","balance":${account.balance},"type":"${account.type}","icon":"${account.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ACCOUNTS] CREATE - Sucesso: '${account.name}' criada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ACCOUNTS] CREATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] CREATE - Erro ao criar '${account.name}' após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateAccount(account: Account): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] UPDATE - Iniciando: id=${account.id}, name='${account.name}'")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.${account.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${account.name}","balance":${account.balance},"type":"${account.type}","icon":"${account.icon}"}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ACCOUNTS] UPDATE - Sucesso: '${account.name}' atualizada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ACCOUNTS] UPDATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
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
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"is_archived":true}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ACCOUNTS] ARCHIVE - Sucesso: id=$accountId arquivada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ACCOUNTS] ARCHIVE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
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
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"is_archived":false}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ACCOUNTS] REACTIVATE - Sucesso: id=$accountId reativada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ACCOUNTS] REACTIVATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
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
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ACCOUNTS] DELETE - Sucesso: id=$accountId excluída (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ACCOUNTS] DELETE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] DELETE - Erro ao excluir id=$accountId após ${duration}ms", e)
            false
        }
    }
    
    private fun parseAccounts(jsonArray: JSONArray): List<Account> {
        val list = mutableListOf<Account>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(Account(
                id = json.optString("id"),
                name = json.optString("name"),
                balance = json.optDouble("balance", 0.0),
                type = json.optString("type", "corrente"),
                icon = json.optString("icon", "wallet"),
                isArchived = json.optBoolean("is_archived", false)
            ))
        }
        return list
    }
    
    // ============== TRANSACTIONS ==============
    
    suspend fun getTransactions(userId: String, limit: Int = 50): List<Transaction> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] GET - Iniciando busca (userId: $userId, limit: $limit)")
        
        try {
            val endpoint = "$BASE_URL/rest/v1/transactions?user_id=eq.$userId&order=date.desc&limit=$limit&select=*"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val transactions = parseTransactions(JSONArray(response))
                Log.i(TAG, "[TRANSACTIONS] GET - Sucesso: ${transactions.size} transações encontradas (${duration}ms)")
                transactions
            } else {
                Log.w(TAG, "[TRANSACTIONS] GET - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
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
            val conn = URL("$BASE_URL/rest/v1/transactions").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = buildString {
                append("{")
                append("\"user_id\":\"${transaction.userId}\",")
                append("\"type\":\"${transaction.type}\",")
                append("\"amount\":${transaction.amount},")
                append("\"description\":\"${transaction.description}\",")
                append("\"category\":\"${transaction.category}\",")
                append("\"date\":\"${transaction.date}\",")
                append("\"status\":\"${transaction.status}\"")
                if (transaction.accountId.isNotEmpty()) {
                    append(",\"account_id\":\"${transaction.accountId}\"")
                }
                if (transaction.isRecurring) {
                    append(",\"is_recurring\":true")
                    append(",\"recurrence_period\":\"${transaction.recurrencePeriod}\"")
                    append(",\"recurrence_count\":${transaction.recurrenceCount}")
                }
                append("}")
            }
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[TRANSACTIONS] CREATE - Sucesso: ${transaction.type} '${transaction.description}' (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[TRANSACTIONS] CREATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] CREATE - Erro ao criar transação após ${duration}ms", e)
            false
        }
    }
    
    private fun parseTransactions(jsonArray: JSONArray): List<Transaction> {
        val list = mutableListOf<Transaction>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val type = json.optString("type")
            list.add(Transaction(
                id = json.optString("id"),
                userId = json.optString("user_id"),
                description = json.optString("description"),
                category = json.optString("category"),
                amount = json.optDouble("amount", 0.0),
                type = type,
                date = json.optString("date"),
                status = json.optString("status", "paid"),
                accountId = json.optString("account_id", ""),
                iconRes = if (type == "income") R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            ))
        }
        return list
    }
}
