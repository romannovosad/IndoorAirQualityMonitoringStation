package com.novosad.indoorairqualitymonitoringstation.activities

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.novosad.indoorairqualitymonitoringstation.R
import com.novosad.indoorairqualitymonitoringstation.contstants.Constants
import com.novosad.indoorairqualitymonitoringstation.drivers.Bmx280
import com.novosad.indoorairqualitymonitoringstation.drivers.Ccs811
import com.novosad.indoorairqualitymonitoringstation.drivers.Sds011
import com.novosad.indoorairqualitymonitoringstation.fragments.InfoDialogFragment
import com.novosad.indoorairqualitymonitoringstation.models.SensorData
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.time.format.DateTimeFormatter


private const val TAG = "Sensor"
private const val SHARED_PREFERENCES_KEY = "mode"
private const val PRIVATE_MODE = Context.MODE_PRIVATE

/**
 * This application will connect to BMX280, CCS811 and SDS011 sensors and collect sensor values,
 * which are then posted to UI in chosen intervals. Ui is interactive, so it will change depending
 * on a number of value thresholds defined based on their real world significance.
 *
 * For BMX280 driver see https://github.com/androidthings/contrib-drivers/tree/master/bmx280.
 * For CCS811 driver see https://github.com/rosterloh/androidthings-drivers/tree/master/ccs811.
 * SDS011 driver is written by me.
 *
 * @author Roman Novosad
 */
class MainActivity : FragmentActivity() {

    private val mBmx280 = Bmx280(Constants.BMX280_PORT)
    private val mCcs811 = Ccs811(Constants.CCS811_PORT)
    private val mSds011 = Sds011(Constants.SDS011_PORT)

    private var mInterval = Constants.UPDATE_INTERVAL_EXTRA_SHORT

    private var mHandler: Handler = Handler()

