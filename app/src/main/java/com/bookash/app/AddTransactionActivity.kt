package com.bookash.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddTransaction"
    }

    // Views
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
    private lateinit var tagsDropdown: AutoCompleteTextView
    private lateinit var tagsSelectedLayout: LinearLayout
    private lateinit var receivedSwitch: MaterialSwitch
    private lateinit var repeatSwitch: MaterialSwitch
    private lateinit var repeatFrequencyLayout: LinearLayout
    private lateinit var frequencyCountInput: TextInputEditText
    private lateinit var frequencyDropdown: AutoCompleteTextView
    private lateinit var reminderSwitch: MaterialSwitch
    private lateinit var reminderDateLayout: TextInputLayout
    private lateinit var reminderDateInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var saveButton: MaterialButton

    // Adapters
    private var categoryAdapter: CategoryDropdownAdapter? = null
    private var accountAdapter: AccountDropdownAdapter? = null
    private var tagAdapter: TagDropdownAdapter? = null

    // Dados selecionados
    private var selectedCategory: Category? = null
    private var selectedAccount: Account? = null
    private var selectedTags: MutableList<Tag> = mutableListOf()
    private var selectedDate: Date = Date()
    private var selectedReminderDate: Date? = null

    // Tipo de transação
    private var transactionType: String = "income"
    
    // User ID
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        
        // Obter userId do UserSession
        userId = UserSession.getUserId()

        initViews()
        setupListeners()
        loadData()
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
        tagsDropdown = findViewById(R.id.tagsDropdown)
        tagsSelectedLayout = findViewById(R.id.tagsSelectedLayout)
        receivedSwitch = findViewById(R.id.receivedSwitch)
        repeatSwitch = findViewById(R.id.repeatSwitch)
        repeatFrequencyLayout = findViewById(R.id.repeatFrequencyLayout)
        frequencyCountInput = findViewById(R.id.frequencyCountInput)
        frequencyDropdown = findViewById(R.id.frequencyDropdown)
        reminderSwitch = findViewById(R.id.reminderSwitch)
        reminderDateLayout = findViewById(R.id.reminderDateLayout)
        reminderDateInput = findViewById(R.id.reminderDateInput)
        notesInput = findViewById(R.id.notesInput)
        saveButton = findViewById(R.id.saveButton)

        // Configurar formatação monetária
        valueInput.addTextChangedListener(CurrencyTextWatcher(valueInput))

        // Configurar dropdown de frequência
        val frequencies = arrayOf("Diário", "Semanal", "Mensal", "Anual")
        val freqAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencies)
        frequencyDropdown.setAdapter(freqAdapter)
        frequencyDropdown.setText("Mensal", false)

        updateDateDisplay()
    }

    private fun setupListeners() {
        // Toggle de tipo
        typeToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> {
                        transactionType = "income"
                        titleText.text = "Nova Receita"
                        receivedLabel?.text = "Recebido"
                    }
                    R.id.btnExpense -> {
                        transactionType = "expense"
                        titleText.text = "Nova Despesa"
                        receivedLabel?.text = "Pago"
                    }
                    R.id.btnTransfer -> {
                        transactionType = "transfer"
                        titleText.text = "Nova Transferência"
                    }
                }
            }
        }

        // Data
        dateInput.setOnClickListener { showDatePicker() }
        dateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        // Lembrete
        reminderDateInput.setOnClickListener { showReminderDatePicker() }
        reminderDateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showReminderDatePicker() }

        // Switch Repetir - mostra/esconde frequência
        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            repeatFrequencyLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Switch Lembrete - mostra/esconde data do lembrete
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderDateLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Categoria
        categoryDropdown.setOnItemClickListener { parent, view, position, id ->
            val selectedName = categoryAdapter?.getItem(position) ?: ""
            selectedCategory = categoryAdapter?.getCategoryByName(selectedName)
            Log.d(TAG, "Categoria selecionada: $selectedName")
        }

        // Conta
        accountDropdown.setOnItemClickListener { parent, view, position, id ->
            val selectedName = accountAdapter?.getItem(position) ?: ""
            selectedAccount = accountAdapter?.getAccountByName(selectedName)
            Log.d(TAG, "Conta selecionada: $selectedName")
        }

        // Tags
        tagsDropdown.setOnItemClickListener { parent, view, position, id ->
            val selectedName = tagAdapter?.getItem(position) ?: ""
            val tag = tagAdapter?.getTagByName(selectedName)
            if (tag != null && selectedTags.none { it.id == tag.id }) {
                selectedTags.add(tag)
                updateSelectedTags()
                ToastManager.showSuccess(this, "Tag '${tag.name}' adicionada")
            }
            // Limpar seleção
            tagsDropdown.setText("", false)
        }

        // Salvar
        saveButton.setOnClickListener { saveTransaction() }
    }

    private fun loadData() {
        loadCategories()
        loadAccounts()
        loadTags()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = userId?.let { SupabaseService.getCategories(it) } ?: emptyList()
            Log.d(TAG, "Categorias carregadas: ${categories.size}")
            categoryAdapter = CategoryDropdownAdapter(this@AddTransactionActivity, categories)
            categoryDropdown.setAdapter(categoryAdapter)
        }
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            val accounts = userId?.let { SupabaseService.getAccounts(it) } ?: emptyList()
            Log.d(TAG, "Contas carregadas: ${accounts.size}")
            accountAdapter = AccountDropdownAdapter(this@AddTransactionActivity, accounts)
            accountDropdown.setAdapter(accountAdapter)
        }
    }

    private fun loadTags() {
        lifecycleScope.launch {
            val tags = userId?.let { SupabaseService.getTags(it) } ?: emptyList()
            Log.d(TAG, "Tags carregadas: ${tags.size}")
            tagAdapter = TagDropdownAdapter(this@AddTransactionActivity, tags)
            tagsDropdown.setAdapter(tagAdapter)
        }
    }

    private fun updateSelectedTags() {
        tagsSelectedLayout.removeAllViews()
        
        for (tag in selectedTags) {
            val tagView = layoutInflater.inflate(R.layout.item_selected_tag, tagsSelectedLayout, false)
            val tagName = tagView.findViewById<TextView>(R.id.tagName)
            val removeBtn = tagView.findViewById<View>(R.id.removeTag)
            
            tagName.text = tag.name
            removeBtn.setOnClickListener {
                selectedTags.remove(tag)
                updateSelectedTags()
            }
            
            tagsSelectedLayout.addView(tagView)
        }
        
        tagsSelectedLayout.visibility = if (selectedTags.isEmpty()) View.GONE else View.VISIBLE
    }

    private val receivedLabel: TextView?
        get() = findViewById(R.id.receivedLabel)

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.time
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showReminderDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedReminderDate ?: Date()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedReminderDate = calendar.time
                updateReminderDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        dateInput.setText(sdf.format(selectedDate))
    }

    private fun updateReminderDateDisplay() {
        selectedReminderDate?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            reminderDateInput.setText(sdf.format(it))
        }
    }

    private fun saveTransaction() {
        val value = CurrencyTextWatcher.parseValue(valueInput.text.toString())
        
        if (value <= 0) {
            ToastManager.showWarning(this, "Digite um valor maior que zero")
            return
        }

        val description = descriptionInput.text.toString().trim()
        if (description.isEmpty()) {
            ToastManager.showWarning(this, "Digite uma descrição")
            return
        }

        if (selectedAccount == null) {
            ToastManager.showWarning(this, "Selecione uma conta")
            return
        }

        saveButton.isEnabled = false
        saveButton.text = "Salvando..."

        lifecycleScope.launch {
            try {
                val tags = selectedTags.map { it.id }
                val frequency = if (repeatSwitch.isChecked) frequencyDropdown.text.toString() else null
                val frequencyCount = if (repeatSwitch.isChecked) {
                    frequencyCountInput.text.toString().toIntOrNull() ?: 1
                } else 1
                val isReceived = receivedSwitch.isChecked

                // Formatar data para ISO 8601
                val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateStr = isoDateFormat.format(selectedDate)
                
                // Formatar data do lembrete se existir
                val reminderDateStr = if (reminderSwitch.isChecked && selectedReminderDate != null) {
                    isoDateFormat.format(selectedReminderDate)
                } else null

                val transaction = Transaction(
                    userId = userId ?: "",
                    description = description,
                    category = selectedCategory?.name ?: "",
                    amount = value,
                    type = transactionType,
                    date = dateStr,
                    status = if (isReceived) "paid" else "pending",
                    accountId = selectedAccount!!.id,
                    tags = tags,
                    reminderDate = reminderDateStr,
                    isRecurring = frequency != null,
                    recurrencePeriod = frequency ?: "",
                    recurrenceCount = frequencyCount
                )

                val token = UserSession.getAccessToken() ?: ""
                
                val success = withContext(Dispatchers.IO) {
                    SupabaseService.saveTransaction(transaction, token)
                }

                if (success) {
                    ToastManager.showSuccess(this@AddTransactionActivity, "Transação salva com sucesso!")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    ToastManager.showError(this@AddTransactionActivity, "Erro ao salvar transação")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar transação", e)
                ToastManager.showError(this@AddTransactionActivity, "Erro: ${e.message}")
            } finally {
                saveButton.isEnabled = true
                saveButton.text = "Salvar Transação"
            }
        }
    }
}
