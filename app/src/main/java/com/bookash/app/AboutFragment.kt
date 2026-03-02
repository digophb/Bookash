package com.bookash.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment() {

    private lateinit var versionText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        versionText = view.findViewById(R.id.versionText)
        
        // Versão do app
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            versionText.text = "Versão ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "Versão 1.0.0"
        }

        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.optionTerms).setOnClickListener {
            openUrl("https://bookash.app/termos")
        }

        view.findViewById<View>(R.id.optionPrivacy).setOnClickListener {
            openUrl("https://bookash.app/privacidade")
        }

        view.findViewById<View>(R.id.optionContact).setOnClickListener {
            sendEmail()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Navegador não disponível
        }
    }

    private fun sendEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:suporte@bookash.app")
                putExtra(Intent.EXTRA_SUBJECT, "Suporte Bookash")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Email não disponível
        }
    }
}
