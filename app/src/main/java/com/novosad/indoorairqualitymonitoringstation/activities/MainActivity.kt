package com.novosad.indoorairqualitymonitoringstation.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.novosad.indoorairqualitymonitoringstation.drivers.Bmx280
import com.novosad.indoorairqualitymonitoringstation.drivers.Ccs811
import com.novosad.indoorairqualitymonitoringstation.drivers.Sds011
import com.novosad.indoorairqualitymonitoringstation.models.SensorData
import com.novosad.indoorairqualitymonitoringstation.contstants.Constants
import com.novosad.indoorairqualitymonitoringstation.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


/**
 *
 *
 */
class MainActivity : Activity() {

    private val mBmx280 = Bmx280(Constants.BMX280_PORT)
    private val mCcs811 = Ccs811(Constants.CCS811_PORT)
    private val mSds011 = Sds011(Constants.SDS011_PORT)

    private val sensorData = SensorData()

    private var mInterval = Constants.UPDATE_INTERVAL_EXTRA_SHORT

    private var mHandler: Handler = Handler()

    private val SHARED_PREFERENCES_KEY = "mode"
    private val PRIVATE_MODE = Context.MODE_PRIVATE

    private var mPeriodicSensorMeasurement: Runnable = object : Runnable {
        override fun run() {
            readValues()
            updateView(sensorData)
            mHandler.postDelayed(this, mInterval.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_KEY, PRIVATE_MODE)
        setIntervalMode(sharedPref.getInt(SHARED_PREFERENCES_KEY, 0))

        val settingsButton = findViewById<ImageView>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val settingsPopup = PopupMenu(this@MainActivity, settingsButton)
            settingsPopup.menuInflater.inflate(R.menu.popup_menu, settingsPopup.menu)
            settingsPopup.menu.getItem(sharedPref.getInt(SHARED_PREFERENCES_KEY, 0)).isChecked = true
            settingsPopup.setOnMenuItemClickListener { item ->

                item.isChecked = !item.isChecked

                sharedPref.edit().putInt(SHARED_PREFERENCES_KEY, item.order).commit()

                setIntervalMode(item.order)
                resetPeriodicMeasurements()

                // prevent menu from closing on item click
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                item.actionView = View(this)
                item.setOnActionExpandListener(object :
                    MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        return false
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        return false
                    }
                })
                false
            }
            settingsPopup.show()
        }

        initializeSensors()
        // start repeating measurements
        mPeriodicSensorMeasurement.run()
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
        } catch (e: IOException) {
            // couldn't configure the device...
        }
    }

    fun readValues() {
        try {
            sensorData.temperature = mBmx280.readTemperature()
            sensorData.humidity = mBmx280.readHumidity()
            sensorData.pressure = mBmx280.readPressure()
        } catch (e: IOException) {
            // error reading temperature/humidity/pressure
        }

        try {
            sensorData.co2 = mCcs811.readAlgorithmResults()[0]
            sensorData.tvoc = mCcs811.readAlgorithmResults()[1]
        } catch (e: IOException) {
            // error reading co2/tvoc
        }

        try {
            sensorData.pm25 = mSds011.readPM()[0]
            sensorData.pm10 = mSds011.readPM()[1]
        } catch (e: IOException) {
            // error reading PM values
        }
    }

    private fun updateView(sensorData: SensorData) {
        val temperatureView = temperature
        val humidityView = humidity
        val pressureView = pressure
        val co2View = co2
        val tvocView = tvoc
        val pm25View = pm25
        val pm10View = pm10

        temperatureView.text = String.format("%.1f", sensorData.temperature)
        humidityView.text = String.format("%.1f", sensorData.temperature)
        pressureView.text = String.format("%.1f", sensorData.pressure)

        co2View.text = sensorData.co2.toString()
        tvocView.text = sensorData.tvoc.toString()

        pm25View.text = String.format("%.1f", sensorData.pm25)
        pm10View.text = String.format("%.1f", sensorData.pm10)
    }

    private fun setIntervalMode(id: Int) {
        when (id) {
            0 -> {
                mInterval = Constants.UPDATE_INTERVAL_EXTRA_SHORT
                mCcs811.setMode(Ccs811.MODE_1S)
                mSds011.setMode(Sds011.MODE_CONTINUOUS)
            }
            1 -> {
                mInterval = Constants.UPDATE_INTERVAL_SHORT
                mCcs811.setMode(Ccs811.MODE_10S)
                mSds011.setMode(Sds011.MODE_CONTINUOUS)
            }
            2 -> {
                mInterval = Constants.UPDATE_INTERVAL_MEDIUM
                mCcs811.setMode(Ccs811.MODE_60S)
                mSds011.setMode(Sds011.MODE_1MIN)
            }
            3 -> {
                mInterval = Constants.UPDATE_INTERVAL_LONG
                mCcs811.setMode(Ccs811.MODE_60S)
                mSds011.setMode(Sds011.MODE_10MIN)
            }
            4 -> {
                mInterval = Constants.UPDATE_INTERVAL_EXTRA_LONG
                mCcs811.setMode(Ccs811.MODE_60S)
                mSds011.setMode(Sds011.MODE_10MIN)
            }
        }
    }

    private fun closeSensors() {
        // Close the environmental sensor when finished:
        try {
            mBmx280.close()
            mCcs811.close()
            mSds011.close()
        } catch (e: IOException) {
            // error closing sensor
        }
    }

    private fun resetPeriodicMeasurements() {
        mHandler.removeCallbacks(mPeriodicSensorMeasurement)
        mPeriodicSensorMeasurement.run()
    }
}
