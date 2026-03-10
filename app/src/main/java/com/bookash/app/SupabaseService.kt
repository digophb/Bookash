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
            // Obter token do usuário para RLS - necessário para ver categorias pessoais
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[CATEGORIES] GET - Erro: usuário não autenticado")
                return@withContext emptyList()
            }
            
            // Busca apenas categorias do usuario (isolamento total)
            val endpoint = if (type != null) {
                "$BASE_URL/rest/v1/categories?user_id=eq.$userId&type=eq.$type&select=*&order=is_default.desc,name.asc"
            } else {
                "$BASE_URL/rest/v1/categories?user_id=eq.$userId&select=*&order=is_default.desc,name.asc"
            }
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[CATEGORIES] CREATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/categories").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[CATEGORIES] DELETE - Erro: usuário não autenticado")
                return@withContext false
            }
            
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
            conn.setRequestProperty("Authorization", "Bearer $token")
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[CATEGORIES] UPDATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            // Primeiro verificar se a categoria pertence ao usuário (não é padrão)
            val checkUrl = "$BASE_URL/rest/v1/categories?id=eq.${category.id}&user_id=eq.$userId&select=id"
            val checkConn = URL(checkUrl).openConnection() as HttpURLConnection
            checkConn.requestMethod = "GET"
            checkConn.setRequestProperty("apikey", API_KEY)
            checkConn.setRequestProperty("Authorization", "Bearer $token")
            
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
            conn.setRequestProperty("Authorization", "Bearer $token")
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
     * Considera tanto categorias do usuário quanto categorias padrão do sistema.
     * Ignora a categoria com o ID fornecido (útil para edição).
     */
    suspend fun categoryExists(name: String, type: String, excludeId: String? = null, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] EXISTS - Verificando: name='$name', type=$type, userId=$userId")
        
        try {
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[CATEGORIES] EXISTS - Erro: usuário não autenticado")
                return@withContext false
            }
            
            // Busca categorias com o mesmo nome (case-insensitive) e tipo
            // Considera tanto categorias do usuário quanto categorias padrão (user_id is null)
            val endpoint = "$BASE_URL/rest/v1/categories?name=ilike.${java.net.URLEncoder.encode(name, "UTF-8")}&type=eq.$type&select=id,user_id"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
                
                // Verificar cada resultado
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val foundId = json.optString("id")
                    val foundUserId = json.optString("user_id", null)
                    
                    // Ignorar a própria categoria (para edição)
                    if (excludeId != null && foundId == excludeId) {
                        continue
                    }
                    
                    // Se é categoria padrão (user_id null) ou do próprio usuário, é duplicata
                    if (foundUserId.isNullOrEmpty() || foundUserId == userId) {
                        Log.i(TAG, "[CATEGORIES] EXISTS - Duplicata encontrada: id=$foundId (${duration}ms)")
                        return@withContext true
                    }
                }
                
                Log.d(TAG, "[CATEGORIES] EXISTS - Não há duplicata (${duration}ms)")
                return@withContext false
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[CATEGORIES] GET_BY_ID - Erro: usuário não autenticado")
                return@withContext null
            }
            
            val endpoint = "$BASE_URL/rest/v1/categories?id=eq.$categoryId&select=*&limit=1"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] GET - Erro: usuário não autenticado")
                return@withContext emptyList()
            }
            
            val endpoint = "$BASE_URL/rest/v1/accounts?user_id=eq.$userId&is_archived=eq.$archived&select=*&order=created_at.desc"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] CREATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/accounts").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${account.name}","balance":${account.balance},"type":"${account.type}","icon":"${account.icon}","include_in_balance":${account.includeInBalance},"user_id":"$userId"}"""
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
     * @deprecated Use createDefaultAccounts() para criar Carteira + Banco
     */
    suspend fun createDefaultAccount(userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] DEFAULT - Criando conta Carteira para userId: $userId")
        
        try {
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] DEFAULT - Erro: usuário não autenticado")
                return@withContext false
            }
            
            // Verificar se já existe conta para este usuário
            val existingAccounts = getAccounts(userId, archived = false)
            if (existingAccounts.isNotEmpty()) {
                Log.i(TAG, "[ACCOUNTS] DEFAULT - Usuário já possui contas, pulando criação")
                return@withContext true
            }
            
            val conn = URL("$BASE_URL/rest/v1/accounts").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
    
    /**
     * Cria as contas padrão 'Carteira' e 'Banco' para um novo usuário.
     * Chamado automaticamente pelo trigger no banco, mas disponível como fallback.
     */
    suspend fun createDefaultAccounts(userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] DEFAULT_ACCOUNTS - Criando Carteira + Banco para userId: $userId")
        
        try {
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] DEFAULT_ACCOUNTS - Erro: usuário não autenticado")
                return@withContext false
            }
            
            // Verificar se já existe conta para este usuário
            val existingAccounts = getAccounts(userId, archived = false)
            if (existingAccounts.isNotEmpty()) {
                Log.i(TAG, "[ACCOUNTS] DEFAULT_ACCOUNTS - Usuário já possui ${existingAccounts.size} contas, pulando criação")
                return@withContext true
            }
            
            // Criar as duas contas padrão: Carteira e Banco
            val defaultAccounts = listOf(
                """{"name":"Carteira","balance":0,"type":"dinheiro","icon":"wallet","user_id":"$userId"}""",
                """{"name":"Banco","balance":0,"type":"corrente","icon":"bank","user_id":"$userId"}"""
            )
            
            var allSuccess = true
            for (accountBody in defaultAccounts) {
                val conn = URL("$BASE_URL/rest/v1/accounts").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                
                conn.outputStream.write(accountBody.toByteArray())
                
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    Log.w(TAG, "[ACCOUNTS] DEFAULT_ACCOUNTS - Falha ao criar conta: HTTP $responseCode")
                    allSuccess = false
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            if (allSuccess) {
                Log.i(TAG, "[ACCOUNTS] DEFAULT_ACCOUNTS - Sucesso: Carteira e Banco criadas (${duration}ms)")
            } else {
                Log.w(TAG, "[ACCOUNTS] DEFAULT_ACCOUNTS - Parcial: algumas contas falharam (${duration}ms)")
            }
            
            allSuccess
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] DEFAULT_ACCOUNTS - Erro ao criar contas padrão após ${duration}ms", e)
            false
        }
    }
    
    /**
     * Cria as categorias padrao para um novo usuario.
     * Chamado automaticamente pelo trigger no banco, mas disponivel como fallback.
     */
    suspend fun createDefaultCategories(userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[CATEGORIES] DEFAULT - Criando categorias padrao para userId: $userId")
        
        try {
            // Obter token do usuario para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[CATEGORIES] DEFAULT - Erro: usuario nao autenticado")
                return@withContext false
            }
            
            // Verificar se ja existe categoria para este usuario
            val existingCategories = getCategories(userId)
            if (existingCategories.isNotEmpty()) {
                Log.i(TAG, "[CATEGORIES] DEFAULT - Usuario ja possui ${existingCategories.size} categorias, pulando criacao")
                return@withContext true
            }
            
            // Criar categorias padrao
            val defaultCategories = listOf(
                """{"name":"Alimentacao","type":"expense","color":"#FF6B6B","icon":"restaurant","user_id":"$userId"}""",
                """{"name":"Transporte","type":"expense","color":"#4ECDC4","icon":"directions_car","user_id":"$userId"}""",
                """{"name":"Salario","type":"income","color":"#45B7D1","icon":"attach_money","user_id":"$userId"}""",
                """{"name":"Lazer","type":"expense","color":"#96CEB4","icon":"sports_esports","user_id":"$userId"}""",
                """{"name":"Saude","type":"expense","color":"#FFEAA7","icon":"local_hospital","user_id":"$userId"}""",
                """{"name":"Educacao","type":"expense","color":"#DDA0DD","icon":"school","user_id":"$userId"}""",
                """{"name":"Moradia","type":"expense","color":"#98D8C8","icon":"home","user_id":"$userId"}""",
                """{"name":"Outros","type":"expense","color":"#B0BEC5","icon":"more_horiz","user_id":"$userId"}"""
            )
            
            var allSuccess = true
            for (categoryBody in defaultCategories) {
                val conn = URL("$BASE_URL/rest/v1/categories").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                
                conn.outputStream.write(categoryBody.toByteArray())
                
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    Log.w(TAG, "[CATEGORIES] DEFAULT - Falha ao criar categoria: HTTP $responseCode")
                    allSuccess = false
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            if (allSuccess) {
                Log.i(TAG, "[CATEGORIES] DEFAULT - Sucesso: 8 categorias padrao criadas (${duration}ms)")
            } else {
                Log.w(TAG, "[CATEGORIES] DEFAULT - Parcial: algumas categorias falharam (${duration}ms)")
            }
            
            allSuccess
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[CATEGORIES] DEFAULT - Erro ao criar categorias padrao após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateAccount(account: Account, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] UPDATE - Iniciando: id=${account.id}, name='${account.name}'")
        
        try {
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] UPDATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.${account.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """{"name":"${account.name}","balance":${account.balance},"type":"${account.type}","icon":"${account.icon}","include_in_balance":${account.includeInBalance}}"""
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] ARCHIVE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] REACTIVATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] DELETE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/accounts?id=eq.$accountId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
    
    /**
     * Calcula o saldo de uma conta baseado nas transacoes.
     * Receitas somam, despesas subtraem, transferencias consideram origem/destino.
     */
    suspend fun getAccountCalculatedBalance(accountId: String): Double = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ACCOUNTS] BALANCE - Calculando saldo para conta: $accountId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ACCOUNTS] BALANCE - Erro: usuario nao autenticado")
                return@withContext 0.0
            }
            
            var balance = 0.0
            
            // 1. Receitas associadas a esta conta (account_id)
            val incomeEndpoint = "$BASE_URL/rest/v1/transactions?account_id=eq.$accountId&type=eq.income&select=amount"
            val incomeConn = URL(incomeEndpoint).openConnection() as HttpURLConnection
            incomeConn.requestMethod = "GET"
            incomeConn.setRequestProperty("apikey", API_KEY)
            incomeConn.setRequestProperty("Authorization", "Bearer $token")
            
            if (incomeConn.responseCode == 200) {
                val incomeResponse = incomeConn.inputStream.bufferedReader().readText()
                val incomeArray = org.json.JSONArray(incomeResponse)
                for (i in 0 until incomeArray.length()) {
                    balance += incomeArray.getJSONObject(i).optDouble("amount", 0.0)
                }
            }
            
            // 2. Despesas associadas a esta conta (account_id)
            val expenseEndpoint = "$BASE_URL/rest/v1/transactions?account_id=eq.$accountId&type=eq.expense&select=amount"
            val expenseConn = URL(expenseEndpoint).openConnection() as HttpURLConnection
            expenseConn.requestMethod = "GET"
            expenseConn.setRequestProperty("apikey", API_KEY)
            expenseConn.setRequestProperty("Authorization", "Bearer $token")
            
            if (expenseConn.responseCode == 200) {
                val expenseResponse = expenseConn.inputStream.bufferedReader().readText()
                val expenseArray = org.json.JSONArray(expenseResponse)
                for (i in 0 until expenseArray.length()) {
                    balance -= expenseArray.getJSONObject(i).optDouble("amount", 0.0)
                }
            }
            
            // 3. Transferencias recebidas (to_account_id = esta conta)
            val transferInEndpoint = "$BASE_URL/rest/v1/transactions?to_account_id=eq.$accountId&type=eq.transfer&select=amount"
            val transferInConn = URL(transferInEndpoint).openConnection() as HttpURLConnection
            transferInConn.requestMethod = "GET"
            transferInConn.setRequestProperty("apikey", API_KEY)
            transferInConn.setRequestProperty("Authorization", "Bearer $token")
            
            if (transferInConn.responseCode == 200) {
                val transferInResponse = transferInConn.inputStream.bufferedReader().readText()
                val transferInArray = org.json.JSONArray(transferInResponse)
                for (i in 0 until transferInArray.length()) {
                    balance += transferInArray.getJSONObject(i).optDouble("amount", 0.0)
                }
            }
            
            // 4. Transferencias enviadas (from_account_id = esta conta)
            val transferOutEndpoint = "$BASE_URL/rest/v1/transactions?from_account_id=eq.$accountId&type=eq.transfer&select=amount"
            val transferOutConn = URL(transferOutEndpoint).openConnection() as HttpURLConnection
            transferOutConn.requestMethod = "GET"
            transferOutConn.setRequestProperty("apikey", API_KEY)
            transferOutConn.setRequestProperty("Authorization", "Bearer $token")
            
            if (transferOutConn.responseCode == 200) {
                val transferOutResponse = transferOutConn.inputStream.bufferedReader().readText()
                val transferOutArray = org.json.JSONArray(transferOutResponse)
                for (i in 0 until transferOutArray.length()) {
                    balance -= transferOutArray.getJSONObject(i).optDouble("amount", 0.0)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "[ACCOUNTS] BALANCE - Saldo calculado: R$ $balance (${duration}ms)")
            balance
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ACCOUNTS] BALANCE - Erro ao calcular saldo após ${duration}ms", e)
            0.0
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
                includeInBalance = json.optBoolean("include_in_balance", true),
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TRANSACTIONS] GET - Erro: usuário não autenticado")
                return@withContext emptyList()
            }
            
            val endpoint = "$BASE_URL/rest/v1/transactions?user_id=eq.$userId&order=date.desc&limit=$limit&select=*"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
    
    /**
     * Salva uma nova transacao e retorna o ID criado.
     */
    suspend fun saveTransaction(transaction: Transaction, token: String): String? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] CREATE - Iniciando: type=${transaction.type}, desc='${transaction.description}'")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/transactions").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true
            
            val body = buildString {
                append("{")
                append("\"user_id\":\"${transaction.userId}\",")
                append("\"type\":\"${transaction.type}\",")
                append("\"amount\":${transaction.amount},")
                append("\"description\":\"${transaction.description}\",")
                append("\"date\":\"${transaction.date}\"")
                if (transaction.categoryId.isNotEmpty()) {
                    append(",\"category_id\":\"${transaction.categoryId}\"")
                }
                if (!transaction.notes.isNullOrEmpty()) {
                    append(",\"notes\":\"${transaction.notes}\"")
                }
                if (transaction.creditCardId != null) {
                    append(",\"credit_card_id\":\"${transaction.creditCardId}\"")
                }
                // Conta associada
                if (transaction.accountId != null) {
                    append(",\"account_id\":\"${transaction.accountId}\"")
                }
                // Campos de transferencia
                if (transaction.fromAccountId != null) {
                    append(",\"from_account_id\":\"${transaction.fromAccountId}\"")
                }
                if (transaction.toAccountId != null) {
                    append(",\"to_account_id\":\"${transaction.toAccountId}\"")
                }
                if (transaction.isRecurring) {
                    append(",\"is_recurring\":true")
                    if (transaction.recurringType != null) {
                        append(",\"recurring_type\":\"${transaction.recurringType}\"")
                    }
                    if (transaction.recurringUntil != null) {
                        append(",\"recurring_until\":\"${transaction.recurringUntil}\"")
                    }
                }
                append("}")
            }
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = org.json.JSONArray(response)
                if (jsonArray.length() > 0) {
                    val transactionId = jsonArray.getJSONObject(0).optString("id")
                    Log.i(TAG, "[TRANSACTIONS] CREATE - Sucesso: id=$transactionId (${duration}ms)")
                    transactionId
                } else {
                    Log.w(TAG, "[TRANSACTIONS] CREATE - Sucesso mas sem ID retornado (${duration}ms)")
                    null
                }
            } else {
                Log.w(TAG, "[TRANSACTIONS] CREATE - Falha: HTTP $responseCode (${duration}ms)")
                null
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] CREATE - Erro ao criar transacao apos ${duration}ms", e)
            null
        }
    }
    
    /**
     * Busca uma transacao especifica por ID.
     */
    suspend fun getTransactionById(transactionId: String): Transaction? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] GET_BY_ID - Buscando: id=$transactionId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TRANSACTIONS] GET_BY_ID - Erro: usuario nao autenticado")
                return@withContext null
            }
            
            val endpoint = "$BASE_URL/rest/v1/transactions?id=eq.$transactionId&select=*&limit=1"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                
                if (jsonArray.length() > 0) {
                    val transaction = parseTransaction(jsonArray.getJSONObject(0))
                    Log.i(TAG, "[TRANSACTIONS] GET_BY_ID - Sucesso: '${transaction.description}' (${duration}ms)")
                    transaction
                } else {
                    Log.w(TAG, "[TRANSACTIONS] GET_BY_ID - Nao encontrada (${duration}ms)")
                    null
                }
            } else {
                Log.w(TAG, "[TRANSACTIONS] GET_BY_ID - Falha: HTTP $responseCode (${duration}ms)")
                null
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] GET_BY_ID - Erro apos ${duration}ms", e)
            null
        }
    }
    
    /**
     * Atualiza uma transacao existente.
     */
    suspend fun updateTransaction(transaction: Transaction, token: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] UPDATE - Iniciando: id=${transaction.id}, type=${transaction.type}")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/transactions?id=eq.${transaction.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = buildString {
                append("{")
                append("\"type\":\"${transaction.type}\",")
                append("\"amount\":${transaction.amount},")
                append("\"description\":\"${transaction.description}\",")
                append("\"date\":\"${transaction.date}\"")
                if (transaction.categoryId.isNotEmpty()) {
                    append(",\"category_id\":\"${transaction.categoryId}\"")
                }
                if (!transaction.notes.isNullOrEmpty()) {
                    append(",\"notes\":\"${transaction.notes}\"")
                }
                if (transaction.creditCardId != null) {
                    append(",\"credit_card_id\":\"${transaction.creditCardId}\"")
                }
                if (transaction.isRecurring) {
                    append(",\"is_recurring\":true")
                    if (transaction.recurringType != null) {
                        append(",\"recurring_type\":\"${transaction.recurringType}\"")
                    }
                    if (transaction.recurringUntil != null) {
                        append(",\"recurring_until\":\"${transaction.recurringUntil}\"")
                    }
                } else {
                    append(",\"is_recurring\":false")
                }
                append("}")
            }
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[TRANSACTIONS] UPDATE - Sucesso: '${transaction.description}' (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[TRANSACTIONS] UPDATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] UPDATE - Erro ao atualizar transacao apos ${duration}ms", e)
            false
        }
    }
    
    /**
     * Exclui uma transacao.
     */
    suspend fun deleteTransaction(transactionId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTIONS] DELETE - Iniciando: id=$transactionId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TRANSACTIONS] DELETE - Erro: usuario nao autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/transactions?id=eq.$transactionId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[TRANSACTIONS] DELETE - Sucesso: id=$transactionId (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[TRANSACTIONS] DELETE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTIONS] DELETE - Erro ao excluir id=$transactionId apos ${duration}ms", e)
            false
        }
    }
    
    private fun parseTransaction(json: org.json.JSONObject): Transaction {
        val type = json.optString("type")
        return Transaction(
            id = json.optString("id"),
            userId = json.optString("user_id"),
            description = json.optString("description"),
            categoryId = json.optString("category_id", ""),
            categoryName = "", // Será preenchido após buscar categoria
            amount = json.optDouble("amount", 0.0),
            type = type,
            date = json.optString("date"),
            accountId = json.optString("account_id").takeIf { it.isNotEmpty() },
            fromAccountId = json.optString("from_account_id").takeIf { it.isNotEmpty() },
            toAccountId = json.optString("to_account_id").takeIf { it.isNotEmpty() },
            creditCardId = json.optString("credit_card_id").takeIf { it.isNotEmpty() },
            notes = json.optString("notes").takeIf { it.isNotEmpty() },
            isRecurring = json.optBoolean("is_recurring", false),
            recurringType = json.optString("recurring_type").takeIf { it.isNotEmpty() },
            recurringUntil = json.optString("recurring_until").takeIf { it.isNotEmpty() },
            isDeleted = json.optBoolean("is_deleted", false),
            iconRes = when (type) {
                "income" -> R.drawable.ic_arrow_up
                "expense" -> R.drawable.ic_arrow_down
                else -> R.drawable.ic_transfer
            }
        )
    }
    
    private fun parseTransactions(jsonArray: JSONArray): List<Transaction> {
        val list = mutableListOf<Transaction>()
        for (i in 0 until jsonArray.length()) {
            list.add(parseTransaction(jsonArray.getJSONObject(i)))
        }
        return list
    }
    
    // ============== TAGS ==============
    
    suspend fun getTags(userId: String): List<Tag> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] READ - Iniciando busca para userId: $userId")
        
        try {
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TAGS] READ - Erro: usuário não autenticado")
                return@withContext emptyList()
            }
            
            val url = "$BASE_URL/rest/v1/tags?user_id=eq.$userId&select=*&order=created_at.desc"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TAGS] CREATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/tags").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
    
    suspend fun createUserProfile(userId: String, email: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[USERS] CREATE - Criando perfil para userId: $userId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[USERS] CREATE - Erro: usuario nao autenticado")
                return@withContext false
            }
            
            val body = """{"id":"$userId","email":"$email","name":"$name"}"""
            
            val conn = URL("$BASE_URL/rest/v1/users").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[USERS] CREATE - Sucesso: perfil criado (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[USERS] CREATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[USERS] CREATE - Erro após ${duration}ms", e)
            false
        }
    }
    
    suspend fun createDefaultTags(userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] DEFAULT - Criando tags padrao para userId: $userId")
        
        try {
            // Obter token do usuario para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TAGS] DEFAULT - Erro: usuario nao autenticado")
                return@withContext false
            }
            
            // Verificar se ja existe tag para este usuario
            val existingTags = getTags(userId)
            if (existingTags.isNotEmpty()) {
                Log.i(TAG, "[TAGS] DEFAULT - Usuario ja possui ${existingTags.size} tags, pulando criacao")
                return@withContext true
            }
            
            // Criar tags padrao
            val defaultTags = listOf(
                """{"name":"Uber","color":"#000000","user_id":"$userId"}""",
                """{"name":"Onibus","color":"#4ECDC4","user_id":"$userId"}""",
                """{"name":"FastFood","color":"#FF9800","user_id":"$userId"}""",
                """{"name":"Restaurante","color":"#E53935","user_id":"$userId"}""",
                """{"name":"Cursinho","color":"#9C27B0","user_id":"$userId"}""",
                """{"name":"Farmacia","color":"#4CAF50","user_id":"$userId"}""",
                """{"name":"Day Trade","color":"#FFD700","user_id":"$userId"}""",
                """{"name":"Passeio","color":"#03A9F4","user_id":"$userId"}""",
                """{"name":"Presente","color":"#E91E63","user_id":"$userId"}""",
                """{"name":"Roupas","color":"#D81B60","user_id":"$userId"}"""
            )
            
            var allSuccess = true
            for (tagBody in defaultTags) {
                val conn = URL("$BASE_URL/rest/v1/tags").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                
                conn.outputStream.write(tagBody.toByteArray())
                
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    Log.w(TAG, "[TAGS] DEFAULT - Falha ao criar tag: HTTP $responseCode")
                    allSuccess = false
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            if (allSuccess) {
                Log.i(TAG, "[TAGS] DEFAULT - Sucesso: 10 tags padrao criadas (${duration}ms)")
            } else {
                Log.w(TAG, "[TAGS] DEFAULT - Parcial: algumas tags falharam (${duration}ms)")
            }
            
            allSuccess
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TAGS] DEFAULT - Erro ao criar tags padrao após ${duration}ms", e)
            false
        }
    }
    
    suspend fun updateTag(tag: Tag): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TAGS] UPDATE - Iniciando: id=${tag.id}, name='${tag.name}'")
        
        try {
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TAGS] UPDATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/tags?id=eq.${tag.id}").openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TAGS] DELETE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/tags?id=eq.$tagId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TAGS] EXISTS - Erro: usuário não autenticado")
                return@withContext false
            }
            
            var url = "$BASE_URL/rest/v1/tags?name=eq.${java.net.URLEncoder.encode(name, "UTF-8")}&user_id=eq.$userId&select=id"
            if (excludeId != null) {
                url += "&id=neq.$excludeId"
            }
            
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[SETTINGS] GET - Erro: usuário não autenticado")
                return@withContext AppSettings(userId = userId)
            }
            
            val endpoint = "$BASE_URL/rest/v1/app_settings?user_id=eq.$userId&select=*&limit=1"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
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
            // Obter token do usuário para RLS
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[SETTINGS] UPDATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/app_settings").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
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
    
    // ============== REMINDERS ==============
    
    /**
     * Salva um novo lembrete de transação.
     * IMPORTANTE: Permite agendar notificações para transações futuras ou recorrentes.
     */
    suspend fun saveReminder(reminder: Reminder, userId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[REMINDERS] CREATE - Iniciando: title='${reminder.title}', date=${reminder.reminderDate}")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[REMINDERS] CREATE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val conn = URL("$BASE_URL/rest/v1/reminders").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val transactionIdPart = reminder.transactionId?.let { """"transaction_id":"$it",""" } ?: ""
            val descriptionPart = reminder.description?.let { """"description":"$it",""" } ?: ""
            val amountPart = reminder.amount?.let { """"amount":$it,""" } ?: ""
            val recurrencePart = if (reminder.isRecurring) {
                """"is_recurring":true,"recurrence_type":"${reminder.recurrenceType}","recurrence_interval":${reminder.recurrenceInterval},"""
            } else {
                """"is_recurring":false,"""
            }
            
            val body = """{"user_id":"$userId",$transactionIdPart"title":"${reminder.title}",$descriptionPart$amountPart"reminder_date":"${reminder.reminderDate}",$recurrencePart"is_active":true}"""
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[REMINDERS] CREATE - Sucesso: '${reminder.title}' criado (${duration}ms)")
                true
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.readText()
                Log.w(TAG, "[REMINDERS] CREATE - Falha: HTTP $responseCode, $errorStream (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[REMINDERS] CREATE - Erro ao criar lembrete '${reminder.title}' após ${duration}ms", e)
            false
        }
    }
    
    /**
     * Busca lembretes do usuário.
     */
    suspend fun getReminders(userId: String): List<Reminder> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[REMINDERS] GET - Iniciando busca (userId: $userId)")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[REMINDERS] GET - Erro: usuário não autenticado")
                return@withContext emptyList()
            }
            
            val endpoint = "$BASE_URL/rest/v1/reminders?user_id=eq.$userId&select=*&order=reminder_date.asc"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val reminders = mutableListOf<Reminder>()
                
                for (i in 0 until jsonArray.length()) {
                    reminders.add(parseReminder(jsonArray.getJSONObject(i)))
                }
                
                Log.i(TAG, "[REMINDERS] GET - Sucesso: ${reminders.size} lembretes (${duration}ms)")
                reminders
            } else {
                Log.w(TAG, "[REMINDERS] GET - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[REMINDERS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    private fun parseReminder(json: org.json.JSONObject): Reminder {
        return Reminder(
            id = json.optString("id"),
            userId = json.optString("user_id"),
            transactionId = json.optString("transaction_id").takeIf { it.isNotEmpty() },
            title = json.optString("title"),
            description = json.optString("description").takeIf { it.isNotEmpty() },
            amount = json.optDouble("amount").takeIf { !it.isNaN() },
            reminderDate = json.optString("reminder_date"),
            isRecurring = json.optBoolean("is_recurring", false),
            recurrenceType = json.optString("recurrence_type").takeIf { it.isNotEmpty() },
            recurrenceInterval = json.optInt("recurrence_interval", 1),
            isActive = json.optBoolean("is_active", true),
            lastTriggeredAt = json.optString("last_triggered_at").takeIf { it.isNotEmpty() },
            nextTriggerAt = json.optString("next_trigger_at").takeIf { it.isNotEmpty() },
            createdAt = json.optString("created_at"),
            updatedAt = json.optString("updated_at")
        )
    }
    
    // ============== TRANSACTION TAGS ==============
    
    /**
     * Salva tags associadas a uma transacao.
     */
    suspend fun saveTransactionTags(transactionId: String, tagIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (tagIds.isEmpty()) return@withContext true
        
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTION_TAGS] CREATE - Salvando ${tagIds.size} tags para transacao $transactionId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TRANSACTION_TAGS] CREATE - Erro: usuario nao autenticado")
                return@withContext false
            }
            
            // Primeiro remove tags existentes
            deleteTransactionTags(transactionId)
            
            // Insere novas tags
            var success = true
            for (tagId in tagIds) {
                val conn = URL("$BASE_URL/rest/v1/transaction_tags").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", API_KEY)
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                
                val body = """{"transaction_id":"$transactionId","tag_id":"$tagId"}"""
                conn.outputStream.write(body.toByteArray())
                
                if (conn.responseCode !in 200..299) {
                    success = false
                    Log.w(TAG, "[TRANSACTION_TAGS] CREATE - Falha ao associar tag $tagId")
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            if (success) {
                Log.i(TAG, "[TRANSACTION_TAGS] CREATE - Sucesso: ${tagIds.size} tags associadas (${duration}ms)")
            }
            success
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTION_TAGS] CREATE - Erro apos ${duration}ms", e)
            false
        }
    }
    
    /**
     * Busca tags de uma transacao.
     */
    suspend fun getTransactionTags(transactionId: String): List<Tag> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[TRANSACTION_TAGS] GET - Buscando tags da transacao $transactionId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[TRANSACTION_TAGS] GET - Erro: usuario nao autenticado")
                return@withContext emptyList()
            }
            
            // Join com tabela tags para pegar dados da tag
            val endpoint = "$BASE_URL/rest/v1/transaction_tags?transaction_id=eq.$transactionId&select=tags(id,name,color,user_id)"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val tags = mutableListOf<Tag>()
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val tagJson = item.getJSONObject("tags")
                    tags.add(Tag(
                        id = tagJson.optString("id"),
                        name = tagJson.optString("name"),
                        color = tagJson.optString("color", "#357266"),
                        userId = tagJson.optString("user_id")
                    ))
                }
                
                Log.i(TAG, "[TRANSACTION_TAGS] GET - Sucesso: ${tags.size} tags (${duration}ms)")
                tags
            } else {
                Log.w(TAG, "[TRANSACTION_TAGS] GET - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[TRANSACTION_TAGS] GET - Erro apos ${duration}ms", e)
            emptyList()
        }
    }
    
    /**
     * Remove todas as tags de uma transacao.
     */
    suspend fun deleteTransactionTags(transactionId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            val token = UserSession.getAccessToken() ?: return@withContext false
            
            val conn = URL("$BASE_URL/rest/v1/transaction_tags?transaction_id=eq.$transactionId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            val success = conn.responseCode in 200..299
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "[TRANSACTION_TAGS] DELETE - ${if (success) "Sucesso" else "Falha"} (${duration}ms)")
            success
        } catch (e: Exception) {
            Log.e(TAG, "[TRANSACTION_TAGS] DELETE - Erro", e)
            false
        }
    }
    
    // ============== ATTACHMENTS ==============
    
    /**
     * Salva um anexo.
     */
    suspend fun saveAttachment(attachment: Attachment, token: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ATTACHMENTS] CREATE - Salvando anexo para transacao ${attachment.transactionId}")
        
        try {
            val conn = URL("$BASE_URL/rest/v1/attachments").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = buildString {
                append("{")
                append("\"transaction_id\":\"${attachment.transactionId}\",")
                append("\"url\":\"${attachment.url}\",")
                append("\"type\":\"${attachment.type}\"")
                append("}")
            }
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ATTACHMENTS] CREATE - Sucesso (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ATTACHMENTS] CREATE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ATTACHMENTS] CREATE - Erro apos ${duration}ms", e)
            false
        }
    }
    
    /**
     * Busca anexos de uma transacao.
     */
    suspend fun getAttachments(transactionId: String): List<Attachment> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ATTACHMENTS] GET - Buscando anexos da transacao $transactionId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[ATTACHMENTS] GET - Erro: usuario nao autenticado")
                return@withContext emptyList()
            }
            
            val endpoint = "$BASE_URL/rest/v1/attachments?transaction_id=eq.$transactionId&select=*&order=created_at.desc"
            
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val attachments = mutableListOf<Attachment>()
                
                for (i in 0 until jsonArray.length()) {
                    attachments.add(parseAttachment(jsonArray.getJSONObject(i)))
                }
                
                Log.i(TAG, "[ATTACHMENTS] GET - Sucesso: ${attachments.size} anexos (${duration}ms)")
                attachments
            } else {
                Log.w(TAG, "[ATTACHMENTS] GET - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ATTACHMENTS] GET - Erro apos ${duration}ms", e)
            emptyList()
        }
    }
    
    /**
     * Exclui um anexo.
     */
    suspend fun deleteAttachment(attachmentId: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[ATTACHMENTS] DELETE - Excluindo anexo $attachmentId")
        
        try {
            val token = UserSession.getAccessToken() ?: return@withContext false
            
            val conn = URL("$BASE_URL/rest/v1/attachments?id=eq.$attachmentId").openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Prefer", "return=minimal")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[ATTACHMENTS] DELETE - Sucesso (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[ATTACHMENTS] DELETE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[ATTACHMENTS] DELETE - Erro apos ${duration}ms", e)
            false
        }
    }
    
    private fun parseAttachment(json: org.json.JSONObject): Attachment {
        return Attachment(
            id = json.optString("id"),
            transactionId = json.optString("transaction_id"),
            url = json.optString("url"),
            type = json.optString("type", "image"),
            createdAt = json.optString("created_at")
        )
    }
    
    // ============== GOALS ==============
    
    /**
     * Busca todas as metas do usuário.
     */
    suspend fun getGoals(userId: String): List<Goal> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[GOALS] GET - Buscando metas para usuario $userId")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[GOALS] GET - Erro: usuário não autenticado")
                return@withContext emptyList<Goal>()
            }
            
            val conn = URL("$BASE_URL/rest/v1/goals?user_id=eq.$userId&select=*").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val goals = mutableListOf<Goal>()
                for (i in 0 until jsonArray.length()) {
                    goals.add(parseGoal(jsonArray.getJSONObject(i)))
                }
                Log.i(TAG, "[GOALS] GET - Sucesso: ${goals.size} metas (${duration}ms)")
                goals
            } else {
                Log.w(TAG, "[GOALS] GET - Falha: HTTP $responseCode (${duration}ms)")
                emptyList()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[GOALS] GET - Erro após ${duration}ms", e)
            emptyList()
        }
    }
    
    /**
     * Salva uma meta (cria ou atualiza).
     */
    suspend fun saveGoal(goal: Goal): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[GOALS] SAVE - Salvando meta: ${goal.type}")
        
        try {
            val token = UserSession.getAccessToken()
            if (token == null) {
                Log.e(TAG, "[GOALS] SAVE - Erro: usuário não autenticado")
                return@withContext false
            }
            
            val url = if (goal.id.isNotEmpty()) {
                "$BASE_URL/rest/v1/goals?id=eq.${goal.id}"
            } else {
                "$BASE_URL/rest/v1/goals"
            }
            
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = if (goal.id.isNotEmpty()) "PATCH" else "POST"
            conn.setRequestProperty("apikey", API_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            
            val body = """
                {
                    "user_id": "${goal.userId}",
                    "type": "${goal.type}",
                    "target_amount": ${goal.targetAmount},
                    "is_enabled": ${goal.isEnabled},
                    "updated_at": "${goal.updatedAt}"
                    ${if (goal.id.isEmpty()) ""","created_at": "${goal.createdAt}"""" else ""}
                }
            """.trimIndent()
            conn.outputStream.write(body.toByteArray())
            
            val responseCode = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            
            if (responseCode in 200..299) {
                Log.i(TAG, "[GOALS] SAVE - Sucesso: ${goal.type} (${duration}ms)")
                true
            } else {
                Log.w(TAG, "[GOALS] SAVE - Falha: HTTP $responseCode (${duration}ms)")
                false
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "[GOALS] SAVE - Erro após ${duration}ms", e)
            false
        }
    }
    
    private fun parseGoal(json: org.json.JSONObject): Goal {
        return Goal(
            id = json.optString("id"),
            userId = json.optString("user_id"),
            type = json.optString("type"),
            targetAmount = json.optDouble("target_amount", 0.0),
            isEnabled = json.optBoolean("is_enabled", false),
            createdAt = json.optString("created_at"),
            updatedAt = json.optString("updated_at")
        )
    }
}
