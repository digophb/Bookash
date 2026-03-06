package com.bookash.app

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * Adapter para dropdown de tags com cor
 */
class TagDropdownAdapter(
    context: Context,
    private val tags: List<Tag>
) : ArrayAdapter<Tag>(context, R.layout.item_dropdown_tag, tags) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // View do item selecionado - mostra APENAS o nome
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)
        
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = tags[position].name
        textView.setTextColor(context.getColor(R.color.text_primary))
        
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
}
