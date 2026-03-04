package com.bookash.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * Adapter para dropdown de categorias com ícone e cor
 */
class CategoryDropdownAdapter(
    context: Context,
    private val categories: List<Category>
) : ArrayAdapter<Category>(context, android.R.layout.simple_dropdown_item_1line, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_category, parent, false)

        val category = categories[position]

        val iconView = view.findViewById<ImageView>(R.id.categoryIcon)
        val nameView = view.findViewById<TextView>(R.id.categoryName)

        // Nome da categoria
        nameView.text = category.name

        // Ícone
        val iconRes = getIconResource(category.icon)
        iconView.setImageResource(iconRes)

        // Cor de fundo
        try {
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(Color.parseColor(category.color))
            iconView.background = drawable
            iconView.setColorFilter(Color.WHITE)
        } catch (e: Exception) {
            iconView.setBackgroundColor(Color.parseColor("#357266"))
        }

        return view
    }

    private fun getIconResource(iconName: String): Int {
        return when (iconName) {
            "salary" -> R.drawable.ic_icon_salary
            "freelance" -> R.drawable.ic_icon_freelance
            "investment" -> R.drawable.ic_icon_investment
            "food", "restaurant" -> R.drawable.ic_category_food
            "transport", "car" -> R.drawable.ic_category_transport
            "home", "rent" -> R.drawable.ic_icon_home
            "health" -> R.drawable.ic_category_health
            "education" -> R.drawable.ic_category_education
            "entertainment", "games" -> R.drawable.ic_icon_games
            "shopping" -> R.drawable.ic_icon_shopping
            "utilities", "electricity" -> R.drawable.ic_icon_electricity
            "travel" -> R.drawable.ic_icon_travel
            "pets" -> R.drawable.ic_icon_pet
            "beauty" -> R.drawable.ic_icon_beauty
            "subscriptions" -> R.drawable.ic_icon_subscriptions
            else -> R.drawable.ic_category
        }
    }
}
