package com.bookash.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
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
    
    private val categories = mutableListOf<Category>()
    private val accounts = mutableListOf<Account>()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        
        userId = UserSession.getUserId()

        initViews()
        setupTypeToggle()
        setupSwitches()
        setupDatePicker()
        setupSaveButton()
        loadData()
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
    
    private fun loadData() {
        if (userId == null) return
        
        lifecycleScope.launch {
            // Carregar categorias
            val loadedCategories = SupabaseService.getCategories(userId!!, "income")
            categories.clear()
            categories.addAll(loadedCategories)
            updateCategoryDropdown()
            
            // Carregar contas
            val loadedAccounts = SupabaseService.getAccounts(userId!!)
            accounts.clear()
            accounts.addAll(loadedAccounts)
            updateAccountDropdown()
        }
    }
    
    private fun updateCategoryDropdown() {
        val names = categories.map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        categoryDropdown.setAdapter(adapter)
    }
    
    private fun updateAccountDropdown() {
        val names = accounts.map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        accountDropdown.setAdapter(adapter)
        if (names.isNotEmpty()) {
            accountDropdown.setText(names[0], false)
        }
    }

    private fun setupTypeToggle() {
        typeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> {
                        transactionType = "income"
                        titleText.text = "Nova Receita"
                        receivedSwitch.text = "Recebido"
                        updateColors(R.color.income)
                        if (userId != null) {
                            lifecycleScope.launch {
                                val cats = SupabaseService.getCategories(userId!!, "income")
                                categories.clear()
                                categories.addAll(cats)
                                updateCategoryDropdown()
                            }
                        }
                    }
                    R.id.btnExpense -> {
                        transactionType = "expense"
                        titleText.text = "Nova Despesa"
                        receivedSwitch.text = "Pago"
                        updateColors(R.color.expense)
                        if (userId != null) {
                            lifecycleScope.launch {
                                val cats = SupabaseService.getCategories(userId!!, "expense")
                                categories.clear()
                                categories.addAll(cats)
                                updateCategoryDropdown()
                            }
                        }
                    }
                }
            }
        }
    }
                            val cats = SupabaseService.getCategories(userId, "expense")
                            categories.clear()
                            categories.addAll(cats)
                            updateCategoryDropdown()
                        }
                    }
                    R.id.btnTransfer -> {
                        transactionType = "transfer"
                        titleText.text = "Nova Transferência"
                        receivedSwitch.text = "Realizado"
                        updateColors(R.color.transfer)
                    }
                }
            }
        }
        
        typeToggle.check(R.id.btnIncome)
    }

    private fun setupSwitches() {
        receivedSwitch.setOnCheckedChangeListener { _, isChecked ->
            isReceived = isChecked
        }
        
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
        
        val periods = arrayOf("Diário", "Semanal", "Mensal", "Anual")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, periods)
        periodDropdown.setAdapter(adapter)
        periodDropdown.setText(recurrenceType, false)
        
        installmentsInput.setText(installments.toString())
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Configurar Recorrência")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                recurrenceType = periodDropdown.text.toString()
                installments = installmentsInput.text.toString().toIntOrNull() ?: 1
            }
            .setNegativeButton("Cancelar") { _, _ ->
                repeatSwitch.isChecked = false
            }
            .show()
    }

    private fun updateColors(colorRes: Int) {
        val color = getColor(colorRes)
        saveButton.setBackgroundColor(color)
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
        
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = formatter.format(Date(selectedDate))
        
        // Pegar user_id e token
        val prefs = getSharedPreferences("bookash_prefs", MODE_PRIVATE)
        val userId = prefs.getString("user_id", "") ?: ""
        val token = prefs.getString("access_token", "") ?: ""
        
        // Encontrar account_id selecionado
        val selectedAccount = accounts.find { it.name == account }
        
        val transaction = Transaction(
            id = "",
            userId = userId,
            description = description,
            category = category,
            amount = value,
            type = transactionType,
            date = dateStr,
            status = if (isReceived) "paid" else "pending",
            accountId = selectedAccount?.id ?: "",
            isRecurring = isRecurring,
            recurrencePeriod = recurrenceType,
            recurrenceCount = installments,
            iconRes = if (transactionType == "income") R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        )
        
        lifecycleScope.launch {
            val success = SupabaseService.saveTransaction(transaction, token)
            
            if (success) {
                val typeLabel = when (transactionType) {
                    "income" -> "Receita"
                    "expense" -> "Despesa"
                    else -> "Transferência"
                }
                ToastManager.showSuccess(this@AddTransactionActivity, "$typeLabel \"${description}\" salva")
                setResult(RESULT_OK)
                finish()
            } else {
                ToastManager.showError(this@AddTransactionActivity, "Erro ao salvar transação")
            }
        }
    }
}
