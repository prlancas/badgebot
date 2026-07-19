package com.badgebot.controller.ble

/**
 * The directional buttons supported by the BadgeBot control pad.
 *
 * The [tag] values match the Adafruit Bluefruit "Controller" protocol so that
 * firmware built for a Bluefruit UART device understands the commands without
 * modification.
 */
enum class ControlButton(val tag: Int) {
    UP(5),
    DOWN(6),
    LEFT(7),
    RIGHT(8),
}
