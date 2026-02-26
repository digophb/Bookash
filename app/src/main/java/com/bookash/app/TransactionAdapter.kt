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

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconImage: ImageView = view.findViewById(R.id.categoryIcon)
        private val descriptionText: TextView = view.findViewById(R.id.transactionDescription)
        private val categoryText: TextView = view.findViewById(R.id.transactionCategory)
        private val amountText: TextView = view.findViewById(R.id.transactionAmount)

        fun bind(transaction: Transaction) {
            descriptionText.text = transaction.description
            categoryText.text = transaction.category
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val formattedAmount = formatter.format(transaction.amount)
            
            if (transaction.type == "income") {
                amountText.text = "+$formattedAmount"
                amountText.setTextColor(itemView.context.getColor(R.color.primary))
            } else {
                amountText.text = "-$formattedAmount"
                amountText.setTextColor(itemView.context.getColor(R.color.error))
            }
            
            iconImage.setImageResource(transaction.iconRes)
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
