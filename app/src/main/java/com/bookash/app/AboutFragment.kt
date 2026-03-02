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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.optionTerms)?.setOnClickListener { openTerms() }
        view.findViewById<View>(R.id.optionPrivacy)?.setOnClickListener { openPrivacyPolicy() }
        view.findViewById<View>(R.id.optionContact)?.setOnClickListener { openContact() }
        view.findViewById<View>(R.id.optionLogout)?.setOnClickListener { logout() }
        
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            view.findViewById<TextView>(R.id.versionText)?.text = "Versão ${pInfo.versionName}"
        } catch (e: Exception) {
            view.findViewById<TextView>(R.id.versionText)?.text = "Versão 1.0.0"
        }
    }

    private fun openTerms() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bookash.app/terms")))
    }

    private fun openPrivacyPolicy() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bookash.app/privacy")))
    }

    private fun openContact() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:suporte@bookash.app")
            putExtra(Intent.EXTRA_SUBJECT, "Suporte Bookash")
        }
        startActivity(Intent.createChooser(intent, "Enviar e-mail"))
    }

    private fun logout() {
        UserSession.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
