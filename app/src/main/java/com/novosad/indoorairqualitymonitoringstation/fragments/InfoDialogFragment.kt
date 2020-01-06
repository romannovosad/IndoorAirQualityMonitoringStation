package com.novosad.indoorairqualitymonitoringstation.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.novosad.indoorairqualitymonitoringstation.R
import com.novosad.indoorairqualitymonitoringstation.contstants.Constants
import kotlinx.android.synthetic.main.info_view_fragment.view.*

class InfoDialogFragment : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // inflate the layout as embedded fragment
        val view = inflater.inflate(R.layout.info_view_fragment, container, false)
        // dismiss dialog on click outside of viewing area
        displayInfoTexts(view)
        view.main_layout.setOnClickListener { this.dismiss() }
        return view
    }

    private fun displayInfoTexts(view: View) {
        view.info_dialog_temperature_description.text = getString(
            R.string.temperature_description,
            Constants.TEMPERATURE_THRESHOLD_LOW,
            Constants.TEMPERATURE_THRESHOLD_MID_LOW,
            Constants.TEMPERATURE_THRESHOLD_MID_HIGH,
            Constants.TEMPERATURE_THRESHOLD_HIGH
        )
        view.info_dialog_humidity_description.text = getString(
            R.string.humidity_description,
            Constants.HUMIDITY_THRESHOLD_LOW,
            Constants.HUMIDITY_THRESHOLD_MID_LOW,
            Constants.HUMIDITY_THRESHOLD_MID_HIGH,
            Constants.HUMIDITY_THRESHOLD_HIGH
        )
        view.info_dialog_co2_description.text = getString(
            R.string.co2_description,
            Constants.CO2_THRESHOLD_MID_HIGH,
            Constants.CO2_THRESHOLD_HIGH
        )
        view.info_dialog_tvoc_description.text = getString(
            R.string.tvoc_description,
            Constants.TVOC_THRESHOLD_MID_HIGH,
            Constants.TVOC_THRESHOLD_HIGH
        )
        view.info_dialog_pm25_description.text = getString(
            R.string.pm25_description,
            Constants.PM25_THRESHOLD_MID_HIGH,
            Constants.PM25_THRESHOLD_HIGH
        )
        view.info_dialog_pm10_description.text = getString(
            R.string.pm10_description,
            Constants.PM10_THRESHOLD_MID_HIGH,
            Constants.PM10_THRESHOLD_HIGH
        )
    }
}