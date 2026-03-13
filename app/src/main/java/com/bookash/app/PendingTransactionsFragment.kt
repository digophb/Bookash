package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bookash.app.Transaction
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import java.text.NumberFormat
import java.util.Locale

class PendingTransactionsFragment : Fragment() {

    companion object {
        private const val ARG_TYPE = "type"
        
        fun newInstance(type: String): PendingTransactionsFragment {
            return PendingTransactionsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: MaterialCardView
    private lateinit var emptyText: MaterialTextView
    private lateinit var totalValue: MaterialTextView
    
    private val type: String by lazy {
        arguments?.getString(ARG_TYPE) ?: "income"
    }
    
    private val adapter = TransactionAdapter { transaction ->
        // Abrir detalhes da transação
        val intent = Intent(requireContext(), TransactionDetailActivity::class.java)
        intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pending_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.pendingRecycler)
        emptyState = view.findViewById(R.id.emptyState)
        emptyText = view.findViewById(R.id.emptyText)
        totalValue = view.findViewById(R.id.totalValue)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
        
        loadTransactions()
    }
    
    private fun loadTransactions() {
        val activity = requireActivity() as? MainActivity
        val transactions: List<Transaction> = when (type) {
            "income" -> activity?.getPendingIncomeTransactions() as? List<Transaction> ?: emptyList()
            "expense" -> activity?.getPendingExpenseTransactions() as? List<Transaction> ?: emptyList()
            else -> emptyList()
        }
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        if (transactions.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            totalValue.visibility = View.GONE
            emptyText.text = when (type) {
                "income" -> "Nenhuma receita pendente"
                "expense" -> "Nenhuma despesa pendente"
                else -> "Nenhuma transação pendente"
            }
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            totalValue.visibility = View.VISIBLE
            
            // Mostrar todas as transações (não limitado)
            adapter.submitList(transactions)
            
            // Calcular total
            val total = transactions.sumOf { it.amount }
            totalValue.text = formatter.format(total)
        }
    }
}
