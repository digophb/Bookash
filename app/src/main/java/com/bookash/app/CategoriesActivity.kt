package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var categoriesRecycler: RecyclerView
    private lateinit var fabAddCategory: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var emptyState: View
    
    private val categories = mutableListOf<Category>()
    private lateinit var categoryAdapter: CategoryAdapter

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
            onEditClick = { category -> openEditCategory(category) },
            onDeleteClick = { category -> showDeleteCategoryDialog(category) }
        )
        categoriesRecycler.layoutManager = LinearLayoutManager(this)
        categoriesRecycler.adapter = categoryAdapter
        
        fabAddCategory.setOnClickListener {
            openAddCategory()
        }
        
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val userId = UserSession.userId
            val loadedCategories = SupabaseService.getCategories(userId)
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
    
    private fun openAddCategory() {
        val intent = Intent(this, AddCategoryActivity::class.java)
        startActivityForResult(intent, REQUEST_ADD_CATEGORY)
    }
    
    private fun openEditCategory(category: Category) {
        val intent = Intent(this, AddCategoryActivity::class.java).apply {
            putExtra("category_id", category.id)
            putExtra("category_name", category.name)
            putExtra("category_type", category.type)
            putExtra("category_color", category.color)
            putExtra("category_icon", category.icon)
        }
        startActivityForResult(intent, REQUEST_ADD_CATEGORY)
    }
    
    private fun showDeleteCategoryDialog(category: Category) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
                ToastManager.showWarning(this@CategoriesActivity, "Categoria \"${category.name}\" excluída")
                loadCategories()
            } else {
                ToastManager.showError(this@CategoriesActivity, "Erro ao excluir categoria")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_ADD_CATEGORY && resultCode == RESULT_OK) {
            loadCategories()
            
            // Show toast based on action
            data?.let {
                val action = it.getStringExtra("category_action")
                val categoryName = it.getStringExtra("category_name") ?: "Categoria"
                
                when (action) {
                    "create" -> ToastManager.showSuccess(this, "Categoria \"$categoryName\" criada")
                    "edit" -> ToastManager.showInfo(this, "Categoria \"$categoryName\" atualizada")
                }
            }
        }
    }
    
    companion object {
        private const val REQUEST_ADD_CATEGORY = 1001
    }
}
