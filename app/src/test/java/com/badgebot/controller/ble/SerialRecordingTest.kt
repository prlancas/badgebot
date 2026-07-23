package com.badgebot.controller.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerialRecordingTest {

    @Test
    fun format_emptyList_returnsPlaceholder() {
        val text = SerialRecording.format(emptyList())
        assertTrue(text.contains("No serial traffic"))
    }

    @Test
    fun format_includesDirectionHexAndText() {
        val events = listOf(
            SerialEvent(SerialDirection.TX, 1_000L, byteArrayOf(0x21, 0x42)),
            SerialEvent(SerialDirection.RX, 1_250L, "OK".toByteArray(Charsets.US_ASCII)),
        )
        val text = SerialRecording.format(events)

        assertTrue(text.contains("2 events"))
        assertTrue(text.contains("TX"))
        assertTrue(text.contains("RX"))
        assertTrue(text.contains("21 42"))
        assertTrue(text.contains("| !B"))
        assertTrue(text.contains("| OK"))
        // Elapsed time of the second event is 0.250s after the first.
        assertTrue("expected +0.250s, got:\n$text", text.contains("+0.250s"))
    }

    @Test
    fun serialFormat_nonPrintableRenderedAsDot() {
        // 0xFF is non-printable, 'A' is printable.
        val rendered = SerialFormat.toPrintable(byteArrayOf(0x41, 0xFF.toByte()))
        assertEquals("A·", rendered)
    }
}
