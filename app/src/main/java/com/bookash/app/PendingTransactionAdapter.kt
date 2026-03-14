package com.bookash.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class PendingTransactionAdapter(
    private val onSelectionChanged: (List<Transaction>) -> Unit = {}
) : ListAdapter<Transaction, PendingTransactionAdapter.ViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<String>()

    val selectedCount: Int get() = selectedIds.size
    val selectedTotal: Double
        get() = currentList.filter { it.id in selectedIds }.sumOf { it.amount }

    val selectedTransaction: Transaction?
        get() = if (selectedIds.size == 1) currentList.find { it.id in selectedIds } else null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction, transaction.id in selectedIds) { isChecked ->
            toggleSelection(transaction.id, isChecked)
        }
    }

    private fun toggleSelection(id: String, isChecked: Boolean) {
        if (isChecked) {
            selectedIds.add(id)
        } else {
            selectedIds.remove(id)
        }
        // Atualizar apenas os itens visíveis para refletir o estado do checkbox
        notifyDataSetChanged()
        onSelectionChanged(getSelectedTransactions())
    }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }

    fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(currentList.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged(getSelectedTransactions())
    }

    fun getSelectedTransactions(): List<Transaction> {
        return currentList.filter { it.id in selectedIds }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val checkbox: CheckBox = view.findViewById(R.id.transactionCheckbox)
        private val iconImage: ImageView = view.findViewById(R.id.categoryIcon)
        private val descriptionText: TextView = view.findViewById(R.id.transactionDescription)
        private val categoryText: TextView = view.findViewById(R.id.transactionCategory)
        private val amountText: TextView = view.findViewById(R.id.transactionAmount)

        fun bind(
            transaction: Transaction,
            isSelected: Boolean,
            onCheckedChange: (Boolean) -> Unit
        ) {
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = isSelected

            if (transaction.type == "transfer") {
                val fromAccount = transaction.fromAccountName ?: "Conta origem"
                val toAccount = transaction.toAccountName ?: "Conta destino"
                descriptionText.text = "$fromAccount → $toAccount"
                categoryText.text = "Transferência"
            } else {
                descriptionText.text = transaction.description
                categoryText.text = transaction.categoryName.ifEmpty { "Sem categoria" }
            }

            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val formattedAmount = formatter.format(transaction.amount)

            when (transaction.type) {
                "income" -> {
                    amountText.text = "+$formattedAmount"
                    amountText.setTextColor(itemView.context.getColor(R.color.primary))
                }
                "transfer" -> {
                    amountText.text = formattedAmount
                    amountText.setTextColor(itemView.context.getColor(R.color.text_primary))
                }
                else -> {
                    amountText.text = "-$formattedAmount"
                    amountText.setTextColor(itemView.context.getColor(R.color.error))
                }
            }

            iconImage.setImageResource(transaction.iconRes)

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(isChecked)
            }

            // Clicar no item inteiro também alterna o checkbox
            itemView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
