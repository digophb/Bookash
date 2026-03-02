package com.bookash.app

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MoreOptionsActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_more_options)
        } catch (e: Exception) {
            Log.e("MoreOptionsActivity", "Erro ao carregar layout: ${e.message}")
            ToastManager.showError(this, "Erro ao carregar tela")
            finish()
            return
        }

        // Inicializar SettingsManager se necessário
        try {
            SettingsManager.init(this)
        } catch (e: Exception) {
            Log.e("MoreOptionsActivity", "Erro ao inicializar SettingsManager: ${e.message}")
        }

        try {
            tabLayout = findViewById(R.id.tabLayout)
            viewPager = findViewById(R.id.viewPager)
            backButton = findViewById(R.id.backButton)

            backButton.setOnClickListener {
                finish()
            }

            setupViewPager()
        } catch (e: Exception) {
            Log.e("MoreOptionsActivity", "Erro ao configurar view: ${e.message}")
            ToastManager.showError(this, "Erro ao configurar tela")
            finish()
        }
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Gerenciar"
                1 -> "Geral"
                2 -> "Sobre"
                else -> ""
            }
        }.attach()
    }

    private class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ManageFragment()
                1 -> GeneralFragment()
                2 -> AboutFragment()
                else -> Fragment()
            }
        }
    }
}
