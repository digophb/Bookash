package com.bookash.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CategoryAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<Category>()

    fun submitList(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        private val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val categoryType: TextView = itemView.findViewById(R.id.categoryType)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.categoryCard)

        fun bind(category: Category) {
            categoryName.text = category.name
            categoryType.text = if (category.type == "income") "Receita" else "Despesa"

            // Color indicator
            try {
                colorIndicator.setBackgroundColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                colorIndicator.setBackgroundColor(Color.parseColor("#357266"))
            }

            // Icon
            val iconRes = itemView.context.resources.getIdentifier(
                category.icon,
                "drawable",
                itemView.context.packageName
            )
            if (iconRes != 0) {
                categoryIcon.setImageResource(iconRes)
            } else {
                categoryIcon.setImageResource(R.drawable.ic_category)
            }

            // Verificar se é categoria padrão (userId == null) ou pessoal
            val isDefaultCategory = category.userId.isNullOrEmpty()

            if (isDefaultCategory) {
                // Categoria padrão: não pode editar nem excluir
                editButton.visibility = View.GONE
                deleteButton.visibility = View.GONE
                
                // Badge visual para categoria padrão
                categoryType.text = if (category.type == "income") "Receita • Padrão" else "Despesa • Padrão"
                categoryType.setTextColor(itemView.context.getColor(R.color.text_secondary))
            } else {
                // Categoria pessoal: pode editar e excluir
                editButton.visibility = View.VISIBLE
                deleteButton.visibility = View.VISIBLE
                
                categoryType.text = if (category.type == "income") "Receita • Personalizada" else "Despesa • Personalizada"
                categoryType.setTextColor(itemView.context.getColor(R.color.primary))

                editButton.setOnClickListener {
                    onEditClick(category)
                }

                deleteButton.setOnClickListener {
                    onDeleteClick(category)
                }
            }
        }
    }
}
