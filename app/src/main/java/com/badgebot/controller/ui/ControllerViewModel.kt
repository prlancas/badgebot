package com.badgebot.controller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.badgebot.controller.ble.BleUartManager
import com.badgebot.controller.ble.ConnectionState
import com.badgebot.controller.ble.ControlButton
import com.badgebot.controller.ble.DiscoveredDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridges the [BleUartManager] to the Compose UI, exposing connection state and
 * the list of discovered devices as observable flows.
 */
class ControllerViewModel @JvmOverloads constructor(
    application: Application,
    private val bleManager: BleUartManager = BleUartManager(application),
) : AndroidViewModel(application) {

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bleManager.discoveredDevices

    val isBluetoothEnabled: Boolean
        get() = bleManager.isBluetoothEnabled

    fun startScan() = bleManager.startScan()

    fun stopScan() = bleManager.stopScan()

    fun connect(address: String) = bleManager.connect(address)

    fun disconnect() = bleManager.disconnect()

    fun onButtonPressed(button: ControlButton) = bleManager.sendControlButton(button, pressed = true)

    fun onButtonReleased(button: ControlButton) =
        bleManager.sendControlButton(button, pressed = false)

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
        bleManager.disconnect()
    }
}
