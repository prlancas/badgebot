package com.badgebot.controller.ble

import java.util.UUID

/**
 * UUIDs for the Nordic UART Service (NUS) used by Adafruit Bluefruit UART
 * capable peripherals.
 */
object BleConstants {
    /** The UART service advertised by the peripheral. */
    val UART_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

    /** Characteristic the central writes to (phone -> robot). */
    val UART_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    /** Characteristic the central subscribes to (robot -> phone). */
    val UART_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    /** Standard Client Characteristic Configuration Descriptor UUID. */
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Maximum bytes to write in a single characteristic write for classic BLE. */
    const val DEFAULT_MTU_PAYLOAD: Int = 20
}
