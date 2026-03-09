package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransactionsActivity"
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

        transactionsRecycler.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener {
            finish()
        }

        filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipIncome -> "income"
                R.id.chipExpense -> "expense"
                R.id.chipTransfer -> "transfer"
                else -> "all"
            }
            applyFilter()
        }
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
        totalContainer.visibility = View.VISIBLE

        // Calcular total
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

        // Adapter
        val adapter = TransactionAdapter(filteredTransactions) { transaction ->
            openTransactionDetail(transaction)
        }
        transactionsRecycler.adapter = adapter
    }

    private fun openTransactionDetail(transaction: Transaction) {
        val intent = Intent(this, TransactionDetailActivity::class.java)
        intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recarregar ao voltar
        loadTransactions()
    }
}
