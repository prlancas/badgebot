package com.badgebot.controller.aruco

/**
 * Generates the bit pattern for markers in the OpenCV `DICT_4X4_50` ArUco
 * dictionary, so the printed markers are detectable by any standard ArUco
 * detector configured with the same dictionary.
 *
 * The values in [CODEWORDS] are the first 50 entries of OpenCV's
 * `DICT_4X4_1000` byte table (a smaller dictionary is simply the prefix of the
 * larger one). Each entry packs the 4×4 = 16 marker bits of the un-rotated
 * marker as a 16-bit value, row-major, most-significant-bit first.
 */
object ArucoMarker {

    /** Marker interior size in bits (4×4). */
    const val MARKER_SIZE = 4

    /** Number of markers available in `DICT_4X4_50`. */
    const val DICTIONARY_SIZE = 50

    /** Rotation-0 codewords for ids 0..49 of `DICT_4X4_50`. */
    private val CODEWORDS = intArrayOf(
        46386, 3994, 13101, 39238, 21662, 31181, 40494, 50418, 65242, 53078,
        63889, 4519, 3767, 10767, 9393, 9790, 18021, 26112, 27742, 30383,
        34443, 45099, 52437, 56706, 65095, 38001, 44260, 42324, 8483, 13423,
        17429, 22450, 40655, 61643, 2222, 2345, 6261, 1279, 3574, 7258,
        5912, 10792, 12940, 14514, 9448, 12011, 11583, 19300, 20526, 20499,
    )

    /** True if [id] is a valid marker id for this dictionary. */
    fun isValidId(id: Int): Boolean = id in 0 until DICTIONARY_SIZE

    /**
     * Returns the 4×4 interior bit matrix for marker [id], where `1` is a white
     * cell and `0` is a black cell.
     */
    fun markerBits(id: Int): Array<IntArray> {
        require(isValidId(id)) { "Marker id $id is out of range 0..${DICTIONARY_SIZE - 1}" }
        val code = CODEWORDS[id]
        return Array(MARKER_SIZE) { row ->
            IntArray(MARKER_SIZE) { col ->
                val bitIndex = row * MARKER_SIZE + col
                // Bits are stored MSB-first across the 16-bit codeword.
                (code shr (15 - bitIndex)) and 1
            }
        }
    }

    /**
     * Returns the full printable cell grid for marker [id] including a black
     * quiet border of [borderBits] cells on each side. `1` = white, `0` = black.
     * The grid is `(MARKER_SIZE + 2 * borderBits)` cells square.
     */
    fun cellGrid(id: Int, borderBits: Int = 1): Array<IntArray> {
        require(borderBits >= 0) { "borderBits must be >= 0" }
        val bits = markerBits(id)
        val total = MARKER_SIZE + 2 * borderBits
        return Array(total) { row ->
            IntArray(total) { col ->
                val innerRow = row - borderBits
                val innerCol = col - borderBits
                if (innerRow in 0 until MARKER_SIZE && innerCol in 0 until MARKER_SIZE) {
                    bits[innerRow][innerCol]
                } else {
                    0 // border cell = black
                }
            }
        }
    }
}
