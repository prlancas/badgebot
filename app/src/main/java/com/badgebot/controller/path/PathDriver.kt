package com.badgebot.controller.path

import com.badgebot.controller.ble.ControlButton
import kotlinx.coroutines.delay

/**
 * Executes a list of [DriveCommand]s by holding the matching control-pad button
 * for each command's duration, sending the same press/release events the manual
 * arrows use. Cancelling the coroutine releases the active button immediately.
 */
class PathDriver(
    private val press: (ControlButton) -> Unit,
    private val release: (ControlButton) -> Unit,
) {
    /**
     * Drives [commands] in order, pausing [gapMs] between each. Suspends until
     * the sequence completes or the coroutine is cancelled.
     */
    suspend fun drive(commands: List<DriveCommand>, gapMs: Long = 150) {
        for (command in commands) {
            press(command.button)
            try {
                delay(command.durationMs)
            } finally {
                // Always release, including on cancellation, so the robot stops.
                release(command.button)
            }
            delay(gapMs)
        }
    }
}
