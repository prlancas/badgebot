package com.badgebot.controller.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.badgebot.controller.path.DriveTuning
import com.badgebot.controller.path.GroundPoint
import com.badgebot.controller.vision.ArucoGroundTracker
import com.badgebot.controller.vision.CameraFrames
import com.badgebot.controller.vision.GroundAnchor
import com.badgebot.controller.vision.Vec2
import java.util.concurrent.Executors

/**
 * Camera view that detects a printed ArUco marker, lets the user tap points on
 * the ground to draw a path pinned to the marker, then drives the robot along
 * that path (open-loop) using the planner.
 *
 * Note: the path is anchored to the ArUco marker (marker-based AR), so it stays
 * pinned while the marker is in view. Overlay alignment assumes the preview
 * fills the view; the drive-tuning and marker-size values can be calibrated
 * from the on-screen panel.
 */
@Composable
fun CameraPathScreen(
    isDriving: Boolean,
    onDrive: (List<GroundPoint>, DriveTuning) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    if (!hasCameraPermission) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Camera access is needed to detect the marker and draw a path.",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant camera access")
            }
        }
        return
    }

    val tracker = remember { ArucoGroundTracker(targetMarkerId = 0) }
    val anchor by tracker.anchor.collectAsState()
    val waypoints = remember { mutableStateListOf<GroundPoint>() }

    // Calibration values, adjustable at runtime from the panel below.
    var forwardSpeed by remember { mutableFloatStateOf(0.15f) }
    var turnRate by remember { mutableFloatStateOf(1.2f) }
    var markerCm by remember { mutableFloatStateOf(10f) }
    var showCalibration by remember { mutableStateOf(false) }

    LaunchedEffect(markerCm) {
        tracker.markerLengthMeters = (markerCm / 100f).toDouble()
    }

    CameraPreview(tracker = tracker, modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            PathOverlay(anchor = anchor, waypoints = waypoints)

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (showCalibration) {
                        CalibrationPanel(
                            forwardSpeed = forwardSpeed,
                            onForwardSpeed = { forwardSpeed = it },
                            turnRate = turnRate,
                            onTurnRate = { turnRate = it },
                            markerCm = markerCm,
                            onMarkerCm = { markerCm = it },
                        )
                        Spacer(Modifier.size(8.dp))
                    }

                    Text(
                        if (anchor != null) {
                            "Marker detected — tap the ground to add path points."
                        } else {
                            "Point the camera at printed marker #0."
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
                            Button(onClick = onStop) { Text("Stop") }
                        } else {
                            Button(
                                onClick = {
                                    onDrive(
                                        listOf(GroundPoint(0.0, 0.0)) + waypoints,
                                        DriveTuning(
                                            forwardSpeedMetersPerSecond = forwardSpeed.toDouble(),
                                            turnRateRadiansPerSecond = turnRate.toDouble(),
                                        ),
                                    )
                                },
                                enabled = anchor != null && waypoints.isNotEmpty(),
                            ) { Text("Drive path") }
                        }
                        OutlinedButton(
                            onClick = { waypoints.clear() },
                            enabled = waypoints.isNotEmpty() && !isDriving,
                        ) { Text("Clear") }
                        TextButton(onClick = { showCalibration = !showCalibration }) {
                            Text(if (showCalibration) "Hide tuning" else "Tune")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationPanel(
    forwardSpeed: Float,
    onForwardSpeed: (Float) -> Unit,
    turnRate: Float,
    onTurnRate: (Float) -> Unit,
    markerCm: Float,
    onMarkerCm: (Float) -> Unit,
) {
    Column {
        LabeledSlider(
            label = "Forward speed",
            valueText = "%.2f m/s".format(forwardSpeed),
            value = forwardSpeed,
            valueRange = 0.02f..0.6f,
            onValueChange = onForwardSpeed,
        )
        LabeledSlider(
            label = "Turn rate",
            valueText = "%.2f rad/s".format(turnRate),
            value = turnRate,
            valueRange = 0.2f..3.0f,
            onValueChange = onTurnRate,
        )
        LabeledSlider(
            label = "Marker size",
            valueText = "%.0f cm".format(markerCm),
            value = markerCm,
            valueRange = 2f..30f,
            onValueChange = onMarkerCm,
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(valueText, style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun CameraPreview(
    tracker: ArucoGroundTracker,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                try {
                    val gray = CameraFrames.toUprightGray(imageProxy)
                    tracker.detect(gray)
                    gray.release()
                } catch (t: Throwable) {
                    Log.e("BadgeBot", "Frame analysis failed", t)
                } finally {
                    imageProxy.close()
                }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (t: Throwable) {
                Log.e("BadgeBot", "Camera bind failed", t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        overlay()
    }
}

@Composable
private fun PathOverlay(
    anchor: GroundAnchor?,
    waypoints: SnapshotStateList<GroundPoint>,
) {
    // The anchor is recreated on every detected frame, so read the latest value
    // via rememberUpdatedState and key the gesture detector on something stable.
    // Otherwise the tap detector would restart each frame and never fire.
    val currentAnchor by rememberUpdatedState(anchor)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val activeAnchor = currentAnchor ?: return@detectTapGestures
                    val normalized = Vec2(
                        (offset.x / size.width).toDouble(),
                        (offset.y / size.height).toDouble(),
                    )
                    val ground = activeAnchor.normalizedImageToGround(normalized)
                    waypoints.add(GroundPoint(ground.x, ground.y))
                }
            },
    ) {
        val activeAnchor = currentAnchor ?: return@Canvas

        // Robot starts at the marker origin (0,0), then visits each waypoint.
        val groundPath = buildList {
            add(GroundPoint(0.0, 0.0))
            addAll(waypoints)
        }
        val screenPoints = groundPath.map { gp ->
            val n = activeAnchor.groundToNormalizedImage(Vec2(gp.x, gp.y))
            Offset((n.x * size.width).toFloat(), (n.y * size.height).toFloat())
        }

        for (i in 0 until screenPoints.size - 1) {
            drawLine(
                color = Color(0xFF4CAF50),
                start = screenPoints[i],
                end = screenPoints[i + 1],
                strokeWidth = 8f,
            )
        }
        screenPoints.forEachIndexed { index, p ->
            drawCircle(
                color = if (index == 0) Color(0xFFFFC107) else Color(0xFF4CAF50),
                radius = if (index == 0) 16f else 12f,
                center = p,
            )
        }
    }
}
