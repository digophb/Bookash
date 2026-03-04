package com.bookash.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter para dropdown de contas com ícone e saldo
 */
class AccountDropdownAdapter(
    context: Context,
    private val accounts: List<Account>
) : ArrayAdapter<Account>(context, android.R.layout.simple_dropdown_item_1line, accounts) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_account, parent, false)

        val account = accounts[position]

        val iconView = view.findViewById<ImageView>(R.id.accountIcon)
        val nameView = view.findViewById<TextView>(R.id.accountName)
        val balanceView = view.findViewById<TextView>(R.id.accountBalance)

        // Nome da conta
        nameView.text = account.name

        // Saldo formatado
        balanceView.text = currencyFormat.format(account.balance)
        
        // Cor do saldo (verde se positivo, vermelho se negativo)
        val balanceColor = if (account.balance >= 0) {
            context.getColor(R.color.income)
        } else {
            context.getColor(R.color.expense)
        }
        balanceView.setTextColor(balanceColor)

        // Ícone do banco
        val iconRes = getBankIconResource(account.icon)
        iconView.setImageResource(iconRes)

        return view
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
}
