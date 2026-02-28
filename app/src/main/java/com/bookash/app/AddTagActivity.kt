package com.bookash.app

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddTagActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var titleText: TextView
    private lateinit var nameInput: TextInputEditText
    private lateinit var colorsRecycler: RecyclerView
    private lateinit var previewColor: View
    private lateinit var previewName: TextView
    private lateinit var btnSave: MaterialButton

    private lateinit var colorAdapter: TagColorAdapter

    private var selectedColor = "#357266"
    private var editingTagId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_tag)

        initViews()
        setupAdapters()
        setupListeners()
        loadEditingData()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        nameInput = findViewById(R.id.nameInput)
        colorsRecycler = findViewById(R.id.colorsRecycler)
        previewColor = findViewById(R.id.previewColor)
        previewName = findViewById(R.id.previewName)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupAdapters() {
        colorAdapter = TagColorAdapter { colorItem ->
            selectedColor = colorItem.colorHex
            updatePreview()
        }
        colorsRecycler.apply {
            layoutManager = GridLayoutManager(this@AddTagActivity, 6)
            adapter = colorAdapter
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        nameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnSave.setOnClickListener {
            saveTag()
        }
    }

    private fun loadEditingData() {
        editingTagId = intent.getStringExtra("tag_id")
        
        if (editingTagId != null) {
            // Edit mode
            titleText.text = "Editar Tag"
            btnSave.text = "Atualizar Tag"
            
            val name = intent.getStringExtra("tag_name") ?: ""
            val color = intent.getStringExtra("tag_color") ?: "#357266"
            
            nameInput.setText(name)
            selectedColor = color
            
            // Set selected color
            colorAdapter.setSelectedColor(color)
        }
        
        updatePreview()
    }

    private fun updatePreview() {
        val name = nameInput.text?.toString()?.trim() ?: ""
        previewName.text = if (name.isEmpty()) "Nome da Tag" else name
        
        // Update preview color
        val drawable = previewColor.background as? GradientDrawable
        drawable?.setColor(android.graphics.Color.parseColor(selectedColor))
        previewColor.background = drawable
    }

    private fun saveTag() {
        val name = nameInput.text.toString().trim()
        
        if (name.isEmpty()) {
            ToastManager.showWarning(this, "Digite um nome para a tag")
            return
        }

        btnSave.isEnabled = false

        lifecycleScope.launch {
            // Verificar se já existe tag com mesmo nome
            val exists = SupabaseService.tagExists(name, editingTagId)
            
            if (exists) {
                btnSave.isEnabled = true
                ToastManager.showWarning(this@AddTagActivity, "Já existe uma tag '$name'")
                return@launch
            }
            
            val tag = Tag(
                id = editingTagId ?: "",
                name = name,
                color = selectedColor
            )

            val success = if (editingTagId != null) {
                SupabaseService.updateTag(tag)
            } else {
                SupabaseService.saveTag(tag)
            }

            btnSave.isEnabled = true

            if (success) {
                val resultIntent = Intent().apply {
                    putExtra("tag_action", if (editingTagId != null) "edit" else "create")
                    putExtra("tag_name", tag.name)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                ToastManager.showError(this@AddTagActivity, "Erro ao salvar tag")
            }
        }
    }
}
