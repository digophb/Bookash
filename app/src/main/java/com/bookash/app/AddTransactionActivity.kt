package com.bookash.app

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    private lateinit var reminderSwitch: MaterialSwitch
    private lateinit var reminderDateLayout: TextInputLayout
    private lateinit var reminderDateInput: TextInputEditText
    private lateinit var tagsDropdown: AutoCompleteTextView
    private lateinit var notesInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    
    private var selectedDate: Long = System.currentTimeMillis()
    private var reminderDate: Long? = null
    private var transactionType: String = "income"
    private var isReceived: Boolean = true
    private var isRecurring: Boolean = false
    private var hasReminder: Boolean = false
    private var recurrenceType: String = "Mensal"
    private var installments: Int = 1
    
    private val categories = mutableListOf<Category>()
    private val accounts = mutableListOf<Account>()
    private val tags = mutableListOf<Tag>()
    private var userId: String? = null
    private var selectedCategory: Category? = null
    private var selectedAccount: Account? = null
    private val selectedTags = mutableListOf<Tag>()

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
        reminderSwitch = findViewById(R.id.reminderSwitch)
        reminderDateLayout = findViewById(R.id.reminderDateLayout)
        reminderDateInput = findViewById(R.id.reminderDateInput)
        tagsDropdown = findViewById(R.id.tagsDropdown)
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
            
            // Carregar tags
            val loadedTags = SupabaseService.getTags(userId!!)
            tags.clear()
            tags.addAll(loadedTags)
            updateTagDropdown()
        }
    }
    
    private fun updateCategoryDropdown() {
        // Adapter personalizado com ícones e cores
        val adapter = CategoryDropdownAdapter(this, categories)
        categoryDropdown.setAdapter(adapter)
        categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories.getOrNull(position)
        }
    }
    
    private fun updateAccountDropdown() {
        // Adapter personalizado com ícones
        val adapter = AccountDropdownAdapter(this, accounts)
        accountDropdown.setAdapter(adapter)
        accountDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedAccount = accounts.getOrNull(position)
        }
        if (accounts.isNotEmpty()) {
            accountDropdown.setText(accounts[0].name, false)
            selectedAccount = accounts[0]
        }
    }
    
    private fun updateTagDropdown() {
        // Adapter personalizado com cores
        val adapter = TagDropdownAdapter(this, tags)
        tagsDropdown.setAdapter(adapter)
        tagsDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedTag = tags.getOrNull(position) ?: return@setOnItemClickListener
            
            // Adicionar tag à lista de selecionadas
            if (!selectedTags.contains(selectedTag)) {
                selectedTags.add(selectedTag)
                updateTagsDisplay()
            }
            
            // Limpar dropdown para permitir selecionar outra tag
            tagsDropdown.text = null
        }
    }
    
    private fun updateTagsDisplay() {
        // Mostrar tags selecionadas como texto separado por vírgula
        val tagsText = selectedTags.joinToString(", ") { it.name }
        tagsDropdown.setText(tagsText)
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
        
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            hasReminder = isChecked
            reminderDateLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        reminderDateInput.setOnClickListener {
            showReminderDatePicker()
        }
    }

    private fun showReminderDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Data do Lembrete")
            .setSelection(reminderDate ?: System.currentTimeMillis())
            .build()
        
        datePicker.addOnPositiveButtonClickListener { selection ->
            reminderDate = selection
            updateReminderDateDisplay()
        }
        
        datePicker.show(supportFragmentManager, "reminderDatePicker")
    }
    
    private fun updateReminderDateDisplay() {
        reminderDate?.let {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            reminderDateInput.setText(formatter.format(Date(it)))
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
        val color = ContextCompat.getColor(this, colorRes)
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
        val category = selectedCategory?.name ?: categoryDropdown.text.toString()
        val account = selectedAccount?.id ?: ""
        val notes = notesInput.text.toString()
        val tags = selectedTags.map { it.name }
        
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = formatter.format(Date(selectedDate))
        
        val reminderDateStr = reminderDate?.let {
            formatter.format(Date(it))
        }
        
        val userId = UserSession.getUserId() ?: ""
        
        val transaction = Transaction(
            id = "",
            userId = userId,
            description = description,
            category = category,
            amount = value,
            type = transactionType,
            date = dateStr,
            status = if (isReceived) "paid" else "pending",
            accountId = account,
            tags = tags,
            reminderDate = reminderDateStr,
            isRecurring = isRecurring,
            recurrencePeriod = recurrenceType,
            recurrenceCount = installments,
            iconRes = if (transactionType == "income") R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        )
        
        lifecycleScope.launch {
            val success = SupabaseService.saveTransaction(transaction, userId)
            
            if (success) {
                // Se tem lembrete, salvar na tabela reminders
                if (hasReminder && reminderDateStr != null) {
                    val reminder = Reminder(
                        userId = userId,
                        transactionId = transaction.id,
                        title = description,
                        amount = value,
                        reminderDate = reminderDateStr,
                        isRecurring = isRecurring,
                        recurrenceType = recurrenceType
                    )
                    SupabaseService.saveReminder(reminder, userId)
                }
                
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
