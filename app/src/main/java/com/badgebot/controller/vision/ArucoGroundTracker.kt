package com.badgebot.controller.vision

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.Mat
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.DetectorParameters
import org.opencv.objdetect.Objdetect

/**
 * A ground reference frame anchored to a detected ArUco marker.
 *
 * Coordinates are expressed in the marker's plane in metres, with the origin at
 * the marker centre, **+x pointing "up" out of the marker (robot forward)** and
 * **+y pointing to the marker's left**. All image coordinates are normalised to
 * `[0, 1]` over the upright camera frame so they can be mapped to the on-screen
 * preview independently of resolution.
 */
data class GroundAnchor(
    val groundToImage: Homography,
    val imageToGround: Homography,
) {
    /** Maps a ground point (metres) to a normalised image point (0..1). */
    fun groundToNormalizedImage(p: Vec2): Vec2 = groundToImage.map(p)

    /** Maps a normalised image point (0..1) to a ground point (metres). */
    fun normalizedImageToGround(p: Vec2): Vec2 = imageToGround.map(p)
}

/**
 * Detects a specific ArUco marker (`DICT_4X4_50`) in camera frames and, when
 * found, publishes a [GroundAnchor] describing the marker-anchored ground plane.
 *
 * The marker is the world anchor: as long as it stays in view the drawn path
 * remains pinned to the ground. Detection runs on grayscale frames off the main
 * thread; the latest result is exposed via [anchor].
 */
class ArucoGroundTracker(
    private val targetMarkerId: Int = 0,
    /** Physical side length of the printed marker, in metres. */
    initialMarkerLengthMeters: Double = 0.10,
) {
    /**
     * Physical side length of the printed marker, in metres. Mutable so it can
     * be calibrated live from the UI; detection reads the latest value.
     */
    @Volatile
    var markerLengthMeters: Double = initialMarkerLengthMeters

    private val detector: ArucoDetector = ArucoDetector(
        Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50),
        DetectorParameters(),
    )

    private val _anchor = MutableStateFlow<GroundAnchor?>(null)
    val anchor: StateFlow<GroundAnchor?> = _anchor.asStateFlow()

    /**
     * Runs detection on an upright grayscale [gray] frame ([Mat] is not
     * modified). Updates [anchor] with the marker-anchored ground frame, or null
     * if the target marker is not visible.
     */
    fun detect(gray: Mat) {
        val corners = ArrayList<Mat>()
        val ids = Mat()
        try {
            detector.detectMarkers(gray, corners, ids)
            if (ids.empty()) {
                _anchor.value = null
                return
            }

            val width = gray.cols().toDouble()
            val height = gray.rows().toDouble()

            for (i in 0 until ids.rows()) {
                val id = ids.get(i, 0)[0].toInt()
                if (id != targetMarkerId) continue

                val markerCorners = corners[i]
                // Detected corner order (clockwise): TL, TR, BR, BL in image px.
                val imgPts = (0 until 4).map { k ->
                    val pt = markerCorners.get(0, k)
                    Vec2(pt[0] / width, pt[1] / height)
                }

                // Ground square (metres) matching the same corner order. The
                // marker "top" edge is robot-forward (+x); left is +y.
                val h = markerLengthMeters / 2.0
                val groundPts = listOf(
                    Vec2(h, h),   // top-left
                    Vec2(h, -h),  // top-right
                    Vec2(-h, -h), // bottom-right
                    Vec2(-h, h),  // bottom-left
                )

                val groundToImage = Homography.fromCorrespondences(groundPts, imgPts)
                val imageToGround = Homography.fromCorrespondences(imgPts, groundPts)
                if (groundToImage != null && imageToGround != null) {
                    _anchor.value = GroundAnchor(groundToImage, imageToGround)
                    return
                }
            }
            // Target marker not present among detections.
            _anchor.value = null
        } finally {
            corners.forEach { it.release() }
            ids.release()
        }
    }

    fun reset() {
        _anchor.value = null
    }
}
