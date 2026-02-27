package com.bookash.app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AccountsActivity : AppCompatActivity() {

    private lateinit var accountsRecycler: RecyclerView
    private lateinit var fabAddAccount: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var emptyState: View
    
    private val accounts = mutableListOf<Account>()
    private lateinit var accountAdapter: AccountAdapter
    private var selectedIcon = "wallet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        accountsRecycler = findViewById(R.id.accountsRecycler)
        fabAddAccount = findViewById(R.id.fabAddAccount)
        emptyState = findViewById(R.id.emptyState)
        
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        accountAdapter = AccountAdapter()
        accountsRecycler.layoutManager = LinearLayoutManager(this)
        accountsRecycler.adapter = accountAdapter
        
        fabAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
        
        loadAccounts()
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            val loadedAccounts = SupabaseService.getAccounts()
            accounts.clear()
            accounts.addAll(loadedAccounts)
            
            if (accounts.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                accountsRecycler.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                accountsRecycler.visibility = View.VISIBLE
                accountAdapter.submitList(accounts)
            }
        }
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
                    saveAccount(name, balance, type)
                } else {
                    Toast.makeText(this, "Digite um nome", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveAccount(name: String, balance: Double, type: String) {
        lifecycleScope.launch {
            val account = Account(
                name = name,
                balance = balance,
                type = type,
                icon = selectedIcon
            )
            
            val success = SupabaseService.saveAccount(account)
            
            if (success) {
                Toast.makeText(this@AccountsActivity, "Conta salva!", Toast.LENGTH_SHORT).show()
                loadAccounts()
            } else {
                Toast.makeText(this@AccountsActivity, "Erro ao salvar conta", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
