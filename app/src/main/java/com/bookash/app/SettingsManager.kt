package com.bookash.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SettingsManager - Gerenciador de configurações do app
 * 
 * Encapsula o acesso às configurações do usuário, fornecendo:
 * - Cache local para acesso rápido
 * - Persistência no Supabase
 * - Valores padrão
 * 
 * Uso:
 *   SettingsManager.init(context)
 *   val theme = SettingsManager.getTheme()
 *   SettingsManager.setTheme("dark")
 */
object SettingsManager {
    
    private const val TAG = "SettingsManager"
    private const val PREFS_NAME = "bookash_settings_cache"
    
    // Cache keys
    private const val KEY_THEME = "theme"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_CACHED = "settings_cached"
    
    private var prefs: android.content.SharedPreferences? = null
    private var cachedSettings: AppSettings? = null
    
    /**
     * Inicializa o SettingsManager. Deve ser chamado no Application ou primeira Activity.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromCache()
        Log.d(TAG, "SettingsManager inicializado")
    }
    
    /**
     * Carrega configurações do cache local.
     */
    private fun loadFromCache() {
        val isCached = prefs?.getBoolean(KEY_CACHED, false) ?: false
        
        if (isCached) {
            cachedSettings = AppSettings(
                theme = prefs?.getString(KEY_THEME, "system") ?: "system",
                language = prefs?.getString(KEY_LANGUAGE, "pt-BR") ?: "pt-BR",
                notificationsEnabled = prefs?.getBoolean(KEY_NOTIFICATIONS, true) ?: true
            )
            Log.d(TAG, "Configurações carregadas do cache: $cachedSettings")
        }
    }
    
    /**
     * Sincroniza configurações com o Supabase.
     * Deve ser chamado após login bem-sucedido.
     */
    suspend fun syncFromServer(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val settings = SupabaseService.getSettings(userId)
                cachedSettings = settings
                saveToCache(settings)
                Log.i(TAG, "Configurações sincronizadas do servidor: $settings")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao sincronizar configurações: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Salva configurações no cache local.
     */
    private fun saveToCache(settings: AppSettings) {
        prefs?.edit()?.apply {
            putString(KEY_THEME, settings.theme)
            putString(KEY_LANGUAGE, settings.language)
            putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            putBoolean(KEY_CACHED, true)
            apply()
        }
    }
    
    /**
     * Retorna o tema atual.
     * Valores: "light", "dark", "system"
     */
    fun getTheme(): String {
        return cachedSettings?.theme ?: "system"
    }
    
    /**
     * Define o tema e persiste no servidor.
     */
    suspend fun setTheme(theme: String, userId: String): Boolean {
        return updateSetting(theme = theme, userId = userId)
    }
    
    /**
     * Retorna o idioma atual.
     * Valores: "pt-BR", "en-US", etc.
     */
    fun getLanguage(): String {
        return cachedSettings?.language ?: "pt-BR"
    }
    
    /**
     * Define o idioma e persiste no servidor.
     */
    suspend fun setLanguage(language: String, userId: String): Boolean {
        return updateSetting(language = language, userId = userId)
    }
    
    /**
     * Retorna se notificações estão habilitadas.
     */
    fun areNotificationsEnabled(): Boolean {
        return cachedSettings?.notificationsEnabled ?: true
    }
    
    /**
     * Define se notificações estão habilitadas e persiste no servidor.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean, userId: String): Boolean {
        return updateSetting(notificationsEnabled = enabled, userId = userId)
    }
    
    /**
     * Atualiza uma configuração específica.
     */
    private suspend fun updateSetting(
        theme: String? = null,
        language: String? = null,
        notificationsEnabled: Boolean? = null,
        userId: String
    ): Boolean {
        val currentSettings = cachedSettings ?: AppSettings(userId = userId)
        
        val newSettings = currentSettings.copy(
            theme = theme ?: currentSettings.theme,
            language = language ?: currentSettings.language,
            notificationsEnabled = notificationsEnabled ?: currentSettings.notificationsEnabled
        )
        
        return withContext(Dispatchers.IO) {
            try {
                val success = SupabaseService.updateSettings(newSettings, userId)
                
                if (success) {
                    cachedSettings = newSettings
                    saveToCache(newSettings)
                    Log.i(TAG, "Configuração atualizada: $newSettings")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao atualizar configuração: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Limpa o cache local. Chamado no logout.
     */
    fun clearCache() {
        cachedSettings = null
        prefs?.edit()?.clear()?.apply()
        Log.d(TAG, "Cache de configurações limpo")
    }
    
    /**
     * Retorna todas as configurações atuais.
     */
    fun getSettings(): AppSettings {
        return cachedSettings ?: AppSettings()
    }
}
