package com.badgebot.controller.vision

import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

/** Helpers to convert CameraX frames into OpenCV [Mat]s. */
object CameraFrames {

    /**
     * Extracts an upright grayscale [Mat] from [image] (the Y plane of the
     * YUV_420_888 frame), rotated according to [ImageProxy.getImageInfo]'s
     * rotation so the marker is oriented as seen on screen. ArUco detection only
     * needs the luminance channel, so colour planes are ignored.
     *
     * The caller owns the returned Mat and must [Mat.release] it.
     */
    fun toUprightGray(image: ImageProxy): Mat {
        val yPlane = image.planes[0]
        val rowStride = yPlane.rowStride
        val height = image.height
        val width = image.width

        val buffer = yPlane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Wrap the padded buffer, then crop away row padding to the real width.
        val padded = Mat(height, rowStride, CvType.CV_8UC1)
        padded.put(0, 0, bytes)
        val gray = padded.submat(0, height, 0, width).clone()
        padded.release()

        when (image.imageInfo.rotationDegrees) {
            90 -> Core.rotate(gray, gray, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(gray, gray, Core.ROTATE_180)
            270 -> Core.rotate(gray, gray, Core.ROTATE_90_COUNTERCLOCKWISE)
        }
        return gray
    }
}
