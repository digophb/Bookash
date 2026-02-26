package com.bookash.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var monthSelector: TextView
    private lateinit var balanceValue: TextView
    private lateinit var incomeValue: TextView
    private lateinit var expenseValue: TextView
    private lateinit var transactionsRecycler: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var nestedScroll: NestedScrollView
    
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBottomNavigation()
        setupTransactionsList()
        setupFab()
        setupScrollBehavior()
        loadUserData()
        loadTransactions()
    }

    private fun initViews() {
        welcomeText = findViewById(R.id.welcomeText)
        monthSelector = findViewById(R.id.monthSelector)
        balanceValue = findViewById(R.id.balanceValue)
        incomeValue = findViewById(R.id.incomeValue)
        expenseValue = findViewById(R.id.expenseValue)
        transactionsRecycler = findViewById(R.id.transactionsRecycler)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabAdd = findViewById(R.id.fabAdd)
        nestedScroll = findViewById(R.id.nestedScroll)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Já está na home
                    true
                }
                R.id.nav_transactions -> {
                    // Navegar para transações
                    true
                }
                R.id.nav_planning -> {
                    // Navegar para planejamento
                    true
                }
                R.id.nav_reports -> {
                    // Navegar para relatórios
                    true
                }
                R.id.nav_more -> {
                    // Navegar para mais
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTransactionsList() {
        transactionAdapter = TransactionAdapter()
        transactionsRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFab() {
        fabAdd.setOnClickListener {
            // Abrir modal de adicionar transação
            showAddTransactionDialog()
        }
    }

    private fun setupScrollBehavior() {
        nestedScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 0) {
                fabAdd.hide()
                bottomNavigation.visibility = View.GONE
            } else {
                fabAdd.show()
                bottomNavigation.visibility = View.VISIBLE
            }
        })
    }

    private fun loadUserData() {
        // Carregar dados do usuário logado
        val prefs = getSharedPreferences("bookash_prefs", MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Usuário") ?: "Usuário"
        welcomeText.text = "Olá, $userName"
        
        // Definir mês atual
        val currentMonth = java.text.SimpleDateFormat("MMMM 'de' yyyy", java.util.Locale("pt", "BR"))
            .format(java.util.Date())
        monthSelector.text = currentMonth.capitalize()
        
        // Carregar saldo (exemplo)
        balanceValue.text = "R$ 0,00"
        incomeValue.text = "R$ 0,00"
        expenseValue.text = "R$ 0,00"
    }

    private fun loadTransactions() {
        // Carregar transações do Supabase
        // Por enquanto, lista vazia
        val transactions = listOf<Transaction>()
        transactionAdapter.submitList(transactions)
    }

    private fun showAddTransactionDialog() {
        // TODO: Implementar modal de adicionar transação
    }
}
