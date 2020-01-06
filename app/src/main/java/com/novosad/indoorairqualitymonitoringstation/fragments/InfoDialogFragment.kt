package com.novosad.indoorairqualitymonitoringstation.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.novosad.indoorairqualitymonitoringstation.R
import kotlinx.android.synthetic.main.info_view_fragment.view.*

class InfoDialogFragment : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // inflate the layout as embedded fragment
        val view = inflater.inflate(R.layout.info_view_fragment, container, false)
        // dismiss dialog on click outside of viewing area
        view.main_layout.setOnClickListener { this.dismiss() }
        return view
    }
}