package com.bookash.app

import android.content.Context
import android.util.Log

/**
 * UserSession - Gerenciador de sessão do usuário
 * 
 * Gerencia os dados do usuário logado via SharedPreferences.
 * 
 * IMPORTANTE: O token e userId são salvos após login/registro.
 */
object UserSession {
    
    private const val TAG = "UserSession"
    private const val PREFS_NAME = "bookash_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_EMAIL = "user_email"
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
     */
    fun setUserData(userId: String, email: String? = null, name: String? = null) {
        Log.d(TAG, "setUserData: userId=$userId, email=$email, name=$name")
        prefs?.edit()?.apply {
            putString(KEY_USER_ID, userId)
            email?.let { putString(KEY_USER_EMAIL, it) }
            name?.let { putString(KEY_USER_NAME, it) }
            apply()
        }
    }
    
    /**
     * Define o token de acesso.
     */
    fun setAccessToken(token: String) {
        prefs?.edit()?.putString(KEY_ACCESS_TOKEN, token)?.apply()
    }
    
    /**
     * Retorna o ID do usuário logado.
     * Retorna null se não houver usuário logado.
     */
    fun getUserId(): String? {
        return prefs?.getString(KEY_USER_ID, null)
    }
    
    /**
     * Retorna o ID do usuário ou lança exceção se não estiver logado.
     */
    fun requireUserId(): String {
        return getUserId() ?: throw IllegalStateException("Usuário não está logado")
    }
    
    /**
     * Retorna o token de acesso.
     */
    fun getAccessToken(): String? {
        return prefs?.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Retorna o email do usuário logado.
     */
    fun getUserEmail(): String? {
        return prefs?.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Retorna o nome do usuário.
     */
    fun getUserName(): String? {
        return prefs?.getString(KEY_USER_NAME, null)
    }
    
    /**
     * Verifica se há um usuário logado.
     */
    fun isLoggedIn(): Boolean {
        return prefs?.getString(KEY_USER_ID, null) != null
    }
    
    /**
     * Limpa a sessão local.
     */
    fun clearLocal() {
        prefs?.edit()?.clear()?.apply()
        Log.d(TAG, "Sessão local limpa")
    }
    
    /**
     * Faz logout completo.
     */
    fun signOut() {
        clearLocal()
        Log.d(TAG, "Logout realizado com sucesso")
    }
}
