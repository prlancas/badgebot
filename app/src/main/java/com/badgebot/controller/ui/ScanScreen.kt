package com.badgebot.controller.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.badgebot.controller.ble.ConnectionState
import com.badgebot.controller.ble.DiscoveredDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ConnectionState,
    devices: List<DiscoveredDevice>,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (DiscoveredDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isScanning = state is ConnectionState.Scanning

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("BadgeBot") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (!hasPermissions) {
                PermissionPrompt(onRequestPermissions)
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = if (isScanning) onStopScan else onStartScan,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (isScanning) "Stop scan" else "Scan for robots")
                }
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            (state as? ConnectionState.Error)?.let {
                Spacer(Modifier.size(12.dp))
                Text(
                    text = it.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.size(16.dp))

            if (devices.isEmpty()) {
                EmptyState(isScanning)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.address }) { device ->
                        DeviceRow(device = device, onClick = { onConnect(device) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = "BadgeBot needs Bluetooth permission to find and drive your robot.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRequestPermissions) {
            Text("Grant permission")
        }
    }
}

@Composable
private fun EmptyState(isScanning: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (isScanning) {
                "Searching for UART-capable robots…"
            } else {
                "No robots yet. Tap \"Scan for robots\" to begin."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DeviceRow(device: DiscoveredDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${device.address} · ${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text("Connect")
            }
        }
    }
}
