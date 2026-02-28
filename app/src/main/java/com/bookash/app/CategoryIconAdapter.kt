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
        IconItem("salary", R.drawable.ic_icon_salary),
        IconItem("freelance", R.drawable.ic_icon_freelance),
        IconItem("investment", R.drawable.ic_icon_investment),
        IconItem("gift", R.drawable.ic_icon_gift),
        IconItem("shopping", R.drawable.ic_icon_shopping),
        IconItem("food", R.drawable.ic_category_food),
        IconItem("transport", R.drawable.ic_category_transport),
        IconItem("home", R.drawable.ic_icon_home),
        IconItem("health", R.drawable.ic_category_health),
        IconItem("education", R.drawable.ic_category_education),
        IconItem("entertainment", R.drawable.ic_icon_entertainment),
        IconItem("pets", R.drawable.ic_icon_pets),
        IconItem("travel", R.drawable.ic_icon_travel),
        IconItem("subscriptions", R.drawable.ic_icon_subscriptions),
        IconItem("utilities", R.drawable.ic_icon_utilities),
        IconItem("insurance", R.drawable.ic_icon_insurance),
        IconItem("beauty", R.drawable.ic_icon_beauty),
        IconItem("refund", R.drawable.ic_icon_refund),
        IconItem("bonus", R.drawable.ic_icon_bonus),
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
