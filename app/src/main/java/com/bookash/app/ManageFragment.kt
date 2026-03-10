package com.bookash.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.card.MaterialCardView
import androidx.fragment.app.Fragment

class ManageFragment : Fragment() {
    
    companion object {
        private const val TAG = "ManageFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")
        return try {
            inflater.inflate(R.layout.fragment_manage, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onCreateView: ${e.message}", e)
            View(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        
        try {
            view.findViewById<MaterialCardView>(R.id.optionCategories)?.setOnClickListener {
                startActivity(android.content.Intent(requireContext(), CategoriesActivity::class.java))
            }

            view.findViewById<MaterialCardView>(R.id.optionAccounts)?.setOnClickListener {
                startActivity(android.content.Intent(requireContext(), AccountsActivity::class.java))
            }

            view.findViewById<MaterialCardView>(R.id.optionTags)?.setOnClickListener {
                startActivity(android.content.Intent(requireContext(), TagsActivity::class.java))
            }

            view.findViewById<MaterialCardView>(R.id.optionGoals)?.setOnClickListener {
                startActivity(android.content.Intent(requireContext(), GoalsActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onViewCreated: ${e.message}", e)
        }
    }
}
