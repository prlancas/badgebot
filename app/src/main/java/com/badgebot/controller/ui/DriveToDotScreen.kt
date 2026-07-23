package com.badgebot.controller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.badgebot.controller.ble.ControlButton
import com.badgebot.controller.vision.ArucoGroundTracker
import com.badgebot.controller.vision.Vec2
import com.badgebot.controller.vision.VisualServoController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Shows a fixed target dot in the centre of the camera view and drives the robot
 * to whatever ground spot currently sits under it. Because the target is the
 * screen centre, panning the camera re-aims the robot continuously. Direction is
 * auto-calibrated by a short forward nudge, so a mis-oriented marker is handled.
 */
@Composable
fun DriveToDotScreen(
    press: (ControlButton) -> Unit,
    release: (ControlButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraGate(modifier) {
        val tracker = remember { ArucoGroundTracker(targetMarkerId = 0) }
        val anchor by tracker.anchor.collectAsState()

        val controller = remember(tracker) {
            VisualServoController(tracker.anchor, press, release)
        }
        val scope = rememberCoroutineScope()
        var driveJob by remember { mutableStateOf<Job?>(null) }
        var isDriving by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            onDispose {
                driveJob?.cancel()
                controller.stopAll()
            }
        }

        CameraPreview(tracker = tracker, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                CenterDotOverlay(markerCenter = anchor?.markerCenterNormalized())

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            when {
                                isDriving -> "Driving to the dot — aiming for the screen centre."
                                anchor != null -> "Marker detected — press to drive to the dot."
                                else -> "Point the camera at printed marker #0."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.size(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isDriving) {
                                Button(onClick = {
                                    driveJob?.cancel()
                                    controller.stopAll()
                                    isDriving = false
                                }) { Text("Stop") }
                            } else {
                                Button(
                                    onClick = {
                                        isDriving = true
                                        driveJob = scope.launch {
                                            controller.resetCalibration()
                                            try {
                                                controller.driveTo(stopWhenReached = false) {
                                                    Vec2(0.5, 0.5)
                                                }
                                            } finally {
                                                controller.stopAll()
                                                isDriving = false
                                            }
                                        }
                                    },
                                    enabled = anchor != null,
                                ) { Text("Drive to dot") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterDotOverlay(markerCenter: Vec2?) {
    val center by rememberUpdatedState(markerCenter)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val target = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color(0x33F44336), radius = 34f, center = target)
        drawCircle(color = Color(0xFFF44336), radius = 16f, center = target)
        center?.let { c ->
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 14f,
                center = Offset((c.x * size.width).toFloat(), (c.y * size.height).toFloat()),
            )
        }
    }
}
