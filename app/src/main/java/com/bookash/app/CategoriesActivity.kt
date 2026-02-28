package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var categoriesRecycler: RecyclerView
    private lateinit var fabAddCategory: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var emptyState: View
    
    private val categories = mutableListOf<Category>()
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedColor = "#357266"
    private var selectedIcon = "category"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        categoriesRecycler = findViewById(R.id.categoriesRecycler)
        fabAddCategory = findViewById(R.id.fabAddCategory)
        emptyState = findViewById(R.id.emptyState)
        
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        categoryAdapter = CategoryAdapter(
            onEditClick = { category -> showEditCategoryDialog(category) },
            onDeleteClick = { category -> showDeleteCategoryDialog(category) }
        )
        categoriesRecycler.layoutManager = LinearLayoutManager(this)
        categoriesRecycler.adapter = categoryAdapter
        
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
        
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val loadedCategories = SupabaseService.getCategories()
            categories.clear()
            categories.addAll(loadedCategories)
            
            if (categories.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                categoriesRecycler.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                categoriesRecycler.visibility = View.VISIBLE
                categoryAdapter.submitList(categories)
            }
        }
    }
    
    private fun showDeleteCategoryDialog(category: Category) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir categoria")
            .setMessage("Deseja excluir \"${category.name}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            val success = SupabaseService.deleteCategory(category.id)
            if (success) {
                Toast.makeText(this@CategoriesActivity, "Categoria excluída", Toast.LENGTH_SHORT).show()
                loadCategories()
            } else {
                Toast.makeText(this@CategoriesActivity, "Erro ao excluir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        
        val typeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.typeToggle)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        
        // Reset valores
        selectedColor = "#357266"
        selectedIcon = "category"
        
        // Configurar seleção de cores
        setupColorSelection(dialogView)
        
        // Configurar seleção de ícones
        setupIconSelection(dialogView)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Nova Categoria")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val type = if (typeToggle.checkedButtonId == R.id.btnIncome) "income" else "expense"
                val description = descriptionInput.text.toString()
                
                if (description.isNotBlank()) {
                    saveCategory(description, type)
                } else {
                    Toast.makeText(this, "Digite uma descrição", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showEditCategoryDialog(category: Category) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        
        val typeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.typeToggle)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        
        // Preencher com dados atuais
        descriptionInput.setText(category.name)
        selectedColor = category.color
        selectedIcon = category.icon
        
        // Selecionar tipo atual
        if (category.type == "income") {
            typeToggle.check(R.id.btnIncome)
        } else {
            typeToggle.check(R.id.btnExpense)
        }
        
        // Destacar cor atual
        highlightSelectedColor(dialogView, category.color)
        
        // Destacar ícone atual
        highlightSelectedIcon(dialogView, category.icon)
        
        // Configurar seleção de cores
        setupColorSelection(dialogView)
        
        // Configurar seleção de ícones
        setupIconSelection(dialogView)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Categoria")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val type = if (typeToggle.checkedButtonId == R.id.btnIncome) "income" else "expense"
                val description = descriptionInput.text.toString()
                
                if (description.isNotBlank()) {
                    updateCategory(category.id, description, type)
                } else {
                    Toast.makeText(this, "Digite uma descrição", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun setupColorSelection(dialogView: View) {
        val colors = listOf(
            Pair(R.id.color1, "#357266"),
            Pair(R.id.color2, "#B85450"),
            Pair(R.id.color3, "#65532F"),
            Pair(R.id.color4, "#2E7D6A"),
            Pair(R.id.color5, "#4A7C8C")
        )
        
        colors.forEach { (id, color) ->
            dialogView.findViewById<View>(id)?.setOnClickListener {
                selectedColor = color
                highlightSelectedColor(dialogView, color)
            }
        }
    }
    
    private fun highlightSelectedColor(dialogView: View, color: String) {
        val colorViews = listOf(R.id.color1, R.id.color2, R.id.color3, R.id.color4, R.id.color5)
        val colors = listOf("#357266", "#B85450", "#65532F", "#2E7D6A", "#4A7C8C")
        
        colorViews.forEachIndexed { index, viewId ->
            val view = dialogView.findViewById<View>(viewId)
            view?.let {
                val drawable = GradientDrawable()
                drawable.cornerRadius = 8f
                drawable.setColor(Color.parseColor(colors[index]))
                
                if (colors[index] == color) {
                    drawable.setStroke(4, Color.WHITE)
                }
                
                it.background = drawable
            }
        }
    }
    
    private fun setupIconSelection(dialogView: View) {
        val icons = listOf(
            Pair(R.id.icon1, "food"),
            Pair(R.id.icon2, "transport"),
            Pair(R.id.icon3, "home"),
            Pair(R.id.icon4, "health"),
            Pair(R.id.icon5, "education")
        )
        
        icons.forEach { (id, icon) ->
            dialogView.findViewById<ImageView>(id)?.setOnClickListener {
                selectedIcon = icon
                highlightSelectedIcon(dialogView, icon)
            }
        }
    }
    
    private fun highlightSelectedIcon(dialogView: View, icon: String) {
        val iconViews = listOf(
            Pair(R.id.icon1, "food"),
            Pair(R.id.icon2, "transport"),
            Pair(R.id.icon3, "home"),
            Pair(R.id.icon4, "health"),
            Pair(R.id.icon5, "education")
        )
        
        iconViews.forEach { (viewId, iconName) ->
            val view = dialogView.findViewById<ImageView>(viewId)
            view?.let {
                if (iconName == icon) {
                    it.setBackgroundColor(Color.parseColor("#357266"))
                    it.setColorFilter(Color.WHITE)
                } else {
                    it.setBackgroundColor(Color.TRANSPARENT)
                    it.setColorFilter(Color.parseColor("#B0B0B0"))
                }
            }
        }
    }

    private fun saveCategory(name: String, type: String) {
        lifecycleScope.launch {
            val category = Category(
                name = name,
                type = type,
                color = selectedColor,
                icon = selectedIcon
            )
            
            val success = SupabaseService.saveCategory(category)
            
            if (success) {
                Toast.makeText(this@CategoriesActivity, "Categoria salva!", Toast.LENGTH_SHORT).show()
                loadCategories()
            } else {
                Toast.makeText(this@CategoriesActivity, "Erro ao salvar categoria", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateCategory(categoryId: String, name: String, type: String) {
        lifecycleScope.launch {
            val category = Category(
                id = categoryId,
                name = name,
                type = type,
                color = selectedColor,
                icon = selectedIcon
            )
            
            val success = SupabaseService.updateCategory(category)
            
            if (success) {
                Toast.makeText(this@CategoriesActivity, "Categoria atualizada!", Toast.LENGTH_SHORT).show()
                loadCategories()
            } else {
                Toast.makeText(this@CategoriesActivity, "Erro ao atualizar categoria", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
