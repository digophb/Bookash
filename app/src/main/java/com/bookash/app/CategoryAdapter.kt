package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val onEditClick: ((Category) -> Unit)? = null,
    private val onDeleteClick: ((Category) -> Unit)? = null
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val items = mutableListOf<Category>()

    fun submitList(newItems: List<Category>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        private val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val categoryType: TextView = itemView.findViewById(R.id.categoryType)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(category: Category) {
            categoryName.text = category.name
            
            // Tipo em português
            categoryType.text = if (category.type == "income") "Receita" else "Despesa"
            
            // Cor do indicador
            try {
                val drawable = GradientDrawable()
                drawable.cornerRadius = 4f
                drawable.setColor(Color.parseColor(category.color))
                colorIndicator.background = drawable
            } catch (e: Exception) {
                colorIndicator.setBackgroundColor(Color.parseColor("#357266"))
            }
            
            // Ícone baseado no nome
            val iconRes = getIconResource(category.icon)
            categoryIcon.setImageResource(iconRes)
            
            // Edit button
            editButton.setOnClickListener {
                onEditClick?.invoke(category)
            }
            
            // Delete button
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(category)
            }
        }
        
        private fun getIconResource(iconName: String): Int {
            return when (iconName) {
                "food", "restaurant" -> R.drawable.ic_category_food
                "transport" -> R.drawable.ic_category_transport
                "home" -> R.drawable.ic_category_home
                "health" -> R.drawable.ic_category_health
                "education" -> R.drawable.ic_category_education
                "attach_money", "work", "trending_up", "more_horiz" -> R.drawable.ic_trending_up
                else -> R.drawable.ic_category
            }
        }
    }
}
