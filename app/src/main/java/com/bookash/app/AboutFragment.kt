package com.bookash.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment() {
    
    companion object {
        private const val TAG = "AboutFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")
        return try {
            inflater.inflate(R.layout.fragment_about, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onCreateView: ${e.message}", e)
            View(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        
        try {
            view.findViewById<View>(R.id.optionTerms)?.setOnClickListener { 
                openUrl("https://bookash.app/terms") 
            }
            view.findViewById<View>(R.id.optionPrivacy)?.setOnClickListener { 
                openUrl("https://bookash.app/privacy") 
            }
            view.findViewById<View>(R.id.optionContact)?.setOnClickListener { 
                openEmail() 
            }
            view.findViewById<View>(R.id.optionLogout)?.setOnClickListener { 
                logout() 
            }
            
            try {
                val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                view.findViewById<TextView>(R.id.versionText)?.text = "Versão ${pInfo.versionName}"
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao obter versão: ${e.message}")
                view.findViewById<TextView>(R.id.versionText)?.text = "Versão 1.0.0"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onViewCreated: ${e.message}", e)
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir URL: ${e.message}")
        }
    }

    private fun openEmail() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:suporte@bookash.app")
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Suporte Bookash")
            }
            startActivity(android.content.Intent.createChooser(intent, "Enviar e-mail"))
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir email: ${e.message}")
        }
    }

    private fun logout() {
        try {
            UserSession.signOut()
            val intent = android.content.Intent(requireContext(), LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no logout: ${e.message}")
        }
    }
}
