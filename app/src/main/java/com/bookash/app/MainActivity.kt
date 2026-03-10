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
    private lateinit var seeAllText: TextView
    
    // Metas
    private lateinit var goalsCard: MaterialCardView
    private lateinit var goalSelector: View
    private lateinit var goalTypeText: TextView
    private lateinit var goalProgressText: TextView
    private lateinit var goalTargetText: TextView
    private lateinit var goalProgressBar: View
    private lateinit var goalPercentText: TextView
    
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()
    private var userId: String? = null
    
    // Meta atual selecionada
    private var currentGoalType: String = "monthly"
    private var goals: List<Goal> = emptyList()
    private var goals: List<Goal> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar UserSession
        UserSession.init(this)
        
        // Se não está logado, ir para Login
        if (!UserSession.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        userId = UserSession.getUserId()
        
        setContentView(R.layout.activity_main)

        initViews()
        setupBottomNavigation()
        setupTransactionsList()
        setupFab()
        setupAvatarClick()
        setupScrollBehavior()
        setupSeeAllClick()
        setupGoalSelector()
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
        seeAllText = findViewById(R.id.seeAllText)
        
        // Metas
        goalsCard = findViewById(R.id.goalsCard)
        goalSelector = findViewById(R.id.goalSelector)
        goalTypeText = findViewById(R.id.goalTypeText)
        goalProgressText = findViewById(R.id.goalProgressText)
        goalTargetText = findViewById(R.id.goalTargetText)
        goalProgressBar = findViewById(R.id.goalProgressBar)
        goalPercentText = findViewById(R.id.goalPercentText)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    false
                }
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
        try {
            SettingsManager.clearCache()
            getSharedPreferences("bookash_prefs", MODE_PRIVATE).edit().clear().apply()
            ToastManager.showInfo(this, "Logout realizado com sucesso")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } catch (e: Exception) {
            ToastManager.showError(this, "Erro ao sair: ${e.message}")
        }
    }

    private fun setupTransactionsList() {
        transactionAdapter = TransactionAdapter { transaction ->
            val intent = Intent(this, TransactionDetailActivity::class.java)
            intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
        }
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

    private fun setupSeeAllClick() {
        seeAllText.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }
    }
    
    private fun setupGoalSelector() {
        goalSelector.setOnClickListener {
            showGoalSelector()
        }
    }
    
    private fun showGoalSelector() {
        val enabledGoals = goals.filter { it.isEnabled }
        if (enabledGoals.isEmpty()) return
        
        val goalNames = enabledGoals.map { goal ->
            goal.getDisplayName()
        }.toTypedArray()
        
        val currentIndex = enabledGoals.indexOfFirst { it.type == currentGoalType }.takeIf { it >= 0 } ?: 0
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Selecionar Meta")
            .setSingleChoiceItems(goalNames as Array<CharSequence>, currentIndex) { dialog, which ->
                currentGoalType = enabledGoals[which].type
                GoalsActivity.setSelectedGoal(this, currentGoalType)
                updateGoalsCard()
                dialog.dismiss()
            }
            .show()
    }
    
    private fun updateGoalsCard() {
        val enabledGoals = goals.filter { it.isEnabled }
        
        if (enabledGoals.isEmpty()) {
            goalsCard.visibility = View.GONE
            return
        }
        
        // Carregar meta selecionada ou a primeira disponível
        var selectedGoalType: String? = GoalsActivity.getSelectedGoal(this)
        if (selectedGoalType == null || enabledGoals.none { it.type == selectedGoalType }) {
            selectedGoalType = enabledGoals.firstOrNull()?.type
        }
        
        val goal = enabledGoals.find { it.type == selectedGoalType }
        
        if (goal == null || goal.targetAmount <= 0) {
            goalsCard.visibility = View.GONE
            return
        }
        
        currentGoalType = goal.type
        goalsCard.visibility = View.VISIBLE
        
        // Atualizar texto do tipo
        goalTypeText.text = goal.getDisplayName()
        
        // Calcular gastos do período
        val spent = calculateSpentForPeriod(goal.type)
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        goalProgressText.text = formatter.format(spent)
        goalTargetText.text = formatter.format(goal.targetAmount)
        
        // Calcular percentual
        val percent = if (goal.targetAmount > 0.0) {
            ((spent / goal.targetAmount) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }
        goalPercentText.text = "$percent% alcançado"
        
        // Atualizar barra de progresso (View com weight)
        val layoutParams = goalProgressBar.layoutParams as android.widget.LinearLayout.LayoutParams
        layoutParams.weight = percent.toFloat()
        goalProgressBar.layoutParams = layoutParams
        
        // Mudar cor se passou da meta
        if (spent > goal.targetAmount) {
            goalProgressBar.setBackgroundResource(R.color.error)
            goalPercentText.setTextColor(getColor(R.color.error))
        } else {
            goalProgressBar.setBackgroundResource(R.drawable.bg_progress_income)
            goalPercentText.setTextColor(getColor(R.color.text_secondary))
        }
    }
    
    private fun calculateSpentForPeriod(period: String): Double {
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val startDate = when (period) {
            "daily" -> {
                dateFormat.format(calendar.time)
            }
            "weekly" -> {
                calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek())
                dateFormat.format(calendar.time)
            }
            "monthly" -> {
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                dateFormat.format(calendar.time)
            }
            "yearly" -> {
                calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
                dateFormat.format(calendar.time)
            }
            else -> dateFormat.format(calendar.time)
        }
        
        // Filtrar transações do período (apenas despesas)
        return transactions.filter { it.type == "expense" && it.date >= startDate }.sumOf { it.amount }
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
            // Mostrar apenas os últimos 3 lançamentos no dashboard
            transactions.addAll(loadedTransactions.take(3))
            
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
            
            // Atualizar card de metas
            updateGoalsCard()
        }
    }
    
    private fun updateTotals() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        var totalIncome = 0.0
        var totalExpense = 0.0
        var dailyIncome = 0.0
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        
        transactions.forEach { t ->
            when (t.type) {
                "income" -> {
                    totalIncome += t.amount
                    if (t.date == today) {
                        dailyIncome += t.amount
                    }
                }
                "expense" -> {
                    totalExpense += t.amount
                }
                // transferência é ignorada pois não altera o saldo total
            }
        }
        
        // Calculate balance from active accounts
        // IMPORTANTE: Apenas contas com includeInBalance=true são somados ao total
        lifecycleScope.launch {
            val activeAccounts = SupabaseService.getAccounts(userId!!, archived = false)
            val accountsBalance = activeAccounts.filter { it.includeInBalance }.sumOf { it.balance }
            
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
    
    override fun onResume() {
        super.onResume()
        // Recarregar dados ao voltar para o dashboard
        // Isso garante que o saldo seja atualizado após criar/editar contas
        loadTransactions()
    }
    
    companion object {
        private const val REQUEST_ADD_TRANSACTION = 1001
    }
}
