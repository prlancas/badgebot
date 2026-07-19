package com.badgebot.controller.ble

/**
 * A UART-capable peripheral found during scanning.
 *
 * [address] is the stable MAC address used as the identity for connecting and
 * de-duplicating scan results. [name] may be null for peripherals that do not
 * advertise a local name.
 */
data class DiscoveredDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Unknown device"
}
