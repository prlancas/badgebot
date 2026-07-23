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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.badgebot.controller.ble.ControlButton
import com.badgebot.controller.vision.ArucoGroundTracker
import com.badgebot.controller.vision.CameraFrames
import com.badgebot.controller.vision.GroundAnchor
import com.badgebot.controller.vision.Vec2
import com.badgebot.controller.vision.VisualServoController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Camera view that detects a printed ArUco marker and lets the user tap points
 * to draw a path, then follows it with **closed-loop** control: the robot is
 * tracked by its marker and steered to each point in turn, only advancing once
 * the marker reaches the current point. Forward/turn directions are learned from
 * motion, so nothing needs calibrating and a mis-oriented marker is handled.
 *
 * The drawn overlay is pinned to screen positions; keep the camera roughly still
 * while following (true world-locked AR would require ARCore SLAM).
 */
@Composable
fun CameraPathScreen(
    press: (ControlButton) -> Unit,
    release: (ControlButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraGate(modifier) {
        val tracker = remember { ArucoGroundTracker(targetMarkerId = 0) }
        val anchor by tracker.anchor.collectAsState()
        val points = remember { mutableStateListOf<Vec2>() }

        val controller = remember(tracker) {
            VisualServoController(tracker.anchor, press, release)
        }
        val scope = rememberCoroutineScope()
        var driveJob by remember { mutableStateOf<Job?>(null) }
        var isFollowing by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            onDispose {
                driveJob?.cancel()
                controller.stopAll()
            }
        }

        CameraPreview(tracker = tracker, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                PathOverlay(
                    anchor = anchor,
                    points = points,
                    enableTaps = !isFollowing,
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            when {
                                isFollowing -> "Following path — tracking marker to each point."
                                anchor != null -> "Marker detected — tap to add path points."
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
                            if (isFollowing) {
                                Button(onClick = {
                                    driveJob?.cancel()
                                    controller.stopAll()
                                    isFollowing = false
                                }) { Text("Stop") }
                            } else {
                                Button(
                                    onClick = {
                                        isFollowing = true
                                        driveJob = scope.launch {
                                            controller.resetCalibration()
                                            try {
                                                for (i in points.indices) {
                                                    val reached =
                                                        controller.driveTo { points.getOrNull(i) }
                                                    if (!reached) break
                                                }
                                            } finally {
                                                controller.stopAll()
                                                isFollowing = false
                                            }
                                        }
                                    },
                                    enabled = anchor != null && points.isNotEmpty(),
                                ) { Text("Follow path") }
                            }
                            OutlinedButton(
                                onClick = { points.clear() },
                                enabled = points.isNotEmpty() && !isFollowing,
                            ) { Text("Clear") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PathOverlay(
    anchor: GroundAnchor?,
    points: SnapshotStateList<Vec2>,
    enableTaps: Boolean,
) {
    val currentAnchor by rememberUpdatedState(anchor)
    val tapsEnabled by rememberUpdatedState(enableTaps)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (!tapsEnabled) return@detectTapGestures
                    points.add(
                        Vec2(
                            (offset.x / size.width).toDouble(),
                            (offset.y / size.height).toDouble(),
                        ),
                    )
                }
            },
    ) {
        val screenPoints = points.map { n ->
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
                radius = 12f,
                center = p,
            )
        }
        // Live marker (robot) position, so the user can see what is tracked.
        currentAnchor?.let { a ->
            val c = a.markerCenterNormalized()
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 14f,
                center = Offset((c.x * size.width).toFloat(), (c.y * size.height).toFloat()),
            )
        }
    }
}

/** Requests camera permission, showing a rationale/CTA until it is granted. */
@Composable
internal fun CameraGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
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
                "Camera access is needed to detect the marker and drive.",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant camera access")
            }
        }
        return
    }
    Box(modifier = modifier.fillMaxSize()) { content() }
}

@Composable
internal fun CameraPreview(
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
