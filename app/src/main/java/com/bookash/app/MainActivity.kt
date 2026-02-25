package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

class MainActivity : AppCompatActivity() {
    
    private lateinit var welcomeText: TextView
    private lateinit var logoutButton: Button
    
    companion object {
        private const val SUPABASE_URL = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
        private const val PREFS_NAME = "bookash_prefs"
        private const val KEY_TOKEN = "access_token"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        welcomeText = findViewById(R.id.welcomeText)
        logoutButton = findViewById(R.id.logoutButton)
        
        loadUserInfo()
        
        logoutButton.setOnClickListener {
            logout()
        }
    }
    
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_TOKEN, null)
                
                if (token == null) {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                    return@launch
                }
                
                val userName = withContext(Dispatchers.IO) {
                    val url = URL("$SUPABASE_URL/auth/v1/user")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("apikey", SUPABASE_KEY)
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        val metadata = json.optJSONObject("user_metadata")
                        metadata?.optString("name") ?: "Usuário"
                    } else {
                        null
                    }
                }
                
                if (userName != null) {
                    welcomeText.text = "Olá, $userName!"
                } else {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro ao carregar usuário", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun logout() {
        lifecycleScope.launch {
            try {
                val token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_TOKEN, null)
                
                if (token != null) {
                    withContext(Dispatchers.IO) {
                        val url = URL("$SUPABASE_URL/auth/v1/logout")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("apikey", SUPABASE_KEY)
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.connect()
                    }
                }
                
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro ao sair", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
