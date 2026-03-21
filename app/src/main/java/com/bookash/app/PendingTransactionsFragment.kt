package com.bookash.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import java.text.NumberFormat
import java.util.Locale

class PendingTransactionsFragment : Fragment() {

    companion object {
        private const val ARG_TYPE = "type"
        private const val REQUEST_EDIT_TRANSACTION = 1001

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
    private lateinit var actionBar: MaterialCardView
    private lateinit var selectedCount: MaterialTextView
    private lateinit var selectedTotal: MaterialTextView
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnPay: MaterialButton
    
    private val type: String by lazy {
        arguments?.getString(ARG_TYPE) ?: "income"
    }
    
    private val adapter = PendingTransactionAdapter { selectedTransactions ->
        onSelectionChanged(selectedTransactions)
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
        actionBar = view.findViewById(R.id.actionBar)
        selectedCount = view.findViewById(R.id.selectedCount)
        selectedTotal = view.findViewById(R.id.selectedTotal)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnPay = view.findViewById(R.id.btnPay)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
        
        btnEdit.setOnClickListener {
            val transaction = adapter.selectedTransaction
            if (transaction != null) {
                openTransactionDetail(transaction)
            }
        }
        
        btnPay.setOnClickListener {
            val selected = adapter.getSelectedTransactions()
            if (selected.isNotEmpty()) {
                confirmPayment(selected)
            }
        }
        
        loadTransactions()
    }
    
    override fun onResume() {
        super.onResume()
        // Recarregar ao voltar (caso tenha editado algo)
        adapter.clearSelection()
        loadTransactions()
    }
    
    private fun onSelectionChanged(selected: List<Transaction>) {
        if (selected.isEmpty()) {
            actionBar.visibility = View.GONE
        } else {
            actionBar.visibility = View.VISIBLE
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            
            if (selected.size == 1) {
                selectedCount.text = "1 selecionado"
                btnEdit.visibility = View.VISIBLE
            } else {
                selectedCount.text = "${selected.size} selecionados"
                btnEdit.visibility = View.GONE
            }
            
            val total = selected.sumOf { it.amount }
            selectedTotal.text = formatter.format(total)
        }
    }
    
    private fun confirmPayment(transactions: List<Transaction>) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val total = transactions.sumOf { it.amount }
        val count = transactions.size
        
        val message = if (count == 1) {
            "Confirmar pagamento de ${formatter.format(total)}?\n\nA transação será marcada como concluída."
        } else {
            "Confirmar pagamento de $count transações?\n\nTotal: ${formatter.format(total)}\nAs transações serão marcadas como concluídas."
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Pagamento")
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Pagar") { _, _ ->
                executePayment(transactions)
            }
            .show()
    }
    
    private fun executePayment(transactions: List<Transaction>) {
        val activity = requireActivity() as? PendingTransactionsActivity
        activity?.markAsCompleted(transactions) { success ->
            if (success) {
                val count = transactions.size
                val msg = if (count == 1) "Transação paga com sucesso!" else "$count transações pagas com sucesso!"
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(requireContext().getColor(R.color.primary))
                    .setTextColor(requireContext().getColor(R.color.text_primary))
                    .show()
                
                adapter.clearSelection()
                loadTransactions()
            } else {
                Snackbar.make(requireView(), "Erro ao processar pagamento", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(requireContext().getColor(R.color.error))
                    .show()
            }
        }
    }
    
    private fun openTransactionDetail(transaction: Transaction) {
        val intent = Intent(requireContext(), AddTransactionActivity::class.java)
        intent.putExtra(AddTransactionActivity.EXTRA_TRANSACTION_ID, transaction.id)
        startActivityForResult(intent, REQUEST_EDIT_TRANSACTION)
    }
    
    private fun loadTransactions() {
        val activity = requireActivity() as? PendingTransactionsActivity
        val transactions: List<Transaction> = when (type) {
            "income" -> activity?.getPendingIncomeTransactions() ?: emptyList()
            "expense" -> activity?.getPendingExpenseTransactions() ?: emptyList()
            else -> emptyList()
        }
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        if (transactions.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            totalValue.visibility = View.GONE
            actionBar.visibility = View.GONE
            emptyText.text = when (type) {
                "income" -> "Nenhuma receita pendente"
                "expense" -> "Nenhuma despesa pendente"
                else -> "Nenhuma transação pendente"
            }
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            totalValue.visibility = View.VISIBLE
            
            adapter.submitList(transactions)
            
            // Calcular total
            val total = transactions.sumOf { it.amount }
            totalValue.text = formatter.format(total)
        }
    }
}
