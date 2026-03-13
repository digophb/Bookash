package com.bookash.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PendingTransactionsPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val typeFilter: String // "income" ou "expense"
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2 // Duas abas: pendentes do tipo e do outro tipo

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PendingTransactionsFragment.newInstance(typeFilter)
            1 -> PendingTransactionsFragment.newInstance(if (typeFilter == "income") "expense" else "income")
            else -> PendingTransactionsFragment.newInstance("income")
        }
    }
}
