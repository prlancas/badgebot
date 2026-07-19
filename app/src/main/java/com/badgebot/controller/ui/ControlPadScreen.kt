package com.badgebot.controller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.badgebot.controller.ble.ControlButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPadScreen(
    deviceName: String,
    onButtonPressed: (ControlButton) -> Unit,
    onButtonReleased: (ControlButton) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Driving $deviceName") },
                actions = {
                    TextButton(onClick = onDisconnect) { Text("Disconnect") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DirectionButton(
                button = ControlButton.UP,
                icon = Icons.Filled.KeyboardArrowUp,
                label = "Up",
                onPressed = onButtonPressed,
                onReleased = onButtonReleased,
            )
            Spacer(Modifier.size(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DirectionButton(
                    button = ControlButton.LEFT,
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    label = "Left",
                    onPressed = onButtonPressed,
                    onReleased = onButtonReleased,
                )
                Spacer(Modifier.size(16.dp))
                DirectionButton(
                    button = ControlButton.RIGHT,
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    label = "Right",
                    onPressed = onButtonPressed,
                    onReleased = onButtonReleased,
                )
            }
            Spacer(Modifier.size(16.dp))
            DirectionButton(
                button = ControlButton.DOWN,
                icon = Icons.Filled.KeyboardArrowDown,
                label = "Down",
                onPressed = onButtonPressed,
                onReleased = onButtonReleased,
            )
        }
    }
}

@Composable
private fun DirectionButton(
    button: ControlButton,
    icon: ImageVector,
    label: String,
    onPressed: (ControlButton) -> Unit,
    onReleased: (ControlButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val background = if (pressed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = if (pressed) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(96.dp)
            .background(background, RoundedCornerShape(20.dp))
            .semantics { contentDescription = label }
            .pointerInput(button) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPressed(button)
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                            onReleased(button)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(56.dp),
        )
    }
}
