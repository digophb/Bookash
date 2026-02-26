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
                R.id.nav_more -> true
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        
        val typeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.typeToggle)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.categoryInput)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Nova Transação")
            .setView(dialogView)
            .setPositiveButton("Salvar") { dialog, _ ->
                val type = if (typeToggle.checkedButtonId == R.id.btnIncome) "income" else "expense"
                val description = descriptionInput.text.toString()
                val amountStr = amountInput.text.toString()
                val category = categoryInput.text.toString()
                
                if (description.isBlank() || amountStr.isBlank()) {
                    Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val amount = amountStr.replace(",", ".").replace("R$", "").replace(" ", "").toDoubleOrNull() ?: 0.0
                
                saveTransaction(type, description, amount, category)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveTransaction(type: String, description: String, amount: Double, category: String) {
        // TODO: Salvar no Supabase
        Toast.makeText(this, "Transação salva: $type - $description - R$ $amount", Toast.LENGTH_SHORT).show()
        
        // Atualizar UI
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        if (type == "income") {
            val current = incomeValue.text.toString()
                .replace("R$", "").replace(" ", "").replace(".", "")
                .replace(",", ".").toDoubleOrNull() ?: 0.0
            incomeValue.text = formatter.format(current + amount)
            
            val daily = dailyIncomeValue.text.toString()
                .replace("R$", "").replace(" ", "").replace(".", "")
                .replace(",", ".").toDoubleOrNull() ?: 0.0
            dailyIncomeValue.text = formatter.format(daily + amount)
        } else {
            val current = expenseValue.text.toString()
                .replace("R$", "").replace(" ", "").replace(".", "")
                .replace(",", ".").toDoubleOrNull() ?: 0.0
            expenseValue.text = formatter.format(current + amount)
        }
    }
}
