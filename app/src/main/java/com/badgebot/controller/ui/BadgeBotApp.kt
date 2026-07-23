package com.badgebot.controller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.badgebot.controller.ble.ConnectionState
import kotlinx.coroutines.launch

private enum class Destination(val title: String, val icon: ImageVector) {
    CONTROL_PAD("Control Pad", Icons.Filled.Gamepad),
    SERIAL("Serial Console", Icons.AutoMirrored.Filled.List),
    MARKER("Print Marker", Icons.Filled.QrCode2),
    CAMERA("Camera & Path", Icons.Filled.CameraAlt),
}

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
        is ConnectionState.Connected -> ConnectedShell(
            deviceName = current.deviceName,
            viewModel = viewModel,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectedShell(
    deviceName: String,
    viewModel: ControllerViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var destination by remember { mutableStateOf(Destination.CONTROL_PAD) }

    val serialEvents by viewModel.serialLog.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isDriving by viewModel.isDriving.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "BadgeBot",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    "Connected to $deviceName",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.size(12.dp))

                Destination.entries.forEach { dest ->
                    NavigationDrawerItem(
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.title) },
                        selected = dest == destination,
                        onClick = {
                            destination = dest
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Recording toggle — works regardless of the active screen.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            tint = if (isRecording) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(Modifier.size(12.dp))
                        Text("Record serial")
                    }
                    Switch(
                        checked = isRecording,
                        onCheckedChange = { checked ->
                            if (checked) {
                                viewModel.startRecording()
                            } else {
                                viewModel.stopRecordingAndShare(context)
                            }
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(destination.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        TextButton(onClick = viewModel::disconnect) { Text("Disconnect") }
                    },
                )
            },
        ) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (destination) {
                Destination.CONTROL_PAD -> ControlPadContent(
                    onButtonPressed = viewModel::onButtonPressed,
                    onButtonReleased = viewModel::onButtonReleased,
                    modifier = contentModifier,
                )

                Destination.SERIAL -> SerialConsoleScreen(
                    events = serialEvents,
                    onSend = { viewModel.sendRaw(it) },
                    onClear = viewModel::clearSerialLog,
                    modifier = contentModifier,
                )

                Destination.MARKER -> MarkerScreen(modifier = contentModifier)

                Destination.CAMERA -> CameraPathScreen(
                    isDriving = isDriving,
                    onDrive = { path, tuning -> viewModel.drivePath(path, tuning) },
                    onStop = viewModel::stopDriving,
                    modifier = contentModifier,
                )
            }
        }
    }
}
