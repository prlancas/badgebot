package com.badgebot.controller.ble

import org.junit.Assert.assertEquals
import org.junit.Test

class ControlButtonTest {

    @Test
    fun tags_matchBluefruitControllerProtocol() {
        assertEquals(5, ControlButton.UP.tag)
        assertEquals(6, ControlButton.DOWN.tag)
        assertEquals(7, ControlButton.LEFT.tag)
        assertEquals(8, ControlButton.RIGHT.tag)
    }

    @Test
    fun tags_areUnique() {
        val tags = ControlButton.entries.map { it.tag }
        assertEquals(tags.size, tags.toSet().size)
    }
}
