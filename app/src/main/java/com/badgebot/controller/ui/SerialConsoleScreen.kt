package com.badgebot.controller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.badgebot.controller.ble.SerialDirection
import com.badgebot.controller.ble.SerialEvent
import com.badgebot.controller.ble.SerialFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Raw serial console: shows the live TX/RX traffic and lets the user type and
 * send raw messages to the robot.
 */
@Composable
fun SerialConsoleScreen(
    events: List<SerialEvent>,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) listState.animateScrollToItem(events.size - 1)
    }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Serial monitor", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) { Text("Clear") }
        }

        if (events.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No serial traffic yet.\nDrive the robot or send a message below.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(events) { event -> SerialRow(event) }
            }
        }

        Spacer(Modifier.size(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Raw message") },
            )
            IconButton(
                onClick = {
                    val text = input
                    if (text.isNotEmpty()) {
                        onSend(text)
                        input = ""
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun SerialRow(event: SerialEvent) {
    val isTx = event.direction == SerialDirection.TX
    val color = if (isTx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${timeFormat.format(Date(event.timestampMillis))} ${if (isTx) "TX" else "RX"}",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "${event.text}   ${SerialFormat.toHex(event.data)}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}
