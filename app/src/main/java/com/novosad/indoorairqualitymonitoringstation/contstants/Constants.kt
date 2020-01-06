package com.novosad.indoorairqualitymonitoringstation.contstants

object Constants {
    const val UPDATE_INTERVAL_EXTRA_SHORT = 1000 // 1s
    const val UPDATE_INTERVAL_SHORT = 3000 // 3s
    const val UPDATE_INTERVAL_MEDIUM = 5000 // 5s
    const val UPDATE_INTERVAL_LONG = 60000 // 1m
    const val UPDATE_INTERVAL_EXTRA_LONG = 600000 // 10m

    const val BMX280_PORT = "I2C1"
    const val CCS811_PORT = "I2C2"
    const val SDS011_PORT = "UART6" // PeripheralManager.getInstance().getUartDeviceList().get(0)

    const val TEMPERATURE_THRESHOLD_MID_HIGH = 25f
    const val TEMPERATURE_THRESHOLD_HIGH = 27f
    const val TEMPERATURE_THRESHOLD_MID_LOW = 19f
    const val TEMPERATURE_THRESHOLD_LOW = 17f
    const val HUMIDITY_THRESHOLD_MID_HIGH = 55f
    const val HUMIDITY_THRESHOLD_HIGH = 65f
    const val HUMIDITY_THRESHOLD_MID_LOW = 40f
    const val HUMIDITY_THRESHOLD_LOW = 30f
    const val CO2_THRESHOLD_MID_HIGH = 700
    const val CO2_THRESHOLD_HIGH = 1000
    const val TVOC_THRESHOLD_MID_HIGH = 25
    const val TVOC_THRESHOLD_HIGH = 45
    const val PM25_THRESHOLD_MID_HIGH = 40f
    const val PM25_THRESHOLD_HIGH = 65f
    const val PM10_THRESHOLD_MID_HIGH = 100f
    const val PM10_THRESHOLD_HIGH = 200f
}