package com.bookash.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class ManageFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.optionCategories)?.setOnClickListener {
            startActivity(Intent(requireContext(), CategoriesActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.optionAccounts)?.setOnClickListener {
            startActivity(Intent(requireContext(), AccountsActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.optionTags)?.setOnClickListener {
            startActivity(Intent(requireContext(), TagsActivity::class.java))
        }
    }
}
