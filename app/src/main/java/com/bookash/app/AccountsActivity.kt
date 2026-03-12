package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AccountsActivity : AppCompatActivity() {

    private lateinit var accountsRecycler: RecyclerView
    private lateinit var fabAddAccount: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var emptyState: View
    private lateinit var btnArchived: ImageView
    
    private val accounts = mutableListOf<Account>()
    private lateinit var accountAdapter: AccountAdapter
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)
        
        userId = UserSession.getUserId()

        accountsRecycler = findViewById(R.id.accountsRecycler)
        fabAddAccount = findViewById(R.id.fabAddAccount)
        emptyState = findViewById(R.id.emptyState)
        btnArchived = findViewById(R.id.btnArchived)
        
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        accountAdapter = AccountAdapter(
            onEditClick = { account -> openEditAccount(account) },
            onArchiveClick = { account -> showArchiveAccountDialog(account) }
        )
        accountsRecycler.layoutManager = LinearLayoutManager(this)
        accountsRecycler.adapter = accountAdapter
        
        fabAddAccount.setOnClickListener {
            openAddAccount()
        }
        
        btnArchived.setOnClickListener {
            showArchivedAccountsBottomSheet()
        }
        
        loadAccounts()
    }

    private fun loadAccounts() {
        if (userId == null) {
            emptyState.visibility = View.VISIBLE
            accountsRecycler.visibility = View.GONE
            return
        }
        
        lifecycleScope.launch {
            val loadedAccounts = SupabaseService.getAccounts(userId!!, archived = false)
            
            // Calcular saldo dinamicamente para cada conta baseado nas transações
            val accountsWithCalculatedBalance = loadedAccounts.map { account ->
                val calculatedBalance = SupabaseService.getAccountCalculatedBalance(account.id)
                account.copy(balance = calculatedBalance)
            }
            
            accounts.clear()
            accounts.addAll(accountsWithCalculatedBalance)
            
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
    
    private fun openAddAccount() {
        val intent = Intent(this, AddAccountActivity::class.java)
        startActivityForResult(intent, REQUEST_ADD_ACCOUNT)
    }
    
    private fun openEditAccount(account: Account) {
        val intent = Intent(this, AddAccountActivity::class.java).apply {
            putExtra("account_id", account.id)
            putExtra("account_name", account.name)
            putExtra("account_type", account.type)
            putExtra("account_balance", account.balance)
            putExtra("account_icon", account.icon)
            putExtra("account_include_in_balance", account.includeInBalance)
        }
        startActivityForResult(intent, REQUEST_ADD_ACCOUNT)
    }
    
    private fun showArchiveAccountDialog(account: Account) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Arquivar conta")
            .setMessage("Deseja arquivar \"${account.name}\"? A conta será movida para \"Contas Arquivadas\" e seu saldo não será considerado no total.")
            .setPositiveButton("Arquivar") { _, _ ->
                archiveAccount(account)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showDeleteAccountDialog(account: Account) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
            val userId = userId!!
            val archivedAccounts = SupabaseService.getAccounts(userId, archived = true)
            
            if (archivedAccounts.isEmpty()) {
                ToastManager.showInfo(this@AccountsActivity, "Nenhuma conta arquivada")
                return@launch
            }
            
            // Calcular saldo dinamicamente para cada conta arquivada
            val accountsWithCalculatedBalance = archivedAccounts.map { account ->
                val calculatedBalance = SupabaseService.getAccountCalculatedBalance(account.id)
                account.copy(balance = calculatedBalance)
            }
            
            val accountNames = accountsWithCalculatedBalance.map { 
                val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
                "${it.name} (${formatter.format(it.balance)})"
            }.toTypedArray()
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this@AccountsActivity)
                .setTitle("Contas Arquivadas")
                .setItems(accountNames) { _, which ->
                    showReactivateDialog(accountsWithCalculatedBalance[which])
                }
                .setNegativeButton("Fechar", null)
                .show()
        }
    }
    
    private fun showReactivateDialog(account: Account) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Reativar conta")
            .setMessage("Deseja reativar \"${account.name}\"? Ela voltará a aparecer na lista principal e seu saldo será considerado no total.")
            .setPositiveButton("Reativar") { _, _ ->
                reactivateAccount(account)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun archiveAccount(account: Account) {
        lifecycleScope.launch {
            val success = SupabaseService.archiveAccount(account.id)
            if (success) {
                ToastManager.showWarning(this@AccountsActivity, "Conta \"${account.name}\" arquivada")
                loadAccounts()
            } else {
                ToastManager.showError(this@AccountsActivity, "Erro ao arquivar conta")
            }
        }
    }
    
    private fun reactivateAccount(account: Account) {
        lifecycleScope.launch {
            val success = SupabaseService.reactivateAccount(account.id)
            if (success) {
                ToastManager.showSuccess(this@AccountsActivity, "Conta \"${account.name}\" reativada")
                loadAccounts()
            } else {
                ToastManager.showError(this@AccountsActivity, "Erro ao reativar conta")
            }
        }
    }
    
    private fun deleteAccount(account: Account) {
        lifecycleScope.launch {
            val success = SupabaseService.deleteAccount(account.id)
            if (success) {
                ToastManager.showWarning(this@AccountsActivity, "Conta \"${account.name}\" excluída")
                loadAccounts()
            } else {
                ToastManager.showError(this@AccountsActivity, "Erro ao excluir conta")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_ADD_ACCOUNT && resultCode == RESULT_OK) {
            loadAccounts()
            
            data?.let {
                val action = it.getStringExtra("account_action")
                val accountName = it.getStringExtra("account_name") ?: "Conta"
                
                when (action) {
                    "create" -> ToastManager.showSuccess(this, "Conta \"$accountName\" criada")
                    "edit" -> ToastManager.showInfo(this, "Conta \"$accountName\" atualizada")
                }
            }
        }
    }
    
    companion object {
        private const val REQUEST_ADD_ACCOUNT = 1001
    }
}
