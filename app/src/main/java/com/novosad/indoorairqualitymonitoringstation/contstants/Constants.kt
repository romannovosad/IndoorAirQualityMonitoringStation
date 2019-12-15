package com.novosad.indoorairqualitymonitoringstation.contstants

object Constants {
    val UPDATE_INTERVAL_EXTRA_SHORT = 1000 // 1s
    val UPDATE_INTERVAL_SHORT = 10000 // 10s
    val UPDATE_INTERVAL_MEDIUM = 60000 // 1m
    val UPDATE_INTERVAL_LONG = 600000 // 10m
    val UPDATE_INTERVAL_EXTRA_LONG = 1200000 // 20m

    val BMX280_PORT = "I2C1"
    val CCS811_PORT = "I2C2"
    val SDS011_PORT = "UART6" // PeripheralManager.getInstance().getUartDeviceList().get(0)
}