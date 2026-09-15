package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Autonomous computer-vision lane detector.
 *
 * Scans camera frame luminance gradients to detect high-contrast gutter rails
 * and the horizontal foul line stripe, computing calibration anchor points automatically.
 */
class AutoLaneDetector(
    private val calibrator: LaneCalibrator = LaneCalibrator()
) {

    data class AutoDetectionResult(
        val isSuccess: Boolean,
        val confidence: Double,
        val calibration: LaneCalibration,
        val foulLineLeft: Point2D,
        val foulLineRight: Point2D,
        val arrowsLeft: Point2D,
        val arrowsRight: Point2D
    )

    /**
     * Analyzes a camera luminance frame to auto-detect the bowling lane.
     */
    fun detectLaneFromFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width
    ): AutoDetectionResult {
        val w = width.toDouble()
        val h = height.toDouble()

        // 1. Scan rows in the lower region (foul line area: ~80% - 90% of image height)
        val foulScanY = (h * 0.85).toInt().coerceIn(0, height - 1)
        val foulEdges = findGutterEdgesAtRow(imageBytes, foulScanY, width, stride)

        // 2. Scan rows in the mid region (arrows area: ~55% - 65% of image height)
        val arrowsScanY = (h * 0.60).toInt().coerceIn(0, height - 1)
        val arrowEdges = findGutterEdgesAtRow(imageBytes, arrowsScanY, width, stride)

        // 3. Fallback defaults if gradient detection does not find clear edges
        val defaultFlL = Point2D(w * 0.12, h * 0.88)
        val defaultFlR = Point2D(w * 0.88, h * 0.88)
        val defaultAlL = Point2D(w * 0.28, h * 0.58)
        val defaultAlR = Point2D(w * 0.72, h * 0.58)

        val flL = foulEdges?.first ?: defaultFlL
        val flR = foulEdges?.second ?: defaultFlR
        val alL = arrowEdges?.first ?: defaultAlL
        val arR = arrowEdges?.second ?: defaultAlR

        // Sanity checks on geometry:
        // - Left edge must be left of right edge
        // - Foul line must be wider than arrows (perspective convergence)
        val foulWidth = flR.x - flL.x
        val arrowWidth = arR.x - alL.x

        val isValidGeometry = (foulWidth > 0.3 * w) && (arrowWidth > 0.15 * w) && (foulWidth > arrowWidth)
        val confidence = if (foulEdges != null && arrowEdges != null && isValidGeometry) 0.92 else 0.60

        val finalFlL = if (isValidGeometry) flL else defaultFlL
        val finalFlR = if (isValidGeometry) flR else defaultFlR
        val finalAlL = if (isValidGeometry) alL else defaultAlL
        val finalAlR = if (isValidGeometry) arR else defaultAlR

        val calibResult = calibrator.calibrate(
            foulLineLeft = finalFlL,
            foulLineRight = finalFlR,
            arrowsLeft = finalAlL,
            arrowsRight = finalAlR
        ) ?: calibrator.createDefaultCalibration(width.toFloat(), height.toFloat())

        return AutoDetectionResult(
            isSuccess = isValidGeometry,
            confidence = confidence,
            calibration = calibResult.first,
            foulLineLeft = finalFlL,
            foulLineRight = finalFlR,
            arrowsLeft = finalAlL,
            arrowsRight = finalAlR
        )
    }

    /**
     * Scans a single horizontal row across the frame to locate the left and right gutter transitions.
     * Gutter transitions present as large horizontal gradient spikes:
     * - Left gutter -> Lane: dark to bright (+dI/dx)
     * - Lane -> Right gutter: bright to dark (-dI/dx)
     */
    private fun findGutterEdgesAtRow(
        imageBytes: ByteArray,
        rowY: Int,
        width: Int,
        stride: Int
    ): Pair<Point2D, Point2D>? {
        if (rowY < 0 || rowY >= (imageBytes.size / stride)) return null

        val rowOffset = rowY * stride
        val midX = width / 2

        var maxLeftGrad = 0
        var leftEdgeX = -1

        // Scan from outer left margin toward center
        val leftStart = (width * 0.05).toInt()
        val leftEnd = (width * 0.40).toInt()

        for (x in leftStart until leftEnd) {
            val idx = rowOffset + x
            if (idx + 4 >= imageBytes.size) break
            val val1 = imageBytes[idx].toInt() and 0xFF
            val val2 = imageBytes[idx + 4].toInt() and 0xFF
            val grad = val2 - val1 // Positive spike entering lane from dark gutter

            if (grad > maxLeftGrad && grad > 25) {
                maxLeftGrad = grad
                leftEdgeX = x + 2
            }
        }

        var maxRightGrad = 0
        var rightEdgeX = -1

        // Scan from center toward outer right margin
        val rightStart = (width * 0.60).toInt()
        val rightEnd = (width * 0.95).toInt()

        for (x in rightStart until rightEnd) {
            val idx = rowOffset + x
            if (idx + 4 >= imageBytes.size) break
            val val1 = imageBytes[idx].toInt() and 0xFF
            val val2 = imageBytes[idx + 4].toInt() and 0xFF
            val grad = val1 - val2 // Negative spike leaving bright lane into dark gutter

            if (grad > maxRightGrad && grad > 25) {
                maxRightGrad = grad
                rightEdgeX = x + 2
            }
        }

        if (leftEdgeX > 0 && rightEdgeX > 0 && rightEdgeX > leftEdgeX) {
            return Pair(
                Point2D(leftEdgeX.toDouble(), rowY.toDouble()),
                Point2D(rightEdgeX.toDouble(), rowY.toDouble())
            )
        }

        return null
    }
}
