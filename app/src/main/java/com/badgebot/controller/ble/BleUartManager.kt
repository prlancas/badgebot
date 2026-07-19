package com.badgebot.controller.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Wraps Android's BLE APIs to scan for, connect to, and send commands to a
 * Bluefruit UART-capable peripheral.
 *
 * Callers are responsible for holding the required runtime permissions before
 * invoking [startScan] or [connect]; the relevant framework calls are annotated
 * with [SuppressLint] accordingly.
 */
class BleUartManager(context: Context) {

    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var connectingName: String = ""
    private var isScanning = false

    /** True when Bluetooth is available and switched on. */
    val isBluetoothEnabled: Boolean
        get() = adapter?.isEnabled == true

    // region Scanning

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            @SuppressLint("MissingPermission")
            val name = result.scanRecord?.deviceName ?: device.name
            val found = DiscoveredDevice(
                address = device.address,
                name = name,
                rssi = result.rssi,
            )
            _discoveredDevices.update { current ->
                val existingIndex = current.indexOfFirst { it.address == found.address }
                if (existingIndex >= 0) {
                    current.toMutableList().apply { this[existingIndex] = found }
                } else {
                    current + found
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionState.value = ConnectionState.Error("Scan failed (code $errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = ConnectionState.Error("Bluetooth is not available")
            return
        }
        if (isScanning) return

        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.UART_SERVICE_UUID))
                .build(),
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        isScanning = true
        scanner.startScan(filters, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    // endregion

    // region Connection

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val adapter = this.adapter ?: run {
            _connectionState.value = ConnectionState.Error("Bluetooth is not available")
            return
        }
        stopScan()

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            _connectionState.value = ConnectionState.Error("Invalid device address")
            return
        }

        connectingName = _discoveredDevices.value
            .firstOrNull { it.address == address }
            ?.displayName
            ?: device.name
            ?: "BadgeBot"

        _connectionState.value = ConnectionState.Connecting(connectingName)
        closeGatt()
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        closeGatt()
        _connectionState.value = ConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        txCharacteristic = null
        gatt?.close()
        gatt = null
    }

    private val gattCallback = object : android.bluetooth.BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Error("Connection error (status $status)")
                closeGatt()
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    closeGatt()
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Error("Service discovery failed")
                return
            }
            val service = gatt.getService(BleConstants.UART_SERVICE_UUID)
            val tx = service?.getCharacteristic(BleConstants.UART_TX_CHARACTERISTIC_UUID)
            if (tx == null) {
                _connectionState.value = ConnectionState.Error("Device is not UART capable")
                return
            }
            txCharacteristic = tx
            _connectionState.value = ConnectionState.Connected(connectingName)
        }
    }

    // endregion

    // region Sending

    /**
     * Sends a control-pad command for [button] (pressed/released) to the robot.
     * Returns true if the write was dispatched, false if there is no active
     * connection.
     */
    fun sendControlButton(button: ControlButton, pressed: Boolean): Boolean {
        val packet = UartProtocol.controllerPadCommand(button, pressed)
        return writeTx(packet)
    }

    @SuppressLint("MissingPermission")
    private fun writeTx(data: ByteArray): Boolean {
        val gatt = this.gatt ?: return false
        val characteristic = txCharacteristic ?: return false

        val writeType = if (characteristic.properties and
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
        ) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, data, writeType) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    // endregion
}
