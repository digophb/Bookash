package com.bookash.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoalsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GoalsActivity"
        private const val PREFS_NAME = "goals_prefs"
        private const val KEY_SELECTED_GOAL = "selected_goal"
        
        fun getSelectedGoal(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SELECTED_GOAL, null)
        }

        fun setSelectedGoal(context: Context, goalType: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_GOAL, goalType).apply()
        }
    }

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var switchDaily: MaterialSwitch
    private lateinit var switchWeekly: MaterialSwitch
    private lateinit var switchMonthly: MaterialSwitch
    private lateinit var switchYearly: MaterialSwitch
    private lateinit var inputDaily: TextInputEditText
    private lateinit var inputWeekly: TextInputEditText
    private lateinit var inputMonthly: TextInputEditText
    private lateinit var inputYearly: TextInputEditText
    private lateinit var inputLayoutDaily: TextInputLayout
    private lateinit var inputLayoutWeekly: TextInputLayout
    private lateinit var inputLayoutMonthly: TextInputLayout
    private lateinit var inputLayoutYearly: TextInputLayout
    private lateinit var btnSaveDaily: MaterialButton
    private lateinit var btnSaveWeekly: MaterialButton
    private lateinit var btnSaveMonthly: MaterialButton
    private lateinit var btnSaveYearly: MaterialButton

    private var userId: String? = null
    private var goals: MutableList<Goal> = mutableListOf()
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        userId = UserSession.getUserId()
        initViews()
        setupListeners()
        loadGoals()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        switchDaily = findViewById(R.id.switchDaily)
        switchWeekly = findViewById(R.id.switchWeekly)
        switchMonthly = findViewById(R.id.switchMonthly)
        switchYearly = findViewById(R.id.switchYearly)
        inputDaily = findViewById(R.id.inputDaily)
        inputWeekly = findViewById(R.id.inputWeekly)
        inputMonthly = findViewById(R.id.inputMonthly)
        inputYearly = findViewById(R.id.inputYearly)
        inputLayoutDaily = findViewById(R.id.inputLayoutDaily)
        inputLayoutWeekly = findViewById(R.id.inputLayoutWeekly)
        inputLayoutMonthly = findViewById(R.id.inputLayoutMonthly)
        inputLayoutYearly = findViewById(R.id.inputLayoutYearly)
        btnSaveDaily = findViewById(R.id.btnSaveDaily)
        btnSaveWeekly = findViewById(R.id.btnSaveWeekly)
        btnSaveMonthly = findViewById(R.id.btnSaveMonthly)
        btnSaveYearly = findViewById(R.id.btnSaveYearly)

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        // Switch listeners - habilitar/desabilitar campos e botões
        switchDaily.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutDaily.isEnabled = isChecked
            btnSaveDaily.isEnabled = isChecked && inputDaily.text?.isNotEmpty() == true
            if (!isChecked) inputDaily.setText("")
        }

        switchWeekly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutWeekly.isEnabled = isChecked
            btnSaveWeekly.isEnabled = isChecked && inputWeekly.text?.isNotEmpty() == true
            if (!isChecked) inputWeekly.setText("")
        }

        switchMonthly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutMonthly.isEnabled = isChecked
            btnSaveMonthly.isEnabled = isChecked && inputMonthly.text?.isNotEmpty() == true
            if (!isChecked) inputMonthly.setText("")
        }

        switchYearly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutYearly.isEnabled = isChecked
            btnSaveYearly.isEnabled = isChecked && inputYearly.text?.isNotEmpty() == true
            if (!isChecked) inputYearly.setText("")
        }

        // Input listeners - habilitar botão de salvar quando houver valor
        inputDaily.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                btnSaveDaily.isEnabled = switchDaily.isChecked && !s.isNullOrBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputWeekly.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                btnSaveWeekly.isEnabled = switchWeekly.isChecked && !s.isNullOrBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputMonthly.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                btnSaveMonthly.isEnabled = switchMonthly.isChecked && !s.isNullOrBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputYearly.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                btnSaveYearly.isEnabled = switchYearly.isChecked && !s.isNullOrBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Botões de salvar
        btnSaveDaily.setOnClickListener { saveGoal("daily", true) }
        btnSaveWeekly.setOnClickListener { saveGoal("weekly", true) }
        btnSaveMonthly.setOnClickListener { saveGoal("monthly", true) }
        btnSaveYearly.setOnClickListener { saveGoal("yearly", true) }
    }

    private fun loadGoals() {
        isLoading = true
        lifecycleScope.launch {
            try {
                val loadedGoals = withContext(Dispatchers.IO) {
                    SupabaseService.getGoals(userId ?: "")
                }
                goals.clear()
                goals.addAll(loadedGoals)

                // Atualizar UI
                goals.forEach { goal ->
                    when (goal.type) {
                        "daily" -> {
                            switchDaily.isChecked = goal.isEnabled
                            inputDaily.setText(if (goal.targetAmount > 0) String.format("%.2f", goal.targetAmount) else "")
                            inputLayoutDaily.isEnabled = goal.isEnabled
                            btnSaveDaily.isEnabled = goal.isEnabled && goal.targetAmount > 0
                        }
                        "weekly" -> {
                            switchWeekly.isChecked = goal.isEnabled
                            inputWeekly.setText(if (goal.targetAmount > 0) String.format("%.2f", goal.targetAmount) else "")
                            inputLayoutWeekly.isEnabled = goal.isEnabled
                            btnSaveWeekly.isEnabled = goal.isEnabled && goal.targetAmount > 0
                        }
                        "monthly" -> {
                            switchMonthly.isChecked = goal.isEnabled
                            inputMonthly.setText(if (goal.targetAmount > 0) String.format("%.2f", goal.targetAmount) else "")
                            inputLayoutMonthly.isEnabled = goal.isEnabled
                            btnSaveMonthly.isEnabled = goal.isEnabled && goal.targetAmount > 0
                        }
                        "yearly" -> {
                            switchYearly.isChecked = goal.isEnabled
                            inputYearly.setText(if (goal.targetAmount > 0) String.format("%.2f", goal.targetAmount) else "")
                            inputLayoutYearly.isEnabled = goal.isEnabled
                            btnSaveYearly.isEnabled = goal.isEnabled && goal.targetAmount > 0
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar metas", e)
                ToastManager.showError(this@GoalsActivity, "Erro ao carregar metas")
            } finally {
                isLoading = false
            }
        }
    }

    private fun saveGoal(type: String, isEnabled: Boolean) {
        val amount = when (type) {
            "daily" -> inputDaily.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            "weekly" -> inputWeekly.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            "monthly" -> inputMonthly.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            "yearly" -> inputYearly.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        // Validar valor
        if (amount <= 0) {
            ToastManager.showError(this@GoalsActivity, "Digite um valor válido para a meta")
            return
        }

        val existingGoal = goals.find { it.type == type }
        
        val goal = Goal(
            id = existingGoal?.id ?: "",
            userId = userId ?: "",
            type = type,
            targetAmount = amount,
            isEnabled = isEnabled
        )

        val btnToDisable = when (type) {
            "daily" -> btnSaveDaily
            "weekly" -> btnSaveWeekly
            "monthly" -> btnSaveMonthly
            "yearly" -> btnSaveYearly
            else -> null
        }

        btnToDisable?.isEnabled = false

        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    SupabaseService.saveGoal(goal)
                }
                
                if (success) {
                    // Atualizar lista local
                    val index = goals.indexOfFirst { it.type == type }
                    if (index >= 0) goals[index] = goal else goals.add(goal)
                    Log.d(TAG, "Meta $type salva com sucesso")
                    ToastManager.showSuccess(this@GoalsActivity, "Meta ${goal.getDisplayName()} salva!")
                    btnToDisable?.isEnabled = true
                } else {
                    ToastManager.showError(this@GoalsActivity, "Erro ao salvar meta")
                    btnToDisable?.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar meta", e)
                ToastManager.showError(this@GoalsActivity, "Erro ao salvar meta: ${e.message}")
                btnToDisable?.isEnabled = true
            }
        }
    }
}
