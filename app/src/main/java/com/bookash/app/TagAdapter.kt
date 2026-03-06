package com.bookash.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TagAdapter(
    private val onEditClick: (Tag) -> Unit,
    private val onDeleteClick: (Tag) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    private val tags = mutableListOf<Tag>()

    fun submitList(newTags: List<Tag>) {
        tags.clear()
        tags.addAll(newTags)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount(): Int = tags.size

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagIcon: ImageView = itemView.findViewById(R.id.tagIcon)
        private val tagName: TextView = itemView.findViewById(R.id.tagName)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(tag: Tag) {
            tagName.text = tag.name
            
            // Set tag icon color
            try {
                tagIcon.setColorFilter(android.graphics.Color.parseColor(tag.color))
            } catch (e: Exception) {
                tagIcon.setColorFilter(android.graphics.Color.parseColor("#A3BBAD"))
            }

            btnEdit.setOnClickListener { onEditClick(tag) }
            btnDelete.setOnClickListener { onDeleteClick(tag) }
        }
    }
}
