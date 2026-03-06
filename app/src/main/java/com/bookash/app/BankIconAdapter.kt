package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BankItem(
    val id: String,
    val name: String,
    val resId: Int, // Fallback drawable
    val svgPath: String? = null // SVG path em assets
)

class BankIconAdapter(
    private val onBankSelected: (String) -> Unit
) : RecyclerView.Adapter<BankIconAdapter.BankViewHolder>() {

    private val banks = listOf(
        BankItem("nubank", "Nubank", R.drawable.ic_bank_nubank, "banks/nubank.svg"),
        BankItem("itau", "Itaú", R.drawable.ic_bank_itau, "banks/itau.svg"),
        BankItem("bradesco", "Bradesco", R.drawable.ic_bank_bradesco, "banks/bradesco.svg"),
        BankItem("bb", "Banco do Brasil", R.drawable.ic_bank_bb, "banks/bb.svg"),
        BankItem("caixa", "Caixa", R.drawable.ic_bank_caixa, "banks/caixa.svg"),
        BankItem("santander", "Santander", R.drawable.ic_bank_santander, "banks/santander.svg"),
        BankItem("inter", "Inter", R.drawable.ic_bank_inter, "banks/inter.svg"),
        BankItem("c6", "C6 Bank", R.drawable.ic_bank_c6, "banks/c6.svg"),
        BankItem("original", "Original", R.drawable.ic_bank_original),
        BankItem("bmg", "BMG", R.drawable.ic_bank_bmg),
        BankItem("safra", "Safra", R.drawable.ic_bank_safra),
        BankItem("btg", "BTG Pactual", R.drawable.ic_bank_btg),
        BankItem("next", "Next", R.drawable.ic_bank_next),
        BankItem("digio", "Digio", R.drawable.ic_bank_digio),
        BankItem("neon", "Neon", R.drawable.ic_bank_neon, "banks/neon.svg"),
        BankItem("pagseguro", "PagSeguro", R.drawable.ic_bank_pagseguro),
        BankItem("mercadopago", "Mercado Pago", R.drawable.ic_bank_mercadopago, "banks/mercadopago.svg"),
        BankItem("picpay", "PicPay", R.drawable.ic_bank_picpay, "banks/picpay.svg"),
        BankItem("banrisul", "Banrisul", R.drawable.ic_bank_banrisul),
        BankItem("votorantim", "Votorantim", R.drawable.ic_bank_votorantim),
        BankItem("nordeste", "B. Nordeste", R.drawable.ic_bank_nordeste),
        BankItem("wallet", "Carteira", R.drawable.ic_bank_wallet)
    )

    private var selectedBank: String = "wallet"
    private var filteredBanks: List<BankItem> = banks
    private var showWalletOnly: Boolean = false

    fun setSelectedBank(bankId: String) {
        val oldPosition = filteredBanks.indexOfFirst { it.id == selectedBank }
        selectedBank = bankId
        val newPosition = filteredBanks.indexOfFirst { it.id == selectedBank }
        if (oldPosition >= 0) notifyItemChanged(oldPosition)
        if (newPosition >= 0) notifyItemChanged(newPosition)
    }

    fun filter(query: String) {
        filteredBanks = if (query.isEmpty()) {
            banks
        } else {
            banks.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.id.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
    
    fun showWalletOnly(show: Boolean) {
        showWalletOnly = show
        filteredBanks = if (show) {
            banks.filter { it.id == "wallet" }
        } else {
            banks
        }
        notifyDataSetChanged()
    }
    
    fun getBankName(bankId: String): String {
        return banks.find { it.id == bankId }?.name ?: bankId.replaceFirstChar { it.uppercase() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bank, parent, false)
        return BankViewHolder(view)
    }

    override fun onBindViewHolder(holder: BankViewHolder, position: Int) {
        holder.bind(filteredBanks[position])
    }

    override fun getItemCount() = filteredBanks.size

    inner class BankViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bankIcon: ImageView = itemView.findViewById(R.id.bankIcon)
        private val bankName: TextView = itemView.findViewById(R.id.bankName)

        fun bind(bank: BankItem) {
            // Carregar SVG se disponivel, senao usar fallback
            if (bank.svgPath != null) {
                SvgLoader.loadSvg(itemView.context, bankIcon, bank.svgPath, bank.resId)
            } else {
                bankIcon.setImageResource(bank.resId)
            }
            
            bankName.text = bank.name
            
            val isSelected = bank.id == selectedBank
            
            // Highlight background when selected
            val background = GradientDrawable()
            background.cornerRadius = 12f
            
            if (isSelected) {
                background.setColor(Color.parseColor("#2A3A35"))
                background.setStroke(2, Color.parseColor("#357266"))
            } else {
                background.setColor(Color.TRANSPARENT)
            }
            
            itemView.background = background
            
            itemView.setOnClickListener {
                setSelectedBank(bank.id)
                onBankSelected(bank.id)
            }
        }
    }
}
