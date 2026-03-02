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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        try {
            // Carregar do SettingsManager
            val theme = SettingsManager.getTheme()
            val language = SettingsManager.getLanguage()
            val notifications = SettingsManager.areNotificationsEnabled()

            themeValue.text = when (theme) {
                "light" -> "Claro"
                "dark" -> "Escuro"
                else -> "Sistema"
            }

            languageValue.text = when (language) {
                "pt-BR" -> "Português"
                "en-US" -> "English"
                "es-ES" -> "Español"
                else -> "Português"
            }

            notificationsSwitch.isChecked = notifications
        } catch (e: Exception) {
            // Valores padrão em caso de erro
            themeValue.text = "Sistema"
            languageValue.text = "Português"
            notificationsSwitch.isChecked = true
        }
    }

    private fun setupClickListeners() {
        view?.findViewById<View>(R.id.card_theme)?.setOnClickListener {
            showThemeDialog()
        }

        view?.findViewById<View>(R.id.card_language)?.setOnClickListener {
            showLanguageDialog()
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationsSetting(isChecked)
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Claro", "Escuro", "Sistema")
        val currentValue = SettingsManager.getTheme()
        val currentIndex = when (currentValue) {
            "light" -> 0
            "dark" -> 1
            else -> 2
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Tema")
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                val selectedTheme = when (which) {
                    0 -> "light"
                    1 -> "dark"
                    else -> "system"
                }
                saveThemeSetting(selectedTheme)
                this.themeValue.text = themes[which]
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Português", "English", "Español")
        val currentValue = SettingsManager.getLanguage()
        val currentIndex = when (currentValue) {
            "pt-BR" -> 0
            "en-US" -> 1
            "es-ES" -> 2
            else -> 0
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Idioma")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val languageCode = when (which) {
                    0 -> "pt-BR"
                    1 -> "en-US"
                    else -> "es-ES"
                }
                saveLanguageSetting(languageCode)
                this.languageValue.text = languages[which]
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveThemeSetting(theme: String) {
        val userId = UserSession.getUserId()
        if (userId == null) {
            ToastManager.showWarning(requireContext(), "Usuário não logado")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val success = SettingsManager.setTheme(theme, userId)
            if (success) {
                ToastManager.showSuccess(requireContext(), "Tema atualizado")
            } else {
                ToastManager.showError(requireContext(), "Erro ao salvar tema")
            }
        }
    }

    private fun saveLanguageSetting(language: String) {
        val userId = UserSession.getUserId()
        if (userId == null) {
            ToastManager.showWarning(requireContext(), "Usuário não logado")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val success = SettingsManager.setLanguage(language, userId)
            if (success) {
                ToastManager.showSuccess(requireContext(), "Idioma atualizado")
            } else {
                ToastManager.showError(requireContext(), "Erro ao salvar idioma")
            }
        }
    }

    private fun saveNotificationsSetting(enabled: Boolean) {
        val userId = UserSession.getUserId()
        if (userId == null) {
            ToastManager.showWarning(requireContext(), "Usuário não logado")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val success = SettingsManager.setNotificationsEnabled(enabled, userId)
            if (!success) {
                ToastManager.showError(requireContext(), "Erro ao salvar configuração")
            }
        }
    }
}
