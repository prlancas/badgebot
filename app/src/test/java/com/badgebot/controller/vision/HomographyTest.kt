package com.badgebot.controller.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomographyTest {

    private fun assertClose(expected: Vec2, actual: Vec2, eps: Double = 1e-6) {
        assertEquals(expected.x, actual.x, eps)
        assertEquals(expected.y, actual.y, eps)
    }

    @Test
    fun identity_mapsPointsToThemselves() {
        val square = listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(1.0, 1.0), Vec2(0.0, 1.0))
        val h = Homography.fromCorrespondences(square, square)!!
        assertClose(Vec2(0.5, 0.25), h.map(Vec2(0.5, 0.25)))
    }

    @Test
    fun affine_scaleAndTranslate() {
        // Map unit square to a 100px square offset by (10, 20): u = 100x + 10, v = 100y + 20.
        val src = listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(1.0, 1.0), Vec2(0.0, 1.0))
        val dst = listOf(Vec2(10.0, 20.0), Vec2(110.0, 20.0), Vec2(110.0, 120.0), Vec2(10.0, 120.0))
        val h = Homography.fromCorrespondences(src, dst)!!
        assertClose(Vec2(60.0, 70.0), h.map(Vec2(0.5, 0.5)))
    }

    @Test
    fun roundTrip_imageToGroundAndBack() {
        val ground = listOf(Vec2(0.0, 0.0), Vec2(0.1, 0.0), Vec2(0.1, 0.1), Vec2(0.0, 0.1))
        // A perspective-ish quad in image pixels.
        val image = listOf(Vec2(300.0, 400.0), Vec2(500.0, 410.0), Vec2(480.0, 600.0), Vec2(320.0, 590.0))
        val groundToImage = Homography.fromCorrespondences(ground, image)!!
        val imageToGround = Homography.fromCorrespondences(image, ground)!!

        val g = Vec2(0.05, 0.05)
        val projected = groundToImage.map(g)
        val back = imageToGround.map(projected)
        assertClose(g, back, eps = 1e-4)
    }

    @Test
    fun degenerate_collinearPoints_returnsNull() {
        val line = listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0), Vec2(2.0, 2.0), Vec2(3.0, 3.0))
        val dst = listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(2.0, 0.0), Vec2(3.0, 0.0))
        assertNull(Homography.fromCorrespondences(line, dst))
    }
}
