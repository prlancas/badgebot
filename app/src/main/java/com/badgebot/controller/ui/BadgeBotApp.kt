package com.badgebot.controller.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.badgebot.controller.ble.ConnectionState

@Composable
fun BadgeBotApp(
    viewModel: ControllerViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.connectionState.collectAsState()
    val devices by viewModel.discoveredDevices.collectAsState()

    when (val current = state) {
        is ConnectionState.Connected -> ControlPadScreen(
            deviceName = current.deviceName,
            onButtonPressed = { viewModel.onButtonPressed(it) },
            onButtonReleased = { viewModel.onButtonReleased(it) },
            onDisconnect = viewModel::disconnect,
            modifier = modifier,
        )

        else -> ScanScreen(
            state = state,
            devices = devices,
            hasPermissions = hasPermissions,
            onRequestPermissions = onRequestPermissions,
            onStartScan = viewModel::startScan,
            onStopScan = viewModel::stopScan,
            onConnect = { viewModel.connect(it.address) },
            modifier = modifier,
        )
    }
}
