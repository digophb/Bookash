package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddAccountActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var searchBankInput: TextInputEditText
    private lateinit var banksRecycler: RecyclerView
    private lateinit var nameInput: TextInputEditText
    private lateinit var typeChipGroup: ChipGroup
    private lateinit var balanceInput: TextInputEditText
    private lateinit var includeInBalanceSwitch: SwitchMaterial
    private lateinit var btnSave: MaterialButton
    private lateinit var bankSectionTitle: TextView
    
    // Saldo calculado
    private lateinit var calculatedBalanceSection: View
    private lateinit var calculatedBalanceText: TextView
    private lateinit var calculatedBalanceProgress: android.widget.ProgressBar

    private lateinit var bankAdapter: BankIconAdapter

    private var selectedBank = "wallet"
    private var selectedType = "corrente"
    private var editingAccountId: String? = null
    private var userId: String? = null
    private var isNameManuallyEdited = false
    
    // Saldo original para detectar mudanças na edição
    private var originalBalance: Double = 0.0
    
    // Para formatação monetária
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var isUpdatingBalance = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)
        
        userId = UserSession.getUserId()

        initViews()
        setupAdapters()
        setupListeners()
        setupMonetaryInput()
        loadEditingData()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        searchBankInput = findViewById(R.id.searchBankInput)
        banksRecycler = findViewById(R.id.banksRecycler)
        nameInput = findViewById(R.id.nameInput)
        typeChipGroup = findViewById(R.id.typeChipGroup)
        balanceInput = findViewById(R.id.balanceInput)
        includeInBalanceSwitch = findViewById(R.id.includeInBalanceSwitch)
        btnSave = findViewById(R.id.btnSave)
        
        // Saldo calculado
        calculatedBalanceSection = findViewById(R.id.calculatedBalanceSection)
        calculatedBalanceText = findViewById(R.id.calculatedBalanceText)
        calculatedBalanceProgress = findViewById(R.id.calculatedBalanceProgress)
    }

    private fun setupAdapters() {
        bankAdapter = BankIconAdapter { bankId ->
            selectedBank = bankId
            // Preencher nome automaticamente se não foi editado manualmente
            if (!isNameManuallyEdited && nameInput.text.isNullOrBlank()) {
                val bankName = bankAdapter.getBankName(bankId)
                nameInput.setText(bankName)
            }
        }
        banksRecycler.apply {
            layoutManager = GridLayoutManager(this@AddAccountActivity, 4)
            adapter = bankAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        // Marcar quando o nome é editado manualmente
        nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isNameManuallyEdited = !s.isNullOrBlank()
            }
        })

        // Search filter
        searchBankInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                bankAdapter.filter(s?.toString() ?: "")
            }
        })

        // Type selection - mostrar/esconder ícones de banco
        typeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val previousType = selectedType
                selectedType = when (checkedIds[0]) {
                    R.id.chipCorrente -> "corrente"
                    R.id.chipPoupanca -> "poupanca"
                    R.id.chipCarteira -> "carteira"
                    R.id.chipDigital -> "digital"
                    else -> "outros"
                }
                
                // Atualizar visibilidade dos bancos baseado no tipo
                updateBankVisibility()
                
                // Se mudou para carteira, selecionar automaticamente
                if (selectedType == "carteira" && previousType != "carteira") {
                    selectedBank = "wallet"
                    bankAdapter.setSelectedBank("wallet")
                    if (!isNameManuallyEdited) {
                        nameInput.setText("Carteira")
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            saveAccount()
        }
    }
    
    private fun updateBankVisibility() {
        // Para tipo "carteira", mostrar apenas o ícone wallet
        // Para outros tipos, mostrar todos os bancos
        if (selectedType == "carteira") {
            searchBankInput.visibility = View.GONE
            bankAdapter.showWalletOnly(true)
        } else {
            searchBankInput.visibility = View.VISIBLE
            bankAdapter.showWalletOnly(false)
        }
    }
    
    private fun setupMonetaryInput() {
        balanceInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingBalance) return
                
                val text = s?.toString() ?: ""
                val cleanText = text.filter { it.isDigit() }
                
                if (cleanText.isEmpty()) {
                    isUpdatingBalance = true
                    balanceInput.setText("")
                    isUpdatingBalance = false
                    return
                }
                
                // Converter centavos para valor
                val value = cleanText.toDouble() / 100.0
                
                isUpdatingBalance = true
                balanceInput.setText(String.format("%.2f", value))
                balanceInput.setSelection(balanceInput.text?.length ?: 0)
                isUpdatingBalance = false
            }
        })
    }

    private fun loadEditingData() {
        editingAccountId = intent.getStringExtra("account_id")

        if (editingAccountId != null) {
            // Edit mode
            titleText.text = "Editar Conta"
            btnSave.text = "Atualizar Conta"

            val name = intent.getStringExtra("account_name") ?: ""
            val type = intent.getStringExtra("account_type") ?: "corrente"
            val balance = intent.getDoubleExtra("account_balance", 0.0)
            val icon = intent.getStringExtra("account_icon") ?: "wallet"
            val includeInBalance = intent.getBooleanExtra("account_include_in_balance", true)

            nameInput.setText(name)
            isNameManuallyEdited = true
            balanceInput.setText(String.format("%.2f", balance))
            originalBalance = balance // Salvar saldo original para detectar mudanças
            selectedBank = icon
            selectedType = type
            includeInBalanceSwitch.isChecked = includeInBalance

            // Set chip selection
            when (type) {
                "corrente" -> typeChipGroup.check(R.id.chipCorrente)
                "poupanca" -> typeChipGroup.check(R.id.chipPoupanca)
                "carteira" -> typeChipGroup.check(R.id.chipCarteira)
                "digital" -> typeChipGroup.check(R.id.chipDigital)
                else -> typeChipGroup.check(R.id.chipOutros)
            }

            // Set selected bank
            bankAdapter.setSelectedBank(icon)
        } else {
            // Create mode - default selection
            typeChipGroup.check(R.id.chipCorrente)
        }
        
        // Atualizar visibilidade inicial
        updateBankVisibility()
        
        // Carregar saldo calculado (apenas em modo edicao)
        if (editingAccountId != null) {
            loadCalculatedBalance()
        }
    }
    
    private fun loadCalculatedBalance() {
        val accountId = editingAccountId ?: return
        
        calculatedBalanceSection.visibility = View.VISIBLE
        calculatedBalanceProgress.visibility = View.VISIBLE
        calculatedBalanceText.text = "Calculando..."
        
        lifecycleScope.launch {
            try {
                val calculatedBalance = SupabaseService.getAccountCalculatedBalance(accountId)
                calculatedBalanceProgress.visibility = View.GONE
                
                val formattedBalance = currencyFormat.format(calculatedBalance)
                calculatedBalanceText.text = formattedBalance
                
                // Cor baseada no valor
                val color = if (calculatedBalance >= 0) {
                    getColor(R.color.success)
                } else {
                    getColor(R.color.error)
                }
                calculatedBalanceText.setTextColor(color)
            } catch (e: Exception) {
                calculatedBalanceProgress.visibility = View.GONE
                calculatedBalanceText.text = "Erro ao calcular"
            }
        }
    }

    private fun saveAccount() {
        val name = nameInput.text.toString().trim()

        if (name.isEmpty()) {
            ToastManager.showWarning(this, "Digite um nome para a conta")
            return
        }

        val balance = balanceInput.text.toString()
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0

        btnSave.isEnabled = false

        lifecycleScope.launch {
            val account = Account(
                id = editingAccountId ?: "",
                name = name,
                balance = balance,
                type = selectedType,
                icon = selectedBank,
                includeInBalance = includeInBalanceSwitch.isChecked
            )

            val success = if (editingAccountId != null) {
                SupabaseService.updateAccount(account, userId)
            } else {
                SupabaseService.saveAccount(account, userId!!)
            }

            if (success) {
                // Criar transação de reajuste se necessário
                createAdjustmentTransactionIfNeeded(account, balance)
                
                val resultIntent = Intent().apply {
                    putExtra("account_action", if (editingAccountId != null) "edit" else "create")
                    putExtra("account_name", account.name)
                    putExtra("needs_refresh", true)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                ToastManager.showError(this@AddAccountActivity, "Erro ao salvar conta")
            }
            
            btnSave.isEnabled = true
        }
    }
    
    /**
     * Cria transação de reajuste quando:
     * 1. Nova conta com saldo inicial != 0
     * 2. Edição que alterou o saldo
     */
    private suspend fun createAdjustmentTransactionIfNeeded(account: Account, newBalance: Double) {
        val token = UserSession.getAccessToken() ?: return
        val currentUserId = userId ?: return
        
        val adjustmentAmount: Double
        val transactionType: String
        
        if (editingAccountId != null) {
            // Edição: calcular diferença
            val difference = newBalance - originalBalance
            
            if (difference == 0.0) {
                // Sem mudança de saldo, não criar transação
                return
            }
            
            adjustmentAmount = kotlin.math.abs(difference)
            transactionType = if (difference > 0) "income" else "expense"
        } else {
            // Nova conta
            if (newBalance == 0.0) {
                // Saldo zero, não criar transação
                return
            }
            
            adjustmentAmount = kotlin.math.abs(newBalance)
            transactionType = if (newBalance > 0) "income" else "expense"
        }
        
        // Criar transação de reajuste
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val description = "Reajuste - ${account.name}"
        
        val transaction = Transaction(
            userId = currentUserId,
            description = description,
            amount = adjustmentAmount,
            type = transactionType,
            date = today,
            categoryId = "",
            categoryName = ""
        )
        
        SupabaseService.saveTransaction(transaction, token)
    }
}
