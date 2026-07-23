package com.badgebot.controller.vision

import com.badgebot.controller.ble.ControlButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** Tuning for the closed-loop visual servo. Distances are normalised image units. */
data class ServoConfig(
    /** How long to hold the forward arrow for one nudge. */
    val forwardPulseMs: Long = 250,
    /** How long to hold a turn arrow for one nudge. */
    val turnPulseMs: Long = 180,
    /** Time to wait after releasing an arrow before reading the new pose. */
    val settleMs: Long = 350,
    /** Poll interval while waiting for a fresh marker detection. */
    val pollMs: Long = 80,
    /** Heading error (radians) below which we drive forward instead of turning. */
    val alignToleranceRad: Double = 0.20,
    /** Distance (0..1) below which the marker counts as "on" the target. */
    val reachedTolerance: Double = 0.06,
    /** Minimum marker travel (0..1) that counts as a usable calibration move. */
    val minCalibrationMove: Double = 0.008,
    /** Give up on a target if the marker stays lost for this long. */
    val lostMarkerTimeoutMs: Long = 4000,
)

/**
 * Closed-loop visual servo that steers the robot toward a target point in the
 * camera image, using its ArUco marker as the position/heading sensor.
 *
 * It calibrates itself from motion, so nothing needs tuning by hand and a marker
 * mounted upside-down or sideways is handled automatically:
 *  - **Forward direction** is learned by driving forward a little and measuring
 *    which way the marker actually moved in the image.
 *  - **Turn direction** is learned by checking whether the first turn reduced or
 *    increased the heading error, flipping the mapping if it made things worse.
 *
 * Control is pulse-based: nudge, wait for a fresh detection, re-evaluate. This
 * self-corrects as the camera or robot moves and needs no speed/size constants.
 */
class VisualServoController(
    private val anchor: StateFlow<GroundAnchor?>,
    private val press: (ControlButton) -> Unit,
    private val release: (ControlButton) -> Unit,
    private val config: ServoConfig = ServoConfig(),
) {
    /** forwardAngle(image) - markerForwardAngle(image), learned once per session. */
    private var forwardOffset: Double? = null
    private var turnFlip = false
    private var turnCalibrated = false

    /** Forgets learned calibration; call when starting a fresh drive session. */
    fun resetCalibration() {
        forwardOffset = null
        turnFlip = false
        turnCalibrated = false
    }

    /** Releases every arrow, e.g. when driving stops or is cancelled. */
    fun stopAll() {
        ControlButton.entries.forEach { release(it) }
    }

    /**
     * Drives until the marker reaches [target] (a normalised image point that may
     * change between calls as the camera moves). Returns true once reached; false
     * if the marker is lost or the target becomes null.
     *
     * When [stopWhenReached] is false the controller idles at the target and keeps
     * re-checking, so it will chase the target again if it moves (drive-to-dot).
     */
    suspend fun driveTo(stopWhenReached: Boolean = true, target: () -> Vec2?): Boolean {
        if (!ensureForwardCalibrated()) return false
        while (coroutineContext.isActive) {
            val a = awaitAnchor() ?: return false
            val tgt = target() ?: return false
            val center = a.markerCenterNormalized()
            val dx = tgt.x - center.x
            val dy = tgt.y - center.y
            if (hypot(dx, dy) < config.reachedTolerance) {
                if (stopWhenReached) return true
                delay(config.settleMs)
                continue
            }

            val forwardAngle = normalize(a.markerForwardAngle() + (forwardOffset ?: 0.0))
            val desired = atan2(dy, dx)
            val err = normalize(desired - forwardAngle)

            if (abs(err) < config.alignToleranceRad) {
                pulse(ControlButton.UP, config.forwardPulseMs)
            } else {
                pulse(turnButtonFor(err), config.turnPulseMs)
                if (!turnCalibrated) calibrateTurn(err, target)
            }
        }
        return false
    }

    private suspend fun ensureForwardCalibrated(): Boolean {
        if (forwardOffset != null) return true
        while (coroutineContext.isActive) {
            val before = awaitAnchor() ?: return false
            val beforeCenter = before.markerCenterNormalized()
            pulse(ControlButton.UP, config.forwardPulseMs)
            val after = awaitAnchor() ?: return false
            val afterCenter = after.markerCenterNormalized()
            val dx = afterCenter.x - beforeCenter.x
            val dy = afterCenter.y - beforeCenter.y
            if (hypot(dx, dy) >= config.minCalibrationMove) {
                forwardOffset = normalize(atan2(dy, dx) - after.markerForwardAngle())
                return true
            }
            // Too little movement to trust; loop and nudge again.
        }
        return false
    }

    private suspend fun calibrateTurn(errBefore: Double, target: () -> Vec2?) {
        val a = anchor.value ?: return
        val tgt = target() ?: return
        val center = a.markerCenterNormalized()
        val forwardAngle = normalize(a.markerForwardAngle() + (forwardOffset ?: 0.0))
        val err = normalize(atan2(tgt.y - center.y, tgt.x - center.x) - forwardAngle)
        // If the first turn made the error worse, we turned the wrong way.
        if (abs(err) > abs(errBefore)) turnFlip = !turnFlip
        turnCalibrated = true
    }

    private fun turnButtonFor(err: Double): ControlButton {
        val useLeft = (err > 0) != turnFlip
        return if (useLeft) ControlButton.LEFT else ControlButton.RIGHT
    }

    private suspend fun pulse(button: ControlButton, durationMs: Long) {
        press(button)
        try {
            delay(durationMs)
        } finally {
            release(button)
        }
        delay(config.settleMs)
    }

    /** Returns the latest detection, or null if none arrives before the timeout. */
    private suspend fun awaitAnchor(): GroundAnchor? {
        val deadline = System.currentTimeMillis() + config.lostMarkerTimeoutMs
        while (coroutineContext.isActive) {
            anchor.value?.let { return it }
            if (System.currentTimeMillis() > deadline) return null
            delay(config.pollMs)
        }
        return null
    }

    private fun normalize(angle: Double): Double {
        var x = angle
        while (x > PI) x -= 2 * PI
        while (x <= -PI) x += 2 * PI
        return x
    }
}
