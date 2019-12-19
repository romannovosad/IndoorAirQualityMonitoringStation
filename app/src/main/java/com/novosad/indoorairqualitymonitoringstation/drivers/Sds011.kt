package com.novosad.indoorairqualitymonitoringstation.drivers

import android.os.Handler
import android.os.HandlerThread
import android.util.Log

import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.UartDevice
import com.google.android.things.pio.UartDeviceCallback

private const val TAG = "Sds011"

/**
 * Driver for Nova PM SDS011 sensor.
 *
 * This class will make sure sensor is connected on appropriate UART port and reports data in given interval.
 *
 * Every time data from the sensor is available, callback is triggered - latest incoming is saved in a global variable.
 * This means that when PM values are queried at any point,  their maximum possible age will be the length of the reporting interval.
 *
 * Sensor data sheet: https://cdn-reichelt.de/documents/datenblatt/X200/SDS011-DATASHEET.pdf
 * Sensor control protocol: https://nettigo.pl/attachments/415
 *
 * @param sds011 the port name sensor is connected to
 * @author Roman Novosad
 */
class Sds011(sds011: String) : AutoCloseable {

    private var inputThread: HandlerThread = HandlerThread("InputThread")
    private var inputHandler: Handler

    private var peripheralManager: PeripheralManager
    lateinit var sds011Sensor: UartDevice

    var mBuffer: ByteArray = ByteArray(10)

    private val callback = object : UartDeviceCallback {
        override fun onUartDeviceDataAvailable(uart: UartDevice): Boolean {
            // Queue up a data transfer
            val buffer = ByteArray(10)
            do {
                val read = sds011Sensor.read(buffer, buffer.size)
                if (read > 0) {
                    mBuffer = buffer.clone()
                }
            } while (read > 0)

            // Continue listening for more interrupts
            return true
        }

        override fun onUartDeviceError(uart: UartDevice?, error: Int) {
            Log.w(TAG, "$uart: Error event $error")
        }
    }

    init {
        // Create a background thread for I/O
        inputThread.start()
        inputHandler = Handler(inputThread.looper)

        // Attempt to access the UART device
        peripheralManager = PeripheralManager.getInstance()
        sds011Sensor = openUart(
            sds011
        )
        sds011Sensor.registerUartDeviceCallback(inputHandler, callback)

    }

    override fun close() {
        // Terminate the worker thread
        inputThread.quitSafely()

        // Attempt to close the UART device
        sds011Sensor.unregisterUartDeviceCallback(callback)
        sds011Sensor.close()
    }

    private fun openUart(name: String): UartDevice {
        return peripheralManager.openUartDevice(name).apply {
            // Configure the UART
            setBaudrate(9600)
            setDataSize(8)
            setParity(UartDevice.PARITY_NONE)
            setStopBits(1)
        }
    }

    fun setMode(mode: ByteArray) {
        inputHandler.post {
                sds011Sensor.write(mode, mode.size)
        }
    }

    fun readPM(): Array<Float> {
        val data = toUnsigned(mBuffer)
        val pm25 = ((data[3] shl 8) + data[2]).toFloat()/10
        val pm10 = ((data[5] shl 8) + data[4]).toFloat()/10
        return arrayOf(pm25,pm10)
    }

    private fun toUnsigned(bytes: ByteArray): IntArray {
        val data = IntArray(10)
        for (i in bytes.indices) {
            data[i] = java.lang.Byte.toUnsignedInt(bytes[i])
        }
        return data
    }

    companion object {
        val MODE_CONTINUOUS = byteArrayOf(
            0xAA.toByte(),
            0xB4.toByte(),
            0x08,
            0x01,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0xFF.toByte(),
            0xFF.toByte(),
            0x07,
            0xAB.toByte()
        )

        val MODE_REPORT = byteArrayOf(
            0xAA.toByte(),
            0xB4.toByte(),
            0x02,
            0x01,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0xFF.toByte(),
            0xFF.toByte(),
            0x01,
            0xAB.toByte()
        )

        val MODE_1MIN = byteArrayOf(
            0xAA.toByte(),
            0xB4.toByte(),
            0x08,
            0x01,
            0x01,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0xFF.toByte(),
            0xFF.toByte(),
            0x08,
            0xAB.toByte()
        )
        var MODE_10MIN = byteArrayOf(
            0xAA.toByte(),
            0xB4.toByte(),
            0x08,
            0x01,
            0x0A,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0xFF.toByte(),
            0xFF.toByte(),
            0x11,
            0xAB.toByte()
        )


    }
}
