package com.badgebot.controller.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class UartProtocolTest {

    /** Reference CRC: sum of bytes truncated to 8 bits, then bitwise inverted. */
    private fun referenceCrc(data: ByteArray): Byte {
        var sum = 0
        for (b in data) sum += b.toInt()
        return sum.inv().toByte()
    }

    @Test
    fun appendCrc_appendsSingleByte() {
        val input = "!B51".toByteArray(Charsets.US_ASCII)
        val result = UartProtocol.appendCrc(input)

        assertEquals(input.size + 1, result.size)
        assertArrayEquals(input, result.copyOfRange(0, input.size))
    }

    @Test
    fun appendCrc_matchesReferenceChecksum() {
        val input = "!B51".toByteArray(Charsets.US_ASCII)
        val result = UartProtocol.appendCrc(input)

        assertEquals(referenceCrc(input), result.last())
    }

    @Test
    fun appendCrc_knownVector_up_pressed() {
        // "!B51" -> bytes 33 + 66 + 53 + 49 = 201; ~201 (as byte) = 54
        val result = UartProtocol.appendCrc("!B51".toByteArray(Charsets.US_ASCII))
        assertEquals(54.toByte(), result.last())
    }

    @Test
    fun controllerPadCommand_pressed_hasExpectedPrefixAndState() {
        val packet = UartProtocol.controllerPadCommand(ControlButton.UP, pressed = true)

        // 4 command bytes + 1 crc byte
        assertEquals(5, packet.size)
        assertEquals('!'.code.toByte(), packet[0])
        assertEquals('B'.code.toByte(), packet[1])
        assertEquals('5'.code.toByte(), packet[2]) // UP tag
        assertEquals('1'.code.toByte(), packet[3]) // pressed
        assertEquals(referenceCrc(packet.copyOfRange(0, 4)), packet[4])
    }

    @Test
    fun controllerPadCommand_released_usesZeroState() {
        val packet = UartProtocol.controllerPadCommand(ControlButton.DOWN, pressed = false)

        assertEquals('6'.code.toByte(), packet[2]) // DOWN tag
        assertEquals('0'.code.toByte(), packet[3]) // released
    }

    @Test
    fun controllerPadCommand_everyButton_producesValidCrc() {
        for (button in ControlButton.entries) {
            for (pressed in listOf(true, false)) {
                val packet = UartProtocol.controllerPadCommand(button, pressed)
                val body = packet.copyOfRange(0, packet.size - 1)
                assertEquals(
                    "CRC mismatch for $button pressed=$pressed",
                    referenceCrc(body),
                    packet.last(),
                )
            }
        }
    }
}
