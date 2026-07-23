package com.badgebot.controller.ble

import org.junit.Assert.assertEquals
import org.junit.Test

class ControlButtonTest {

    @Test
    fun tags_mapScreenArrowsToRobotMotion() {
        // UP = forward, DOWN = backward, LEFT = turn left, RIGHT = turn right,
        // using the tags this robot's firmware associates with each motion.
        assertEquals(7, ControlButton.UP.tag)
        assertEquals(8, ControlButton.DOWN.tag)
        assertEquals(5, ControlButton.LEFT.tag)
        assertEquals(6, ControlButton.RIGHT.tag)
    }

    @Test
    fun tags_areUnique() {
        val tags = ControlButton.entries.map { it.tag }
        assertEquals(tags.size, tags.toSet().size)
    }
}
