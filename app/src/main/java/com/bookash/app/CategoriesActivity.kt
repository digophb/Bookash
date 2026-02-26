package com.bookash.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Category(
    val id: String = "",
    val name: String,
    val type: String,
    val color: String = "#3EBDB2",
    val icon: String = "category"
)

class CategoriesActivity : AppCompatActivity() {

    private lateinit var categoriesRecycler: RecyclerView
    private lateinit var fabAddCategory: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    private val supabaseUrl = "https://gqbxasjoxxslpaxjqfeg.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxYnhhc2pveHhzbHBheGpxZmVnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMzA4MTcsImV4cCI6MjA4NzYwNjgxN30.8arAkeAFEsUSTdyJpmafsp8T2yYgWEaZm9fCGnckaWs"
    
    private var selectedColor = "#3EBDB2"
    private var selectedIcon = "category"
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        categoriesRecycler = findViewById(R.id.categoriesRecycler)
        fabAddCategory = findViewById(R.id.fabAddCategory)
        
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        categoriesRecycler.layoutManager = LinearLayoutManager(this)
        
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
        
        loadCategories()
    }

    private fun loadCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$supabaseUrl/rest/v1/categories?select=*")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("apikey", supabaseKey)
                conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonArray = org.json.JSONArray(response)
                    
                    categories.clear()
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        categories.add(Category(
                            id = json.optString("id"),
                            name = json.optString("name"),
                            type = json.optString("type"),
                            color = json.optString("color", "#3EBDB2"),
                            icon = json.optString("icon", "category")
                        ))
                    }
                    
                    withContext(Dispatchers.Main) {
                        updateCategoriesList()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CategoriesActivity, "Erro ao carregar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCategoriesList() {
        // TODO: Implementar adapter
        Toast.makeText(this, "${categories.size} categorias carregadas", Toast.LENGTH_SHORT).show()
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        
        val typeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.typeToggle)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        
        // Configurar seleção de cores
        val colors = listOf(
            Pair(R.id.color1, "#3EBDB2"),
            Pair(R.id.color2, "#FF6B6B"),
            Pair(R.id.color3, "#E3C931"),
            Pair(R.id.color4, "#7FA1E0"),
            Pair(R.id.color5, "#9B59B6")
        )
        
        colors.forEach { (id, color) ->
            dialogView.findViewById<View>(id).setOnClickListener {
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
            dialogView.findViewById<ImageView>(id).setOnClickListener {
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
                    saveCategory(type, description, selectedColor, selectedIcon)
                } else {
                    Toast.makeText(this, "Digite uma descrição", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveCategory(type: String, description: String, color: String, icon: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$supabaseUrl/rest/v1/categories")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", supabaseKey)
                conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.doOutput = true
                
                val body = """{"name":"$description","type":"$type","color":"$color","icon":"$icon"}"""
                conn.outputStream.write(body.toByteArray())
                
                if (conn.responseCode in 200..299) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CategoriesActivity, "Categoria salva!", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CategoriesActivity, "Erro: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CategoriesActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
