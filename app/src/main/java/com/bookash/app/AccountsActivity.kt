package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    private lateinit var btnArchived: ImageView
    
    private val accounts = mutableListOf<Account>()
    private lateinit var accountAdapter: AccountAdapter
    private var selectedIcon = "wallet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        accountsRecycler = findViewById(R.id.accountsRecycler)
        fabAddAccount = findViewById(R.id.fabAddAccount)
        emptyState = findViewById(R.id.emptyState)
        btnArchived = findViewById(R.id.btnArchived)
        
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        accountAdapter = AccountAdapter(
            onEditClick = { account -> showEditAccountDialog(account) },
            onArchiveClick = { account -> showArchiveAccountDialog(account) },
            onDeleteClick = { account -> showDeleteAccountDialog(account) }
        )
        accountsRecycler.layoutManager = LinearLayoutManager(this)
        accountsRecycler.adapter = accountAdapter
        
        fabAddAccount.setOnClickListener {
            showAddAccountDialog()
        }
        
        btnArchived.setOnClickListener {
            showArchivedAccountsBottomSheet()
        }
        
        loadAccounts()
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            val loadedAccounts = SupabaseService.getAccounts(archived = false)
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
        
        // Reset
        selectedIcon = "wallet"
        
        // Configurar tipos de conta
        val types = arrayOf("Corrente", "Poupança", "Carteira", "Digital")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        typeDropdown.setAdapter(adapter)
        typeDropdown.setText("Corrente", false)
        typeDropdown.setTextColor(getColor(R.color.text_primary))
        typeDropdown.setDropDownBackgroundResource(R.color.surface)
        
        // Configurar seleção de ícones
        setupIconSelection(dialogView)
        
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
    
    private fun showEditAccountDialog(account: Account) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_account, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val balanceInput = dialogView.findViewById<TextInputEditText>(R.id.balanceInput)
        val typeDropdown = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.typeDropdown)
        
        // Preencher com dados atuais
        nameInput.setText(account.name)
        balanceInput.setText(String.format("%.2f", account.balance))
        selectedIcon = account.icon
        
        // Configurar tipos de conta
        val types = arrayOf("Corrente", "Poupança", "Carteira", "Digital")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        typeDropdown.setAdapter(adapter)
        typeDropdown.setText(account.type, false)
        typeDropdown.setTextColor(getColor(R.color.text_primary))
        typeDropdown.setDropDownBackgroundResource(R.color.surface)
        
        // Destacar ícone atual
        highlightSelectedIcon(dialogView, account.icon)
        
        // Configurar seleção de ícones
        setupIconSelection(dialogView)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Conta")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val name = nameInput.text.toString()
                val balance = balanceInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                val type = typeDropdown.text.toString()
                
                if (name.isNotBlank()) {
                    updateAccount(account.id, name, balance, type)
                } else {
                    Toast.makeText(this, "Digite um nome", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showArchiveAccountDialog(account: Account) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Arquivar conta")
            .setMessage("Deseja arquivar \"${account.name}\"? A conta será movida para \"Contas Arquivadas\" e seu saldo não será considerado no total.")
            .setPositiveButton("Arquivar") { _, _ ->
                archiveAccount(account)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showDeleteAccountDialog(account: Account) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir conta")
            .setMessage("Deseja excluir \"${account.name}\" permanentemente? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                deleteAccount(account)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showArchivedAccountsBottomSheet() {
        lifecycleScope.launch {
            val archivedAccounts = SupabaseService.getAccounts(archived = true)
            
            if (archivedAccounts.isEmpty()) {
                Toast.makeText(this@AccountsActivity, "Nenhuma conta arquivada", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val accountNames = archivedAccounts.map { it.name }.toTypedArray()
            
            MaterialAlertDialogBuilder(this@AccountsActivity)
                .setTitle("Contas Arquivadas")
                .setItems(accountNames) { _, which ->
                    showReactivateDialog(archivedAccounts[which])
                }
                .setNegativeButton("Fechar", null)
                .show()
        }
    }
    
    private fun showReactivateDialog(account: Account) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reativar conta")
            .setMessage("Deseja reativar \"${account.name}\"? Ela voltará a aparecer na lista principal e seu saldo será considerado no total.")
            .setPositiveButton("Reativar") { _, _ ->
                reactivateAccount(account)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupIconSelection(dialogView: View) {
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
                highlightSelectedIcon(dialogView, icon)
            }
        }
    }
    
    private fun highlightSelectedIcon(dialogView: View, icon: String) {
        val iconViews = listOf(
            Pair(R.id.iconNubank, "nubank"),
            Pair(R.id.iconItau, "itau"),
            Pair(R.id.iconBradesco, "bradesco"),
            Pair(R.id.iconBB, "bb"),
            Pair(R.id.iconWallet, "wallet")
        )
        
        iconViews.forEach { (viewId, iconName) ->
            val view = dialogView.findViewById<ImageView>(viewId)
            view?.let {
                if (iconName == icon) {
                    val drawable = GradientDrawable()
                    drawable.cornerRadius = 8f
                    drawable.setColor(Color.parseColor("#357266"))
                    it.background = drawable
                    it.setColorFilter(Color.WHITE)
                } else {
                    it.setBackgroundColor(Color.TRANSPARENT)
                    it.setColorFilter(Color.parseColor("#B0B0B0"))
                }
            }
        }
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
    
    private fun updateAccount(accountId: String, name: String, balance: Double, type: String) {
        lifecycleScope.launch {
            val account = Account(
                id = accountId,
                name = name,
                balance = balance,
                type = type,
                icon = selectedIcon
            )
            
            val success = SupabaseService.updateAccount(account)
            
            if (success) {
                Toast.makeText(this@AccountsActivity, "Conta atualizada!", Toast.LENGTH_SHORT).show()
                loadAccounts()
            } else {
                Toast.makeText(this@AccountsActivity, "Erro ao atualizar conta", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun archiveAccount(account: Account) {
        lifecycleScope.launch {
            val success = SupabaseService.archiveAccount(account.id)
            if (success) {
                Toast.makeText(this@AccountsActivity, "Conta arquivada", Toast.LENGTH_SHORT).show()
                loadAccounts()
            } else {
                Toast.makeText(this@AccountsActivity, "Erro ao arquivar", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun reactivateAccount(account: Account) {
        lifecycleScope.launch {
            val success = SupabaseService.reactivateAccount(account.id)
            if (success) {
                Toast.makeText(this@AccountsActivity, "Conta reativada!", Toast.LENGTH_SHORT).show()
                loadAccounts()
            } else {
                Toast.makeText(this@AccountsActivity, "Erro ao reativar", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteAccount(account: Account) {
        lifecycleScope.launch {
            val success = SupabaseService.deleteAccount(account.id)
            if (success) {
                Toast.makeText(this@AccountsActivity, "Conta excluída", Toast.LENGTH_SHORT).show()
                loadAccounts()
            } else {
                Toast.makeText(this@AccountsActivity, "Erro ao excluir", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
