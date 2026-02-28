package com.bookash.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddAccountActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var searchBankInput: TextInputEditText
    private lateinit var banksRecycler: RecyclerView
    private lateinit var nameInput: TextInputEditText
    private lateinit var typeChipGroup: ChipGroup
    private lateinit var balanceInput: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private lateinit var bankAdapter: BankIconAdapter

    private var selectedBank = "wallet"
    private var selectedType = "corrente"
    private var editingAccountId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        initViews()
        setupAdapters()
        setupListeners()
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
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupAdapters() {
        bankAdapter = BankIconAdapter { bankId ->
            selectedBank = bankId
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

        // Search filter
        searchBankInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                bankAdapter.filter(s?.toString() ?: "")
            }
        })

        // Type selection
        typeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedType = when (checkedIds[0]) {
                    R.id.chipCorrente -> "corrente"
                    R.id.chipPoupanca -> "poupanca"
                    R.id.chipCarteira -> "carteira"
                    R.id.chipDigital -> "digital"
                    else -> "outros"
                }
            }
        }

        btnSave.setOnClickListener {
            saveAccount()
        }
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

            nameInput.setText(name)
            balanceInput.setText(String.format("%.2f", balance))
            selectedBank = icon
            selectedType = type

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
        
        val userId = UserSession.userId

        lifecycleScope.launch {
            val account = Account(
                id = editingAccountId ?: "",
                name = name,
                balance = balance,
                type = selectedType,
                icon = selectedBank
            )

            val success = if (editingAccountId != null) {
                SupabaseService.updateAccount(account, userId)
            } else {
                SupabaseService.saveAccount(account, userId)
            }

            btnSave.isEnabled = true

            if (success) {
                val message = if (editingAccountId != null) {
                    "Conta \"${account.name}\" atualizada"
                } else {
                    "Conta \"${account.name}\" criada"
                }
                ToastManager.showSuccess(this@AddAccountActivity, message)
                setResult(RESULT_OK)
                finish()
            } else {
                ToastManager.showError(this@AddAccountActivity, "Erro ao salvar conta")
            }
        }
    }
}
