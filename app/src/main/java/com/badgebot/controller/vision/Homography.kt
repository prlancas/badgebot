package com.badgebot.controller.vision

/** A 2D point (pixels or metres depending on context). */
data class Vec2(val x: Double, val y: Double)

/**
 * A planar homography mapping 2D points to 2D points (a 3×3 projective
 * transform with the bottom-right element fixed at 1).
 *
 * This is pure Kotlin so the ground↔image mapping used by the camera path
 * feature can be unit-tested without OpenCV / a device.
 */
class Homography private constructor(private val h: DoubleArray) {

    /** Applies the transform to [p]. */
    fun map(p: Vec2): Vec2 {
        val denom = h[6] * p.x + h[7] * p.y + h[8]
        val x = (h[0] * p.x + h[1] * p.y + h[2]) / denom
        val y = (h[3] * p.x + h[4] * p.y + h[5]) / denom
        return Vec2(x, y)
    }

    companion object {
        /**
         * Builds the homography mapping the four [src] points to the four [dst]
         * points (order-corresponding). Returns null if the system is
         * degenerate (e.g. collinear points).
         */
        fun fromCorrespondences(src: List<Vec2>, dst: List<Vec2>): Homography? {
            require(src.size == 4 && dst.size == 4) { "Need exactly 4 correspondences" }

            // Solve for 8 unknowns [h11 h12 h13 h21 h22 h23 h31 h32] with h33 = 1.
            val a = Array(8) { DoubleArray(8) }
            val b = DoubleArray(8)
            for (i in 0 until 4) {
                val (x, y) = src[i]
                val (u, v) = dst[i]
                val r0 = 2 * i
                val r1 = 2 * i + 1
                a[r0] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -x * u, -y * u)
                b[r0] = u
                a[r1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -x * v, -y * v)
                b[r1] = v
            }

            val solution = solve(a, b) ?: return null
            val h = DoubleArray(9)
            System.arraycopy(solution, 0, h, 0, 8)
            h[8] = 1.0
            return Homography(h)
        }

        /** Gaussian elimination with partial pivoting; returns null if singular. */
        private fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
            val n = b.size
            for (col in 0 until n) {
                var pivot = col
                for (row in col + 1 until n) {
                    if (kotlin.math.abs(a[row][col]) > kotlin.math.abs(a[pivot][col])) pivot = row
                }
                if (kotlin.math.abs(a[pivot][col]) < 1e-12) return null

                val tmp = a[col]; a[col] = a[pivot]; a[pivot] = tmp
                val tb = b[col]; b[col] = b[pivot]; b[pivot] = tb

                for (row in 0 until n) {
                    if (row == col) continue
                    val factor = a[row][col] / a[col][col]
                    for (k in col until n) a[row][k] -= factor * a[col][k]
                    b[row] -= factor * b[col]
                }
            }
            return DoubleArray(n) { b[it] / a[it][it] }
        }
    }
}
