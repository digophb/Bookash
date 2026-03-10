package com.bookash.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class GoalsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GoalsActivity"
        const val PREFS_NAME = "goals_prefs"
        const val KEY_DAILY_ENABLED = "daily_enabled"
        const val KEY_DAILY_AMOUNT = "daily_amount"
        const val KEY_WEEKLY_ENABLED = "weekly_enabled"
        const val KEY_WEEKLY_AMOUNT = "weekly_amount"
        const val KEY_MONTHLY_ENABLED = "monthly_enabled"
        const val KEY_MONTHLY_AMOUNT = "monthly_amount"
        const val KEY_YEARLY_ENABLED = "yearly_enabled"
        const val KEY_YEARLY_AMOUNT = "yearly_amount"
        const val KEY_SELECTED_GOAL = "selected_goal"
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
            saveGoals()
        }

        switchWeekly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutWeekly.isEnabled = isChecked
            if (!isChecked) inputWeekly.setText("")
            saveGoals()
        }

        switchMonthly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutMonthly.isEnabled = isChecked
            if (!isChecked) inputMonthly.setText("")
            saveGoals()
        }

        switchYearly.setOnCheckedChangeListener { _, isChecked ->
            inputLayoutYearly.isEnabled = isChecked
            if (!isChecked) inputYearly.setText("")
            saveGoals()
        }

        // Input listeners - salvar ao perder foco
        inputDaily.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveGoals()
        }
        inputWeekly.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveGoals()
        }
        inputMonthly.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveGoals()
        }
        inputYearly.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveGoals()
        }
    }

    private fun loadGoals() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Carregar meta diária
        switchDaily.isChecked = prefs.getBoolean(KEY_DAILY_ENABLED, false)
        inputDaily.setText(prefs.getString(KEY_DAILY_AMOUNT, ""))
        inputLayoutDaily.isEnabled = switchDaily.isChecked

        // Carregar meta semanal
        switchWeekly.isChecked = prefs.getBoolean(KEY_WEEKLY_ENABLED, false)
        inputWeekly.setText(prefs.getString(KEY_WEEKLY_AMOUNT, ""))
        inputLayoutWeekly.isEnabled = switchWeekly.isChecked

        // Carregar meta mensal
        switchMonthly.isChecked = prefs.getBoolean(KEY_MONTHLY_ENABLED, false)
        inputMonthly.setText(prefs.getString(KEY_MONTHLY_AMOUNT, ""))
        inputLayoutMonthly.isEnabled = switchMonthly.isChecked

        // Carregar meta anual
        switchYearly.isChecked = prefs.getBoolean(KEY_YEARLY_ENABLED, false)
        inputYearly.setText(prefs.getString(KEY_YEARLY_AMOUNT, ""))
        inputLayoutYearly.isEnabled = switchYearly.isChecked
    }

    private fun saveGoals() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Salvar meta diária
        editor.putBoolean(KEY_DAILY_ENABLED, switchDaily.isChecked)
        editor.putString(KEY_DAILY_AMOUNT, inputDaily.text.toString())

        // Salvar meta semanal
        editor.putBoolean(KEY_WEEKLY_ENABLED, switchWeekly.isChecked)
        editor.putString(KEY_WEEKLY_AMOUNT, inputWeekly.text.toString())

        // Salvar meta mensal
        editor.putBoolean(KEY_MONTHLY_ENABLED, switchMonthly.isChecked)
        editor.putString(KEY_MONTHLY_AMOUNT, inputMonthly.text.toString())

        // Salvar meta anual
        editor.putBoolean(KEY_YEARLY_ENABLED, switchYearly.isChecked)
        editor.putString(KEY_YEARLY_AMOUNT, inputYearly.text.toString())

        editor.apply()

        // Se nenhuma meta está selecionada, definir a primeira ativa como selecionada
        if (getSelectedGoal() == null) {
            val firstEnabled = when {
                switchDaily.isChecked -> "daily"
                switchWeekly.isChecked -> "weekly"
                switchMonthly.isChecked -> "monthly"
                switchYearly.isChecked -> "yearly"
                else -> null
            }
            firstEnabled?.let { setSelectedGoal(it) }
        }

        Log.d(TAG, "Metas salvas")
    }

    companion object {
        fun getSelectedGoal(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SELECTED_GOAL, null)
        }

        fun setSelectedGoal(context: Context, goalType: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_GOAL, goalType).apply()
        }

        fun getGoal(context: Context, type: String): Pair<Boolean, Double> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = when (type) {
                "daily" -> prefs.getBoolean(KEY_DAILY_ENABLED, false)
                "weekly" -> prefs.getBoolean(KEY_WEEKLY_ENABLED, false)
                "monthly" -> prefs.getBoolean(KEY_MONTHLY_ENABLED, false)
                "yearly" -> prefs.getBoolean(KEY_YEARLY_ENABLED, false)
                else -> false
            }
            val amountKey = when (type) {
                "daily" -> KEY_DAILY_AMOUNT
                "weekly" -> KEY_WEEKLY_AMOUNT
                "monthly" -> KEY_MONTHLY_AMOUNT
                "yearly" -> KEY_YEARLY_AMOUNT
                else -> ""
            }
            val amount = prefs.getString(amountKey, "0")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            return Pair(enabled, amount)
        }

        fun hasEnabledGoals(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_DAILY_ENABLED, false) ||
                    prefs.getBoolean(KEY_WEEKLY_ENABLED, false) ||
                    prefs.getBoolean(KEY_MONTHLY_ENABLED, false) ||
                    prefs.getBoolean(KEY_YEARLY_ENABLED, false)
        }

        fun getEnabledGoals(context: Context): List<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val goals = mutableListOf<String>()
            if (prefs.getBoolean(KEY_DAILY_ENABLED, false)) goals.add("daily")
            if (prefs.getBoolean(KEY_WEEKLY_ENABLED, false)) goals.add("weekly")
            if (prefs.getBoolean(KEY_MONTHLY_ENABLED, false)) goals.add("monthly")
            if (prefs.getBoolean(KEY_YEARLY_ENABLED, false)) goals.add("yearly")
            return goals
        }
    }
}
