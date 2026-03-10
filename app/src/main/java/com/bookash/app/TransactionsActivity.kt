package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransactionsActivity"
        private const val REQUEST_ADD_TRANSACTION = 1001
    }

    // Views
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var filterChipGroup: com.google.android.material.chip.ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipIncome: Chip
    private lateinit var chipExpense: Chip
    private lateinit var chipTransfer: Chip
    private lateinit var totalContainer: View
    private lateinit var totalValue: android.widget.TextView
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
    private var currentFilter: String = "all"

    // User
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        userId = UserSession.getUserId()
        initViews()
        setupListeners()
        loadTransactions()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        chipAll = findViewById(R.id.chipAll)
        chipIncome = findViewById(R.id.chipIncome)
        chipExpense = findViewById(R.id.chipExpense)
        chipTransfer = findViewById(R.id.chipTransfer)
        totalContainer = findViewById(R.id.totalContainer)
        totalValue = findViewById(R.id.totalValue)
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
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener {
            finish()
        }

        chipAll.setOnClickListener { currentFilter = "all"; applyFilter() }
        chipIncome.setOnClickListener { currentFilter = "income"; applyFilter() }
        chipExpense.setOnClickListener { currentFilter = "expense"; applyFilter() }
        chipTransfer.setOnClickListener { currentFilter = "transfer"; applyFilter() }
        
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
        }
        
        setupBottomNavigation()
        setupScrollBehavior()
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
        nestedScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 0) {
                fabAdd.hide()
            } else {
                fabAdd.show()
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
                applyFilter()
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar transacoes", e)
                progressBar.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                ToastManager.showError(this@TransactionsActivity, "Erro ao carregar transacoes")
            }
        }
    }

    private fun applyFilter() {
        filteredTransactions = when (currentFilter) {
            "income" -> allTransactions.filter { it.type == "income" }
            "expense" -> allTransactions.filter { it.type == "expense" }
            "transfer" -> allTransactions.filter { it.type == "transfer" }
            else -> allTransactions
        }

        updateUI()
    }

    private fun updateUI() {
        if (filteredTransactions.isEmpty()) {
            transactionsRecycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            totalContainer.visibility = View.GONE
            return
        }

        transactionsRecycler.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        // Transferências não alteram o saldo total, então escondemos o total
        if (currentFilter == "transfer") {
            totalContainer.visibility = View.GONE
        } else {
            totalContainer.visibility = View.VISIBLE

            // Calcular total (transferências são ignoradas pois não alteram o saldo)
            val total = filteredTransactions.sumOf { transaction ->
                when (transaction.type) {
                    "income" -> transaction.amount
                    "expense" -> -transaction.amount
                    else -> 0.0
                }
            }

            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            totalValue.text = formatter.format(total)
            totalValue.setTextColor(
                if (total >= 0) getColor(R.color.primary) else getColor(R.color.error)
            )
        }

        // Atualizar adapter
        transactionAdapter.submitList(filteredTransactions)
    }

    private fun openTransactionDetail(transaction: Transaction) {
        val intent = Intent(this, TransactionDetailActivity::class.java)
        intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
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
        // Recarregar ao voltar
        loadTransactions()
    }
}
