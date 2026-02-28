package com.bookash.app

import android.content.Context

/**
 * Singleton para gerenciar a sessão do usuário logado.
 * 
 * Centraliza o acesso ao userId e token, garantindo que todas
 * as operações do app usem o usuário correto.
 * 
 * IMPORTANTE: Deve ser inicializado no Login/Register antes de usar.
 */
object UserSession {
    
    private const val PREFS_NAME = "bookash_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    
    private lateinit var prefs: android.content.SharedPreferences
    
    /**
     * Inicializa a sessão. Deve ser chamado no Application ou Login.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Salva os dados do usuário após login/registro.
     */
    fun saveSession(userId: String, token: String, email: String? = null, name: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, token)
            email?.let { putString(KEY_USER_EMAIL, it) }
            name?.let { putString(KEY_USER_NAME, it) }
            apply()
        }
    }
    
    /**
     * Retorna o ID do usuário logado.
     * Lança exceção se não estiver logado.
     */
    val userId: String
        get() {
            val id = prefs.getString(KEY_USER_ID, null)
            require(!id.isNullOrEmpty()) { "UserSession: usuário não está logado" }
            return id
        }
    
    /**
     * Retorna o token de acesso.
     */
    val token: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
    
    /**
     * Retorna o email do usuário.
     */
    val email: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
    
    /**
     * Retorna o nome do usuário.
     */
    val userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
    
    /**
     * Verifica se há um usuário logado.
     */
    val isLoggedIn: Boolean
        get() = !prefs.getString(KEY_USER_ID, null).isNullOrEmpty()
    
    /**
     * Limpa a sessão (logout).
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
