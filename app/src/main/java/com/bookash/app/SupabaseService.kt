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
    
    /**
     * Busca categorias padrão (user_id NULL) + categorias do usuário.
     * IMPORTANTE: Retorna categorias do sistema visíveis para todos + pessoais do usuário.
     */
    suspend fun getCategories(userId: String, type: String? = null): List<Category> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = type ?: "all"
        Log.d(TAG, "[CATEGORIES] GET - Iniciando busca (userId: $userId, filtro: $filter)")
        
        try {
            // Busca categorias padrão (user_id NULL) + categorias do usuário
            // Usa or=(user_id.is.null,user_id.eq.$userId) para combinar ambas
            val endpoint = if (type != null) {
                "$BASE_URL/rest/v1/categories?or=(user_id.is.null,user_id.eq.$userId)&type=eq.$type&select=*&order=is_default.desc,name.asc"
            } else {
                "$BASE_URL/rest/v1/categories?or=(user_id.is.null,user_id.eq.$userId)&select=*&order=is_default.desc,name.asc"
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
                val defaultCount = categories.count { it.isDefault }
                val personalCount = categories.count { !it.isDefault }
                Log.i(TAG, "[CATEGORIES] GET - Sucesso: $defaultCount padrão + $personalCount pessoais (${duration}ms)")
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
    
    /**
     * Salva uma nova categoria para o usuário logado.
     * IMPORTANTE: Inclui user_id para garantir propriedade do dado.
     */
    suspend fun saveCategory(category: Category, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] CREATE - Iniciando: name='${category.name}', type=${category.type}, userId=$userId")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/categories").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${category.name}","type":"${category.type}","color":"${category.color}","icon":"${category.icon}","user_id":"$userId"}"""
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
    
    suspend fun deleteCategory(categoryId: String, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] DELETE - Iniciando: id=$categoryId, userId=$userId")
        
        try {
            // Verificar se a categoria é padrão (não pode excluir)
            if (userId != null) {
                val category = getCategoryById(categoryId, userId)
                if (category != null && category.isDefault) {
                    Log.w(TAG, "[CATEGORIES] DELETE - Bloqueado: categoria padrão não pode ser excluída")
                    return@withContext false
                }
            }
            
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
    
    suspend fun updateCategory(category: Category, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] UPDATE - Iniciando: id=${category.id}, name='${category.name}', userId=$userId")
        
        try {
            // Primeiro verificar se a categoria pertence ao usuário (não é padrão)
            val checkUrl = "$BASE_URL/rest/v1/categories?id=eq.${category.id}&user_id=eq.$userId&select=id"
            val checkConn = URL(checkUrl).openConnection() as HttpURLConnection
            checkConn.requestMethod = "GET"
            checkConn.setRequestProperty("apikey", API_KEY)
            checkConn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            if (checkConn.responseCode != 200) {
                Log.w(TAG, "[CATEGORIES] UPDATE - Erro ao verificar propriedade")
                return@withContext false
            }
            
            val checkResponse = checkConn.inputStream.bufferedReader().readText()
            val checkArray = JSONArray(checkResponse)
            
            if (checkArray.length() == 0) {
                Log.w(TAG, "[CATEGORIES] UPDATE - Categoria não pertence ao usuário ou é padrão (não pode editar)")
                return@withContext false
            }
            
            // Agora pode atualizar
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
     * Verifica se já existe uma categoria com o mesmo nome e tipo para o usuário.
     * Ignora a categoria com o ID fornecido (útil para edição).
     */
    suspend fun categoryExists(name: String, type: String, excludeId: String? = null, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] EXISTS - Verificando: name='$name', type=$type, userId=$userId")
        
        try {
            // Busca categorias com o mesmo nome (case-insensitive) e tipo
            var endpoint = "$BASE_URL/rest/v1/categories?name=ilike.$name&type=eq.$type&select=id"
            if (userId != null) {
                endpoint += "&user_id=eq.$userId"
            }
            
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
    
    /**
     * Busca uma categoria específica por ID.
     * Retorna null se não encontrar ou se for de outro usuário.
     */
    suspend fun getCategoryById(categoryId: String, userId: String): Category? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] GET_BY_ID - Buscando: id=$categoryId, userId=$userId")
        
        try {
            val endpoint = "$BASE_URL/rest/v1/categories?id=eq.$categoryId&select=*&limit=1"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                
                if (jsonArray.length() > 0) {
                    val category = parseCategories(jsonArray).first()
                    Log.i(TAG, "[CATEGORIES] GET_BY_ID - Sucesso: '${category.name}' (${duration}ms)")
                    category
                } else {
                    Log.w(TAG, "[CATEGORIES] GET_BY_ID - Não encontrada (${duration}ms)")
                    null
                }
            } else {
                Log.w(TAG, "[CATEGORIES] GET_BY_ID - Falha: HTTP $responseCode (${duration}ms)")
                null
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] GET_BY_ID - Erro após ${duration}ms", e)
            null
        }
    }
    
    private fun parseCategories(jsonArray: JSONArray): List<Category> {
        val list = mutableListOf<Category>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val userId = json.optString("user_id", "")
            val isDefault = json.optBoolean("is_default", userId.isEmpty())
            val category = Category(
                id = json.optString("id"),
                name = json.optString("name"),
                type = json.optString("type"),
                color = json.optString("color", "#357266"),
                icon = json.optString("icon", "category"),
                userId = userId,
                isDefault = isDefault
            )
            list.add(category)
        }
        return list
    }
    
    // ============== ACCOUNTS ==============
    
    /**
     * Busca contas do usuário logado.
     * IMPORTANTE: Sempre filtra por user_id para isolamento de dados.
     */
    suspend fun getAccounts(userId: String, archived: Boolean = false): List<Account> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val filter = if (archived) "arquivadas" else "ativas"
        Log.d(TAG, "[ACCOUNTS] GET - Iniciando busca (userId: $userId, filtro: $filter)")
        
        try {
            val endpoint = "$BASE_URL/rest/v1/accounts?user_id=eq.$userId&is_archived=eq.$archived&select=*&order=created_at.desc"
            
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
    
    /**
     * Salva uma nova conta para o usuário logado.
     * IMPORTANTE: Inclui user_id para garantir propriedade do dado.
     */
    suspend fun saveAccount(account: Account, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] CREATE - Iniciando: name='${account.name}', type=${account.type}, userId=$userId")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/accounts").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${account.name}","balance":${account.balance},"type":"${account.type}","icon":"${account.icon}","user_id":"$userId"}"""
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
    
    /**
     * Cria a conta padrão 'Carteira' para um novo usuário.
     * Chamado automaticamente pelo trigger no banco, mas disponível como fallback.
     */
    suspend fun createDefaultAccount(userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] DEFAULT - Criando conta Carteira para userId: $userId")
        
        try {
            // Verificar se já existe conta para este usuário
            val existingAccounts = getAccounts(userId, archived = false)
            if (existingAccounts.isNotEmpty()) {
                Log.i(TAG, "[ACCOUNTS] DEFAULT - Usuário já possui contas, pulando criação")
                return@withContext true
            }
            
            val conn = URL("$BASE_URL/rest/v1/accounts").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"Carteira","balance":0,"type":"dinheiro","icon":"wallet","user_id":"$userId"}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ACCOUNTS] DEFAULT - Sucesso: conta 'Carteira' criada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ACCOUNTS] DEFAULT - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] DEFAULT - Erro ao criar conta padrão após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateAccount(account: Account, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
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
                isArchived = json.optBoolean("is_archived", false),
                userId = json.optString("user_id", "")
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
    
    // ============== TAGS ==============
    
    suspend fun getTags(userId: String): List<Tag> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] READ - Iniciando busca para userId: $userId")
        
        try {
            val url = "$BASE_URL/rest/v1/tags?user_id=eq.$userId&select=*&order=created_at.desc"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val tags = parseTags(JSONArray(response))
                Log.i(TAG, "[TAGS] READ - Sucesso: ${tags.size} tags encontradas (${duration}ms)")
                tags
            } else {
                Log.w(TAG, "[TAGS] READ - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
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
            val conn = URL("$BASE_URL/rest/v1/tags").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${tag.name}","color":"${tag.color}","user_id":"$userId"}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[TAGS] CREATE - Sucesso: '${tag.name}' criada (${duration}ms)")
                true
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.readText()
                Log.w(TAG, "[TAGS] CREATE - Falha: HTTP $responseCode, $errorStream (${duration}ms)")
                false
            }
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
            val conn = URL("$BASE_URL/rest/v1/tags?id=eq.${tag.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${tag.name}","color":"${tag.color}"}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[TAGS] UPDATE - Sucesso: '${tag.name}' atualizada (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[TAGS] UPDATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
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
            val conn = URL("$BASE_URL/rest/v1/tags?id=eq.$tagId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[TAGS] DELETE - Sucesso: tag excluída (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[TAGS] DELETE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] DELETE - Erro após ${duration}ms", e)
            false
        }
    }
    
    suspend fun tagExists(name: String, userId: String, excludeId: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            var url = "$BASE_URL/rest/v1/tags?name=eq.${java.net.URLEncoder.encode(name, "UTF-8")}&user_id=eq.$userId&select=id"
            if (excludeId != null) {
                url += "&id=neq.$excludeId"
            }
            
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                jsonArray.length() > 0
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TAGS] EXISTS - Erro ao verificar existência", e)
            false
        }
    }
    
    private fun parseTags(jsonArray: JSONArray): List<Tag> {
        val list = mutableListOf<Tag>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(Tag(
                id = json.optString("id"),
                name = json.optString("name"),
                color = json.optString("color", "#357266"),
                userId = json.optString("user_id", "")
            ))
        }
        return list
    }
    
    // ============== APP SETTINGS ==============
    
    /**
     * Busca as configurações do usuário.
     * Se não existir, retorna configurações padrão.
     * IMPORTANTE: Sempre filtra por user_id para isolamento de dados.
     */
    suspend fun getSettings(userId: String): AppSettings = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[SETTINGS] GET - Iniciando busca (userId: $userId)")
        
        try {
            val endpoint = "$BASE_URL/rest/v1/app_settings?user_id=eq.$userId&select=*&limit=1"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                
                if (jsonArray.length() > 0) {
                    val settings = parseAppSettings(jsonArray.getJSONObject(0))
                    Log.i(TAG, "[SETTINGS] GET - Sucesso: settings encontradas (${duration}ms)")
                    settings
                } else {
                    // No settings found, return defaults
                    Log.i(TAG, "[SETTINGS] GET - Nenhuma configuração encontrada, usando padrão (${duration}ms)")
                    AppSettings(userId = userId)
                }
            } else {
                Log.w(TAG, "[SETTINGS] GET - Falha: HTTP $responseCode (${duration}ms)")
                AppSettings(userId = userId)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[SETTINGS] GET - Erro após ${duration}ms", e)
            AppSettings(userId = userId)
        }
    }
    
    /**
     * Salva ou atualiza as configurações do usuário.
     * Usa upsert para criar se não existir ou atualizar se já existir.
     * IMPORTANTE: Inclui user_id para garantir propriedade do dado.
     */
    suspend fun updateSettings(settings: AppSettings, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[SETTINGS] UPDATE - Iniciando: theme=${settings.theme}, language=${settings.language}, userId=$userId")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/app_settings").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $API_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.doOutput = true
            
            val body = """{"user_id":"$userId","theme":"${settings.theme}","language":"${settings.language}","notifications_enabled":${settings.notificationsEnabled}}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[SETTINGS] UPDATE - Sucesso: configurações salvas (${duration}ms)")
                true
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.readText()
                Log.w(TAG, "[SETTINGS] UPDATE - Falha: HTTP $responseCode, $errorStream (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[SETTINGS] UPDATE - Erro ao salvar configurações após ${duration}ms", e)
            false
        }
    }
    
    private fun parseAppSettings(json: org.json.JSONObject): AppSettings {
        return AppSettings(
            id = json.optString("id"),
            userId = json.optString("user_id"),
            theme = json.optString("theme", "system"),
            language = json.optString("language", "pt-BR"),
            notificationsEnabled = json.optBoolean("notifications_enabled", true),
            createdAt = json.optString("created_at", ""),
            updatedAt = json.optString("updated_at", "")
        )
    }
}
