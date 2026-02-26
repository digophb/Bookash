package com.bookash.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bookash.app.R

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImage: ImageView = view.findViewById(R.id.iconImage)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.titleText.text = page.title
        holder.descriptionText.text = page.description
        
        // Definir ícone baseado na posição
        val iconRes = when (position) {
            0 -> R.drawable.ic_spending
            1 -> R.drawable.ic_organize
            2 -> R.drawable.ic_goals
            3 -> R.drawable.ic_insights
            4 -> R.drawable.ic_control
            else -> R.drawable.ic_spending
        }
        holder.iconImage.setImageResource(iconRes)
    }

    override fun getItemCount(): Int = pages.size
}
