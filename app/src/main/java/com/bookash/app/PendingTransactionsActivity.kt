package com.bookash.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PendingTransactionsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    private var incomeTransactions: List<Transaction> = emptyList()
    private var expenseTransactions: List<Transaction> = emptyList()

    companion object {
        const val EXTRA_TYPE = "type" // "income" ou "expense"
        const val EXTRA_INCOME_LIST = "income_list"
        const val EXTRA_EXPENSE_LIST = "expense_list"
        const val REQUEST_EDIT_TRANSACTION = 1001
        private const val TAG = "PendingTransactions"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_transactions)

        toolbar = findViewById(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Configurar ViewPager com adapter
        val typeFilter = intent.getStringExtra(EXTRA_TYPE) ?: "income"
        val adapter = PendingTransactionsPagerAdapter(this, typeFilter)
        viewPager.adapter = adapter

        // Conectar TabLayout com ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val isIncomeFirst = typeFilter == "income"
            tab.text = when (position) {
                0 -> if (isIncomeFirst) "Receitas" else "Despesas"
                1 -> if (isIncomeFirst) "Despesas" else "Receitas"
                else -> "Outros"
            }
        }.attach()

        // Carregar listas iniciais do intent
        incomeTransactions = (intent.getSerializableExtra(EXTRA_INCOME_LIST) as? ArrayList<Transaction>)?.toList() ?: emptyList()
        expenseTransactions = (intent.getSerializableExtra(EXTRA_EXPENSE_LIST) as? ArrayList<Transaction>)?.toList() ?: emptyList()
    }
    
    // Métodos para que o fragment acesse as listas
    fun getPendingIncomeTransactions(): List<Transaction> = incomeTransactions
    
    fun getPendingExpenseTransactions(): List<Transaction> = expenseTransactions

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_EDIT_TRANSACTION && resultCode == Activity.RESULT_OK) {
            val action = data?.getStringExtra("action")
            val message = data?.getStringExtra("message") ?: "Operação realizada"
            
            Log.d(TAG, "onActivityResult: action=$action, message=$message")
            
            // Recarregar dados do banco
            refreshData { success ->
                if (success) {
                    // Mostrar toast na tela
                    ToastManager.showSuccess(this, message)
                }
            }
        }
    }

    private fun refreshData(callback: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                // Buscar transações pendentes atualizadas
                val pending = withContext(Dispatchers.IO) {
                    SupabaseService.getPendingTransactions()
                }
                
                incomeTransactions = pending.filter { it.type == "income" }
                expenseTransactions = pending.filter { it.type == "expense" }
                
                // Os fragments vão recarregar no onResume automaticamente
                callback(true)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao recarregar dados", e)
                callback(false)
            }
        }
    }
    
    /**
     * Marca transações como concluídas (pagas).
     */
    fun markAsCompleted(transactions: List<Transaction>, callback: (Boolean) -> Unit) {
        lifecycleScope.launch {
            var allSuccess = true
            
            for (transaction in transactions) {
                val success = SupabaseService.updateTransactionStatus(transaction.id, "completed")
                if (!success) {
                    allSuccess = false
                }
            }
            
            // Atualizar as listas locais para refletir a mudança
            if (allSuccess) {
                updateLocalLists(transactions)
            }
            
            callback(allSuccess)
        }
    }
    
    /**
     * Remove transações pagas das listas locais no Intent.
     */
    private fun updateLocalLists(paidTransactions: List<Transaction>) {
        val paidIds = paidTransactions.map { it.id }.toSet()
        
        val incomeList = getPendingIncomeTransactions().filter { it.id !in paidIds }.toCollection(ArrayList())
        val expenseList = getPendingExpenseTransactions().filter { it.id !in paidIds }.toCollection(ArrayList())
        
        intent.putExtra(EXTRA_INCOME_LIST, incomeList)
        intent.putExtra(EXTRA_EXPENSE_LIST, expenseList)
    }
}
