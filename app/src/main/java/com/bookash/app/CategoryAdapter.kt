package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
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
            val categoryColor = try {
                if (category.color.isNotEmpty()) Color.parseColor(category.color) else Color.parseColor("#357266")
            } catch (e: Exception) {
                Log.e("CategoryAdapter", "Erro ao definir cor: ${category.color}", e)
                Color.parseColor("#357266")
            }
            
            // Indicador de cor lateral
            val indicatorDrawable = GradientDrawable()
            indicatorDrawable.cornerRadius = 4f
            indicatorDrawable.setColor(categoryColor)
            colorIndicator.background = indicatorDrawable
            
            // Background do ícone com a cor da categoria
            val iconBgDrawable = GradientDrawable()
            iconBgDrawable.shape = GradientDrawable.OVAL
            iconBgDrawable.setColor(categoryColor)
            categoryIcon.background = iconBgDrawable
            categoryIcon.setColorFilter(Color.WHITE)
            
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
            val icon = iconName.lowercase().trim()
            return when (icon) {
                // Alimentação
                "food", "restaurant", "ic_category_food" -> R.drawable.ic_category_food
                
                // Transporte
                "transport", "directions_car", "car", "ic_category_transport" -> R.drawable.ic_category_transport
                
                // Casa
                "home", "house", "ic_category_home" -> R.drawable.ic_category_home
                
                // Saúde
                "health", "local_hospital", "hospital", "medical_services", "ic_category_health" -> R.drawable.ic_category_health
                
                // Educação
                "education", "school", "ic_category_education" -> R.drawable.ic_category_education
                
                // Entretenimento/Lazer
                "entertainment", "movie", "theaters", "sports_esports", "ic_category_movie" -> R.drawable.ic_category_movie
                
                // Compras
                "shopping", "store", "local_mall", "ic_category_shopping" -> R.drawable.ic_category_shopping
                
                // Salário/Renda
                "salary", "attach_money", "work", "payments", "ic_category_salary" -> R.drawable.ic_category_salary
                
                // Freelance
                "freelance", "ic_icon_freelance" -> R.drawable.ic_icon_freelance
                
                // Investimentos
                "investment", "trending_up", "ic_icon_investment" -> R.drawable.ic_icon_investment
                
                // Presente
                "gift", "card_giftcard", "ic_icon_gift" -> R.drawable.ic_icon_gift
                
                // Pets
                "pets", "pets_icon", "ic_icon_pets" -> R.drawable.ic_icon_pets
                
                // Viagem
                "travel", "flight", "ic_icon_travel" -> R.drawable.ic_icon_travel
                
                // Assinaturas
                "subscriptions", "subscriptions_icon", "ic_icon_subscriptions" -> R.drawable.ic_icon_subscriptions
                
                // Contas/Utilidades
                "utilities", "receipt_long", "ic_icon_utilities" -> R.drawable.ic_icon_utilities
                
                // Seguro
                "insurance", "security", "ic_icon_insurance" -> R.drawable.ic_icon_insurance
                
                // Beleza
                "beauty", "spa", "ic_icon_beauty" -> R.drawable.ic_icon_beauty
                
                // Reembolso
                "refund", "restore", "ic_icon_refund" -> R.drawable.ic_icon_refund
                
                // Bônus
                "bonus", "star", "ic_icon_bonus" -> R.drawable.ic_icon_bonus
                
                // Outros
                "other", "others", "more_horiz", "category" -> R.drawable.ic_category_other
                
                // Default fallback
                else -> {
                    Log.w("CategoryAdapter", "Ícone não mapeado: '$iconName', usando fallback")
                    R.drawable.ic_category
                }
            }
        }
    }
}
