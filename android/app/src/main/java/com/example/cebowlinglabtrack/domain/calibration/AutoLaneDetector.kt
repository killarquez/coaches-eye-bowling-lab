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
        val arrowsRight: Point2D,
        val optimalZoomRatio: Float = 1.0f
    )

    /**
     * Analyzes a camera luminance frame to auto-detect the bowling lane.
     */
    fun detectLaneFromFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width,
        zoomRatio: Float = 1.0f
    ): AutoDetectionResult {
        val w = width.toDouble()
        val h = height.toDouble()

        // Multi-row candidate scan across the physical lane region (excluding bowler approach)
        // If zoomed in (>= 2.0x), lane occupies lower-middle to upper (40% to 80% of height)
        // If unzoomed (1.0x), lane occupies middle band (38% to 62% of height)
        val (foulMinPct, foulMaxPct) = if (zoomRatio >= 2.0f) Pair(0.70, 0.82) else Pair(0.50, 0.60)
        val (arrowsMinPct, arrowsMaxPct) = if (zoomRatio >= 2.0f) Pair(0.42, 0.52) else Pair(0.38, 0.46)

        val foulScanRows = listOf(
            (h * foulMinPct).toInt().coerceIn(0, height - 1),
            (h * ((foulMinPct + foulMaxPct) / 2.0)).toInt().coerceIn(0, height - 1),
            (h * foulMaxPct).toInt().coerceIn(0, height - 1)
        )
        val foulEdges = foulScanRows.mapNotNull { rowY ->
            findGutterEdgesAtRow(imageBytes, rowY, width, stride)
        }.maxByOrNull { it.second.x - it.first.x }

        val arrowsScanRows = listOf(
            (h * arrowsMinPct).toInt().coerceIn(0, height - 1),
            (h * ((arrowsMinPct + arrowsMaxPct) / 2.0)).toInt().coerceIn(0, height - 1),
            (h * arrowsMaxPct).toInt().coerceIn(0, height - 1)
        )
        val arrowEdges = arrowsScanRows.mapNotNull { rowY ->
            findGutterEdgesAtRow(imageBytes, rowY, width, stride)
        }.minByOrNull { it.second.x - it.first.x }

        // Default USBC perspective anchor fallback
        val defaultCalibPair = calibrator.createDefaultCalibration(
            viewWidth = width.toFloat(),
            viewHeight = height.toFloat(),
            zoomRatio = zoomRatio,
            anchorMode = CalibrationAnchorMode.GUTTERS_AT_ARROWS
        )
        val defaultCalib = defaultCalibPair.first

        val flL = foulEdges?.first ?: defaultCalib.foulLineLeftScreen
        val flR = foulEdges?.second ?: defaultCalib.foulLineRightScreen
        val alL = arrowEdges?.first ?: defaultCalib.arrowsLeftScreen
        val arR = arrowEdges?.second ?: defaultCalib.arrowsRightScreen

        // Sanity checks on geometry:
        // - Left edge must be left of right edge
        // - Foul line must be wider than arrows (perspective convergence)
        val foulWidth = flR.x - flL.x
        val arrowWidth = arR.x - alL.x

        val isValidGeometry = (foulWidth > 0.25 * w) && (arrowWidth > 0.12 * w) && (foulWidth > arrowWidth * 0.95)
        val confidence = if (foulEdges != null && arrowEdges != null && isValidGeometry) 0.92 else 0.65

        // Compute optimal optical zoom ratio to fill ~82% of viewport with lane
        val laneCoverage = foulWidth / w
        val optimalZoom = if (isValidGeometry && laneCoverage in 0.15..0.95) {
            (0.82 / laneCoverage).toFloat().coerceIn(1.0f, 3.5f)
        } else {
            zoomRatio.coerceIn(1.0f, 3.5f)
        }

        val finalFlL = if (isValidGeometry) flL else defaultCalib.foulLineLeftScreen
        val finalFlR = if (isValidGeometry) flR else defaultCalib.foulLineRightScreen
        val finalAlL = if (isValidGeometry) alL else defaultCalib.arrowsLeftScreen
        val finalAlR = if (isValidGeometry) arR else defaultCalib.arrowsRightScreen

        val calibResult = calibrator.calibrateGutters(
            foulLineLeft = finalFlL,
            foulLineRight = finalFlR,
            gutterLeft15ft = finalAlL,
            gutterRight15ft = finalAlR,
            calibrationZoomRatio = zoomRatio
        ) ?: defaultCalibPair

        return AutoDetectionResult(
            isSuccess = isValidGeometry,
            confidence = confidence,
            calibration = calibResult.first,
            foulLineLeft = finalFlL,
            foulLineRight = finalFlR,
            arrowsLeft = finalAlL,
            arrowsRight = finalAlR,
            optimalZoomRatio = optimalZoom
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

        var maxLeftGrad = 0
        var leftEdgeX = -1

        // Scan from outer left margin toward center
        val leftStart = (width * 0.05).toInt()
        val leftEnd = (width * 0.45).toInt()

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
        val rightStart = (width * 0.55).toInt()
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
