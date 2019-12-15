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
import com.novosad.indoorairqualitymonitoringstation.contstants.Constants
import com.novosad.indoorairqualitymonitoringstation.R
import java.io.IOException


/**
 *
 *
 */
class MainActivity : Activity() {

    private val mBmx280 = Bmx280(Constants.BMX280_PORT)
    private val mCcs811 = Ccs811(Constants.CCS811_PORT)
    private val mSds011 = Sds011(Constants.SDS011_PORT)

    private var mInterval = Constants.UPDATE_INTERVAL_EXTRA_SHORT

    private var mHandler: Handler = Handler()

    private val SHARED_PREFERENCES_KEY = "mode"
    private val PRIVATE_MODE = Context.MODE_PRIVATE

    private var mPeriodicSensorMeasurement: Runnable = object : Runnable {
        override fun run() {
            readValues()
            mHandler.postDelayed(this, mInterval.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences(SHARED_PREFERENCES_KEY, PRIVATE_MODE)
        setIntervalMode(sharedPref.getInt(SHARED_PREFERENCES_KEY, 0))

        val settingsButton = findViewById<ImageView>(R.id.button)
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
        val temperatureView = findViewById<TextView>(R.id.temperature)
        val humidityView = findViewById<TextView>(R.id.humidity)
        val pressureView = findViewById<TextView>(R.id.pressure)
        val co2View = findViewById<TextView>(R.id.co2)
        val tvocView = findViewById<TextView>(R.id.tvoc)
        val pm25View = findViewById<TextView>(R.id.pm25)
        val pm10View = findViewById<TextView>(R.id.pm10)

        try {
            val temperature = mBmx280.readTemperature()
            val humidity = mBmx280.readHumidity()
            val pressure = mBmx280.readPressure()
            temperatureView.text = temperature.toString()
            humidityView.text = humidity.toString()
            pressureView.text = pressure.toString()
        } catch (e: IOException) {
            // error reading temperature/humidity/pressure
        }

        try {
            val co2 = mCcs811.readAlgorithmResults()[0]
            val tvoc = mCcs811.readAlgorithmResults()[1]
            co2View.text = co2.toString()
            tvocView.text = tvoc.toString()
        } catch (e: IOException) {
            // error reading co2/tvoc
        }

        try {
            val pm25 = mSds011.readPM()[0]
            val pm10 = mSds011.readPM()[1]
            pm25View.text = pm25.toString()
            pm10View.text = pm10.toString()
        } catch (e: IOException) {
            // error reading PM values
        }
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
