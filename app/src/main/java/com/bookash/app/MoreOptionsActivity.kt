package com.bookash.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MoreOptionsActivity : AppCompatActivity() {

    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager2? = null
    private var backButton: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_more_options)
            
            tabLayout = findViewById(R.id.tabLayout)
            viewPager = findViewById(R.id.viewPager)
            backButton = findViewById(R.id.backButton)

            backButton?.setOnClickListener {
                finish()
            }

            setupViewPager()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun setupViewPager() {
        try {
            val adapter = SimplePagerAdapter(this)
            viewPager?.adapter = adapter

            TabLayoutMediator(tabLayout!!, viewPager!!) { tab, position ->
                tab.text = when (position) {
                    0 -> "Gerenciar"
                    1 -> "Geral"
                    2 -> "Sobre"
                    else -> ""
                }
            }.attach()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ViewPager: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private class SimplePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return try {
                when (position) {
                    0 -> ManageFragment()
                    1 -> GeneralFragment()
                    2 -> AboutFragment()
                    else -> EmptyFragment()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                EmptyFragment()
            }
        }
    }
}
