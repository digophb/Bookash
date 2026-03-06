package com.bookash.app

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    
    companion object {
        private const val SUPABASE_URL = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
        private const val TAG = "BookashRegister"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerButton = findViewById(R.id.registerButton)
        loginLink = findViewById(R.id.loginLink)
        
        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            
            if (name.isBlank()) {
                Toast.makeText(this, "Digite seu nome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (email.isBlank()) {
                Toast.makeText(this, "Digite seu email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isBlank()) {
                Toast.makeText(this, "Digite sua senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Toast.makeText(this, "Senhas não conferem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(this, "Senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            register(name, email, password)
        }
        
        loginLink.setOnClickListener {
            finish()
        }
    }
    
    private fun register(name: String, email: String, password: String) {
        registerButton.isEnabled = false
        registerButton.text = "Criando conta..."
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Iniciando cadastro para: $email")
                    
                    // Passo 1: Criar conta no Auth do Supabase
                    val url = URL("$SUPABASE_URL/auth/v1/signup")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("apikey", SUPABASE_KEY)
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 30000
                    conn.readTimeout = 30000
                    
                    val body = """{"email":"$email","password":"$password","data":{"name":"$name"}}"""
                    Log.d(TAG, "Enviando: $body")
                    conn.outputStream.write(body.toByteArray(Charsets.UTF_8))
                    
                    val responseCode = conn.responseCode
                    Log.d(TAG, "Response code: $responseCode")
                    
                    if (responseCode == 200 || responseCode == 201) {
                        val response = conn.inputStream.bufferedReader().readText()
                        Log.d(TAG, "Response: $response")
                        val json = JSONObject(response)
                        
                        // O ID do usuário está dentro de "user" na resposta do Supabase
                        val userJson = json.optJSONObject("user")
                        val userId = userJson?.optString("id", "") ?: json.optString("id", "")
                        val accessToken = json.optString("access_token", "")
                        Log.d(TAG, "User ID: $userId")
                        
                        if (userId.isNotEmpty()) {
                            // Salvar dados do usuário
                            val prefs = getSharedPreferences("bookash_prefs", MODE_PRIVATE).edit()
                            prefs.putString("user_id", userId)
                            prefs.putString("access_token", accessToken)
                            prefs.putString("user_email", email)
                            prefs.putString("user_name", name)
                            prefs.apply()
                            
                            // Inicializar UserSession
                            UserSession.init(this@RegisterActivity)
                            UserSession.setUserData(
                                userId = userId,
                                email = email,
                                name = name
                            )
                            UserSession.setAccessToken(accessToken)
                            
                            // NOTA: O trigger no Supabase cria automaticamente:
                            // 1. Registro na tabela public.users
                            // 2. Contas padrao 'Carteira' e 'Banco'
                            // 3. Categorias padrao (Alimentacao, Transporte, etc)
                            // 4. Tags padrao (Uber, Onibus, FastFood, etc)
                            // Fallback: criar manualmente se trigger falhar
                            if (accessToken.isNotEmpty()) {
                                try {
                                    val accountsCreated = SupabaseService.createDefaultAccounts(userId)
                                    Log.d(TAG, "Contas padrao criadas (fallback): $accountsCreated")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Erro ao criar contas padrao: ${e.message}")
                                }
                                try {
                                    val categoriesCreated = SupabaseService.createDefaultCategories(userId)
                                    Log.d(TAG, "Categorias padrao criadas (fallback): $categoriesCreated")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Erro ao criar categorias padrao: ${e.message}")
                                }
                                try {
                                    val tagsCreated = SupabaseService.createDefaultTags(userId)
                                    Log.d(TAG, "Tags padrao criadas (fallback): $tagsCreated")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Erro ao criar tags padrao: ${e.message}")
                                }
                            }
                            true
                        } else {
                            Log.e(TAG, "userId vazio")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@RegisterActivity, "Resposta inválida do servidor", Toast.LENGTH_LONG).show()
                            }
                            false
                        }
                    } else {
                        val errorResponse = conn.errorStream?.bufferedReader()?.readText() ?: "Sem resposta"
                        Log.e(TAG, "Erro HTTP $responseCode: $errorResponse")
                        
                        var errorMsg = "Erro ao criar conta"
                        try {
                            val errorJson = JSONObject(errorResponse)
                            errorMsg = errorJson.optString("msg", 
                                errorJson.optString("message", 
                                    errorJson.optString("error_description", errorMsg)))
                        } catch (e: Exception) {
                            errorMsg = "Erro HTTP $responseCode"
                        }
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                        false
                    }
                }
                
                withContext(Dispatchers.Main) {
                    registerButton.isEnabled = true
                    registerButton.text = "Criar Conta"
                    
                    if (result) {
                        Toast.makeText(this@RegisterActivity, "Conta criada! Verifique seu email ou faça login.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exceção: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    registerButton.isEnabled = true
                    registerButton.text = "Criar Conta"
                    Toast.makeText(this@RegisterActivity, "Erro de conexão: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
