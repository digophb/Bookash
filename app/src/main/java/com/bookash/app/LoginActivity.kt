package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.tennert.supabase.gotrue.auth
import io.github.jan.tennert.supabase.gotrue.providers.builtin.OTP
import io.github.jan.tennert.supabase.gotrue.providers.builtin.Email
import io.github.jan.tennert.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        
        // Verificar se já está logado via SDK
        lifecycleScope.launch {
            val session = SupabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                // Usuário já logado - ir para MainActivity
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        }
        
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            login(email, password)
        }
        
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun login(email: String, password: String) {
        loginButton.isEnabled = false
        loginButton.text = "Entrando..."
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Usar SDK para login
                    SupabaseClient.auth.signInWithPassword(Email) {
                        this.email = email
                        this.password = password
                    }
                    
                    // Após login bem-sucedido, obter dados do usuário
                    val session = SupabaseClient.auth.currentSessionOrNull()
                    session != null
                }
                
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Entrar"
                    
                    if (result) {
                        // Atualizar UserSession com dados do SDK
                        val user = SupabaseClient.auth.currentUserOrNull()
                        UserSession.setUserData(
                            userId = user?.id ?: "",
                            email = user?.email ?: email,
                            name = user?.userMetadata?.get("name")?.toString()?.trim('"')
                        )
                        
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Email ou senha incorretos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Entrar"
                    val errorMsg = e.message ?: "Erro desconhecido"
                    Toast.makeText(this@LoginActivity, "Erro: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
