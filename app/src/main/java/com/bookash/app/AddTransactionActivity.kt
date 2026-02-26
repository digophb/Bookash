package com.bookash.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var typeToggle: MaterialButtonToggleGroup
    private lateinit var btnIncome: MaterialButton
    private lateinit var btnExpense: MaterialButton
    private lateinit var btnTransfer: MaterialButton
    private lateinit var titleText: TextView
    private lateinit var valueInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var accountDropdown: AutoCompleteTextView
    private lateinit var dateInput: TextInputEditText
    private lateinit var recurrenceDropdown: AutoCompleteTextView
    private lateinit var installmentInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    
    private var selectedDate: Long = System.currentTimeMillis()
    private var transactionType: String = "income"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        initViews()
        setupTypeToggle()
        setupDropdowns()
        setupDatePicker()
        setupSaveButton()
    }

    private fun initViews() {
        typeToggle = findViewById(R.id.typeToggle)
        btnIncome = findViewById(R.id.btnIncome)
        btnExpense = findViewById(R.id.btnExpense)
        btnTransfer = findViewById(R.id.btnTransfer)
        titleText = findViewById(R.id.titleText)
        valueInput = findViewById(R.id.valueInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        categoryDropdown = findViewById(R.id.categoryDropdown)
        accountDropdown = findViewById(R.id.accountDropdown)
        dateInput = findViewById(R.id.dateInput)
        recurrenceDropdown = findViewById(R.id.recurrenceDropdown)
        installmentInput = findViewById(R.id.installmentInput)
        notesInput = findViewById(R.id.notesInput)
        saveButton = findViewById(R.id.saveButton)
        
        // Data atual
        updateDateDisplay()
    }

    private fun setupTypeToggle() {
        typeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> {
                        transactionType = "income"
                        titleText.text = "Nova Receita"
                        updateCategoryList(true)
                        updateColors(R.color.primary)
                    }
                    R.id.btnExpense -> {
                        transactionType = "expense"
                        titleText.text = "Nova Despesa"
                        updateCategoryList(false)
                        updateColors(R.color.error)
                    }
                    R.id.btnTransfer -> {
                        transactionType = "transfer"
                        titleText.text = "Nova Transferência"
                        updateColors(R.color.highlight_secondary)
                    }
                }
            }
        }
        
        // Selecionar Receita por padrão
        typeToggle.check(R.id.btnIncome)
    }

    private fun updateCategoryList(isIncome: Boolean) {
        val categories = if (isIncome) {
            arrayOf("Salário", "Freelance", "Investimentos", "Vendas", "Empréstimos", "Outros")
        } else {
            arrayOf("Alimentação", "Transporte", "Moradia", "Saúde", "Educação", "Lazer", "Compras", "Contas", "Outros")
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categoryDropdown.setAdapter(adapter)
    }

    private fun updateColors(colorRes: Int) {
        val color = getColor(colorRes)
        saveButton.setBackgroundColor(color)
    }

    private fun setupDropdowns() {
        // Categorias (será atualizado pelo tipo)
        updateCategoryList(true)
        
        // Contas
        val accounts = arrayOf("Carteira", "Conta Corrente", "Poupança", "Nubank", "Itaú", "Bradesco")
        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accounts)
        accountDropdown.setAdapter(accountAdapter)
        accountDropdown.setText("Carteira", false)
        
        // Recorrência
        val recurrenceOptions = arrayOf("Não repetir", "Diário", "Semanal", "Mensal", "Anual")
        val recurrenceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, recurrenceOptions)
        recurrenceDropdown.setAdapter(recurrenceAdapter)
        recurrenceDropdown.setText("Não repetir", false)
    }

    private fun setupDatePicker() {
        dateInput.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecione a data")
                .setSelection(selectedDate)
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                updateDateDisplay()
            }
            
            datePicker.show(supportFragmentManager, "datePicker")
        }
        
        dateInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dateInput.performClick()
            }
        }
    }

    private fun updateDateDisplay() {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        dateInput.setText(formatter.format(Date(selectedDate)))
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (validateFields()) {
                saveTransaction()
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true
        
        // Validar valor
        if (valueInput.text.isNullOrBlank()) {
            valueInput.error = "Digite o valor"
            isValid = false
        }
        
        // Validar descrição
        if (descriptionInput.text.isNullOrBlank()) {
            descriptionInput.error = "Digite uma descrição"
            isValid = false
        }
        
        return isValid
    }

    private fun saveTransaction() {
        val value = valueInput.text.toString()
            .replace("R$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0
        
        val description = descriptionInput.text.toString()
        val category = categoryDropdown.text.toString()
        val account = accountDropdown.text.toString()
        val recurrence = recurrenceDropdown.text.toString()
        val installment = installmentInput.text.toString().toIntOrNull() ?: 1
        val notes = notesInput.text.toString()
        
        // TODO: Salvar no Supabase
        // Por enquanto, retorna os dados
        
        val resultIntent = android.content.Intent().apply {
            putExtra("type", transactionType)
            putExtra("value", value)
            putExtra("description", description)
            putExtra("category", category)
            putExtra("account", account)
            putExtra("date", selectedDate)
            putExtra("recurrence", recurrence)
            putExtra("installment", installment)
            putExtra("notes", notes)
        }
        
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
