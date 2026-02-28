package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TagsActivity : AppCompatActivity() {

    private lateinit var tagsRecycler: RecyclerView
    private lateinit var fabAddTag: FloatingActionButton
    private lateinit var emptyState: View
    
    private val tags = mutableListOf<Tag>()
    private lateinit var tagAdapter: TagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)

        tagsRecycler = findViewById(R.id.tagsRecycler)
        fabAddTag = findViewById(R.id.fabAddTag)
        emptyState = findViewById(R.id.emptyState)
        
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        tagAdapter = TagAdapter(
            onEditClick = { tag -> openEditTag(tag) },
            onDeleteClick = { tag -> showDeleteTagDialog(tag) }
        )
        tagsRecycler.layoutManager = LinearLayoutManager(this)
        tagsRecycler.adapter = tagAdapter
        
        fabAddTag.setOnClickListener {
            openAddTag()
        }
        
        loadTags()
    }

    private fun loadTags() {
        val userId = UserSession.getUserId()
        if (userId == null) {
            ToastManager.showError(this, "Usuário não logado")
            return
        }
        
        lifecycleScope.launch {
            val loadedTags = SupabaseService.getTags(userId)
            tags.clear()
            tags.addAll(loadedTags)
            
            if (tags.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                tagsRecycler.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                tagsRecycler.visibility = View.VISIBLE
                tagAdapter.submitList(tags)
            }
        }
    }
    
    private fun openAddTag() {
        val intent = Intent(this, AddTagActivity::class.java)
        startActivityForResult(intent, REQUEST_ADD_TAG)
    }
    
    private fun openEditTag(tag: Tag) {
        val intent = Intent(this, AddTagActivity::class.java).apply {
            putExtra("tag_id", tag.id)
            putExtra("tag_name", tag.name)
            putExtra("tag_color", tag.color)
        }
        startActivityForResult(intent, REQUEST_ADD_TAG)
    }
    
    private fun showDeleteTagDialog(tag: Tag) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Excluir tag")
            .setMessage("Deseja excluir \"${tag.name}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                deleteTag(tag)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteTag(tag: Tag) {
        lifecycleScope.launch {
            val success = SupabaseService.deleteTag(tag.id)
            if (success) {
                ToastManager.showWarning(this@TagsActivity, "Tag \"${tag.name}\" excluída")
                loadTags()
            } else {
                ToastManager.showError(this@TagsActivity, "Erro ao excluir tag")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_ADD_TAG && resultCode == RESULT_OK) {
            loadTags()
            
            data?.let {
                val action = it.getStringExtra("tag_action")
                val tagName = it.getStringExtra("tag_name") ?: "Tag"
                
                when (action) {
                    "create" -> ToastManager.showSuccess(this, "Tag \"$tagName\" criada")
                    "edit" -> ToastManager.showInfo(this, "Tag \"$tagName\" atualizada")
                }
            }
        }
    }
    
    companion object {
        private const val REQUEST_ADD_TAG = 2001
    }
}
