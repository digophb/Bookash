package com.bookash.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransactionDetail"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_TRANSACTION = "transaction"
    }

    // Views
    private lateinit var typeToggle: MaterialButtonToggleGroup
    private lateinit var btnIncome: MaterialButton
    private lateinit var btnExpense: MaterialButton
    private lateinit var btnTransfer: MaterialButton
    private lateinit var titleText: TextView
    private lateinit var valueInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categoryField: LinearLayout
    private lateinit var categoryIcon: ImageView
    private lateinit var categoryText: TextView
    private lateinit var accountField: LinearLayout
    private lateinit var accountIcon: ImageView
    private lateinit var accountText: TextView
    private lateinit var dateInput: TextInputEditText
    private lateinit var deleteButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    
    // Modos
    private lateinit var normalModeLayout: LinearLayout
    private lateinit var transferModeLayout: LinearLayout
    
    // Transferência
    private lateinit var fromAccountField: LinearLayout
    private lateinit var fromAccountIcon: ImageView
    private lateinit var fromAccountText: TextView
    private lateinit var toAccountField: LinearLayout
    private lateinit var toAccountIcon: ImageView
    private lateinit var toAccountText: TextView

    // Dados
    private var transaction: Transaction? = null
    private var transactionId: String? = null
    private var categories: List<Category> = emptyList()
    private var accounts: List<Account> = emptyList()

    // Dados selecionados
    private var selectedCategory: Category? = null
    private var selectedAccount: Account? = null
    private var fromAccount: Account? = null
    private var toAccount: Account? = null
    private var selectedDate: Date = Date()

    // Tipo de transação
    private var transactionType: String = "income"
    
    // User ID
    private var userId: String? = null
    
    // Edit mode
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)
        
        userId = UserSession.getUserId()

        initViews()
        setupListeners()
        loadData()
        
        // Verificar se é edição ou visualização
        transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        transaction = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TRANSACTION, Transaction::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_TRANSACTION)
        }
        
        if (transaction != null) {
            loadTransactionData()
        } else if (transactionId != null) {
            loadTransactionById()
        } else {
            // Nova transação
            isEditMode = true
            updateEditMode()
        }
    }

    private fun initViews() {
        typeToggle = findViewById(R.id.typeToggle)
        btnIncome = findViewById(R.id.btnIncome)
        btnExpense = findViewById(R.id.btnExpense)
        btnTransfer = findViewById(R.id.btnTransfer)
        titleText = findViewById(R.id.titleText)
        valueInput = findViewById(R.id.valueInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        categoryField = findViewById(R.id.categoryField)
        categoryIcon = findViewById(R.id.categoryIcon)
        categoryText = findViewById(R.id.categoryText)
        accountField = findViewById(R.id.accountField)
        accountIcon = findViewById(R.id.accountIcon)
        accountText = findViewById(R.id.accountText)
        dateInput = findViewById(R.id.dateInput)
        deleteButton = findViewById(R.id.deleteButton)
        saveButton = findViewById(R.id.saveButton)
        
        normalModeLayout = findViewById(R.id.normalModeLayout)
        transferModeLayout = findViewById(R.id.transferModeLayout)
        
        fromAccountField = findViewById(R.id.fromAccountField)
        fromAccountIcon = findViewById(R.id.fromAccountIcon)
        fromAccountText = findViewById(R.id.fromAccountText)
        toAccountField = findViewById(R.id.toAccountField)
        toAccountIcon = findViewById(R.id.toAccountIcon)
        toAccountText = findViewById(R.id.toAccountText)

        valueInput.addTextChangedListener(CurrencyTextWatcher(valueInput))
        updateDateDisplay()
    }

    private fun setupListeners() {
        typeToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> {
                        transactionType = "income"
                        titleText.text = "Editar Receita"
                    }
                    R.id.btnExpense -> {
                        transactionType = "expense"
                        titleText.text = "Editar Despesa"
                    }
                    R.id.btnTransfer -> {
                        transactionType = "transfer"
                        titleText.text = "Editar Transferência"
                    }
                }
                updateModeVisibility()
            }
        }

        dateInput.setOnClickListener { showDatePicker() }
        dateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        categoryField.setOnClickListener { showCategoryPicker() }
        accountField.setOnClickListener { showAccountPicker() }
        fromAccountField.setOnClickListener { showFromAccountPicker() }
        toAccountField.setOnClickListener { showToAccountPicker() }

        deleteButton.setOnClickListener { confirmDelete() }
        saveButton.setOnClickListener { saveTransaction() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            categories = userId?.let { SupabaseService.getCategories(it) } ?: emptyList()
            accounts = userId?.let { SupabaseService.getAccounts(it) } ?: emptyList()
            Log.d(TAG, "Dados carregados: ${categories.size} categorias, ${accounts.size} contas")
        }
    }

    private fun loadTransactionById() {
        lifecycleScope.launch {
            transaction = transactionId?.let { SupabaseService.getTransactionById(it) }
            if (transaction != null) {
                loadTransactionData()
            } else {
                ToastManager.showError(this@TransactionDetailActivity, "Transacao nao encontrada")
                finish()
            }
        }
    }

    private fun loadTransactionData() {
        transaction?.let { t ->
            // Tipo
            transactionType = t.type
            when (t.type) {
                "income" -> {
                    typeToggle.check(R.id.btnIncome)
                    titleText.text = "Detalhes da Receita"
                }
                "expense" -> {
                    typeToggle.check(R.id.btnExpense)
                    titleText.text = "Detalhes da Despesa"
                }
                "transfer" -> {
                    typeToggle.check(R.id.btnTransfer)
                    titleText.text = "Detalhes da Transferência"
                }
            }
            updateModeVisibility()
            
            // Valor
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            valueInput.setText(formatter.format(t.amount))
            
            // Descricao
            descriptionInput.setText(t.description)
            
            // Data
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                selectedDate = sdf.parse(t.date) ?: Date()
                updateDateDisplay()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao parsear data", e)
            }
            
            // Status
            // TODO: carregar status se houver switch
            
            // Carregar categoria pelo ID
            lifecycleScope.launch {
                // Buscar categoria pelo ID
                selectedCategory = categories.find { it.id == t.categoryId }
                selectedCategory?.let { cat ->
                    categoryText.text = cat.name
                    categoryText.setTextColor(getColor(R.color.text_primary))
                    categoryIcon.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun updateModeVisibility() {
        if (transactionType == "transfer") {
            normalModeLayout.visibility = View.GONE
            transferModeLayout.visibility = View.VISIBLE
        } else {
            normalModeLayout.visibility = View.VISIBLE
            transferModeLayout.visibility = View.GONE
        }
    }

    private fun updateEditMode() {
        val enabled = isEditMode
        valueInput.isEnabled = enabled
        descriptionInput.isEnabled = enabled
        dateInput.isEnabled = enabled
        categoryField.isEnabled = enabled
        accountField.isEnabled = enabled
        fromAccountField.isEnabled = enabled
        toAccountField.isEnabled = enabled
        typeToggle.isEnabled = enabled
        
        deleteButton.visibility = if (transaction != null) View.VISIBLE else View.GONE
        saveButton.text = if (transaction != null) "Salvar" else "Criar"
    }

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

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        dateInput.setText(sdf.format(selectedDate))
    }

    private fun showCategoryPicker() {
        if (categories.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma categoria cadastrada")
            return
        }
        
        val filteredCategories = categories.filter { 
            it.type == transactionType || it.type == "all" 
        }
        
        if (filteredCategories.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma categoria para este tipo")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredCategories.map { it.name }))
        popup.anchorView = categoryField
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = filteredCategories[position]
            categoryText.text = selectedCategory?.name
            categoryText.setTextColor(getColor(R.color.text_primary))
            categoryIcon.visibility = View.VISIBLE
            popup.dismiss()
        }
        popup.show()
    }

    private fun showAccountPicker() {
        if (accounts.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma conta cadastrada")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, accounts.map { it.name }))
        popup.anchorView = accountField
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedAccount = accounts[position]
            accountText.text = selectedAccount?.name
            accountText.setTextColor(getColor(R.color.text_primary))
            accountIcon.visibility = View.VISIBLE
            popup.dismiss()
        }
        popup.show()
    }

    private fun showFromAccountPicker() {
        if (accounts.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma conta cadastrada")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, accounts.map { it.name }))
        popup.anchorView = fromAccountField
        popup.setOnItemClickListener { _, _, position, _ ->
            fromAccount = accounts[position]
            fromAccountText.text = fromAccount?.name
            fromAccountText.setTextColor(getColor(R.color.text_primary))
            fromAccountIcon.visibility = View.VISIBLE
            popup.dismiss()
        }
        popup.show()
    }

    private fun showToAccountPicker() {
        val availableAccounts = accounts.filter { it.id != fromAccount?.id }
        if (availableAccounts.isEmpty()) {
            ToastManager.showWarning(this, "Selecione uma conta de origem diferente")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, availableAccounts.map { it.name }))
        popup.anchorView = toAccountField
        popup.setOnItemClickListener { _, _, position, _ ->
            toAccount = availableAccounts[position]
            toAccountText.text = toAccount?.name
            toAccountText.setTextColor(getColor(R.color.text_primary))
            toAccountIcon.visibility = View.VISIBLE
            popup.dismiss()
        }
        popup.show()
    }

    private fun saveTransaction() {
        val value = CurrencyTextWatcher.parseValue(valueInput.text.toString())
        
        if (value <= 0) {
            ToastManager.showWarning(this, "Digite um valor maior que zero")
            return
        }

        if (transactionType != "transfer") {
            val description = descriptionInput.text.toString().trim()
            if (description.isEmpty()) {
                ToastManager.showWarning(this, "Digite uma descricao")
                return
            }
            if (selectedAccount == null) {
                ToastManager.showWarning(this, "Selecione uma conta")
                return
            }
        } else {
            if (fromAccount == null) {
                ToastManager.showWarning(this, "Selecione a conta de origem")
                return
            }
            if (toAccount == null) {
                ToastManager.showWarning(this, "Selecione a conta de destino")
                return
            }
            if (fromAccount?.id == toAccount?.id) {
                ToastManager.showWarning(this, "Contas devem ser diferentes")
                return
            }
        }

        saveButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateStr = isoDateFormat.format(selectedDate)
                
                val newTransaction = Transaction(
                    id = transaction?.id ?: "",
                    userId = userId ?: "",
                    description = descriptionInput.text.toString().trim(),
                    categoryId = selectedCategory?.id ?: "",
                    categoryName = selectedCategory?.name ?: "",
                    amount = value,
                    type = transactionType,
                    date = dateStr
                )

                val token = UserSession.getAccessToken() ?: ""
                
                val success = if (transaction != null) {
                    // Update
                    withContext(Dispatchers.IO) {
                        SupabaseService.updateTransaction(newTransaction, token)
                    }
                } else {
                    // Create - retorna ID da transação
                    val transactionId = withContext(Dispatchers.IO) {
                        SupabaseService.saveTransaction(newTransaction, token)
                    }
                    transactionId != null
                }

                if (success) {
                    val msg = if (transaction != null) "Transacao atualizada!" else "Transacao criada!"
                    ToastManager.showSuccess(this@TransactionDetailActivity, msg)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    ToastManager.showError(this@TransactionDetailActivity, "Erro ao salvar transacao")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar", e)
                ToastManager.showError(this@TransactionDetailActivity, "Erro: ${e.message}")
            } finally {
                saveButton.isEnabled = true
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Transacao")
            .setMessage("Tem certeza que deseja excluir esta transacao?")
            .setPositiveButton("Excluir") { _, _ -> deleteTransaction() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteTransaction() {
        val id = transaction?.id ?: return
        
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                SupabaseService.deleteTransaction(id)
            }
            
            if (success) {
                ToastManager.showSuccess(this@TransactionDetailActivity, "Transacao excluida")
                setResult(RESULT_OK)
                finish()
            } else {
                ToastManager.showError(this@TransactionDetailActivity, "Erro ao excluir transacao")
            }
        }
    }
}
