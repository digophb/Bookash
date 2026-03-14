package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransactionsActivity"
        private const val REQUEST_ADD_TRANSACTION = 1001
        private const val REQUEST_FILTER = 1002
    }

    // Views
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var typeDropdown: AutoCompleteTextView
    private lateinit var monthSelector: View
    private lateinit var prevMonth: ImageView
    private lateinit var nextMonth: ImageView
    private lateinit var monthText: TextView
    private lateinit var balanceCardsContainer: View
    private lateinit var currentBalanceCard: View
    private lateinit var currentBalanceIcon: ImageView
    private lateinit var currentBalanceLabel: TextView
    private lateinit var currentBalanceValue: TextView
    private lateinit var monthlyBalanceCard: View
    private lateinit var monthlyBalanceIcon: ImageView
    private lateinit var monthlyBalanceLabel: TextView
    private lateinit var monthlyBalanceValue: TextView
    private lateinit var transactionsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var nestedScroll: NestedScrollView

    // Data
    private var allTransactions: List<Transaction> = emptyList()
    private var filteredTransactions: List<Transaction> = emptyList()
    private var currentTypeFilter: String = "all"
    
    // Filtros avançados
    private var filterStatus: String = "all"
    private var filterCategoryId: String = ""
    private var filterAccountId: String = ""
    private var filterTagId: String = ""
    private var filterStartDate: String = ""
    private var filterEndDate: String = ""
    private var filterPeriodEnabled: Boolean = false
    
    // Controle de mês
    private var currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private val months = arrayOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    // User
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        userId = UserSession.getUserId()
        initViews()
        setupListeners()
        updateMonthText()
        loadTransactions()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        typeDropdown = findViewById(R.id.typeDropdown)
        monthSelector = findViewById(R.id.monthSelector)
        prevMonth = findViewById(R.id.prevMonth)
        nextMonth = findViewById(R.id.nextMonth)
        monthText = findViewById(R.id.monthText)
        balanceCardsContainer = findViewById(R.id.balanceCardsContainer)
        currentBalanceCard = findViewById(R.id.currentBalanceCard)
        currentBalanceIcon = findViewById(R.id.currentBalanceIcon)
        currentBalanceLabel = findViewById(R.id.currentBalanceLabel)
        currentBalanceValue = findViewById(R.id.currentBalanceValue)
        monthlyBalanceCard = findViewById(R.id.monthlyBalanceCard)
        monthlyBalanceIcon = findViewById(R.id.monthlyBalanceIcon)
        monthlyBalanceLabel = findViewById(R.id.monthlyBalanceLabel)
        monthlyBalanceValue = findViewById(R.id.monthlyBalanceValue)
        transactionsRecycler = findViewById(R.id.transactionsRecycler)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabAdd = findViewById(R.id.fabAdd)
        nestedScroll = findViewById(R.id.nestedScroll)

        transactionAdapter = TransactionAdapter { transaction ->
            openTransactionDetail(transaction)
        }
        transactionsRecycler.layoutManager = LinearLayoutManager(this)
        transactionsRecycler.adapter = transactionAdapter
        
        // Marcar item de transações como selecionado
        bottomNavigation.selectedItemId = R.id.nav_transactions
        
        // Configurar dropdown de tipo
        val typeOptions = listOf("Todas", "Receitas", "Despesas", "Transferências")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, typeOptions)
        typeDropdown.setAdapter(typeAdapter)
        typeDropdown.setText("Todas", false)
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Menu do toolbar (pesquisa e filtro)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    openSearchActivity()
                    true
                }
                R.id.action_filter -> {
                    openFilterActivity()
                    true
                }
                else -> false
            }
        }

        // Dropdown de tipo
        typeDropdown.setOnItemClickListener { _, _, position, _ ->
            currentTypeFilter = when (position) {
                0 -> "all"
                1 -> "income"
                2 -> "expense"
                3 -> "transfer"
                else -> "all"
            }
            applyFilters()
        }
        
        // Seletor mensal
        prevMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 0) {
                currentMonth = 11
                currentYear--
            }
            updateMonthText()
            applyFilters()
        }
        
        nextMonth.setOnClickListener {
            currentMonth++
            if (currentMonth > 11) {
                currentMonth = 0
                currentYear++
            }
            updateMonthText()
            applyFilters()
        }
        
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
        }
        
        setupBottomNavigation()
        setupScrollBehavior()
    }
    
    private fun updateMonthText() {
        monthText.text = "${months[currentMonth]} de $currentYear"
    }
    
    private fun getMonthDateRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        
        return Pair(startDate, endDate)
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    finish()
                    true
                }
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
    
    private fun setupScrollBehavior() {
        nestedScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY && scrollY > 50) {
                // Rolando para baixo
                fabAdd.hide()
                bottomNavigation.animate()
                    .translationY(bottomNavigation.height.toFloat())
                    .setDuration(200)
                    .start()
            } else if (scrollY < oldScrollY) {
                // Rolando para cima
                fabAdd.show()
                bottomNavigation.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        })
    }

    private fun loadTransactions() {
        progressBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val transactions = userId?.let {
                    withContext(Dispatchers.IO) {
                        SupabaseService.getTransactions(it)
                    }
                } ?: emptyList()

                allTransactions = transactions
                applyFilters()
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar transacoes", e)
                progressBar.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                ToastManager.showError(this@TransactionsActivity, "Erro ao carregar transacoes")
            }
        }
    }

    private fun applyFilters() {
        var result = allTransactions
        
        // Filtro por mês selecionado (sempre aplicado)
        val (monthStart, monthEnd) = getMonthDateRange()
        result = result.filter { transaction ->
            val transactionDate = transaction.date.substring(0, 10)
            transactionDate >= monthStart && transactionDate <= monthEnd
        }
        
        // Filtro por tipo
        result = when (currentTypeFilter) {
            "income" -> result.filter { it.type == "income" }
            "expense" -> result.filter { it.type == "expense" }
            "transfer" -> result.filter { it.type == "transfer" }
            else -> result
        }
        
        // Filtro por situação
        result = when (filterStatus) {
            "completed" -> result.filter { it.status == "completed" || it.status == null }
            "pending" -> result.filter { it.status == "pending" }
            else -> result
        }
        
        // Filtro por categoria
        if (filterCategoryId.isNotEmpty()) {
            result = result.filter { it.categoryId == filterCategoryId }
        }
        
        // Filtro por conta
        if (filterAccountId.isNotEmpty()) {
            result = result.filter { it.toAccountId == filterAccountId || it.fromAccountId == filterAccountId }
        }
        
        // Filtro por período adicional (se ativado)
        if (filterPeriodEnabled && filterStartDate.isNotEmpty() && filterEndDate.isNotEmpty()) {
            result = result.filter { transaction ->
                val transactionDate = transaction.date.substring(0, 10)
                transactionDate >= filterStartDate && transactionDate <= filterEndDate
            }
        }

        filteredTransactions = result
        updateUI()
    }

    private fun updateUI() {
        if (filteredTransactions.isEmpty()) {
            transactionsRecycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        transactionsRecycler.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        // Cards de saldo - comportamento baseado no tipo selecionado
        when (currentTypeFilter) {
            "all" -> {
                // Todas: mostrar Saldo Atual e Balanço Mensal
                balanceCardsContainer.visibility = View.VISIBLE
                setupBalanceCardsAsDefault()
                calculateBalanceCards()
            }
            "income" -> {
                // Receitas: mostrar Total Pendente e Total Recebido
                balanceCardsContainer.visibility = View.VISIBLE
                setupBalanceCardsForIncome()
                calculatePendingCompletedCards("income")
            }
            "expense" -> {
                // Despesas: mostrar Total Pendente e Total Pago
                balanceCardsContainer.visibility = View.VISIBLE
                setupBalanceCardsForExpense()
                calculatePendingCompletedCards("expense")
            }
            else -> {
                balanceCardsContainer.visibility = View.GONE
            }
        }

        // Atualizar adapter
        transactionAdapter.submitList(filteredTransactions)
    }
    
    private fun calculateBalanceCards() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        // Calcular saldo atual (de todas as contas) - usar lifecycleScope para chamar suspend functions
        lifecycleScope.launch {
            try {
                val accounts = withContext(Dispatchers.IO) {
                    SupabaseService.getAccounts(userId ?: "")
                }
                val balances = withContext(Dispatchers.IO) {
                    SupabaseService.getAllAccountBalances(userId ?: "")
                }
                
                // Somar saldos de todas as contas (que estão incluídas no saldo)
                val totalBalance = accounts.filter { it.includeInBalance }.sumOf { account ->
                    balances[account.id] ?: 0.0
                }
                
                currentBalanceValue.text = formatter.format(totalBalance)
                currentBalanceValue.setTextColor(
                    if (totalBalance >= 0) getColor(R.color.primary) else getColor(R.color.error)
                )
            } catch (e: Exception) {
                currentBalanceValue.text = "Erro"
            }
        }
        
        // Calcular balanço mensal (receitas - despesas do mês) - pode ser feito sem coroutine
        val monthlyIncome = filteredTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val monthlyExpense = filteredTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        val monthlyBalance = monthlyIncome - monthlyExpense
        
        monthlyBalanceValue.text = formatter.format(monthlyBalance)
        monthlyBalanceValue.setTextColor(
            if (monthlyBalance >= 0) getColor(R.color.primary) else getColor(R.color.error)
        )
    }

    private fun setupBalanceCardsAsDefault() {
        currentBalanceIcon.setImageResource(R.drawable.ic_wallet)
        currentBalanceLabel.text = "Saldo Atual"
        monthlyBalanceIcon.setImageResource(R.drawable.ic_chart)
        monthlyBalanceLabel.text = "Balanço Mensal"
    }

    private fun setupBalanceCardsForIncome() {
        currentBalanceIcon.setImageResource(R.drawable.ic_pending)
        currentBalanceLabel.text = "Total Pendente"
        monthlyBalanceIcon.setImageResource(R.drawable.ic_completed)
        monthlyBalanceLabel.text = "Total Recebido"
    }

    private fun setupBalanceCardsForExpense() {
        currentBalanceIcon.setImageResource(R.drawable.ic_pending)
        currentBalanceLabel.text = "Total Pendente"
        monthlyBalanceIcon.setImageResource(R.drawable.ic_completed)
        monthlyBalanceLabel.text = "Total Pago"
    }

    private fun calculatePendingCompletedCards(type: String) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val transactionsOfType = filteredTransactions.filter { it.type == type }

        val pendingTotal = transactionsOfType.filter { it.status == "pending" }.sumOf { it.amount }
        val completedTotal = transactionsOfType.filter { it.status == "completed" || it.status == null }.sumOf { it.amount }

        currentBalanceValue.text = formatter.format(pendingTotal)
        currentBalanceValue.setTextColor(getColor(R.color.text_secondary))

        monthlyBalanceValue.text = formatter.format(completedTotal)
        monthlyBalanceValue.setTextColor(getColor(R.color.primary))
    }
    
    private fun openSearchActivity() {
        val intent = Intent(this, SearchTransactionsActivity::class.java)
        startActivity(intent)
    }

    private fun openFilterActivity() {
        val intent = Intent(this, FilterTransactionsActivity::class.java)
        intent.putExtra(FilterTransactionsActivity.EXTRA_FILTER_STATUS, filterStatus)
        intent.putExtra(FilterTransactionsActivity.EXTRA_FILTER_CATEGORY, filterCategoryId)
        intent.putExtra(FilterTransactionsActivity.EXTRA_FILTER_ACCOUNT, filterAccountId)
        intent.putExtra(FilterTransactionsActivity.EXTRA_FILTER_TAG, filterTagId)
        intent.putExtra(FilterTransactionsActivity.EXTRA_FILTER_START_DATE, filterStartDate)
        intent.putExtra(FilterTransactionsActivity.EXTRA_FILTER_END_DATE, filterEndDate)
        intent.putExtra(FilterTransactionsActivity.EXTRA_FILTER_PERIOD_ENABLED, filterPeriodEnabled)
        startActivityForResult(intent, REQUEST_FILTER)
    }

    private fun openTransactionDetail(transaction: Transaction) {
        val intent = Intent(this, TransactionDetailActivity::class.java)
        intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_ADD_TRANSACTION -> {
                if (resultCode == RESULT_OK) {
                    loadTransactions()
                }
            }
            REQUEST_FILTER -> {
                if (resultCode == RESULT_OK) {
                    data?.let {
                        filterStatus = it.getStringExtra(FilterTransactionsActivity.RESULT_FILTER_STATUS) ?: "all"
                        filterCategoryId = it.getStringExtra(FilterTransactionsActivity.RESULT_FILTER_CATEGORY) ?: ""
                        filterAccountId = it.getStringExtra(FilterTransactionsActivity.RESULT_FILTER_ACCOUNT) ?: ""
                        filterTagId = it.getStringExtra(FilterTransactionsActivity.RESULT_FILTER_TAG) ?: ""
                        filterStartDate = it.getStringExtra(FilterTransactionsActivity.RESULT_FILTER_START_DATE) ?: ""
                        filterEndDate = it.getStringExtra(FilterTransactionsActivity.RESULT_FILTER_END_DATE) ?: ""
                        filterPeriodEnabled = it.getBooleanExtra("filter_period_enabled", false)
                        
                        applyFilters()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar ao voltar
        loadTransactions()
    }
}
