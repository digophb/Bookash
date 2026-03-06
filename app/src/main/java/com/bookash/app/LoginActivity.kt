package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
        
        val token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_TOKEN, null)
        val savedUserId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("user_id", null)
        
        if (token != null && savedUserId != null) {
            UserSession.init(this)
            UserSession.setUserData(userId = savedUserId)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            
            if (email.isBlank() || password.isBlank()) {
                ToastManager.showWarning(this, "Preencha todos os campos")
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
                    val conn = URL("https://gqbxasjoxxslpaxjqfeg.supabase.co/auth/v1/token?grant_type=password").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    
                    val body = """{"email":"$email","password":"$password"}"""
                    conn.outputStream.write(body.toByteArray())
                    
                    val responseCode = conn.responseCode
                    
                    if (responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        
                        val token = json.optString("access_token", "")
                        val user = json.optJSONObject("user")
                        val userId = user?.optString("id", "") ?: json.optString("id", "")
                        val userEmail = user?.optString("email", email)
                        val userMetadata = user?.optJSONObject("user_metadata")
                        val userName = userMetadata?.optString("name")
                        
                        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        prefs.putString(KEY_TOKEN, token)
                        prefs.putString("user_id", userId)
                        prefs.putString("user_email", userEmail)
                        if (!userName.isNullOrEmpty()) {
                            prefs.putString("user_name", userName)
                        }
                        prefs.apply()
                        
                        UserSession.init(this@LoginActivity)
                        UserSession.setUserData(userId = userId, email = userEmail ?: email, name = userName)
                        UserSession.setAccessToken(token)
                        
                        Pair(true, "")
                    } else {
                        val errorStream = conn.errorStream?.bufferedReader()?.readText()
                        var errorMsg = "Email ou senha incorretos"
                        
                        try {
                            if (errorStream != null) {
                                val errorJson = JSONObject(errorStream)
                                errorMsg = errorJson.optString("error_description",
                                    errorJson.optString("msg",
                                        errorJson.optString("message",
                                            errorJson.optString("error", errorMsg))))
                            }
                        } catch (e: Exception) {
                            // Manter mensagem padrão
                        }
                        
                        Pair(false, errorMsg)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Entrar"
                    
                    if (result.first) {
                        ToastManager.showSuccess(this@LoginActivity, "Login realizado com sucesso!")
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        ToastManager.showError(this@LoginActivity, result.second)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Entrar"
                    ToastManager.showError(this@LoginActivity, "Erro de conexão. Verifique sua internet.")
                }
            }
        }
    }
}
