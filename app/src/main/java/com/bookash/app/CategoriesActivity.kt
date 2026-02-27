package com.bookash.app

import android.graphics.Color
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

        categoryAdapter = CategoryAdapter { category ->
            showDeleteCategoryDialog(category)
        }
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
        
        // Configurar seleção de cores
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
                Toast.makeText(this, "Cor selecionada", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Configurar seleção de ícones
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
                Toast.makeText(this, "Ícone selecionado", Toast.LENGTH_SHORT).show()
            }
        }
        
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
}
