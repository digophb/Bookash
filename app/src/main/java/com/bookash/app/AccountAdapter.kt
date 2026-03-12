package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
    private val onArchiveClick: ((Account) -> Unit)? = null
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
                "outros" -> "Outros"
                else -> account.type.replaceFirstChar { it.uppercase() }
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
            
            // Esconder botão de deletar
            deleteButton.visibility = View.GONE
        }
        
        private fun getIconResource(iconName: String): Int {
            val icon = iconName.lowercase().trim()
            return when (icon) {
                // Bancos principais
                "nubank" -> R.drawable.ic_bank_nubank
                "itau" -> R.drawable.ic_bank_itau
                "bradesco" -> R.drawable.ic_bank_bradesco
                "bb", "bancodobrasil" -> R.drawable.ic_bank_bb
                "caixa" -> R.drawable.ic_bank_caixa
                "santander" -> R.drawable.ic_bank_santander
                
                // Bancos digitais
                "inter" -> R.drawable.ic_bank_inter
                "c6" -> R.drawable.ic_bank_c6
                "original" -> R.drawable.ic_bank_original
                "next" -> R.drawable.ic_bank_next
                "digio" -> R.drawable.ic_bank_digio
                "neon" -> R.drawable.ic_bank_neon
                
                // Outros bancos
                "bmg" -> R.drawable.ic_bank_bmg
                "safra" -> R.drawable.ic_bank_safra
                "btg" -> R.drawable.ic_bank_btg
                "votorantim" -> R.drawable.ic_bank_votorantim
                "banrisul" -> R.drawable.ic_bank_banrisul
                "nordeste" -> R.drawable.ic_bank_nordeste
                
                // Pagamentos
                "pagseguro" -> R.drawable.ic_bank_pagseguro
                "mercadopago" -> R.drawable.ic_bank_mercadopago
                "picpay" -> R.drawable.ic_bank_picpay
                
                // Carteira / outros
                "wallet", "carteira" -> R.drawable.ic_bank_wallet
                
                // Fallback
                else -> R.drawable.ic_bank_wallet
            }
        }
    }
}