    private var mPeriodicSensorMeasurement: Runnable = object : Runnable {
        override fun run() {
            val sensorData = readValues()
            updateView(sensorData)
            mHandler.postDelayed(this, mInterval.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_KEY, PRIVATE_MODE)

        settings_button.setOnClickListener {
            val settingsPopup = PopupMenu(this@MainActivity, settings_button)
            settingsPopup.menuInflater.inflate(R.menu.popup_menu, settingsPopup.menu)
            settingsPopup.menu.getItem(sharedPref.getInt(SHARED_PREFERENCES_KEY, 0)).isChecked =
                true
            settingsPopup.setOnMenuItemClickListener { item ->

                item.isChecked = !item.isChecked

                sharedPref.edit().putInt(SHARED_PREFERENCES_KEY, item.order).commit()

                setIntervalMode(item.order)
                resetPeriodicMeasurements()

                false
            }
            settingsPopup.show()
        }

        initializeSensors()
        setIntervalMode(sharedPref.getInt(SHARED_PREFERENCES_KEY, 0))
        // start repeating measurements
        mPeriodicSensorMeasurement.run()

        info_button.setOnClickListener {
            // show the fragment fullscreen
            val transaction = supportFragmentManager.beginTransaction()
            // transition animation
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            // 'content' root view as the container for fullscreen fragment
            transaction
                .add(android.R.id.content, InfoDialogFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //stop repeating measurements
        mHandler.removeCallbacks(mPeriodicSensorMeasurement)
        closeSensors()
    }

    private fun initializeSensors() {
        try {
            // Configure driver settings
            mBmx280.temperatureOversampling = Bmx280.OVERSAMPLING_1X
            mBmx280.humidityOversampling = Bmx280.OVERSAMPLING_1X
            mBmx280.pressureOversampling = Bmx280.OVERSAMPLING_1X
            // Ensure the sensor is powered and not sleeping before trying to read from it
            mBmx280.setMode(Bmx280.MODE_NORMAL)
            mSds011.setMode(Sds011.MODE_REPORT)
            mCcs811.setMode(Ccs811.MODE_1S)
        } catch (e: IOException) {
            // couldn't configure the device...
            Log.e(TAG, "Error opening the sensors" + e.stackTrace)
        }
    }

    fun readValues(): SensorData {
        val sensorData = SensorData()

        try {
            sensorData.temperature = correctTemperature(mBmx280.readTemperature())
            sensorData.humidity = mBmx280.readHumidity()
            sensorData.pressure = mBmx280.readPressure()
        } catch (e: IOException) {
            Log.e(TAG, "Error reading temperature/humidity/pressure")
        }

        try {
            val ccs811Results = mCcs811.readAlgorithmResults().clone()
            sensorData.co2 = ccs811Results[0]
            sensorData.tvoc = ccs811Results[1]
        } catch (e: IOException) {
            Log.e(TAG, "Error reading co2/tvoc")
        }

        // sds011 will not throw an exception because the sensor decides when the data is available
        // if the sensor becomes disconnected, it will simply not provide any data
        sensorData.pm25 = mSds011.readPM()[0]
        sensorData.pm10 = mSds011.readPM()[1]

        return sensorData
    }

    private fun updateView(sensorData: SensorData) {
        updateValues(sensorData)
        updateGraphics(sensorData)
        updateStatusText(sensorData)
    }

    private fun updateValues(sensorData: SensorData) {
        val (temperature, humidity, pressure, co2, tvoc, pm25, pm10) = sensorData

        // update values
        temperature_value.text = String.format("%.1f", temperature)
        humidity_value.text = String.format("%.1f", humidity)
        pressure_value.text = String.format("%.1f", pressure)

        co2_value.text = co2.toString()
        tvoc_value.text = tvoc.toString()

        pm25_value.text = String.format("%.1f", pm25)
        pm10_value.text = String.format("%.1f", pm10)
    }

    private fun updateGraphics(sensorData: SensorData) {
        val (temperature, humidity, _, co2, tvoc, pm25, pm10) = sensorData

        // update status graphics
        if (temperature !in Constants.TEMPERATURE_THRESHOLD_LOW..Constants.TEMPERATURE_THRESHOLD_HIGH || humidity !in Constants.HUMIDITY_THRESHOLD_LOW..Constants.HUMIDITY_THRESHOLD_HIGH || co2 > Constants.CO2_THRESHOLD_HIGH || pm25 > Constants.PM25_THRESHOLD_HIGH || pm10 > Constants.PM10_THRESHOLD_HIGH || tvoc > Constants.TVOC_THRESHOLD_HIGH) {
            background_image.setImageResource(R.drawable.background_image_bad)
            status_image.setImageResource(R.drawable.bad_main_graphic)
            status.text = getString(R.string.status_bad)
        } else if (temperature !in Constants.TEMPERATURE_THRESHOLD_MID_LOW..Constants.TEMPERATURE_THRESHOLD_MID_HIGH || humidity !in Constants.HUMIDITY_THRESHOLD_MID_LOW..Constants.HUMIDITY_THRESHOLD_MID_HIGH || co2 > Constants.CO2_THRESHOLD_MID_HIGH || pm25 > Constants.PM25_THRESHOLD_MID_HIGH || pm10 > Constants.PM10_THRESHOLD_MID_HIGH || tvoc > Constants.TVOC_THRESHOLD_MID_HIGH) {
            background_image.setImageResource(R.drawable.background_image_fair)
            status_image.setImageResource(R.drawable.fair_main_graphic)
            status.text = getString(R.string.status_fair)
        } else {
            background_image.setImageResource(R.drawable.background_image_good)
            status_image.setImageResource(R.drawable.good_main_graphic)
            status.text = getString(R.string.status_good)
        }

        // update individual sensor status graphics
        if (temperature !in Constants.TEMPERATURE_THRESHOLD_MID_LOW..Constants.TEMPERATURE_THRESHOLD_MID_HIGH) {
            background_overlay_left.setImageResource(R.drawable.background_overlay_warning_left)
        } else {
            background_overlay_left.setImageResource(R.drawable.background_overlay_left)
        }

        if (humidity !in Constants.HUMIDITY_THRESHOLD_MID_LOW..Constants.HUMIDITY_THRESHOLD_MID_HIGH) {
            background_overlay_middle_1.setImageResource(R.drawable.background_overlay_warning_middle)
            humidity_icon.visibility = View.VISIBLE
        } else {
            background_overlay_middle_1.setImageResource(R.drawable.background_overlay_middle)
            humidity_icon.visibility = View.GONE
        }

        if (co2 > Constants.CO2_THRESHOLD_MID_HIGH) {
            background_overlay_middle_2.setImageResource(R.drawable.background_overlay_warning_middle)
            co2_icon.visibility = View.VISIBLE
        } else {
            background_overlay_middle_2.setImageResource(R.drawable.background_overlay_middle)
            co2_icon.visibility = View.GONE
        }

        if (pm25 > Constants.PM25_THRESHOLD_MID_HIGH) {
            background_overlay_middle_3.setImageResource(R.drawable.background_overlay_warning_middle)
            pm25_icon.visibility = View.VISIBLE
        } else {
            background_overlay_middle_3.setImageResource(R.drawable.background_overlay_middle)
            pm25_icon.visibility = View.GONE
        }

        if (tvoc > Constants.TVOC_THRESHOLD_MID_HIGH) {
            background_overlay_right_2.setImageResource(R.drawable.background_overlay_warning_right)
            tvoc_icon.visibility = View.VISIBLE
        } else {
            background_overlay_right_2.setImageResource(R.drawable.background_overlay_right)
            tvoc_icon.visibility = View.GONE
        }

        if (pm10 > Constants.PM10_THRESHOLD_MID_HIGH) {
            background_overlay_right_3.setImageResource(R.drawable.background_overlay_warning_right)
            pm10_icon.visibility = View.VISIBLE
        } else {
            background_overlay_right_3.setImageResource(R.drawable.background_overlay_right)
            pm10_icon.visibility = View.GONE
        }
    }

    private fun updateStatusText(sensorData: SensorData) {
        val (temperature, humidity, _, co2, tvoc, pm25, pm10) = sensorData

        status_label.text = ""

        if (temperature < Constants.TEMPERATURE_THRESHOLD_MID_LOW) {
            status_label.append(
                if (status_label.text.isBlank()) getString(R.string.low_temperaure_advice) else getString(
                    R.string.low_temperaure_advice_cont
                )
            )
        }
        if (temperature > Constants.TEMPERATURE_THRESHOLD_MID_HIGH) {
            status_label.append(
                if (status_label.text.isBlank()) getString(R.string.high_temperaure_advice) else getString(
                    R.string.high_temperaure_advice_cont
                )
            )
        }
        if (humidity < Constants.HUMIDITY_THRESHOLD_MID_LOW) {
            status_label.append(
                if (status_label.text.isBlank()) getString(R.string.low_humidity_advice) else getString(
                    R.string.low_humidity_advice_cont
                )
            )
        }
        if (humidity > Constants.HUMIDITY_THRESHOLD_MID_HIGH) {
            status_label.append(
                if (status_label.text.isBlank()) getString(R.string.high_humidity_advice) else getString(
                    R.string.high_humidity_advice_cont
                )
            )
        }
        if (co2 > Constants.CO2_THRESHOLD_MID_HIGH || tvoc > Constants.TVOC_THRESHOLD_MID_HIGH) {
            status_label.append(
                if (status_label.text.isBlank()) getString(R.string.high_co2_tvoc_advice) else getString(
                    R.string.high_co2_tvoc_advice_cont
                )
            )
        }
        if (pm25 > Constants.PM25_THRESHOLD_MID_HIGH || pm10 > Constants.PM10_THRESHOLD_MID_HIGH) {
            status_label.append(
                if (status_label.text.isBlank()) getString(R.string.high_pm25_pm10_advice) else getString(
                    R.string.high_pm25_pm10_advice_cont
                )
            )
        }
        if (temperature in Constants.TEMPERATURE_THRESHOLD_MID_LOW..Constants.TEMPERATURE_THRESHOLD_MID_HIGH && humidity in Constants.HUMIDITY_THRESHOLD_MID_LOW..Constants.HUMIDITY_THRESHOLD_MID_HIGH && co2 <= Constants.CO2_THRESHOLD_MID_HIGH && pm25 <= Constants.PM25_THRESHOLD_MID_HIGH && pm10 <= Constants.PM10_THRESHOLD_MID_HIGH && tvoc <= Constants.TVOC_THRESHOLD_MID_HIGH) {
            status_label.append(
                if (status_label.text.isBlank()) getString(R.string.everything_good) else getString(
                    R.string.everything_good_cont
                )
            )
        }
    }

    private fun setIntervalMode(id: Int) {
        when (id) {
            0 -> {
                mInterval = Constants.UPDATE_INTERVAL_EXTRA_SHORT
                mSds011.setMode(Sds011.MODE_CONTINUOUS)
            }
            1 -> {
                mInterval = Constants.UPDATE_INTERVAL_SHORT
                mSds011.setMode(Sds011.MODE_CONTINUOUS)
            }
            2 -> {
                mInterval = Constants.UPDATE_INTERVAL_MEDIUM
                mSds011.setMode(Sds011.MODE_CONTINUOUS)
            }
            3 -> {
                mInterval = Constants.UPDATE_INTERVAL_LONG
                mSds011.setMode(Sds011.MODE_1MIN)
            }
            4 -> {
                mInterval = Constants.UPDATE_INTERVAL_EXTRA_LONG
                mSds011.setMode(Sds011.MODE_1MIN)
            }
        }
    }

    private fun closeSensors() {
        // Close the environmental sensors when finished
        try {
            mBmx280.close()
            mCcs811.close()
            mSds011.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing the sensors")
        }
    }

    private fun resetPeriodicMeasurements() {
        mHandler.removeCallbacks(mPeriodicSensorMeasurement)
        mPeriodicSensorMeasurement.run()
    }

    private fun correctTemperature(temperature: Float): Float {
        return temperature - Constants.TEMPERATURE_CORRECTION_FACTOR
    }
}
