package com.badgebot.controller.ble

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveredDeviceTest {

    @Test
    fun displayName_usesNameWhenPresent() {
        val device = DiscoveredDevice(address = "AA:BB", name = "BadgeBot", rssi = -40)
        assertEquals("BadgeBot", device.displayName)
    }

    @Test
    fun displayName_fallsBackWhenNameNull() {
        val device = DiscoveredDevice(address = "AA:BB", name = null, rssi = -40)
        assertEquals("Unknown device", device.displayName)
    }

    @Test
    fun displayName_fallsBackWhenNameBlank() {
        val device = DiscoveredDevice(address = "AA:BB", name = "   ", rssi = -40)
        assertEquals("Unknown device", device.displayName)
    }
}
