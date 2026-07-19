package com.badgebot.controller.ble

import android.Manifest
import android.os.Build

/** Helper describing which runtime permissions are needed for BLE on this device. */
object BlePermissions {
    /** The runtime permissions required to scan for and connect to BLE devices. */
    val required: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
}
