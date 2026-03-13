package com.bookash.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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
    private lateinit var goalProgressSpace: View
    private lateinit var goalPercentText: TextView

    // Dados das metas
    private lateinit var goals: List<Goal>
    private var currentGoalType: String = "monthly"

    // Pendentes
    private lateinit var pendingIncomeCount: TextView
    private lateinit var pendingIncomeTotal: TextView
    private lateinit var pendingIncomeRecycler: RecyclerView
    private lateinit var pendingExpenseCount: TextView
    private lateinit var pendingExpenseTotal: TextView
    private lateinit var pendingExpenseRecycler: RecyclerView

    private lateinit var pendingIncomeCard: MaterialCardView
    private lateinit var pendingExpenseCard: MaterialCardView
    private lateinit var monthSelector: LinearLayout

    // Controle de mês
    private var currentMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    private var currentYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()
    private var allTransactions: List<Transaction> = emptyList() // Todas para cálculos
    private var userId: String? = null

    // Adaptadores para pendentes
    private lateinit var pendingIncomeAdapter: TransactionAdapter
    private lateinit var pendingExpenseAdapter: TransactionAdapter
    private val pendingIncomeTransactions = mutableListOf<Transaction>()
    private val pendingExpenseTransactions = mutableListOf<Transaction>()
    
    // Getters públicos para acesso pelo fragment
    fun getPendingIncomeTransactions(): List<Transaction> = pendingIncomeTransactions.toList()
    fun getPendingExpenseTransactions(): List<Transaction> = pendingExpenseTransactions.toList()

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
        setupPendingCardsClick()
        setupMonthSelector()
        loadUserData()
        loadGoals()
        loadTransactions()

        // Mostrar toast de boas-vindas se veio do login
        if (intent.getBooleanExtra("SHOW_WELCOME_TOAST", false)) {
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
            ToastManager.showSuccess(this, "Bem-vindo de volta, $displayName!")
        }
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
        goalProgressSpace = findViewById(R.id.goalProgressSpace)
        goalPercentText = findViewById(R.id.goalPercentText)

        // Pendentes
        pendingIncomeCount = findViewById(R.id.pendingIncomeCount)
        pendingIncomeTotal = findViewById(R.id.pendingIncomeTotal)
        pendingIncomeRecycler = findViewById(R.id.pendingIncomeRecycler)
        pendingExpenseCount = findViewById(R.id.pendingExpenseCount)
        pendingExpenseTotal = findViewById(R.id.pendingExpenseTotal)
        pendingExpenseRecycler = findViewById(R.id.pendingExpenseRecycler)

        // Cards de pendentes
        pendingIncomeCard = findViewById(R.id.pendingIncomeCard)
        pendingExpenseCard = findViewById(R.id.pendingExpenseCard)

        // Seletor de mês
        monthSelector = findViewById(R.id.monthSelector)

        // Setup adapters pendentes
        pendingIncomeAdapter = TransactionAdapter { transaction ->
            val intent = Intent(this, TransactionDetailActivity::class.java)
            intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
        }
        pendingIncomeRecycler.layoutManager = LinearLayoutManager(this)
        pendingIncomeRecycler.adapter = pendingIncomeAdapter
        pendingIncomeRecycler.isNestedScrollingEnabled = false

        pendingExpenseAdapter = TransactionAdapter { transaction ->
            val intent = Intent(this, TransactionDetailActivity::class.java)
            intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
        }
        pendingExpenseRecycler.layoutManager = LinearLayoutManager(this)
        pendingExpenseRecycler.adapter = pendingExpenseAdapter
        pendingExpenseRecycler.isNestedScrollingEnabled = false
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
                updateGoalsCard()
                dialog.dismiss()
            }
            .show()
    }

    private fun updateGoalsCard(transactions: List<Transaction> = emptyList()) {
        val enabledGoals = goals.filter { it.isEnabled }

        if (enabledGoals.isEmpty()) {
            goalsCard.visibility = View.GONE
            return
        }

        // Usar currentGoalType se disponível, senão a primeira meta
        var selectedGoalType = currentGoalType
        if (selectedGoalType.isEmpty() || enabledGoals.none { it.type == selectedGoalType }) {
            selectedGoalType = enabledGoals.firstOrNull()?.type ?: ""
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

        // Calcular gastos do período (usar transações fornecidas ou todas)
        val transactionsToUse = if (transactions.isNotEmpty()) transactions else allTransactions
        val spent = calculateSpentForPeriod(goal.type, transactionsToUse)
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

        // Atualizar barra de progresso (duas Views com weight)
        val progressParams = goalProgressBar.layoutParams as android.widget.LinearLayout.LayoutParams
        val spaceParams = goalProgressSpace.layoutParams as android.widget.LinearLayout.LayoutParams
        progressParams.weight = percent.toFloat()
        spaceParams.weight = (100 - percent).toFloat()
        goalProgressBar.layoutParams = progressParams
        goalProgressSpace.layoutParams = spaceParams

        // Mudar cor se passou da meta
        if (spent > goal.targetAmount) {
            goalProgressBar.setBackgroundResource(R.color.error)
            goalPercentText.setTextColor(getColor(R.color.error))
        } else {
            goalProgressBar.setBackgroundResource(R.drawable.bg_progress_income)
            goalPercentText.setTextColor(getColor(R.color.text_secondary))
        }
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            try {
                goals = SupabaseService.getGoals(userId ?: "")
                updateGoalsCard()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Erro ao carregar metas", e)
            }
        }
    }

    private fun calculateSpentForPeriod(period: String, transactions: List<Transaction>): Double {
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

        // Filtrar transações do período (apenas RECEITAS/ganhos, não despesas)
        // Extrair apenas a parte da data (YYYY-MM-DD) para comparação
        return transactions.filter {
            it.type == "income" && it.date.substring(0, 10) >= startDate
        }.sumOf { it.amount }
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
            allTransactions = loadedTransactions // Salvar todas para cálculos

            // Filtrar transações completadas pelo mês selecionado
            val filteredCompleted = filterTransactionsByMonth(loadedTransactions.filter { it.status == "completed" || it.status == null })

            transactions.clear()
            // Mostrar apenas os últimos 3 lançamentos no dashboard
            transactions.addAll(filteredCompleted.take(3))

            if (transactions.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                transactionsRecycler.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                transactionsRecycler.visibility = View.VISIBLE
                transactionAdapter.submitList(transactions)
            }

            // Calcular totais (usar apenas transações filtradas do mês)
            updateTotals(filteredCompleted)

            // Atualizar card de metas (usar transações filtradas)
            updateGoalsCard(filteredCompleted)

            // Carregar transações pendentes (já filtra por mês dentro da função)
            loadPendingTransactions(loadedTransactions)
        }
    }

    private fun loadPendingTransactions(allTransactions: List<Transaction>) {
        // Pendentes: apenas status "pending"
        // NÃO afetam saldo, então vamos apenas mostrar quantidade e total no card
        val pendingList = allTransactions.filter { it.status == "pending" }
        val pendingIncome = pendingList.filter { it.type == "income" }
        val pendingExpense = pendingList.filter { it.type == "expense" }
        
        // Atualizar listas para acesso externo (fragment)
        pendingIncomeTransactions.clear()
        pendingIncomeTransactions.addAll(pendingIncome)
        pendingExpenseTransactions.clear()
        pendingExpenseTransactions.addAll(pendingExpense)

        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        // Atualizar receitas pendentes - apenas quantidade e total, sem lista
        if (pendingIncome.isEmpty()) {
            findViewById<View>(R.id.pendingIncomeCount).visibility = View.GONE
            findViewById<View>(R.id.pendingIncomeTotal).visibility = View.GONE
            pendingIncomeRecycler.visibility = View.GONE
        } else {
            findViewById<View>(R.id.pendingIncomeCount).visibility = View.VISIBLE
            findViewById<View>(R.id.pendingIncomeTotal).visibility = View.VISIBLE
            pendingIncomeRecycler.visibility = View.GONE // Ocultar lista
            pendingIncomeCount.text = pendingIncome.size.toString()
            val totalIncome = pendingIncome.sumOf { it.amount }
            pendingIncomeTotal.text = formatter.format(totalIncome)
            // Não submeter lista - card será clicável para abrir tela de detalhes
        }

        // Atualizar despesas pendentes - apenas quantidade e total, sem lista
        if (pendingExpense.isEmpty()) {
            findViewById<View>(R.id.pendingExpenseCount).visibility = View.GONE
            findViewById<View>(R.id.pendingExpenseTotal).visibility = View.GONE
            pendingExpenseRecycler.visibility = View.GONE
        } else {
            findViewById<View>(R.id.pendingExpenseCount).visibility = View.VISIBLE
            findViewById<View>(R.id.pendingExpenseTotal).visibility = View.VISIBLE
            pendingExpenseRecycler.visibility = View.GONE // Ocultar lista
            pendingExpenseCount.text = pendingExpense.size.toString()
            val totalExpense = pendingExpense.sumOf { it.amount }
            pendingExpenseTotal.text = formatter.format(totalExpense)
            // Não submeter lista - card será clicável para abrir tela de detalhes
        }
    }

    private fun updateTotals(filteredTransactions: List<Transaction> = emptyList()) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        // Data de hoje no formato ISO (apenas a parte da data)
        val isoDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val today = isoDateFormat.format(java.util.Date())
        
        var totalIncome = 0.0
        var totalExpense = 0.0
        var dailyIncome = 0.0
        
        // Usar transações filtradas se fornecidas, senão todas
        val transactionsToUse = if (filteredTransactions.isNotEmpty()) filteredTransactions else allTransactions
        
        transactionsToUse.forEach { t ->
            // Extrair apenas a parte da data (YYYY-MM-DD) da string ISO completa
            val transactionDate = t.date.substring(0, 10)
            
            when (t.type) {
                "income" -> {
                    totalIncome += t.amount
                    if (transactionDate == today) {
                        dailyIncome += t.amount
                    }
                }
                "expense" -> {
                    totalExpense += t.amount
                }
            }
        }
        
        // Calculate balance from active accounts (saldo calculado dinamicamente)
        lifecycleScope.launch {
            val activeAccounts = SupabaseService.getAccounts(userId!!, archived = false)
            
            // Calcular saldo de cada conta dinamicamente baseado nas transações
            val accountsWithCalculatedBalance = activeAccounts.map { account ->
                val calculatedBalance = SupabaseService.getAccountCalculatedBalance(account.id)
                account.copy(balance = calculatedBalance)
            }
            
            val accountsBalance = accountsWithCalculatedBalance.filter { it.includeInBalance }.sumOf { it.balance }
            
            balanceValue.text = formatter.format(accountsBalance)
            incomeValue.text = formatter.format(totalIncome)
            expenseValue.text = formatter.format(totalExpense)
            dailyIncomeValue.text = formatter.format(dailyIncome)
            
            android.util.Log.d("MainActivity", "Hoje: $today, Ganhos do dia: $dailyIncome, Total transações filtradas: ${transactionsToUse.size}")
        }
    }

    // ====== NOVAS FUNÇÕES PARA DASHBOARD ======

    /** Retorna o primeiro e último dia do mês/ano selecionado */
    private fun getMonthDateRange(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1) // Dia 1 do mês

        val startDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        // Último dia do mês
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
        val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        return Pair(startDate, endDate)
    }

    /** Filtra transações por data (inclusive) */
    private fun filterTransactionsByMonth(transactions: List<Transaction>): List<Transaction> {
        val (startDate, endDate) = getMonthDateRange()
        return transactions.filter { t ->
            t.date >= startDate && t.date <= endDate
        }
    }

    /** Configura clique nos cards de pendentes para abrir tela de detalhes */
    private fun setupPendingCardsClick() {
        pendingIncomeCard.setOnClickListener {
            val intent = Intent(this, PendingTransactionsActivity::class.java)
            intent.putExtra("TYPE", "income")
            startActivity(intent)
        }

        pendingExpenseCard.setOnClickListener {
            val intent = Intent(this, PendingTransactionsActivity::class.java)
            intent.putExtra("TYPE", "expense")
            startActivity(intent)
        }
    }

    /** Configura seletor de mês */
    private fun setupMonthSelector() {
        monthSelector.setOnClickListener {
            showMonthPicker()
        }
    }

    /** Mostra diálogo para selecionar mês/ano */
    private fun showMonthPicker() {
        val months = arrayOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )

        val currentMonthName = months[currentMonth]
        val years = (2020..2030).toList().map { it.toString() }.toTypedArray()
        val yearIdx = years.indexOf(currentYear.toString())

        // Usar MaterialAlertDialogBuilder para picker de mês/ano
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Selecionar mês")

        // Criar vista personalizada com TextView atualizável e botões de navegação
        // Para simplificar agora, vamos usar um SimpleListItem de mês e depois perguntar o ano
        // Em uma próxima versão, podemos implementar um DatePickerDialog com modo mês/ano

        builder.setSingleChoiceItems(months, currentMonth) { dialog, which ->
            currentMonth = which
            updateMonthText()
            dialog.dismiss()
        }

        builder.setPositiveButton("Ano") { dialog, which ->
            MaterialAlertDialogBuilder(this)
                .setTitle("Selecionar ano")
                .setSingleChoiceItems(years, yearIdx) { d, yearWhich ->
                    currentYear = years[yearWhich].toInt()
                    updateMonthText()
                    d.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    /** Atualiza texto do seletor de mês */
    private fun updateMonthText() {
        val months = arrayOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        monthText.text = "${months[currentMonth]} de $currentYear".capitalize()
        loadTransactions() // Recarregar com novo filtro
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ADD_TRANSACTION && resultCode == RESULT_OK) {
            ToastManager.showSuccess(this, "Transação salva com sucesso!")
            loadTransactions()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar dados ao voltar para o dashboard
        // Isso garante que o saldo seja atualizado após criar/editar contas
        loadGoals()
        loadTransactions()
    }
    
    // Getters públicos para acesso pelo fragment
    fun getPendingIncomeTransactions(): List<Transaction> {
        return pendingIncomeTransactions.toList()
    }

    fun getPendingExpenseTransactions(): List<Transaction> {
        return pendingExpenseTransactions.toList()
    }
    
    companion object {
        private const val REQUEST_ADD_TRANSACTION = 1001
    }
