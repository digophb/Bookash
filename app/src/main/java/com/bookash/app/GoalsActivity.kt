package com.bookash.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
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
    private lateinit var switchDaily: SwitchMaterial
    private lateinit var switchWeekly: SwitchMaterial
    private lateinit var switchMonthly: SwitchMaterial
    private lateinit var switchYearly: SwitchMaterial
    private lateinit var inputDaily: TextInputEditText
    private lateinit var inputWeekly: TextInputEditText
    private lateinit var inputMonthly: TextInputEditText
    private lateinit var inputYearly: TextInputEditText
    private lateinit var inputLayoutDaily: TextInputLayout
    private lateinit var inputLayoutWeekly: TextInputLayout
    private lateinit var inputLayoutMonthly: TextInputLayout
    private lateinit var inputLayoutYearly: TextInputLayout

    private var userId: String? = null
    private var goals: MutableList<Goal> = mutableListOf()

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

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        // Switch listeners - habilitar/desabilitar campos
        switchDaily.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutDaily.isEnabled = isChecked
            if (!isChecked) inputDaily.setText("")
            saveGoal("daily", isChecked)
        }

        switchWeekly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutWeekly.isEnabled = isChecked
            if (!isChecked) inputWeekly.setText("")
            saveGoal("weekly", isChecked)
        }

        switchMonthly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutMonthly.isEnabled = isChecked
            if (!isChecked) inputMonthly.setText("")
            saveGoal("monthly", isChecked)
        }

        switchYearly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutYearly.isEnabled = isChecked
            if (!isChecked) inputYearly.setText("")
            saveGoal("yearly", isChecked)
        }

        // Input listeners - salvar ao perder foco
        inputDaily.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveGoal("daily", switchDaily.isChecked) }
        inputWeekly.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveGoal("weekly", switchWeekly.isChecked) }
        inputMonthly.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveGoal("monthly", switchMonthly.isChecked) }
        inputYearly.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveGoal("yearly", switchYearly.isChecked) }
    }

    private fun loadGoals() {
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
                            inputDaily.setText(String.format("%.2f", goal.targetAmount))
                            inputLayoutDaily.isEnabled = goal.isEnabled
                        }
                        "weekly" -> {
                            switchWeekly.isChecked = goal.isEnabled
                            inputWeekly.setText(String.format("%.2f", goal.targetAmount))
                            inputLayoutWeekly.isEnabled = goal.isEnabled
                        }
                        "monthly" -> {
                            switchMonthly.isChecked = goal.isEnabled
                            inputMonthly.setText(String.format("%.2f", goal.targetAmount))
                            inputLayoutMonthly.isEnabled = goal.isEnabled
                        }
                        "yearly" -> {
                            switchYearly.isChecked = goal.isEnabled
                            inputYearly.setText(String.format("%.2f", goal.targetAmount))
                            inputLayoutYearly.isEnabled = goal.isEnabled
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar metas", e)
                ToastManager.showError(this@GoalsActivity, "Erro ao carregar metas")
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

        val existingGoal = goals.find { it.type == type }
        
        val goal = Goal(
            id = existingGoal?.id ?: "",
            userId = userId ?: "",
            type = type,
            targetAmount = amount,
            isEnabled = isEnabled
        )

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
                } else {
                    ToastManager.showError(this@GoalsActivity, "Erro ao salvar meta")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar meta", e)
                ToastManager.showError(this@GoalsActivity, "Erro ao salvar meta")
            }
        }
    }
}
