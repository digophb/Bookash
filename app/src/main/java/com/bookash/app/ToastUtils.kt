package com.bookash.app

import android.content.Context
import android.widget.Toast

/**
 * Utilitário para mensagens Toast padronizadas
 */
object ToastUtils {
    
    /**
     * Mostra mensagem de sucesso
     */
    fun showSuccess(context: Context, message: String) {
        Toast.makeText(context, "✅ $message", Toast.LENGTH_LONG).show()
    }
    
    /**
     * Mostra mensagem de erro
     */
    fun showError(context: Context, message: String) {
        Toast.makeText(context, "❌ $message", Toast.LENGTH_LONG).show()
    }
    
    /**
     * Mostra mensagem de aviso
     */
    fun showWarning(context: Context, message: String) {
        Toast.makeText(context, "⚠️ $message", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Mostra mensagem informativa
     */
    fun showInfo(context: Context, message: String) {
        Toast.makeText(context, "ℹ️ $message", Toast.LENGTH_SHORT).show()
    }
    
    // ==========================================
    // MENSAGENS PADRÃO - LOGIN
    // ==========================================
    
    fun loginSuccess(context: Context) {
        showSuccess(context, "Login realizado com sucesso!")
    }
    
    fun loginError(context: Context, reason: String = "Email ou senha incorretos") {
        showError(context, "Erro no login: $reason")
    }
    
    fun loginFieldsEmpty(context: Context) {
        showWarning(context, "Preencha todos os campos")
    }
    
    // ==========================================
    // MENSAGENS PADRÃO - REGISTRO
    // ==========================================
    
    fun registerSuccess(context: Context) {
        showSuccess(context, "Conta criada com sucesso!")
    }
    
    fun registerError(context: Context, reason: String = "Erro ao criar conta") {
        showError(context, "Erro no registro: $reason")
    }
    
    fun registerEmailExists(context: Context) {
        showError(context, "Este email já está cadastrado")
    }
    
    fun registerPasswordMismatch(context: Context) {
        showWarning(context, "As senhas não conferem")
    }
    
    fun registerPasswordTooShort(context: Context) {
        showWarning(context, "A senha deve ter pelo menos 6 caracteres")
    }
    
    fun registerFieldEmpty(context: Context, fieldName: String) {
        showWarning(context, "Digite $fieldName")
    }
    
    // ==========================================
    // MENSAGENS PADRÃO - LOGOUT
    // ==========================================
    
    fun logoutSuccess(context: Context) {
        showInfo(context, "Logout realizado com sucesso")
    }
    
    fun logoutError(context: Context, reason: String = "Erro ao sair") {
        showError(context, "Erro no logout: $reason")
    }
    
    // ==========================================
    // MENSAGENS PADRÃO - CONEXÃO
    // ==========================================
    
    fun connectionError(context: Context) {
        showError(context, "Erro de conexão. Verifique sua internet")
    }
    
    fun serverError(context: Context) {
        showError(context, "Erro no servidor. Tente novamente")
    }
    
    // ==========================================
    // MENSAGENS PADRÃO - VALIDAÇÃO
    // ==========================================
    
    fun invalidEmail(context: Context) {
        showWarning(context, "Digite um email válido")
    }
    
    fun invalidPassword(context: Context) {
        showWarning(context, "Senha inválida")
    }
}
