package com.bookash.app

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.tennert.supabase.gotrue.auth
import io.github.jan.tennert.supabase.gotrue.providers.builtin.Email
import io.github.jan.tennert.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    
    companion object {
        private const val TAG = "BookashRegister"
    }
    
    @Serializable
    data class UserDTO(
        val id: String,
        val email: String,
        val name: String
    )
    
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
                    
                    try {
                        // Passo 1: Criar conta no Auth do Supabase via SDK
                        SupabaseClient.auth.signUpWith(Email) {
                            this.email = email
                            this.password = password
                            this.data = mapOf("name" to name)
                        }
                        
                        // Obter usuário criado
                        val user = SupabaseClient.auth.currentUserOrNull()
                        
                        if (user != null) {
                            val userId = user.id
                            Log.d(TAG, "Usuário criado: $userId")
                            
                            // Atualizar UserSession
                            UserSession.setUserData(
                                userId = userId,
                                email = email,
                                name = name
                            )
                            
                            // Passo 2: Inserir na tabela public.users via SDK
                            try {
                                SupabaseClient.client.from("users").insert(
                                    UserDTO(
                                        id = userId,
                                        email = email,
                                        name = name
                                    )
                                )
                                Log.d(TAG, "Usuário inserido na tabela users")
                            } catch (e: Exception) {
                                Log.e(TAG, "Erro ao inserir na tabela users: ${e.message}")
                                // Não falha o registro se a inserção na tabela falhar
                            }
                            
                            true
                        } else {
                            Log.e(TAG, "Usuário retornou null após signUp")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@RegisterActivity, "Erro ao criar conta. Tente novamente.", Toast.LENGTH_LONG).show()
                            }
                            false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro no cadastro: ${e.message}", e)
                        val errorMsg = parseAuthError(e.message)
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
                        Toast.makeText(this@RegisterActivity, "Conta criada com sucesso!", Toast.LENGTH_LONG).show()
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
    
    private fun parseAuthError(message: String?): String {
        if (message == null) return "Erro desconhecido"
        
        return when {
            message.contains("already registered", ignoreCase = true) -> 
                "Este email já está cadastrado"
            message.contains("invalid email", ignoreCase = true) -> 
                "Email inválido"
            message.contains("password", ignoreCase = true) -> 
                "Senha inválida"
            message.contains("network", ignoreCase = true) -> 
                "Erro de conexão"
            else -> message
        }
    }
}
