package com.bookash.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AccountsActivity : AppCompatActivity() {

    private lateinit var accountsRecycler: RecyclerView
    private lateinit var fabAddAccount: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    private val supabaseUrl = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        accountsRecycler = findViewById(R.id.accountsRecycler)
        fabAddAccount = findViewById(R.id.fabAddAccount)

        accountsRecycler.layoutManager = LinearLayoutManager(this)
        
        fabAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
        
        loadAccounts()
    }

    private fun loadAccounts() {
        // TODO: Carregar do Supabase
    }

    private fun showAddAccountDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_account, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val balanceInput = dialogView.findViewById<TextInputEditText>(R.id.balanceInput)
        val typeDropdown = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.typeDropdown)
        
        // Configurar tipos de conta
        val types = arrayOf("Corrente", "Poupança", "Carteira", "Digital")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        typeDropdown.setAdapter(adapter)
        typeDropdown.setText("Corrente", false)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Nova Conta")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val name = nameInput.text.toString()
                val balance = balanceInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                val type = typeDropdown.text.toString()
                
                if (name.isNotBlank()) {
                    saveAccount(name, balance, type)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveAccount(name: String, balance: Double, type: String) {
        // TODO: Salvar no Supabase
        Toast.makeText(this, "Conta salva: $name - R$ $balance ($type)", Toast.LENGTH_SHORT).show()
    }
}
