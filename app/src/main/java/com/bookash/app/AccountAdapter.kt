package com.bookash.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class AccountAdapter(
    private val onEditClick: ((Account) -> Unit)? = null,
    private val onArchiveClick: ((Account) -> Unit)? = null,
    private val onDeleteClick: ((Account) -> Unit)? = null
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private val items = mutableListOf<Account>()

    fun submitList(newItems: List<Account>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val accountIcon: ImageView = itemView.findViewById(R.id.accountIcon)
        private val accountName: TextView = itemView.findViewById(R.id.accountName)
        private val accountType: TextView = itemView.findViewById(R.id.accountType)
        private val accountBalance: TextView = itemView.findViewById(R.id.accountBalance)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val archiveButton: ImageButton = itemView.findViewById(R.id.archiveButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(account: Account) {
            accountName.text = account.name
            
            // Tipo em português
            accountType.text = when (account.type.lowercase()) {
                "corrente" -> "Conta Corrente"
                "poupança", "poupanca" -> "Poupança"
                "carteira" -> "Carteira"
                "digital" -> "Conta Digital"
                else -> account.type
            }
            
            // Saldo formatado
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            accountBalance.text = formatter.format(account.balance)
            
            // Cor do saldo (verde se positivo, vermelho se negativo)
            accountBalance.setTextColor(
                if (account.balance >= 0) {
                    Color.parseColor("#2E7D6A") // income color
                } else {
                    Color.parseColor("#B85450") // expense color
                }
            )
            
            // Ícone baseado no banco/tipo
            val iconRes = getIconResource(account.icon)
            accountIcon.setImageResource(iconRes)
            
            // Action buttons
            editButton.setOnClickListener {
                onEditClick?.invoke(account)
            }
            
            archiveButton.setOnClickListener {
                onArchiveClick?.invoke(account)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(account)
            }
        }
        
        private fun getIconResource(iconName: String): Int {
            return when (iconName.lowercase()) {
                "nubank" -> R.drawable.ic_bank_nubank
                "itau" -> R.drawable.ic_bank_itau
                "bradesco" -> R.drawable.ic_bank_bradesco
                "bb", "bancodobrasil" -> R.drawable.ic_bank_bb
                "wallet", "carteira" -> R.drawable.ic_wallet
                else -> R.drawable.ic_account
            }
        }
    }
}
