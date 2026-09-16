package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.Handedness
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
    private val calibrator: LaneCalibrator = LaneCalibrator(),
    private val laneRecognizer: AutonomousLaneRecognizer = AutonomousLaneRecognizer(calibrator)
) {

    data class AutoDetectionResult(
        val isSuccess: Boolean,
        val confidence: Double,
        val calibration: LaneCalibration,
        val foulLineLeft: Point2D,
        val foulLineRight: Point2D,
        val arrowsLeft: Point2D,
        val arrowsRight: Point2D,
        val optimalZoomRatio: Float = 1.0f,
        val statusMessage: String = "",
        val pinRackDetected: Boolean = false,
        val guttersDetected: Boolean = false,
        val foulLineDetected: Boolean = false,
        val autoCenterGuidance: String? = null
    )

    /**
     * Analyzes a camera luminance frame to auto-detect the bowling lane.
     * Enforces strict hierarchical trigger: Pin Rack -> Gutters -> Foul Line.
     * Rejects bedrooms/offices immediately if no 10-pin rack is detected.
     */
    fun detectLaneFromFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width,
        zoomRatio: Float = 1.0f,
        alignment: Handedness = Handedness.RIGHT
    ): AutoDetectionResult {
        // Run full hierarchical autonomous recognition (Pins -> Gutters -> Foul Line -> Arrows)
        val recognition = laneRecognizer.recognizeLane(imageBytes, width, height, stride, zoomRatio, alignment)

        val defaultCalibPair = calibrator.createDefaultCalibration(
            viewWidth = width.toFloat(),
            viewHeight = height.toFloat(),
            zoomRatio = zoomRatio,
            anchorMode = CalibrationAnchorMode.GUTTERS_AT_ARROWS,
            alignment = alignment
        )
        val defaultCalib = defaultCalibPair.first

        if (recognition.isSuccess && recognition.calibration != null) {
            return AutoDetectionResult(
                isSuccess = true,
                confidence = recognition.confidence,
                calibration = recognition.calibration,
                foulLineLeft = recognition.foulLineLeft ?: defaultCalib.foulLineLeftScreen,
                foulLineRight = recognition.foulLineRight ?: defaultCalib.foulLineRightScreen,
                arrowsLeft = recognition.arrowsLeft ?: defaultCalib.arrowsLeftScreen,
                arrowsRight = recognition.arrowsRight ?: defaultCalib.arrowsRightScreen,
                optimalZoomRatio = recognition.optimalZoomRatio,
                statusMessage = recognition.statusMessage,
                pinRackDetected = recognition.pinRackDetected,
                guttersDetected = recognition.guttersDetected,
                foulLineDetected = recognition.foulLineDetected,
                autoCenterGuidance = recognition.autoCenterGuidance
            )
        } else {
            return AutoDetectionResult(
                isSuccess = false,
                confidence = recognition.confidence,
                calibration = defaultCalib,
                foulLineLeft = recognition.foulLineLeft ?: defaultCalib.foulLineLeftScreen,
                foulLineRight = recognition.foulLineRight ?: defaultCalib.foulLineRightScreen,
                arrowsLeft = recognition.arrowsLeft ?: defaultCalib.arrowsLeftScreen,
                arrowsRight = recognition.arrowsRight ?: defaultCalib.arrowsRightScreen,
                optimalZoomRatio = recognition.optimalZoomRatio,
                statusMessage = recognition.statusMessage,
                pinRackDetected = recognition.pinRackDetected,
                guttersDetected = recognition.guttersDetected,
                foulLineDetected = recognition.foulLineDetected,
                autoCenterGuidance = recognition.autoCenterGuidance
            )
        }
    }
}
