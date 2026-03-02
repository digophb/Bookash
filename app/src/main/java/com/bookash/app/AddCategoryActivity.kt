package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var typeToggle: MaterialButtonToggleGroup
    private lateinit var nameInput: TextInputEditText
    private lateinit var colorsRecycler: RecyclerView
    private lateinit var iconsRecycler: RecyclerView
    private lateinit var btnSave: MaterialButton

    private lateinit var colorAdapter: CategoryColorAdapter
    private lateinit var iconAdapter: CategoryIconAdapter

    private var selectedColor = "#357266"
    private var selectedIcon = "category"
    private var editingCategoryId: String? = null
    private var editingCategoryType: String = "expense"
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)
        
        userId = UserSession.getUserId()
        
        // Proteção: se não há usuário logado, voltar para login
        if (userId == null) {
            ToastManager.showWarning(this, "Sessão expirada. Faça login novamente.")
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        initViews()
        setupAdapters()
        setupListeners()
        loadEditingData()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        typeToggle = findViewById(R.id.typeToggle)
        nameInput = findViewById(R.id.nameInput)
        colorsRecycler = findViewById(R.id.colorsRecycler)
        iconsRecycler = findViewById(R.id.iconsRecycler)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupAdapters() {
        // Colors adapter - 8 columns
        colorAdapter = CategoryColorAdapter { color ->
            selectedColor = color
            iconAdapter.setSelectedColor(color)
        }
        colorsRecycler.apply {
            layoutManager = GridLayoutManager(this@AddCategoryActivity, 8)
            adapter = colorAdapter
        }

        // Icons adapter - 5 columns
        iconAdapter = CategoryIconAdapter { icon ->
            selectedIcon = icon
        }
        iconsRecycler.apply {
            layoutManager = GridLayoutManager(this@AddCategoryActivity, 5)
            adapter = iconAdapter
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        typeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                editingCategoryType = if (checkedId == R.id.btnIncome) "income" else "expense"
            }
        }

        btnSave.setOnClickListener {
            saveCategory()
        }
    }

    private fun loadEditingData() {
        editingCategoryId = intent.getStringExtra("category_id")
        
        if (editingCategoryId != null) {
            // Edit mode
            titleText.text = "Editar Categoria"
            btnSave.text = "Atualizar Categoria"
            
            val name = intent.getStringExtra("category_name") ?: ""
            val type = intent.getStringExtra("category_type") ?: "expense"
            val color = intent.getStringExtra("category_color") ?: "#357266"
            val icon = intent.getStringExtra("category_icon") ?: "category"
            
            nameInput.setText(name)
            editingCategoryType = type
            selectedColor = color
            selectedIcon = icon
            
            // Set type toggle
            if (type == "income") {
                typeToggle.check(R.id.btnIncome)
            } else {
                typeToggle.check(R.id.btnExpense)
            }
            
            // Set selected color
            colorAdapter.setSelectedColor(color)
            iconAdapter.setSelectedColor(color)
            
            // Set selected icon
            iconAdapter.setSelectedIcon(icon)
        } else {
            // Create mode - default to income (Receita)
            typeToggle.check(R.id.btnIncome)
        }
    }

    private fun saveCategory() {
        val name = nameInput.text.toString().trim()
        
        if (name.isEmpty()) {
            ToastManager.showWarning(this, "Digite um nome para a categoria")
            return
        }

        btnSave.isEnabled = false

        lifecycleScope.launch {
            // Verificar se já existe categoria com mesmo nome e tipo
            val exists = SupabaseService.categoryExists(name, editingCategoryType, editingCategoryId, userId)
            
            if (exists) {
                btnSave.isEnabled = true
                val typeLabel = if (editingCategoryType == "income") "Receita" else "Despesa"
                ToastManager.showWarning(
                    this@AddCategoryActivity,
                    "Já existe uma categoria '$name' do tipo $typeLabel"
                )
                return@launch
            }
            
            val category = Category(
                id = editingCategoryId ?: "",
                name = name,
                type = editingCategoryType,
                color = selectedColor,
                icon = selectedIcon
            )

            val success = if (editingCategoryId != null) {
                SupabaseService.updateCategory(category, userId)
            } else {
                SupabaseService.saveCategory(category, userId!!)
            }

            btnSave.isEnabled = true

            if (success) {
                // Pass result data to parent activity for toast display
                val resultIntent = Intent().apply {
                    putExtra("category_action", if (editingCategoryId != null) "edit" else "create")
                    putExtra("category_name", category.name)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                ToastManager.showError(this@AddCategoryActivity, "Erro ao salvar categoria")
            }
        }
    }
}
