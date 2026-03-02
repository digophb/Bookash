package com.bookash.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var monthText: TextView
    private lateinit var balanceValue: TextView
    private lateinit var dailyIncomeValue: TextView
    private lateinit var incomeValue: TextView
    private lateinit var expenseValue: TextView
    private lateinit var transactionsRecycler: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var nestedScroll: NestedScrollView
    private lateinit var avatarCard: MaterialCardView
    private lateinit var emptyState: View
    
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar UserSession
        UserSession.init(this)
        userId = UserSession.getUserId()
        
        setContentView(R.layout.activity_main)

        initViews()
        setupBottomNavigation()
        setupTransactionsList()
        setupFab()
        setupAvatarClick()
        setupScrollBehavior()
        loadUserData()
        loadTransactions()
    }

    private fun initViews() {
        welcomeText = findViewById(R.id.welcomeText)
        monthText = findViewById(R.id.monthText)
        balanceValue = findViewById(R.id.balanceValue)
        dailyIncomeValue = findViewById(R.id.dailyIncomeValue)
        incomeValue = findViewById(R.id.incomeValue)
        expenseValue = findViewById(R.id.expenseValue)
        transactionsRecycler = findViewById(R.id.transactionsRecycler)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabAdd = findViewById(R.id.fabAdd)
        nestedScroll = findViewById(R.id.nestedScroll)
        avatarCard = findViewById(R.id.avatarCard)
        emptyState = findViewById(R.id.emptyState)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_transactions -> true
                R.id.nav_planning -> true
                R.id.nav_reports -> true
                R.id.nav_more -> {
                    startActivity(Intent(this, MoreOptionsActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
    
    private fun logout() {
        SettingsManager.clearCache()
        getSharedPreferences("bookash_prefs", MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
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
            startActivityForResult(Intent(this, AddTransactionActivity::class.java), REQUEST_ADD_TRANSACTION)
        }
    }

    private fun setupAvatarClick() {
        avatarCard.setOnClickListener {
            showUserMenu()
        }
    }

    private fun showUserMenu() {
        val options = arrayOf("Meu Perfil", "Sair")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Conta")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Perfil em desenvolvimento", Toast.LENGTH_SHORT).show()
                    1 -> logout()
                }
            }
            .show()
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
        val prefs = getSharedPreferences("bookash_prefs", MODE_PRIVATE)
        val userName = prefs.getString("user_name", null)
        
        val displayName = if (!userName.isNullOrEmpty()) {
            userName.split(" ").firstOrNull() ?: "Usuário"
        } else {
            val email = prefs.getString("user_email", null)
            if (!email.isNullOrEmpty()) {
                email.split("@").firstOrNull()?.capitalize() ?: "Usuário"
            } else {
                "Usuário"
            }
        }
        
        welcomeText.text = "Olá, $displayName"
        
        val currentMonth = java.text.SimpleDateFormat("MMMM 'de' yyyy", java.util.Locale("pt", "BR"))
            .format(java.util.Date())
        monthText.text = currentMonth.capitalize()
    }
    
    private fun loadTransactions() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("bookash_prefs", MODE_PRIVATE)
            val userId = prefs.getString("user_id", "") ?: ""
            
            if (userId.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                transactionsRecycler.visibility = View.GONE
                return@launch
            }
            
            val loadedTransactions = SupabaseService.getTransactions(userId)
            transactions.clear()
            transactions.addAll(loadedTransactions)
            
            if (transactions.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                transactionsRecycler.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                transactionsRecycler.visibility = View.VISIBLE
                transactionAdapter.submitList(transactions)
            }
            
            // Calcular totais
            updateTotals()
        }
    }
    
    private fun updateTotals() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        var totalIncome = 0.0
        var totalExpense = 0.0
        var dailyIncome = 0.0
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        
        transactions.forEach { t ->
            if (t.type == "income") {
                totalIncome += t.amount
                if (t.date == today) {
                    dailyIncome += t.amount
                }
            } else {
                totalExpense += t.amount
            }
        }
        
        // Calculate balance from active accounts
        lifecycleScope.launch {
            val activeAccounts = SupabaseService.getAccounts(userId!!, archived = false)
            val accountsBalance = activeAccounts.sumOf { it.balance }
            
            val balance = totalIncome - totalExpense + accountsBalance
            
            balanceValue.text = formatter.format(balance)
            incomeValue.text = formatter.format(totalIncome)
            expenseValue.text = formatter.format(totalExpense)
            dailyIncomeValue.text = formatter.format(dailyIncome)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_ADD_TRANSACTION && resultCode == RESULT_OK) {
            loadTransactions()
        }
    }
    
    companion object {
        private const val REQUEST_ADD_TRANSACTION = 1001
    }
}
