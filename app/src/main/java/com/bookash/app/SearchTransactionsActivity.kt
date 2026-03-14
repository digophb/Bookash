package com.bookash.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SearchTransactionsActivity : AppCompatActivity() {

    companion object {
        private const val SEARCH_DELAY_MS = 300L
    }

    // Views
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var searchInput: TextInputEditText
    private lateinit var periodChipGroup: ChipGroup
    private lateinit var chipPeriodAll: Chip
    private lateinit var chipPeriod7Days: Chip
    private lateinit var chipPeriod30Days: Chip
    private lateinit var chipPeriodThisMonth: Chip
    private lateinit var chipPeriodLastMonth: Chip
    private lateinit var chipPeriodCustom: Chip
    private lateinit var customPeriodContainer: View
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var resultCount: TextView
    private lateinit var resultsRecycler: RecyclerView
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    
    // Data
    private var allTransactions: List<Transaction> = emptyList()
    private var filteredTransactions: List<Transaction> = emptyList()
    private lateinit var transactionAdapter: TransactionAdapter
    private var searchJob: Job? = null
    
    // Filtros
    private var currentPeriod: String = "all"
    private var customStartDate: String = ""
    private var customEndDate: String = ""
    
    // User
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_transactions)
        
        userId = UserSession.getUserId()
        initViews()
        setupListeners()
        loadTransactions()
        
        // Focar no campo de busca e abrir teclado
        searchInput.requestFocus()
        searchInput.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        searchInput = findViewById(R.id.searchInput)
        periodChipGroup = findViewById(R.id.periodChipGroup)
        chipPeriodAll = findViewById(R.id.chipPeriodAll)
        chipPeriod7Days = findViewById(R.id.chipPeriod7Days)
        chipPeriod30Days = findViewById(R.id.chipPeriod30Days)
        chipPeriodThisMonth = findViewById(R.id.chipPeriodThisMonth)
        chipPeriodLastMonth = findViewById(R.id.chipPeriodLastMonth)
        chipPeriodCustom = findViewById(R.id.chipPeriodCustom)
        customPeriodContainer = findViewById(R.id.customPeriodContainer)
        startDateInput = findViewById(R.id.startDateInput)
        endDateInput = findViewById(R.id.endDateInput)
        resultCount = findViewById(R.id.resultCount)
        resultsRecycler = findViewById(R.id.resultsRecycler)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)
        
        // Setup adapter
        transactionAdapter = TransactionAdapter { transaction ->
            openTransactionDetail(transaction)
        }
        resultsRecycler.layoutManager = LinearLayoutManager(this)
        resultsRecycler.adapter = transactionAdapter
    }
    
    private fun setupListeners() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Busca com debounce
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(SEARCH_DELAY_MS)
                    performSearch()
                }
            }
        })
        
        // Busca ao pressionar "Pesquisar" no teclado
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
        
        // Chips de período
        chipPeriodAll.setOnClickListener { 
            currentPeriod = "all"
            customPeriodContainer.visibility = View.GONE
            performSearch()
        }
        chipPeriod7Days.setOnClickListener { 
            currentPeriod = "7days"
            customPeriodContainer.visibility = View.GONE
            performSearch()
        }
        chipPeriod30Days.setOnClickListener { 
            currentPeriod = "30days"
            customPeriodContainer.visibility = View.GONE
            performSearch()
        }
        chipPeriodThisMonth.setOnClickListener { 
            currentPeriod = "thisMonth"
            customPeriodContainer.visibility = View.GONE
            performSearch()
        }
        chipPeriodLastMonth.setOnClickListener { 
            currentPeriod = "lastMonth"
            customPeriodContainer.visibility = View.GONE
            performSearch()
        }
        chipPeriodCustom.setOnClickListener { 
            currentPeriod = "custom"
            customPeriodContainer.visibility = View.VISIBLE
            setDefaultCustomDates()
            performSearch()
        }
        
        // Datepickers
        startDateInput.setOnClickListener { showStartDatePicker() }
        endDateInput.setOnClickListener { showEndDatePicker() }
    }
    
    private fun loadTransactions() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val transactions = userId?.let {
                    withContext(Dispatchers.IO) {
                        SupabaseService.getTransactions(it)
                    }
                } ?: emptyList()
                
                allTransactions = transactions
                performSearch()
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                ToastManager.showError(this@SearchTransactionsActivity, "Erro ao carregar transações")
            }
        }
    }
    
    private fun performSearch() {
        val query = searchInput.text?.toString()?.trim()?.lowercase() ?: ""
        
        progressBar.visibility = View.VISIBLE
        
        // Buscar em coroutine para usar funções suspend
        lifecycleScope.launch {
            try {
                // Carregar categorias e contas para busca
                val categories = withContext(Dispatchers.IO) {
                    SupabaseService.getCategories(userId ?: "")
                }
                val accounts = withContext(Dispatchers.IO) {
                    SupabaseService.getAccounts(userId ?: "")
                }
                
                var result = allTransactions
                
                // Filtro por período
                result = filterByPeriod(result)
                
                // Filtro por texto (busca em descrição, categoria, conta, valor)
                if (query.isNotEmpty()) {
                    result = result.filter { transaction ->
                        // Buscar nome da categoria
                        val categoryName = categories.find { it.id == transaction.categoryId }?.name?.lowercase() ?: ""
                        // Buscar nome da conta
                        val accountName = accounts.find { it.id == transaction.toAccountId || it.id == transaction.fromAccountId }?.name?.lowercase() ?: ""
                        
                        transaction.description?.lowercase()?.contains(query) == true ||
                        categoryName.contains(query) ||
                        accountName.contains(query) ||
                        transaction.amount.toString().contains(query)
                    }
                }
                
                filteredTransactions = result
                updateUI()
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                ToastManager.showError(this@SearchTransactionsActivity, "Erro ao buscar transações")
            }
        }
    }
    
    private fun filterByPeriod(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        return when (currentPeriod) {
            "all" -> transactions
            "7days" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = dateFormat.format(calendar.time)
                transactions.filter { it.date.substring(0, 10) >= startDate }
            }
            "30days" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val startDate = dateFormat.format(calendar.time)
                transactions.filter { it.date.substring(0, 10) >= startDate }
            }
            "thisMonth" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = dateFormat.format(calendar.time)
                transactions.filter { it.date.substring(0, 10) >= startDate }
            }
            "lastMonth" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endDate = dateFormat.format(calendar.time)
                transactions.filter { 
                    val date = it.date.substring(0, 10)
                    date >= startDate && date <= endDate
                }
            }
            "custom" -> {
                if (customStartDate.isNotEmpty() && customEndDate.isNotEmpty()) {
                    transactions.filter { 
                        val date = it.date.substring(0, 10)
                        date >= customStartDate && date <= customEndDate
                    }
                } else transactions
            }
            else -> transactions
        }
    }
    
    private fun updateUI() {
        if (filteredTransactions.isEmpty()) {
            resultsRecycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            resultCount.visibility = View.GONE
        } else {
            resultsRecycler.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            resultCount.visibility = View.VISIBLE
            resultCount.text = "${filteredTransactions.size} resultado${if (filteredTransactions.size != 1) "s" else ""}"
            transactionAdapter.submitList(filteredTransactions)
        }
    }
    
    private fun setDefaultCustomDates() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Data final: hoje
        customEndDate = dateFormat.format(calendar.time)
        endDateInput.setText(formatDateForDisplay(customEndDate))
        
        // Data inicial: 30 dias atrás
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        customStartDate = dateFormat.format(calendar.time)
        startDateInput.setText(formatDateForDisplay(customStartDate))
    }
    
    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        if (customStartDate.isNotEmpty()) {
            try {
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(customStartDate) ?: Date()
            } catch (e: Exception) {}
        }
        
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                customStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time)
                startDateInput.setText(formatDateForDisplay(customStartDate))
                performSearch()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        if (customEndDate.isNotEmpty()) {
            try {
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(customEndDate) ?: Date()
            } catch (e: Exception) {}
        }
        
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                customEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time)
                endDateInput.setText(formatDateForDisplay(customEndDate))
                performSearch()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun formatDateForDisplay(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }
    
    private fun openTransactionDetail(transaction: Transaction) {
        val intent = Intent(this, TransactionDetailActivity::class.java)
        intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
    }
}
