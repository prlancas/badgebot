package com.badgebot.controller.ble

/**
 * Builds the byte packets sent over the Nordic UART Service to drive the robot.
 *
 * The format mirrors the Adafruit Bluefruit "Controller" protocol:
 *
 *   `!B` <button-tag> <state> <crc>
 *
 * where `state` is `1` while a button is held and `0` when released, and `crc`
 * is a single trailing checksum byte (the bitwise-inverted sum of every
 * preceding byte).
 *
 * All functions here are pure so they can be exercised by fast JVM unit tests.
 */
object UartProtocol {

    /** Prefix that identifies a controller-pad button command. */
    const val COMMAND_PREFIX: String = "!B"

    /**
     * Returns [data] with a trailing CRC byte appended.
     *
     * The checksum is the sum of every byte, truncated to 8 bits and then
     * bitwise inverted, matching the reference Bluefruit implementation.
     */
    fun appendCrc(data: ByteArray): ByteArray {
        var checksum = 0
        for (b in data) {
            checksum += b.toInt()
        }
        val crc = checksum.inv().toByte()
        return data + crc
    }

    /**
     * Builds the full packet (including CRC) for a control-pad [button] being
     * pressed ([pressed] = true) or released ([pressed] = false).
     */
    fun controllerPadCommand(button: ControlButton, pressed: Boolean): ByteArray {
        val state = if (pressed) '1' else '0'
        val command = "$COMMAND_PREFIX${button.tag}$state"
        return appendCrc(command.toByteArray(Charsets.US_ASCII))
    }
}
