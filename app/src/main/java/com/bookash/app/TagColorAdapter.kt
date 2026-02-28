package com.bookash.app

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

data class TagColorItem(val name: String, val colorHex: String)

class TagColorAdapter(
    private val onColorSelected: (TagColorItem) -> Unit
) : RecyclerView.Adapter<TagColorAdapter.ColorViewHolder>() {

    private val colors = listOf(
        TagColorItem("Verde", "#357266"),
        TagColorItem("Azul", "#2196F3"),
        TagColorItem("Roxo", "#9C27B0"),
        TagColorItem("Rosa", "#E91E63"),
        TagColorItem("Vermelho", "#F44336"),
        TagColorItem("Laranja", "#FF9800"),
        TagColorItem("Amarelo", "#FFEB3B"),
        TagColorItem("Verde Claro", "#4CAF50"),
        TagColorItem("Ciano", "#00BCD4"),
        TagColorItem("Azul Escuro", "#3F51B5"),
        TagColorItem("Marrom", "#795548"),
        TagColorItem("Cinza", "#607D8B")
    )

    private var selectedColor: String = colors[0].colorHex

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
           .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size

    fun setSelectedColor(colorHex: String) {
        selectedColor = colorHex
        notifyDataSetChanged()
    }

    fun getSelectedColor(): String = selectedColor

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorCircle: View = itemView.findViewById(R.id.colorCircle)
        private val checkIcon: ImageView = itemView.findViewById(R.id.checkIcon)

        fun bind(colorItem: TagColorItem) {
            // Set color
            val drawable = colorCircle.background as? GradientDrawable
            drawable?.setColor(android.graphics.Color.parseColor(colorItem.colorHex))
            colorCircle.background = drawable

            // Show check if selected
            checkIcon.visibility = if (colorItem.colorHex == selectedColor) {
                View.VISIBLE
            } else {
                View.GONE
            }

            itemView.setOnClickListener {
                selectedColor = colorItem.colorHex
                onColorSelected(colorItem)
                notifyDataSetChanged()
            }
        }
    }
}
