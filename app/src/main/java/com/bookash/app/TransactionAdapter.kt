package com.bookash.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit = {}
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction, onItemClick)
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconImage: ImageView = view.findViewById(R.id.categoryIcon)
        private val descriptionText: TextView = view.findViewById(R.id.transactionDescription)
        private val categoryText: TextView = view.findViewById(R.id.transactionCategory)
        private val amountText: TextView = view.findViewById(R.id.transactionAmount)

        fun bind(transaction: Transaction, onItemClick: (Transaction) -> Unit) {
            // Para transferências, mostrar contas de origem e destino na descrição
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
            
            itemView.setOnClickListener { onItemClick(transaction) }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
