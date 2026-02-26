package com.bookash.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Account(
    val id: String = "",
    val name: String,
    val balance: Double,
    val type: String,
    val icon: String = "wallet"
)

class AccountsActivity : AppCompatActivity() {

    private lateinit var accountsRecycler: RecyclerView
    private lateinit var fabAddAccount: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    private val supabaseUrl = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
    
    private var selectedIcon = "wallet"
    private val accounts = mutableListOf<Account>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        accountsRecycler = findViewById(R.id.accountsRecycler)
        fabAddAccount = findViewById(R.id.fabAddAccount)
        
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        accountsRecycler.layoutManager = LinearLayoutManager(this)
        
        fabAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
        
        loadAccounts()
    }

    private fun loadAccounts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$supabaseUrl/rest/v1/accounts?select=*")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("apikey", supabaseKey)
                conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonArray = JSONArray(response)
                    
                    accounts.clear()
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        accounts.add(Account(
                            id = json.optString("id"),
                            name = json.optString("name"),
                            balance = json.optDouble("balance", 0.0),
                            type = json.optString("type", "corrente"),
                            icon = json.optString("icon", "wallet")
                        ))
                    }
                    
                    withContext(Dispatchers.Main) {
                        updateAccountsList()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountsActivity, "Erro ao carregar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateAccountsList() {
        Toast.makeText(this, "${accounts.size} contas carregadas", Toast.LENGTH_SHORT).show()
    }

    private fun showAddAccountDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_account, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val balanceInput = dialogView.findViewById<TextInputEditText>(R.id.balanceInput)
        val typeDropdown = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.typeDropdown)
        
        // Configurar tipos de conta com cores corretas
        val types = arrayOf("Corrente", "Poupança", "Carteira", "Digital")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        typeDropdown.setAdapter(adapter)
        typeDropdown.setText("Corrente", false)
        typeDropdown.setTextColor(getColor(R.color.text_primary))
        typeDropdown.setDropDownBackgroundResource(R.color.surface)
        
        // Configurar seleção de ícones
        val icons = listOf(
            Pair(R.id.iconNubank, "nubank"),
            Pair(R.id.iconItau, "itau"),
            Pair(R.id.iconBradesco, "bradesco"),
            Pair(R.id.iconBB, "bb"),
            Pair(R.id.iconWallet, "wallet")
        )
        
        icons.forEach { (id, icon) ->
            dialogView.findViewById<ImageView>(id)?.setOnClickListener {
                selectedIcon = icon
                Toast.makeText(this, "Banco selecionado", Toast.LENGTH_SHORT).show()
            }
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Nova Conta")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val name = nameInput.text.toString()
                val balance = balanceInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                val type = typeDropdown.text.toString()
                
                if (name.isNotBlank()) {
                    saveAccount(name, balance, type, selectedIcon)
                } else {
                    Toast.makeText(this, "Digite um nome", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveAccount(name: String, balance: Double, type: String, icon: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$supabaseUrl/rest/v1/accounts")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", supabaseKey)
                conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                
                val body = """{"name":"$name","balance":$balance,"type":"$type","icon":"$icon"}"""
                conn.outputStream.write(body.toByteArray())
                
                if (conn.responseCode in 200..299) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AccountsActivity, "Conta salva!", Toast.LENGTH_SHORT).show()
                        loadAccounts()
                    }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AccountsActivity, "Erro: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountsActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
