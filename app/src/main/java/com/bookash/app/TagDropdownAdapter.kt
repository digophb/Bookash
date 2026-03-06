package com.bookash.app

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView

/**
 * Adapter para dropdown de tags com cor
 */
class TagDropdownAdapter(
    context: Context,
    private val tags: List<Tag>
) : ArrayAdapter<String>(context, R.layout.item_dropdown_tag_selected) {

    private val tagNames = tags.map { it.name }

    override fun getCount(): Int = tagNames.size

    override fun getItem(position: Int): String = tagNames[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View do item selecionado - mostra ícone + nome
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_tag_selected, parent, false)

        val tag = tags[position]

        val tagIcon = view.findViewById<ImageView>(R.id.tagIcon)
        val tagName = view.findViewById<TextView>(R.id.tagName)

        tagName.text = tag.name

        // Cor da tag no ícone
        try {
            tagIcon.setColorFilter(Color.parseColor(tag.color))
        } catch (e: Exception) {
            tagIcon.setColorFilter(Color.parseColor("#A3BBAD"))
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View do dropdown - mostra ícone + nome
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_tag, parent, false)

        val tag = tags[position]

        val tagIcon = view.findViewById<ImageView>(R.id.tagIcon)
        val tagName = view.findViewById<TextView>(R.id.tagName)

        tagName.text = tag.name

        // Cor da tag no ícone
        try {
            tagIcon.setColorFilter(Color.parseColor(tag.color))
        } catch (e: Exception) {
            tagIcon.setColorFilter(Color.parseColor("#A3BBAD"))
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                results.values = tagNames
                results.count = tagNames.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return resultValue?.toString() ?: ""
            }
        }
    }
    
    /**
     * Retorna a tag pelo nome selecionado
     */
    fun getTagByName(name: String): Tag? {
        return tags.find { it.name == name }
    }
}
