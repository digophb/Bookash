package com.bookash.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

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
        // TODO: Carregar do SupabaseService.getSettings()
        themeValue.text = "Sistema"
        languageValue.text = "Português"
        notificationsSwitch.isChecked = true
    }

    private fun setupClickListeners() {
        view?.findViewById<View>(R.id.optionTheme)?.setOnClickListener {
            showThemeDialog()
        }

        view?.findViewById<View>(R.id.optionLanguage)?.setOnClickListener {
            showLanguageDialog()
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Salvar no SupabaseService.updateSettings()
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Claro", "Escuro", "Sistema")
        val currentIndex = when (themeValue.text.toString()) {
            "Claro" -> 0
            "Escuro" -> 1
            else -> 2
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Tema")
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                themeValue.text = themes[which]
                // TODO: Aplicar tema e salvar
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Português", "English", "Español")
        val currentIndex = when (languageValue.text.toString()) {
            "Português" -> 0
            "English" -> 1
            else -> 2
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Idioma")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                languageValue.text = languages[which]
                // TODO: Salvar preferência
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
