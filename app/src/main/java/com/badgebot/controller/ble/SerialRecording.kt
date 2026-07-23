package com.badgebot.controller.ble

/** Formats recorded serial traffic into a plain-text transcript. */
object SerialRecording {

    /**
     * Formats [events] as a text transcript. Each line shows the time elapsed
     * since the first event, the direction, the hex bytes and a printable
     * rendering, e.g.:
     *
     * ```
     * +0.000s TX  21 42 37 31 36  | !B716·
     * +0.204s RX  4F 4B           | OK
     * ```
     */
    fun format(events: List<SerialEvent>): String {
        if (events.isEmpty()) return "No serial traffic recorded.\n"
        val start = events.first().timestampMillis
        return buildString {
            appendLine("BadgeBot serial recording — ${events.size} events")
            appendLine("time     dir  bytes")
            for (event in events) {
                val elapsed = (event.timestampMillis - start) / 1000.0
                append("+%.3fs ".format(elapsed))
                append(if (event.direction == SerialDirection.TX) "TX  " else "RX  ")
                append(SerialFormat.toHex(event.data))
                append("  | ")
                appendLine(event.text)
            }
        }
    }
}
