package com.bookash.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class GeneralFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeValue = view.findViewById<TextView>(R.id.themeValue)
        val languageValue = view.findViewById<TextView>(R.id.languageValue)
        val notificationsSwitch = view.findViewById<SwitchMaterial>(R.id.notificationsSwitch)

        // Valores padrão
        themeValue?.text = "Sistema"
        languageValue?.text = "Português"
        notificationsSwitch?.isChecked = true
        
        // Click listeners
        view.findViewById<View>(R.id.cardTheme)?.setOnClickListener {
            showThemeDialog(themeValue)
        }
        
        view.findViewById<View>(R.id.cardLanguage)?.setOnClickListener {
            showLanguageDialog(languageValue)
        }
    }
    
    private fun showThemeDialog(themeValue: TextView) {
        val themes = arrayOf("Claro", "Escuro", "Sistema")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Tema")
            .setSingleChoiceItems(themes, 2) { dialog, which ->
                themeValue.text = themes[which]
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showLanguageDialog(languageValue: TextView) {
        val languages = arrayOf("Português", "English", "Español")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Idioma")
            .setSingleChoiceItems(languages, 0) { dialog, which ->
                languageValue.text = languages[which]
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
