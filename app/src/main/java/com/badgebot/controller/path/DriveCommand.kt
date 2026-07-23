package com.badgebot.controller.path

import com.badgebot.controller.ble.ControlButton

/**
 * A single open-loop driving instruction: hold one control-pad direction for a
 * fixed duration. A sequence of these drives the robot along a planned path
 * without any position feedback.
 */
sealed interface DriveCommand {
    val durationMs: Long

    /** The control-pad button to hold for [durationMs]. */
    val button: ControlButton

    data class Forward(override val durationMs: Long) : DriveCommand {
        override val button get() = ControlButton.UP
    }

    data class TurnLeft(override val durationMs: Long) : DriveCommand {
        override val button get() = ControlButton.LEFT
    }

    data class TurnRight(override val durationMs: Long) : DriveCommand {
        override val button get() = ControlButton.RIGHT
    }
}
