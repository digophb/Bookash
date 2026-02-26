package com.bookash.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_transactions -> true
                R.id.nav_planning -> true
                R.id.nav_reports -> true
                R.id.nav_more -> {
                    showMoreMenu()
                    false
                }
                else -> false
            }
        }
    }
    
    private fun showMoreMenu() {
        val options = arrayOf("Gerenciar", "Sair")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Mais Opções")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showManageMenu()
                    1 -> logout()
                }
            }
            .show()
    }
    
    private fun showManageMenu() {
        val options = arrayOf("Categorias", "Contas")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Gerenciar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(android.content.Intent(this, CategoriesActivity::class.java))
                    1 -> startActivity(android.content.Intent(this, AccountsActivity::class.java))
                }
            }
            .show()
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
            showAddTransactionDialog()
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
                    0 -> {
                        // Perfil - TODO
                        Toast.makeText(this, "Perfil em desenvolvimento", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        logout()
                    }
                }
            }
            .show()
    }

    private fun logout() {
        // Limpar dados salvos
        getSharedPreferences("bookash_prefs", MODE_PRIVATE).edit().clear().apply()
        
        // Ir para tela de login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
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
        
        balanceValue.text = "R$ 0,00"
        dailyIncomeValue.text = "R$ 0,00"
        incomeValue.text = "R$ 0,00"
        expenseValue.text = "R$ 0,00"
    }

    private fun loadTransactions() {
        val transactions = listOf<Transaction>()
        transactionAdapter.submitList(transactions)
    }

    private fun showAddTransactionDialog() {
        startActivityForResult(android.content.Intent(this, AddTransactionActivity::class.java), REQUEST_ADD_TRANSACTION)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_ADD_TRANSACTION && resultCode == RESULT_OK && data != null) {
            val type = data.getStringExtra("type") ?: "income"
            val value = data.getDoubleExtra("value", 0.0)
            val description = data.getStringExtra("description") ?: ""
            
            Toast.makeText(this, "${if (type == "income") "Receita" else "Despesa"} salva: $description - R$ ${String.format("%.2f", value)}", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        private const val REQUEST_ADD_TRANSACTION = 1001
    }
}