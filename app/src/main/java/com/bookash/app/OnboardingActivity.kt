package com.bookash.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bookash.app.R

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var continueButton: Button
    private lateinit var skipText: TextView
    
    private val pages = listOf(
        OnboardingPage(
            title = "Acompanhe seus gastos",
            description = "Registre e categorize suas despesas do dia a dia com facilidade"
        ),
        OnboardingPage(
            title = "Organize suas finanças",
            description = "Gerencie receitas, despesas e contas em um só lugar"
        ),
        OnboardingPage(
            title = "Alcance seus objetivos",
            description = "Defina metas financeiras e acompanhe seu progresso"
        ),
        OnboardingPage(
            title = "Insights inteligentes",
            description = "Visualize relatórios com gráficos claros e detalhados"
        ),
        OnboardingPage(
            title = "Assuma o controle",
            description = "Tome decisões financeiras com mais segurança e clareza"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        dotsLayout = findViewById(R.id.dotsLayout)
        continueButton = findViewById(R.id.continueButton)
        skipText = findViewById(R.id.skipText)

        setupViewPager()
        setupDots(0)
        setupButtons()
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
                updateButtonText(position)
            }
        })
    }

    private fun setupDots(position: Int) {
        dotsLayout.removeAllViews()
        
        val dots = arrayOfNulls<android.view.View>(pages.size)
        
        for (i in pages.indices) {
            dots[i] = android.view.View(this).apply {
                val params = LinearLayout.LayoutParams(24, 8)
                params.setMargins(4, 0, 4, 0)
                layoutParams = params
                
                if (i == position) {
                    setBackgroundResource(R.drawable.dot_active)
                } else {
                    setBackgroundResource(R.drawable.dot_inactive)
                }
            }
            dotsLayout.addView(dots[i])
        }
    }

    private fun updateButtonText(position: Int) {
        if (position == pages.size - 1) {
            continueButton.text = "Começar"
            skipText.visibility = android.view.View.GONE
        } else {
            continueButton.text = "Continuar"
            skipText.visibility = android.view.View.VISIBLE
        }
    }

    private fun setupButtons() {
        continueButton.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                finishOnboarding()
            }
        }

        skipText.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        // Salvar que onboarding foi concluído
        val prefs = getSharedPreferences("bookash_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        
        // Ir para tela de login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
