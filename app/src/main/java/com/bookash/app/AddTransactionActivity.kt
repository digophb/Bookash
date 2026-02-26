package com.bookash.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var typeToggle: MaterialButtonToggleGroup
    private lateinit var btnIncome: MaterialButton
    private lateinit var btnExpense: MaterialButton
    private lateinit var btnTransfer: MaterialButton
    private lateinit var titleText: android.widget.TextView
    private lateinit var receivedSwitch: MaterialSwitch
    private lateinit var valueInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var accountDropdown: AutoCompleteTextView
    private lateinit var dateInput: TextInputEditText
    private lateinit var repeatSwitch: MaterialSwitch
    private lateinit var notesInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    
    private var selectedDate: Long = System.currentTimeMillis()
    private var transactionType: String = "income"
    private var isReceived: Boolean = true
    private var isRecurring: Boolean = false
    private var recurrenceType: String = "Mensal"
    private var installments: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        initViews()
        setupTypeToggle()
        setupSwitches()
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
        receivedSwitch = findViewById(R.id.receivedSwitch)
        valueInput = findViewById(R.id.valueInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        categoryDropdown = findViewById(R.id.categoryDropdown)
        accountDropdown = findViewById(R.id.accountDropdown)
        dateInput = findViewById(R.id.dateInput)
        repeatSwitch = findViewById(R.id.repeatSwitch)
        notesInput = findViewById(R.id.notesInput)
        saveButton = findViewById(R.id.saveButton)
        
        updateDateDisplay()
    }

    private fun setupTypeToggle() {
        typeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> {
                        transactionType = "income"
                        titleText.text = "Nova Receita"
                        receivedSwitch.text = "Recebido"
                        updateCategoryList(true)
                        updateColors(R.color.primary)
                    }
                    R.id.btnExpense -> {
                        transactionType = "expense"
                        titleText.text = "Nova Despesa"
                        receivedSwitch.text = "Pago"
                        updateCategoryList(false)
                        updateColors(R.color.error)
                    }
                    R.id.btnTransfer -> {
                        transactionType = "transfer"
                        titleText.text = "Nova Transferência"
                        receivedSwitch.text = "Realizado"
                        updateColors(R.color.highlight_secondary)
                    }
                }
            }
        }
        
        typeToggle.check(R.id.btnIncome)
    }

    private fun setupSwitches() {
        // Switch Recebido/Pago
        receivedSwitch.setOnCheckedChangeListener { _, isChecked ->
            isReceived = isChecked
        }
        
        // Switch Repetir
        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            isRecurring = isChecked
            if (isChecked) {
                showRecurrenceDialog()
            }
        }
    }

    private fun showRecurrenceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recurrence, null)
        
        val periodDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.periodDropdown)
        val installmentsInput = dialogView.findViewById<TextInputEditText>(R.id.installmentsInput)
        
        // Configurar dropdown de período
        val periods = arrayOf("Diário", "Semanal", "Mensal", "Anual")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, periods)
        periodDropdown.setAdapter(adapter)
        periodDropdown.setText(recurrenceType, false)
        
        // Configurar parcelas
        installmentsInput.setText(installments.toString())
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Configurar Recorrência")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                recurrenceType = periodDropdown.text.toString()
                installments = installmentsInput.text.toString().toIntOrNull() ?: 1
            }
            .setNegativeButton("Cancelar") { _, _ ->
                repeatSwitch.isChecked = false
            }
            .setOnDismissListener {
                if (!isRecurring) {
                    repeatSwitch.isChecked = false
                }
            }
            .show()
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
        updateCategoryList(true)
        
        // Contas
        val accounts = arrayOf("Carteira", "Conta Corrente", "Poupança", "Nubank", "Itaú", "Bradesco")
        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, accounts)
        accountDropdown.setAdapter(accountAdapter)
        accountDropdown.setText("Carteira", false)
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
        
        if (valueInput.text.isNullOrBlank()) {
            valueInput.error = "Digite o valor"
            isValid = false
        }
        
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
        val notes = notesInput.text.toString()
        val status = if (isReceived) "received" else "pending"
        
        // TODO: Salvar no Supabase
        
        val resultIntent = android.content.Intent().apply {
            putExtra("type", transactionType)
            putExtra("value", value)
            putExtra("description", description)
            putExtra("category", category)
            putExtra("account", account)
            putExtra("date", selectedDate)
            putExtra("status", status)
            putExtra("isRecurring", isRecurring)
            if (isRecurring) {
                putExtra("recurrenceType", recurrenceType)
                putExtra("installments", installments)
            }
            putExtra("notes", notes)
        }
        
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
