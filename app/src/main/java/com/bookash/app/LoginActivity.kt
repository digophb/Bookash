package com.bookash.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

class LoginActivity : AppCompatActivity() {
    
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    
    companion object {
        private const val SUPABASE_URL = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
        private const val PREFS_NAME = "bookash_prefs"
        private const val KEY_TOKEN = "access_token"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        
        // Verificar se já está logado
        val token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_TOKEN, null)
        if (token != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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
                    val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("apikey", SUPABASE_KEY)
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    
                    val body = """{"email":"$email","password":"$password"}"""
                    conn.outputStream.write(body.toByteArray(Charsets.UTF_8))
                    
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        
                        val token = json.getString("access_token")
                        val user = json.optJSONObject("user")
                        val userName = user?.optJSONObject("user_metadata")?.optString("name", null)
                        val userEmail = user?.optString("email", email)
                        
                        // Salvar dados do usuário
                        val userId = user?.optString("id", "")
                        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        prefs.putString(KEY_TOKEN, token)
                        prefs.putString("user_id", userId)
                        prefs.putString("user_email", userEmail)
                        if (!userName.isNullOrEmpty()) {
                            prefs.putString("user_name", userName)
                        }
                        prefs.apply()
                        
                        token
                    } else {
                        null
                    }
                }
                
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Entrar"
                    
                    if (result != null) {
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
                    Toast.makeText(this@LoginActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
