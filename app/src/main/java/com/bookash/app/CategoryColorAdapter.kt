package com.bookash.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CategoryColorAdapter(
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<CategoryColorAdapter.ColorViewHolder>() {

    private val colors = listOf(
        "#357266", // Teal
        "#2E7D6A", // Green
        "#4CAF50", // Light Green
        "#8BC34A", // Lime
        "#CDDC39", // Yellow Green
        "#FF9800", // Orange
        "#FF5722", // Deep Orange
        "#B85450", // Coral
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#673AB7", // Deep Purple
        "#3F51B5", // Indigo
        "#2196F3", // Blue
        "#00BCD4", // Cyan
        "#4A7C8C", // Steel Blue
        "#65532F"  // Brown
    )

    private var selectedColor: String = colors[0]

    fun setSelectedColor(color: String) {
        val oldPosition = colors.indexOf(selectedColor)
        selectedColor = color
        val newPosition = colors.indexOf(selectedColor)
        if (oldPosition >= 0) notifyItemChanged(oldPosition)
        if (newPosition >= 0) notifyItemChanged(newPosition)
    }

    fun getSelectedColor(): String = selectedColor

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], colors[position] == selectedColor)
    }

    override fun getItemCount() = colors.size

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorCircle: View = itemView.findViewById(R.id.colorCircle)
        private val checkIcon: ImageView = itemView.findViewById(R.id.checkIcon)

        fun bind(color: String, isSelected: Boolean) {
            // Set circle color
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(Color.parseColor(color))
            
            if (isSelected) {
                drawable.setStroke(4, Color.WHITE)
            } else {
                drawable.setStroke(0, Color.TRANSPARENT)
            }
            
            colorCircle.background = drawable
            
            // Show/hide check icon
            checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener {
                setSelectedColor(color)
                onColorSelected(color)
            }
        }
    }
}
