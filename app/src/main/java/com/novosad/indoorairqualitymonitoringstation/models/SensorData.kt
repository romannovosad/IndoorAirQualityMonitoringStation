package com.novosad.indoorairqualitymonitoringstation.models

data class SensorData(
    var temperature: Float = 0f,
    var humidity: Float = 0f,
    var pressure: Float = 0f,
    var co2: Int = 0,
    var tvoc: Int = 0,
    var pm25: Float = 0f,
    var pm10: Float = 0f
)