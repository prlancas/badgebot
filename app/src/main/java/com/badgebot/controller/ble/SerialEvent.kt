package com.badgebot.controller.ble

/** Direction of a serial packet relative to the phone. */
enum class SerialDirection {
    /** Phone → robot. */
    TX,

    /** Robot → phone. */
    RX,
}

/**
 * A single chunk of serial traffic exchanged over the UART characteristics.
 *
 * [data] is the raw bytes; [text] is a printable rendering (non-printable bytes
 * shown as `·`). [timestampMillis] is wall-clock time captured when the event
 * was observed.
 */
data class SerialEvent(
    val direction: SerialDirection,
    val timestampMillis: Long,
    val data: ByteArray,
) {
    val text: String get() = SerialFormat.toPrintable(data)

    // ByteArray needs explicit equals/hashCode for value semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerialEvent) return false
        return direction == other.direction &&
            timestampMillis == other.timestampMillis &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = direction.hashCode()
        result = 31 * result + timestampMillis.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/** Helpers for rendering serial bytes as human-readable text. */
object SerialFormat {
    /** Renders [data] as ASCII, replacing non-printable bytes with `·`. */
    fun toPrintable(data: ByteArray): String = buildString {
        for (b in data) {
            val v = b.toInt() and 0xFF
            append(if (v in 0x20..0x7E) v.toChar() else '·')
        }
    }

    /** Renders [data] as space-separated two-digit hex, e.g. `21 42 37 31`. */
    fun toHex(data: ByteArray): String =
        data.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
