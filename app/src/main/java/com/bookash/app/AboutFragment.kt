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

        view.findViewById<View>(R.id.card_share)?.setOnClickListener { shareApp() }
        view.findViewById<View>(R.id.card_rate)?.setOnClickListener { openPlayStore() }
        view.findViewById<View>(R.id.card_privacy)?.setOnClickListener { openPrivacyPolicy() }
        view.findViewById<View>(R.id.card_terms)?.setOnClickListener { openTerms() }
        
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            view.findViewById<TextView>(R.id.versionValue)?.text = pInfo.versionName
        } catch (e: Exception) {
            view.findViewById<TextView>(R.id.versionValue)?.text = "1.0.0"
        }
    }

    private fun shareApp() {
        val shareText = "Check out Bookash - o app de gestão financeira pessoal!\n\n📊 Controle gastos e receitas\n💰 Gerencie múltiplas contas\n🏷️ Organize com categorias e tags\n\nBaixe agora: https://play.google.com/store/apps/details?id=com.bookash.app"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Bookash - Gestão Financeira")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar via"))
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.bookash.app")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.bookash.app")))
        }
    }

    private fun openPrivacyPolicy() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bookash.app/privacy")))
    }

    private fun openTerms() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bookash.app/terms")))
    }
}
