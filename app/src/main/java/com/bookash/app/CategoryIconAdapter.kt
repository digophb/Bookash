package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

data class IconItem(
    val name: String,
    val resId: Int
)

class CategoryIconAdapter(
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<CategoryIconAdapter.IconViewHolder>() {

    private val icons = listOf(
        // Receitas
        IconItem("salary", R.drawable.ic_icon_salary),
        IconItem("freelance", R.drawable.ic_icon_freelance),
        IconItem("investment", R.drawable.ic_icon_investment),
        IconItem("bonus", R.drawable.ic_icon_bonus),
        IconItem("refund", R.drawable.ic_icon_refund),
        IconItem("loan", R.drawable.ic_icon_loan),
        IconItem("savings", R.drawable.ic_icon_savings),
        // donation removido - duplicado de heart
        
        // Alimentação
        IconItem("food", R.drawable.ic_category_food),
        IconItem("restaurant", R.drawable.ic_icon_restaurant),
        IconItem("coffee", R.drawable.ic_icon_coffee),
        IconItem("drinks", R.drawable.ic_icon_drinks),
        IconItem("bar", R.drawable.ic_icon_bar),
        // grocery removido - duplicado de shopping
        
        // Transporte
        IconItem("transport", R.drawable.ic_category_transport),
        // car e taxi removidos - duplicados de transport
        IconItem("fuel", R.drawable.ic_icon_fuel),
        IconItem("parking", R.drawable.ic_icon_parking),
        IconItem("toll", R.drawable.ic_icon_toll),
        IconItem("bus", R.drawable.ic_icon_bus),
        IconItem("metro", R.drawable.ic_icon_metro),
        IconItem("bike", R.drawable.ic_icon_bike),
        IconItem("moto", R.drawable.ic_icon_moto),
        
        // Casa
        IconItem("home", R.drawable.ic_icon_home),
        IconItem("rent", R.drawable.ic_icon_rent),
        IconItem("furniture", R.drawable.ic_icon_furniture),
        IconItem("repair", R.drawable.ic_icon_repair),
        // gas removido - duplicado de home
        
        // Contas
        IconItem("utilities", R.drawable.ic_icon_utilities),
        IconItem("electricity", R.drawable.ic_icon_electricity),
        IconItem("water", R.drawable.ic_icon_water),
        IconItem("internet", R.drawable.ic_icon_internet),
        IconItem("phone", R.drawable.ic_icon_phone),
        IconItem("taxes", R.drawable.ic_icon_taxes),
        
        // Saúde
        IconItem("health", R.drawable.ic_category_health),
        IconItem("pharmacy", R.drawable.ic_icon_pharmacy),
        IconItem("gym", R.drawable.ic_icon_gym),
        IconItem("sports", R.drawable.ic_icon_sports),
        
        // Educação e Trabalho
        IconItem("education", R.drawable.ic_category_education),
        IconItem("books", R.drawable.ic_icon_books),
        IconItem("work", R.drawable.ic_icon_work),
        
        // Lazer
        IconItem("entertainment", R.drawable.ic_icon_entertainment),
        IconItem("music", R.drawable.ic_icon_music),
        IconItem("games", R.drawable.ic_icon_games),
        IconItem("travel", R.drawable.ic_icon_travel),
        IconItem("party", R.drawable.ic_icon_party),
        
        // Compras
        IconItem("shopping", R.drawable.ic_icon_shopping),
        IconItem("clothes", R.drawable.ic_icon_clothes),
        IconItem("shoes", R.drawable.ic_icon_shoes),
        IconItem("tech", R.drawable.ic_icon_tech),
        
        // Outros
        IconItem("pets", R.drawable.ic_icon_pet),
        IconItem("beauty", R.drawable.ic_icon_beauty),
        IconItem("kids", R.drawable.ic_icon_kids),
        IconItem("baby", R.drawable.ic_icon_baby),
        IconItem("subscriptions", R.drawable.ic_icon_subscriptions),
        IconItem("insurance", R.drawable.ic_icon_insurance),
        IconItem("gift", R.drawable.ic_icon_gift),
        IconItem("heart", R.drawable.ic_icon_heart),
        IconItem("bookmark", R.drawable.ic_icon_bookmark),
        IconItem("tag", R.drawable.ic_icon_tag),
        IconItem("info", R.drawable.ic_icon_info),
        IconItem("category", R.drawable.ic_category)
    )

    private var selectedIcon: String = "category"
    private var selectedColor: String = "#357266"

    fun setSelectedIcon(iconName: String) {
        val oldPosition = icons.indexOfFirst { it.name == selectedIcon }
        selectedIcon = iconName
        val newPosition = icons.indexOfFirst { it.name == selectedIcon }
        if (oldPosition >= 0) notifyItemChanged(oldPosition)
        if (newPosition >= 0) notifyItemChanged(newPosition)
    }

    fun setSelectedColor(color: String) {
        selectedColor = color
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position])
    }

    override fun getItemCount() = icons.size

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImage: ImageView = itemView.findViewById(R.id.iconImage)

        fun bind(icon: IconItem) {
            iconImage.setImageResource(icon.resId)
            
            val isSelected = icon.name == selectedIcon
            
            // Create background drawable
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            
            if (isSelected) {
                drawable.setColor(Color.parseColor(selectedColor))
                iconImage.setColorFilter(Color.WHITE)
            } else {
                drawable.setColor(Color.parseColor("#2A2A2A"))
                iconImage.setColorFilter(Color.parseColor("#B0B0B0"))
            }
            
            iconImage.background = drawable
            
            itemView.setOnClickListener {
                setSelectedIcon(icon.name)
                onIconSelected(icon.name)
            }
        }
    }
}
