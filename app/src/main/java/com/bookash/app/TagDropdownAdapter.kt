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
) : ArrayAdapter<Tag>(context, android.R.layout.simple_dropdown_item_1line, tags) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_tag, parent, false)

        val tag = tags[position]

        val tagIcon = view.findViewById<ImageView>(R.id.tagIcon)
        val tagName = view.findViewById<TextView>(R.id.tagName)

        // Nome da tag
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
