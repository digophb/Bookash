package com.bookash.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CategoriesActivity : AppCompatActivity() {

    private lateinit var categoriesRecycler: RecyclerView
    private lateinit var fabAddCategory: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    private val supabaseUrl = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        categoriesRecycler = findViewById(R.id.categoriesRecycler)
        fabAddCategory = findViewById(R.id.fabAddCategory)

        categoriesRecycler.layoutManager = LinearLayoutManager(this)
        
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
        
        loadCategories()
    }

    private fun loadCategories() {
        // TODO: Carregar do Supabase
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        
        val typeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.typeToggle)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Nova Categoria")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val type = if (typeToggle.checkedButtonId == R.id.btnIncome) "income" else "expense"
                val description = descriptionInput.text.toString()
                
                if (description.isNotBlank()) {
                    saveCategory(type, description)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveCategory(type: String, description: String) {
        // TODO: Salvar no Supabase
        Toast.makeText(this, "Categoria salva: $description ($type)", Toast.LENGTH_SHORT).show()
    }
}
