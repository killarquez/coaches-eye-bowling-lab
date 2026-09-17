package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.camera.ViewfinderCoordinateTransformer
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.ml.TFLiteBallDetector
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
        val pinDeckLeft: Point2D? = null,
        val pinDeckRight: Point2D? = null,
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
        alignment: Handedness = Handedness.RIGHT,
        anchorMode: CalibrationAnchorMode = CalibrationAnchorMode.PIN_DECK,
        tfliteDetector: TFLiteBallDetector? = null,
        screenWidth: Float = width.toFloat(),
        screenHeight: Float = height.toFloat()
    ): AutoDetectionResult {
        // Run full hierarchical autonomous recognition in camera buffer space
        val recognition = laneRecognizer.recognizeLane(
            imageBytes, width, height, stride, zoomRatio, alignment, anchorMode, tfliteDetector
        )

        val transformer = ViewfinderCoordinateTransformer(
            cameraWidth = width.toFloat(),
            cameraHeight = height.toFloat(),
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )

        val defaultDeckPair = calibrator.createDefaultCalibration(
            viewWidth = screenWidth,
            viewHeight = screenHeight,
            zoomRatio = zoomRatio,
            anchorMode = CalibrationAnchorMode.PIN_DECK,
            alignment = alignment
        )
        val defaultDeck = defaultDeckPair.first

        val defaultArrowsPair = calibrator.createDefaultCalibration(
            viewWidth = screenWidth,
            viewHeight = screenHeight,
            zoomRatio = zoomRatio,
            anchorMode = CalibrationAnchorMode.GUTTERS_AT_ARROWS,
            alignment = alignment
        )
        val defaultArrows = defaultArrowsPair.first

        if (recognition.isSuccess && recognition.calibration != null) {
            // Map detected landmarks from camera buffer pixels to screen space
            val screenFlL = recognition.foulLineLeft?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.foulLineLeftScreen
            val screenFlR = recognition.foulLineRight?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.foulLineRightScreen
            val screenDeckL = recognition.pinDeckLeft?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.arrowsLeftScreen
            val screenDeckR = recognition.pinDeckRight?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.arrowsRightScreen

            // Compute screen-space calibration
            val screenCalibResult = calibrator.calibrate(
                foulLineLeft = screenFlL,
                foulLineRight = screenFlR,
                arrowsLeft = screenDeckL,
                arrowsRight = screenDeckR,
                anchorMode = CalibrationAnchorMode.PIN_DECK,
                calibrationZoomRatio = zoomRatio
            )
            val screenCalib = screenCalibResult?.first ?: defaultDeck
            val screenH = screenCalibResult?.second

            val screenArrL = recognition.arrowsLeft?.let { transformer.cameraToScreen(it) }
                ?: screenH?.projectLaneToPixel(1.0, LaneConstants.ARROWS_DISTANCE_FT)
                ?: defaultArrows.arrowsLeftScreen
            val screenArrR = recognition.arrowsRight?.let { transformer.cameraToScreen(it) }
                ?: screenH?.projectLaneToPixel(39.0, LaneConstants.ARROWS_DISTANCE_FT)
                ?: defaultArrows.arrowsRightScreen

            return AutoDetectionResult(
                isSuccess = true,
                confidence = recognition.confidence,
                calibration = screenCalib,
                foulLineLeft = screenFlL,
                foulLineRight = screenFlR,
                arrowsLeft = screenArrL,
                arrowsRight = screenArrR,
                pinDeckLeft = screenDeckL,
                pinDeckRight = screenDeckR,
                optimalZoomRatio = recognition.optimalZoomRatio,
                statusMessage = recognition.statusMessage,
                pinRackDetected = recognition.pinRackDetected,
                guttersDetected = recognition.guttersDetected,
                foulLineDetected = recognition.foulLineDetected,
                autoCenterGuidance = recognition.autoCenterGuidance
            )
        } else {
            val screenFlL = recognition.foulLineLeft?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.foulLineLeftScreen
            val screenFlR = recognition.foulLineRight?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.foulLineRightScreen
            val screenDeckL = recognition.pinDeckLeft?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.arrowsLeftScreen
            val screenDeckR = recognition.pinDeckRight?.let { transformer.cameraToScreen(it) }
                ?: defaultDeck.arrowsRightScreen
            val screenArrL = recognition.arrowsLeft?.let { transformer.cameraToScreen(it) }
                ?: defaultArrows.arrowsLeftScreen
            val screenArrR = recognition.arrowsRight?.let { transformer.cameraToScreen(it) }
                ?: defaultArrows.arrowsRightScreen

            return AutoDetectionResult(
                isSuccess = false,
                confidence = recognition.confidence,
                calibration = defaultDeck,
                foulLineLeft = screenFlL,
                foulLineRight = screenFlR,
                arrowsLeft = screenArrL,
                arrowsRight = screenArrR,
                pinDeckLeft = screenDeckL,
                pinDeckRight = screenDeckR,
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
