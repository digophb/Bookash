package com.bookash.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class GeneralFragment : Fragment() {

    private lateinit var themeValue: TextView
    private lateinit var languageValue: TextView
    private lateinit var notificationsSwitch: SwitchMaterial

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        themeValue = view.findViewById(R.id.themeValue)
        languageValue = view.findViewById(R.id.languageValue)
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch)

        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        val userId = UserSession.getUserId()
        if (userId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val settings = SupabaseService.getSettings(userId)
                    themeValue.text = when (settings.theme) { "light" -> "Claro"; "dark" -> "Escuro"; else -> "Sistema" }
                    languageValue.text = when (settings.language) { "pt-BR" -> "Português"; "en-US" -> "English"; "es-ES" -> "Español"; else -> "Português" }
                    notificationsSwitch.isChecked = settings.notificationsEnabled
                } catch (e: Exception) {
                    setDefaultValues()
                }
            }
        } else {
            setDefaultValues()
        }
    }

    private fun setDefaultValues() {
        themeValue.text = "Sistema"
        languageValue.text = "Português"
        notificationsSwitch.isChecked = true
    }

    private fun setupClickListeners() {
        themeValue.setOnClickListener { showThemeDialog() }
        languageValue.setOnClickListener { showLanguageDialog() }
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked -> saveNotificationsSetting(isChecked) }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Claro", "Escuro", "Sistema")
        val currentTheme = themeValue.text.toString()
        val currentIndex = when (currentTheme) { "Claro" -> 0; "Escuro" -> 1; else -> 2 }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Tema")
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                val selectedTheme = when (which) { 0 -> "light"; 1 -> "dark"; else -> "system" }
                themeValue.text = themes[which]
                saveThemeSetting(selectedTheme)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Português", "English", "Español")
        val currentLanguage = languageValue.text.toString()
        val currentIndex = when (currentLanguage) { "English" -> 1; "Español" -> 2; else -> 0 }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Idioma")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val languageCode = when (which) { 0 -> "pt-BR"; 1 -> "en-US"; else -> "es-ES" }
                languageValue.text = languages[which]
                saveLanguageSetting(languageCode)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveThemeSetting(theme: String) {
        val userId = UserSession.getUserId() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = AppSettings(userId = userId, theme = theme)
            SupabaseService.updateSettings(settings, userId)
        }
    }

    private fun saveLanguageSetting(language: String) {
        val userId = UserSession.getUserId() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = AppSettings(userId = userId, language = language)
            SupabaseService.updateSettings(settings, userId)
        }
    }

    private fun saveNotificationsSetting(enabled: Boolean) {
        val userId = UserSession.getUserId() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = AppSettings(userId = userId, notificationsEnabled = enabled)
            SupabaseService.updateSettings(settings, userId)
        }
    }
}
