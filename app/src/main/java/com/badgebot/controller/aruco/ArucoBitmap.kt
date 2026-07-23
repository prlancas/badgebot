package com.badgebot.controller.aruco

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Renders ArUco markers to [Bitmap]s for display, printing and sharing. */
object ArucoBitmap {

    /**
     * Renders marker [id] as a square bitmap of [sizePx] pixels on a side.
     * [borderBits] adds a black quiet-zone border (recommended: 1).
     */
    fun render(id: Int, sizePx: Int = 720, borderBits: Int = 1): Bitmap {
        val grid = ArucoMarker.cellGrid(id, borderBits)
        val cells = grid.size
        val cellPx = sizePx / cells
        val actualSize = cellPx * cells // keep cells pixel-aligned

        val bitmap = Bitmap.createBitmap(actualSize, actualSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val black = Paint().apply { color = Color.BLACK; isAntiAlias = false }
        for (row in 0 until cells) {
            for (col in 0 until cells) {
                if (grid[row][col] == 0) {
                    val left = (col * cellPx).toFloat()
                    val top = (row * cellPx).toFloat()
                    canvas.drawRect(left, top, left + cellPx, top + cellPx, black)
                }
            }
        }
        return bitmap
    }
}
