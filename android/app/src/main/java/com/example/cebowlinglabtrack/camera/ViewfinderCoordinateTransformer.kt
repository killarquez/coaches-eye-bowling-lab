package com.example.cebowlinglabtrack.camera

import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.max

/**
 * Bidirectional coordinate transformer between upright CameraX frame buffer space (W_cam, H_cam)
 * and Compose Viewfinder screen space (W_screen, H_screen) under PreviewView.ScaleType.FILL_CENTER.
 *
 * Ensures:
 * 1. Auto-detected landmarks in camera pixels map 1:1 onto the exact visual features on the user\'s screen.
 * 2. Ball detections in camera pixels map 1:1 into the screen-calibrated homography matrix.
 */
class ViewfinderCoordinateTransformer(
    val cameraWidth: Float,
    val cameraHeight: Float,
    val screenWidth: Float,
    val screenHeight: Float
) {
    val scale: Float
    val offsetX: Float
    val offsetY: Float

    init {
        val safeCamW = if (cameraWidth > 10f) cameraWidth else 1080f
        val safeCamH = if (cameraHeight > 10f) cameraHeight else 1920f
        val safeScreenW = if (screenWidth > 10f) screenWidth else safeCamW
        val safeScreenH = if (screenHeight > 10f) screenHeight else safeCamH

        scale = max(safeScreenW / safeCamW, safeScreenH / safeCamH)
        val scaledW = safeCamW * scale
        val scaledH = safeCamH * scale
        offsetX = (scaledW - safeScreenW) / 2f
        offsetY = (scaledH - safeScreenH) / 2f
    }

    /**
     * Transforms a point from upright camera buffer coordinates to Compose screen pixels.
     */
    fun cameraToScreen(xCam: Double, yCam: Double): Point2D {
        val xScreen = xCam * scale - offsetX
        val yScreen = yCam * scale - offsetY
        return Point2D(xScreen, yScreen)
    }

    fun cameraToScreen(pt: Point2D): Point2D = cameraToScreen(pt.x, pt.y)

    /**
     * Transforms a point from Compose screen pixels to upright camera buffer coordinates.
     */
    fun screenToCamera(xScreen: Double, yScreen: Double): Point2D {
        val xCam = (xScreen + offsetX) / scale
        val yCam = (yScreen + offsetY) / scale
        return Point2D(xCam, yCam)
    }

    fun screenToCamera(pt: Point2D): Point2D = screenToCamera(pt.x, pt.y)
}
