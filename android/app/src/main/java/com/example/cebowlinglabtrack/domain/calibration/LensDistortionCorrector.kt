package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.Point2D

/**
 * Corrects radial lens distortion using the standard polynomial model:
 *   x_distorted = x_undistorted * (1 + k1 * r^2 + k2 * r^4)
 *   y_distorted = y_undistorted * (1 + k1 * r^2 + k2 * r^4)
 * 
 * For real-time lane calibration, we invert this mapping using a fast iterative
 * Newton-Raphson approximation or first-order backward expansion.
 */
class LensDistortionCorrector(
    private val opticalCenterX: Double,
    private val opticalCenterY: Double,
    private val focalLengthPx: Double,
    private val k1: Double = -0.08, // Typical subtle barrel for mobile wide-angle
    private val k2: Double = 0.01
) {

    /**
     * Maps a distorted sensor/screen point (u_dist, v_dist) to an undistorted point (u_rect, v_rect).
     */
    fun undistortPoint(point: Point2D): Point2D {
        // Normalize coordinates relative to optical principal point
        val xNorm = (point.x - opticalCenterX) / focalLengthPx
        val yNorm = (point.y - opticalCenterY) / focalLengthPx

        val r2 = xNorm * xNorm + yNorm * yNorm
        val r4 = r2 * r2

        // Direct inverse approximation for low-to-moderate barrel distortion
        val distortionFactor = 1.0 + k1 * r2 + k2 * r4
        val xCorr = xNorm / distortionFactor
        val yCorr = yNorm / distortionFactor

        return Point2D(
            x = xCorr * focalLengthPx + opticalCenterX,
            y = yCorr * focalLengthPx + opticalCenterY
        )
    }

    /**
     * Undistorts the 4 calibration anchor corners prior to computing DLT.
     */
    fun undistortCalibrationCorners(
        foulLeft: Point2D,
        foulRight: Point2D,
        pinLeft: Point2D,
        pinRight: Point2D
    ): Array<Point2D> {
        return arrayOf(
            undistortPoint(foulLeft),
            undistortPoint(foulRight),
            undistortPoint(pinLeft),
            undistortPoint(pinRight)
        )
    }
}
