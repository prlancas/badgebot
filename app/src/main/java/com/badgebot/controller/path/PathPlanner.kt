package com.badgebot.controller.path

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToLong

/** A point on the ground plane, in metres, in the marker's reference frame. */
data class GroundPoint(val x: Double, val y: Double)

/**
 * Robot-specific motion calibration. Because following is open-loop (no
 * position feedback), these values must be measured/tuned on the real robot:
 * how fast it drives forward and how fast it turns while a button is held.
 */
data class DriveTuning(
    val forwardSpeedMetersPerSecond: Double = 0.15,
    val turnRateRadiansPerSecond: Double = 1.2,
    /** Heading changes smaller than this are treated as "straight ahead". */
    val minTurnRadians: Double = Math.toRadians(4.0),
    /** Moves shorter than this are skipped. */
    val minMoveMeters: Double = 0.02,
)

/**
 * Converts a drawn ground path into a sequence of timed [DriveCommand]s.
 *
 * The robot is assumed to start at the first point with heading
 * [initialHeadingRadians] (measured as `atan2(dy, dx)`, counter-clockwise
 * positive). For each subsequent waypoint the planner turns to face it, then
 * drives straight to it. A positive required turn is a **left** turn.
 *
 * This is pure and deterministic so it is fully unit-tested; only the timing
 * accuracy depends on the real-world [tuning] values.
 */
object PathPlanner {

    fun plan(
        points: List<GroundPoint>,
        initialHeadingRadians: Double = 0.0,
        tuning: DriveTuning = DriveTuning(),
    ): List<DriveCommand> {
        if (points.size < 2) return emptyList()

        val commands = mutableListOf<DriveCommand>()
        var heading = initialHeadingRadians
        var current = points.first()

        for (i in 1 until points.size) {
            val target = points[i]
            val dx = target.x - current.x
            val dy = target.y - current.y
            val distance = hypot(dx, dy)
            if (distance < tuning.minMoveMeters) continue

            val targetHeading = atan2(dy, dx)
            val turn = normalize(targetHeading - heading)

            if (abs(turn) >= tuning.minTurnRadians) {
                val turnMs = (abs(turn) / tuning.turnRateRadiansPerSecond * 1000.0).roundToLong()
                if (turnMs > 0) {
                    commands += if (turn > 0) {
                        DriveCommand.TurnLeft(turnMs)
                    } else {
                        DriveCommand.TurnRight(turnMs)
                    }
                }
            }

            val forwardMs = (distance / tuning.forwardSpeedMetersPerSecond * 1000.0).roundToLong()
            if (forwardMs > 0) {
                commands += DriveCommand.Forward(forwardMs)
            }

            heading = targetHeading
            current = target
        }

        return commands
    }

    /** Normalises [angle] to the range (-π, π]. */
    private fun normalize(angle: Double): Double {
        var a = angle
        while (a > PI) a -= 2 * PI
        while (a <= -PI) a += 2 * PI
        return a
    }
}
