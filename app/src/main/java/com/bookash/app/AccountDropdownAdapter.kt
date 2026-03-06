package com.bookash.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView

/**
 * Adapter para dropdown de contas com ícone
 */
class AccountDropdownAdapter(
    context: Context,
    private val accounts: List<Account>
) : ArrayAdapter<String>(context, R.layout.item_dropdown_account_selected) {

    private val accountNames = accounts.map { it.name }

    override fun getCount(): Int = accountNames.size

    override fun getItem(position: Int): String = accountNames[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View do item selecionado - mostra ícone + nome
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_account_selected, parent, false)

        val account = accounts[position]

        val iconView = view.findViewById<ImageView>(R.id.accountIcon)
        val nameView = view.findViewById<TextView>(R.id.accountName)

        nameView.text = account.name

        val iconRes = getBankIconResource(account.icon)
        iconView.setImageResource(iconRes)

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View do dropdown - mostra ícone + nome
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_account, parent, false)

        val account = accounts[position]

        val iconView = view.findViewById<ImageView>(R.id.accountIcon)
        val nameView = view.findViewById<TextView>(R.id.accountName)

        nameView.text = account.name

        val iconRes = getBankIconResource(account.icon)
        iconView.setImageResource(iconRes)

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = accountNames
                results.count = accountNames.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return resultValue?.toString() ?: ""
            }
        }
    }

    private fun getBankIconResource(bankId: String): Int {
        return when (bankId) {
            "nubank" -> R.drawable.ic_bank_nubank
            "itau" -> R.drawable.ic_bank_itau
            "bradesco" -> R.drawable.ic_bank_bradesco
            "bb" -> R.drawable.ic_bank_bb
            "caixa" -> R.drawable.ic_bank_caixa
            "santander" -> R.drawable.ic_bank_santander
            "inter" -> R.drawable.ic_bank_inter
            "c6" -> R.drawable.ic_bank_c6
            "original" -> R.drawable.ic_bank_original
            "bmg" -> R.drawable.ic_bank_bmg
            "safra" -> R.drawable.ic_bank_safra
            "btg" -> R.drawable.ic_bank_btg
            "next" -> R.drawable.ic_bank_next
            "digio" -> R.drawable.ic_bank_digio
            "neon" -> R.drawable.ic_bank_neon
            "pagseguro" -> R.drawable.ic_bank_pagseguro
            "mercadopago" -> R.drawable.ic_bank_mercadopago
            "picpay" -> R.drawable.ic_bank_picpay
            "banrisul" -> R.drawable.ic_bank_banrisul
            "votorantim" -> R.drawable.ic_bank_votorantim
            "nordeste" -> R.drawable.ic_bank_nordeste
            "wallet" -> R.drawable.ic_bank_wallet
            else -> R.drawable.ic_bank_wallet
        }
    }
    
    /**
     * Retorna a conta pelo nome selecionado
     */
    fun getAccountByName(name: String): Account? {
        return accounts.find { it.name == name }
    }
}
