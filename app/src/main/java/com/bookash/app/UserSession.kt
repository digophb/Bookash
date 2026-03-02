package com.bookash.app

import android.content.Context
import android.util.Log

/**
 * UserSession - Gerenciador de sessão do usuário
 * 
 * Wrapper para o sistema de autenticação do Supabase SDK.
 * Fornece acesso conveniente aos dados do usuário logado.
 * 
 * IMPORTANTE: A sessão é gerenciada automaticamente pelo SDK.
 * Este objeto apenas expõe os dados de forma conveniente.
 */
object UserSession {
    
    private const val TAG = "UserSession"
    private const val PREFS_NAME = "bookash_session"
    private const val KEY_USER_NAME = "user_name"
    
    private var prefs: android.content.SharedPreferences? = null
    
    /**
     * Inicializa a sessão. Deve ser chamado no Application ou na primeira Activity.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "UserSession inicializado")
    }
    
    /**
     * Define os dados do usuário após login/registro.
     * O SDK gerencia userId e token automaticamente.
     */
    fun setUserData(userId: String, email: String? = null, name: String? = null) {
        Log.d(TAG, "setUserData: userId=$userId, email=$email, name=$name")
        // Apenas salvamos o nome, pois o SDK gerencia o resto
        name?.let { 
            prefs?.edit()?.putString(KEY_USER_NAME, it)?.apply()
        }
    }
    
    /**
     * Retorna o ID do usuário logado via SDK.
     * Retorna null se não houver usuário logado.
     */
    fun getUserId(): String? {
        return try {
            SupabaseClient.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter userId: ${e.message}")
            null
        }
    }
    
    /**
     * Retorna o ID do usuário ou lança exceção se não estiver logado.
     */
    fun requireUserId(): String {
        return getUserId() ?: throw IllegalStateException("Usuário não está logado")
    }
    
    /**
     * Retorna o email do usuário logado via SDK.
     */
    fun getUserEmail(): String? {
        return try {
            SupabaseClient.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter email: ${e.message}")
            null
        }
    }
    
    /**
     * Retorna o nome do usuário.
     * Prioriza userMetadata do SDK, fallback para SharedPreferences.
     */
    fun getUserName(): String? {
        return try {
            val user = SupabaseClient.auth.currentUserOrNull()
            val metadataName = user?.userMetadata?.get("name")?.toString()?.trim('"')
            
            if (!metadataName.isNullOrBlank()) {
                metadataName
            } else {
                prefs?.getString(KEY_USER_NAME, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter nome: ${e.message}")
            prefs?.getString(KEY_USER_NAME, null)
        }
    }
    
    /**
     * Verifica se há um usuário logado via SDK.
     */
    fun isLoggedIn(): Boolean {
        return try {
            SupabaseClient.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar login: ${e.message}")
            false
        }
    }
    
    /**
     * Limpa a sessão local (chamado após signOut do SDK).
     */
    fun clearLocal() {
        prefs?.edit()?.clear()?.apply()
        Log.d(TAG, "Sessão local limpa")
    }
    
    /**
     * Faz logout completo via SDK e limpa dados locais.
     */
    suspend fun signOut() {
        try {
            SupabaseClient.auth.signOut()
            clearLocal()
            Log.d(TAG, "Logout realizado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer logout: ${e.message}")
            clearLocal()
        }
    }
}
