package com.badgebot.controller.ble

/**
 * The directional buttons shown on the control pad, mapped to the protocol
 * [tag] that makes *this* robot perform the matching real-world motion.
 *
 * The stock Adafruit Bluefruit "Controller" numbering is up=5, down=6, left=7,
 * right=8. On this robot those tags are wired differently, so the on-screen
 * arrows were previously mismatched:
 *
 *   - tag 7 drives the robot forward
 *   - tag 8 drives the robot backward
 *   - tag 5 turns the robot left
 *   - tag 6 turns the robot right
 *
 * The tags below therefore map each screen arrow to the command that produces
 * the intuitive motion (UP = forward, DOWN = backward, LEFT = turn left,
 * RIGHT = turn right).
 */
enum class ControlButton(val tag: Int) {
    /** Forward. */
    UP(7),

    /** Backward. */
    DOWN(8),

    /** Turn left. */
    LEFT(5),

    /** Turn right. */
    RIGHT(6),
}
