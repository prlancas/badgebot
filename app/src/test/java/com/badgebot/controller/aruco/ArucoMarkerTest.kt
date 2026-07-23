package com.badgebot.controller.aruco

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArucoMarkerTest {

    @Test
    fun markerBits_id0_matchesOpenCvDict4x4() {
        // Reference decoding of DICT_4X4_50 id 0 (from OpenCV's dictionary table).
        val expected = arrayOf(
            intArrayOf(1, 0, 1, 1),
            intArrayOf(0, 1, 0, 1),
            intArrayOf(0, 0, 1, 1),
            intArrayOf(0, 0, 1, 0),
        )
        val actual = ArucoMarker.markerBits(0)
        for (row in expected.indices) {
            assertArrayEquals("row $row", expected[row], actual[row])
        }
    }

    @Test
    fun markerBits_areBinaryAndCorrectShape() {
        for (id in 0 until ArucoMarker.DICTIONARY_SIZE) {
            val bits = ArucoMarker.markerBits(id)
            assertEquals(ArucoMarker.MARKER_SIZE, bits.size)
            for (row in bits) {
                assertEquals(ArucoMarker.MARKER_SIZE, row.size)
                for (cell in row) assertTrue(cell == 0 || cell == 1)
            }
        }
    }

    @Test
    fun cellGrid_addsBlackBorder() {
        val grid = ArucoMarker.cellGrid(0, borderBits = 1)
        assertEquals(ArucoMarker.MARKER_SIZE + 2, grid.size)
        // Entire outer border must be black (0).
        val last = grid.size - 1
        for (i in grid.indices) {
            assertEquals("top row", 0, grid[0][i])
            assertEquals("bottom row", 0, grid[last][i])
            assertEquals("left col", 0, grid[i][0])
            assertEquals("right col", 0, grid[i][last])
        }
    }

    @Test
    fun isValidId_boundsChecked() {
        assertTrue(ArucoMarker.isValidId(0))
        assertTrue(ArucoMarker.isValidId(49))
        assertFalse(ArucoMarker.isValidId(-1))
        assertFalse(ArucoMarker.isValidId(50))
        assertThrows(IllegalArgumentException::class.java) { ArucoMarker.markerBits(50) }
    }
}
