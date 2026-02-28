package com.bookash.app

import android.content.Context
import android.content.SharedPreferences

/**
 * UserSession - Gerenciador de sessão do usuário
 * 
 * Singleton que armazena e gerencia o ID do usuário logado.
 * Garante que todas as operações usem o userId correto.
 * 
 * Uso:
 *   UserSession.init(context)
 *   val userId = UserSession.getUserId()
 */
object UserSession {
    
    private const val PREFS_NAME = "bookash_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    
    private lateinit var prefs: SharedPreferences
    private var _userId: String? = null
    
    /**
     * Inicializa a sessão. Deve ser chamado no Application ou na primeira Activity.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _userId = prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Salva os dados do usuário após login/registro.
     */
    fun saveSession(userId: String, token: String, email: String? = null, name: String? = null) {
        _userId = userId
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCESS_TOKEN, token)
            email?.let { putString(KEY_USER_EMAIL, it) }
            name?.let { putString(KEY_USER_NAME, it) }
            apply()
        }
    }
    
    /**
     * Define o usuário logado. Chamado após login bem-sucedido.
     */
    fun setUserId(userId: String) {
        _userId = userId
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    /**
     * Define o email do usuário logado.
     */
    fun setUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }
    
    /**
     * Retorna o ID do usuário logado.
     * Retorna null se não houver usuário logado.
     */
    fun getUserId(): String? = _userId
    
    /**
     * Retorna o ID do usuário ou lança exceção se não estiver logado.
     * Usar quando o login é obrigatório.
     */
    fun requireUserId(): String {
        return _userId ?: throw IllegalStateException("Usuário não está logado")
    }
    
    /**
     * Retorna o token de acesso.
     */
    fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    
    /**
     * Retorna o email do usuário logado.
     */
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    
    /**
     * Retorna o nome do usuário.
     */
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    
    /**
     * Verifica se há um usuário logado.
     */
    fun isLoggedIn(): Boolean = _userId != null
    
    /**
     * Limpa a sessão. Chamado no logout.
     */
    fun clear() {
        _userId = null
        prefs.edit().clear().apply()
    }
    
    /**
     * Atualiza o userId em memória (caso tenha sido alterado externamente)
     */
    fun refresh() {
        _userId = prefs.getString(KEY_USER_ID, null)
    }
}
