package com.bookash.app

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

/**
 * TextWatcher para formatar valores monetários em tempo real
 */
class CurrencyTextWatcher(
    private val editText: EditText
) : TextWatcher {

    private val locale = Locale("pt", "BR")
    private val numberFormat = NumberFormat.getCurrencyInstance(locale)
    private var current = ""
    private val currencySymbol = "R$"

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (s.toString() == current) {
            return
        }

        editText.removeTextChangedListener(this)

        // Remover tudo que não é dígito
        var cleanString = s.toString()
            .replace("[R$]".toRegex(), "")
            .replace("[.]".toRegex(), "")
            .replace("[,]".toRegex(), "")
            .replace(" ".toRegex(), "")
            .trim()

        if (cleanString.isEmpty()) {
            editText.setText("")
            editText.addTextChangedListener(this)
            return
        }

        // Converter para double (centavos)
        val parsed = cleanString.toDoubleOrNull()?.div(100.0) ?: 0.0

        // Formatar como moeda
        val formatted = numberFormat.format(parsed)

        current = formatted
        editText.setText(formatted)
        editText.setSelection(formatted.length)

        editText.addTextChangedListener(this)
    }

    companion object {
        /**
         * Extrai o valor numérico de uma string formatada como moeda
         */
        fun parseValue(formattedValue: String): Double {
            return formattedValue
                .replace("[R$]".toRegex(), "")
                .replace(" ".toRegex(), "")
                .replace("[.]".toRegex(), "")
                .replace(",", ".")
                .trim()
                .toDoubleOrNull() ?: 0.0
        }

        /**
         * Formata um valor double como moeda
         */
        fun formatValue(value: Double): String {
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return numberFormat.format(value)
        }
    }
}
