package com.badgebot.controller.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.badgebot.controller.ble.BleUartManager
import com.badgebot.controller.ble.ConnectionState
import com.badgebot.controller.ble.ControlButton
import com.badgebot.controller.ble.DiscoveredDevice
import com.badgebot.controller.ble.SerialEvent
import com.badgebot.controller.ble.SerialRecording
import com.badgebot.controller.path.DriveTuning
import com.badgebot.controller.path.GroundPoint
import com.badgebot.controller.path.PathDriver
import com.badgebot.controller.path.PathPlanner
import com.badgebot.controller.util.Sharing
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bridges the [BleUartManager] to the Compose UI, exposing connection state,
 * discovered devices, serial traffic and recording controls as observable flows.
 */
class ControllerViewModel @JvmOverloads constructor(
    application: Application,
    private val bleManager: BleUartManager = BleUartManager(application),
) : AndroidViewModel(application) {

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = bleManager.discoveredDevices
    val serialLog: StateFlow<List<SerialEvent>> = bleManager.serialLog
    val isRecording: StateFlow<Boolean> = bleManager.isRecording

    val isBluetoothEnabled: Boolean
        get() = bleManager.isBluetoothEnabled

    fun startScan() = bleManager.startScan()

    fun stopScan() = bleManager.stopScan()

    fun connect(address: String) = bleManager.connect(address)

    fun disconnect() = bleManager.disconnect()

    fun onButtonPressed(button: ControlButton) = bleManager.sendControlButton(button, pressed = true)

    fun onButtonReleased(button: ControlButton) =
        bleManager.sendControlButton(button, pressed = false)

    // region Serial console

    fun sendRaw(text: String): Boolean = bleManager.sendRaw(text)

    fun clearSerialLog() = bleManager.clearSerialLog()

    // endregion

    // region Recording

    fun startRecording() = bleManager.startRecording()

    /** Stops recording and opens a share sheet with the captured transcript. */
    fun stopRecordingAndShare(context: Context) {
        val events = bleManager.stopRecording()
        val transcript = SerialRecording.format(events)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        Sharing.shareText(
            context = context,
            text = transcript,
            fileName = "badgebot-serial-$stamp.txt",
            title = "Share serial recording",
        )
    }

    // endregion

    // region Path following

    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

    private var driveJob: Job? = null

    /**
     * Plans and drives [path] (ground waypoints in metres, marker frame),
     * open-loop, using the manual arrow commands. The robot is assumed to start
     * at the first waypoint facing marker-forward (+x).
     */
    fun drivePath(path: List<GroundPoint>, tuning: DriveTuning = DriveTuning()) {
        val commands = PathPlanner.plan(path, initialHeadingRadians = 0.0, tuning = tuning)
        if (commands.isEmpty()) return
        driveJob?.cancel()
        val driver = PathDriver(
            press = { bleManager.sendControlButton(it, pressed = true) },
            release = { bleManager.sendControlButton(it, pressed = false) },
        )
        _isDriving.value = true
        driveJob = viewModelScope.launch {
            try {
                driver.drive(commands)
            } finally {
                _isDriving.value = false
            }
        }
    }

    fun stopDriving() {
        driveJob?.cancel()
        driveJob = null
        _isDriving.value = false
    }

    // endregion

    override fun onCleared() {
        super.onCleared()
        stopDriving()
        bleManager.stopScan()
        bleManager.disconnect()
    }
}
