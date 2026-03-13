package com.bookash.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PendingTransactionsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    companion object {
        const val EXTRA_TYPE = "type" // "income" ou "expense"
        const val EXTRA_INCOME_LIST = "income_list"
        const val EXTRA_EXPENSE_LIST = "expense_list"
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
            // Determinar títulos baseado no tipo selecionado
            val isIncomeFirst = typeFilter == "income"
            tab.text = when (position) {
                0 -> if (isIncomeFirst) "Receitas" else "Despesas"
                1 -> if (isIncomeFirst) "Despesas" else "Receitas"
                else -> "Outros"
            }
        }.attach()
    }
    
    // Métodos para que o fragment acesse as listas
    fun getPendingIncomeTransactions(): List<Transaction> {
        return (intent.getSerializableExtra(EXTRA_INCOME_LIST) as? ArrayList<Transaction>)?.toList() ?: emptyList()
    }
    
    fun getPendingExpenseTransactions(): List<Transaction> {
        return (intent.getSerializableExtra(EXTRA_EXPENSE_LIST) as? ArrayList<Transaction>)?.toList() ?: emptyList()
    }
}
