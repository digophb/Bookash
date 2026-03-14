package com.bookash.app

import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FilterTransactionsActivity : AppCompatActivity() {

    companion object {
        const val RESULT_FILTER_STATUS = "filter_status"
        const val RESULT_FILTER_CATEGORY = "filter_category"
        const val RESULT_FILTER_ACCOUNT = "filter_account"
        const val RESULT_FILTER_TAG = "filter_tag"
        const val RESULT_FILTER_START_DATE = "filter_start_date"
        const val RESULT_FILTER_END_DATE = "filter_end_date"
        
        const val EXTRA_FILTER_STATUS = "extra_filter_status"
        const val EXTRA_FILTER_CATEGORY = "extra_filter_category"
        const val EXTRA_FILTER_ACCOUNT = "extra_filter_account"
        const val EXTRA_FILTER_TAG = "extra_filter_tag"
        const val EXTRA_FILTER_START_DATE = "extra_filter_start_date"
        const val EXTRA_FILTER_END_DATE = "extra_filter_end_date"
        const val EXTRA_FILTER_PERIOD_ENABLED = "extra_filter_period_enabled"
    }

    // Views
    private lateinit var statusChipGroup: com.google.android.material.chip.ChipGroup
    private lateinit var chipStatusAll: Chip
    private lateinit var chipStatusCompleted: Chip
    private lateinit var chipStatusPending: Chip
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var accountDropdown: AutoCompleteTextView
    private lateinit var tagDropdown: AutoCompleteTextView
    private lateinit var periodSwitch: SwitchMaterial
    private lateinit var periodContainer: LinearLayout
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var clearButton: com.google.android.material.button.MaterialButton
    private lateinit var applyButton: com.google.android.material.button.MaterialButton
    
    // Data
    private var categories: List<Category> = emptyList()
    private var accounts: List<Account> = emptyList()
    private var tags: List<Tag> = emptyList()
    
    private var selectedCategoryId: String = ""
    private var selectedAccountId: String = ""
    private var selectedTagId: String = ""
    private var selectedStatus: String = "all"
    
    private var startDate: String = ""
    private var endDate: String = ""
    
    // User
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter_transactions)
        
        userId = UserSession.getUserId()
        initViews()
        setupListeners()
        loadData()
        
        // Carregar filtros atuais (se houver)
        loadCurrentFilters()
    }
    
    private fun initViews() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        statusChipGroup = findViewById(R.id.statusChipGroup)
        chipStatusAll = findViewById(R.id.chipStatusAll)
        chipStatusCompleted = findViewById(R.id.chipStatusCompleted)
        chipStatusPending = findViewById(R.id.chipStatusPending)
        categoryDropdown = findViewById(R.id.categoryDropdown)
        accountDropdown = findViewById(R.id.accountDropdown)
        tagDropdown = findViewById(R.id.tagDropdown)
        periodSwitch = findViewById(R.id.periodSwitch)
        periodContainer = findViewById(R.id.periodContainer)
        startDateInput = findViewById(R.id.startDateInput)
        endDateInput = findViewById(R.id.endDateInput)
        clearButton = findViewById(R.id.clearButton)
        applyButton = findViewById(R.id.applyButton)
    }
    
    private fun setupListeners() {
        // Chips de situação
        chipStatusAll.setOnClickListener { selectedStatus = "all" }
        chipStatusCompleted.setOnClickListener { selectedStatus = "completed" }
        chipStatusPending.setOnClickListener { selectedStatus = "pending" }
        
        // Switch de período com animação
        periodSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showPeriodContainer()
                // Definir datas padrão (primeiro e último dia do mês atual)
                setDefaultDates()
            } else {
                hidePeriodContainer()
            }
        }
        
        // Datepickers
        startDateInput.setOnClickListener { showStartDatePicker() }
        endDateInput.setOnClickListener { showEndDatePicker() }
        
        // Botões
        clearButton.setOnClickListener { clearFilters() }
        applyButton.setOnClickListener { applyFilters() }
    }
    
    private fun loadData() {
        lifecycleScope.launch {
            try {
                // Carregar categorias
                categories = SupabaseService.getCategories(userId ?: "")
                val categoryNames = mutableListOf("Todas")
                categoryNames.addAll(categories.map { it.name })
                val categoryAdapter = ArrayAdapter(this@FilterTransactionsActivity, 
                    android.R.layout.simple_dropdown_item_1line, categoryNames)
                categoryDropdown.setAdapter(categoryAdapter)
                categoryDropdown.setOnItemClickListener { _, _, position, _ ->
                    selectedCategoryId = if (position == 0) "" else categories[position - 1].id
                }
                
                // Carregar contas
                accounts = SupabaseService.getAccounts(userId ?: "")
                val accountNames = mutableListOf("Todas")
                accountNames.addAll(accounts.map { it.name })
                val accountAdapter = ArrayAdapter(this@FilterTransactionsActivity, 
                    android.R.layout.simple_dropdown_item_1line, accountNames)
                accountDropdown.setAdapter(accountAdapter)
                accountDropdown.setOnItemClickListener { _, _, position, _ ->
                    selectedAccountId = if (position == 0) "" else accounts[position - 1].id
                }
                
                // Carregar tags
                tags = SupabaseService.getTags(userId ?: "")
                val tagNames = mutableListOf("Todas")
                tagNames.addAll(tags.map { it.name })
                val tagAdapter = ArrayAdapter(this@FilterTransactionsActivity, 
                    android.R.layout.simple_dropdown_item_1line, tagNames)
                tagDropdown.setAdapter(tagAdapter)
                tagDropdown.setOnItemClickListener { _, _, position, _ ->
                    selectedTagId = if (position == 0) "" else tags[position - 1].id
                }
                
            } catch (e: Exception) {
                ToastManager.showError(this@FilterTransactionsActivity, "Erro ao carregar dados")
            }
        }
    }
    
    private fun loadCurrentFilters() {
        // Carregar filtros passados pela TransactionsActivity
        intent.getStringExtra(EXTRA_FILTER_STATUS)?.let {
            selectedStatus = it
            when (it) {
                "all" -> chipStatusAll.isChecked = true
                "completed" -> chipStatusCompleted.isChecked = true
                "pending" -> chipStatusPending.isChecked = true
            }
        }
        
        intent.getStringExtra(EXTRA_FILTER_CATEGORY)?.let { selectedCategoryId = it }
        intent.getStringExtra(EXTRA_FILTER_ACCOUNT)?.let { selectedAccountId = it }
        intent.getStringExtra(EXTRA_FILTER_TAG)?.let { selectedTagId = it }
        
        intent.getStringExtra(EXTRA_FILTER_START_DATE)?.let { 
            startDate = it
            startDateInput.setText(formatDateForDisplay(it))
        }
        intent.getStringExtra(EXTRA_FILTER_END_DATE)?.let { 
            endDate = it
            endDateInput.setText(formatDateForDisplay(it))
        }
        
        val periodEnabled = intent.getBooleanExtra(EXTRA_FILTER_PERIOD_ENABLED, false)
        periodSwitch.isChecked = periodEnabled
        if (periodEnabled) {
            periodContainer.visibility = View.VISIBLE
        }
    }
    
    private fun showPeriodContainer() {
        periodContainer.visibility = View.VISIBLE
    }
    
    private fun hidePeriodContainer() {
        periodContainer.visibility = View.GONE
    }
    
    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()
        
        // Primeiro dia do mês
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        startDateInput.setText(formatDateForDisplay(startDate))
        
        // Último dia do mês
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        endDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        endDateInput.setText(formatDateForDisplay(endDate))
    }
    
    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        if (startDate.isNotEmpty()) {
            try {
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startDate) ?: Date()
            } catch (e: Exception) {}
        }
        
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time)
                startDateInput.setText(formatDateForDisplay(startDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        if (endDate.isNotEmpty()) {
            try {
                calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(endDate) ?: Date()
            } catch (e: Exception) {}
        }
        
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                endDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time)
                endDateInput.setText(formatDateForDisplay(endDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun formatDateForDisplay(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }
    
    private fun clearFilters() {
        selectedStatus = "all"
        selectedCategoryId = ""
        selectedAccountId = ""
        selectedTagId = ""
        startDate = ""
        endDate = ""
        
        chipStatusAll.isChecked = true
        categoryDropdown.setText("Todas", false)
        accountDropdown.setText("Todas", false)
        tagDropdown.setText("Todas", false)
        periodSwitch.isChecked = false
        startDateInput.text?.clear()
        endDateInput.text?.clear()
    }
    
    private fun applyFilters() {
        val resultIntent = Intent()
        resultIntent.putExtra(RESULT_FILTER_STATUS, selectedStatus)
        resultIntent.putExtra(RESULT_FILTER_CATEGORY, selectedCategoryId)
        resultIntent.putExtra(RESULT_FILTER_ACCOUNT, selectedAccountId)
        resultIntent.putExtra(RESULT_FILTER_TAG, selectedTagId)
        resultIntent.putExtra(RESULT_FILTER_START_DATE, startDate)
        resultIntent.putExtra(RESULT_FILTER_END_DATE, endDate)
        resultIntent.putExtra("filter_period_enabled", periodSwitch.isChecked)
        
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
