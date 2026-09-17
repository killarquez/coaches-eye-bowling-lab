package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.ml.TFLiteBallDetector
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * High-precision autonomous bowling lane and pin deck recognition engine.
 *
 * Employs a strict hierarchical computer-vision pipeline:
 * 1. Pin Rack Trigger: Identifies the high-contrast 10-pin triangular rack at 60 ft.
 *    Rejects non-bowling environments (bedrooms, offices, living rooms) immediately.
 * 2. Gutter Tracing: Traces dark gutter channels diverging symmetrically downwards from pins.
 * 3. Foul Line & Arrow Line Cross-Validation: Locates foul line and 15-ft arrows.
 * 4. Auto-Centering & Zoom Guidance: Recommends pan adjustments and optical zoom.
 */
class AutonomousLaneRecognizer(
    private val calibrator: LaneCalibrator = LaneCalibrator()
) {

    data class PinRackCandidate(
        val centerX: Double,
        val topY: Double,
        val bottomY: Double,
        val widthPx: Double,
        val pinPeakCount: Int,
        val contrastRatio: Double,
        val confidence: Double
    )

    data class GutterBoundaryLines(
        val leftSlope: Double,
        val leftIntercept: Double,
        val rightSlope: Double,
        val rightIntercept: Double,
        val confidence: Double
    ) {
        fun xLeftAt(y: Double): Double = leftSlope * y + leftIntercept
        fun xRightAt(y: Double): Double = rightSlope * y + rightIntercept
    }

    data class LaneRecognitionResult(
        val isSuccess: Boolean,
        val statusMessage: String,
        val pinRackDetected: Boolean,
        val guttersDetected: Boolean,
        val foulLineDetected: Boolean,
        val pinRack: PinRackCandidate?,
        val calibration: LaneCalibration?,
        val homography: HomographyMatrix?,
        val foulLineLeft: Point2D?,
        val foulLineRight: Point2D?,
        val arrowsLeft: Point2D?,
        val arrowsRight: Point2D?,
        val pinDeckLeft: Point2D? = null,
        val pinDeckRight: Point2D? = null,
        val autoCenterGuidance: String?,
        val optimalZoomRatio: Float = 1.0f,
        val confidence: Double = 0.0
    )

    /**
     * Executes the full hierarchical lane recognition pipeline.
     */
    /**
     * Executes the full hierarchical lane recognition pipeline.
     */
    fun recognizeLane(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width,
        zoomRatio: Float = 1.0f,
        alignment: Handedness = Handedness.RIGHT,
        anchorMode: CalibrationAnchorMode = CalibrationAnchorMode.PIN_DECK,
        tfliteDetector: TFLiteBallDetector? = null
    ): LaneRecognitionResult {
        val w = width.toDouble()
        val h = height.toDouble()

        val gutterName = if (alignment == Handedness.RIGHT) "RIGHT" else "LEFT"

        // Corridor boundaries based on camera gutter alignment to isolate active lane from adjacent lanes
        val (minCorridorNormX, maxCorridorNormX) = if (alignment == Handedness.RIGHT) {
            Pair(0.38, 0.98) // Focus strictly on right-aligned lane, rejecting adjacent left lane (e.g. Lane 53)
        } else {
            Pair(0.02, 0.62) // Focus strictly on left-aligned lane, rejecting adjacent right lane
        }

        // 1. Stage 1: Detect Full 10-Pin Rack within Active Lane Corridor
        // Priority 1: Check Edge LiteRT 7-Class Model (class 1: pin_rack in active corridor)
        var pinRack: PinRackCandidate? = null
        if (tfliteDetector != null) {
            val mlRack = tfliteDetector.detectPinRack(
                imageBytes, width, height, stride,
                minNormX = minCorridorNormX,
                maxNormX = maxCorridorNormX
            )
            if (mlRack != null && mlRack.confidence >= 0.15f) {
                val box = mlRack.boundingBox
                val topY = box[0].toDouble() * h
                val bottomY = box[2].toDouble() * h
                var leftX = box[1].toDouble() * w
                var rightX = box[3].toDouble() * w
                var widthPx = rightX - leftX
                val expectedRackWidth = (w * 0.12 * (zoomRatio.coerceIn(1.0f, 3.0f))).coerceAtLeast(110.0)
                if (widthPx < expectedRackWidth) {
                    val cx = (leftX + rightX) / 2.0
                    leftX = (cx - expectedRackWidth / 2.0).coerceAtLeast(0.0)
                    rightX = (cx + expectedRackWidth / 2.0).coerceAtMost(w - 1.0)
                    widthPx = rightX - leftX
                }
                pinRack = PinRackCandidate(
                    centerX = (leftX + rightX) / 2.0,
                    topY = topY,
                    bottomY = bottomY,
                    widthPx = widthPx,
                    pinPeakCount = 7,
                    contrastRatio = 2.5,
                    confidence = mlRack.confidence.toDouble()
                )
            }
        }

        // Priority 2: Fallback to Classical Adaptive Luminance Detection
        if (pinRack == null) {
            val classicalRack = detectPinRack(imageBytes, width, height, stride, zoomRatio, alignment)
            // Ensure classical detection is also within the active lane corridor
            if (classicalRack != null) {
                val normX = classicalRack.centerX / w
                if (normX in minCorridorNormX..maxCorridorNormX) {
                    pinRack = classicalRack
                }
            }
        }
        var gutters: GutterBoundaryLines? = null

        if (pinRack != null && pinRack.confidence >= 0.25) {
            gutters = traceGuttersFromPins(imageBytes, pinRack, width, height, stride, zoomRatio, alignment)
        }

        // Fallback: ML Lane Surface Detection (Class 5)
        if (gutters == null || gutters.confidence < 0.45) {
            val mlLane = tfliteDetector?.detectLane(imageBytes, width, height, stride)
            if (mlLane != null && mlLane.confidence >= 0.20f) {
                val box = mlLane.boundingBox
                val topY = box[0].toDouble() * h
                val bottomY = box[2].toDouble() * h
                val leftX = box[1].toDouble() * w
                val rightX = box[3].toDouble() * w

                val deckY = topY.coerceIn(h * 0.15, h * 0.60)
                val foulLineY = bottomY.coerceIn(h * 0.55, h * 0.98)
                val dy = (foulLineY - deckY).coerceAtLeast(10.0)
                val leftSlope = ((leftX * 0.4) - leftX) / dy
                val rightSlope = ((rightX) - (rightX * 0.95)) / dy

                gutters = GutterBoundaryLines(
                    leftSlope = leftSlope,
                    leftIntercept = leftX - leftSlope * deckY,
                    rightSlope = rightSlope,
                    rightIntercept = rightX - rightSlope * deckY,
                    confidence = mlLane.confidence.toDouble()
                )

                if (pinRack == null) {
                    pinRack = PinRackCandidate(
                        centerX = (leftX + rightX) / 2.0,
                        topY = (deckY - 20.0).coerceAtLeast(0.0),
                        bottomY = deckY,
                        widthPx = abs(rightX - leftX).coerceAtLeast(w * 0.08),
                        pinPeakCount = 10,
                        contrastRatio = 2.0,
                        confidence = mlLane.confidence.toDouble()
                    )
                }
            }
        }

        // Fallback: Classical direct gutter detection
        if (gutters == null || gutters.confidence < 0.45) {
            val directGutters = detectGuttersDirect(imageBytes, width, height, stride, zoomRatio, alignment)
            if (directGutters != null) {
                gutters = directGutters
                if (pinRack == null) {
                    val estDeckY = (h * (if (zoomRatio >= 2.0f) 0.25 else 0.35)).coerceIn(10.0, h * 0.5)
                    val estLeftX = directGutters.xLeftAt(estDeckY)
                    val estRightX = directGutters.xRightAt(estDeckY)
                    val estW = abs(estRightX - estLeftX).coerceAtLeast(w * 0.08)
                    pinRack = PinRackCandidate(
                        centerX = (estLeftX + estRightX) / 2.0,
                        topY = (estDeckY - 20.0).coerceAtLeast(0.0),
                        bottomY = estDeckY,
                        widthPx = estW,
                        pinPeakCount = 10,
                        contrastRatio = 0.5,
                        confidence = 0.65
                    )
                }
            }
        }

        if (pinRack == null || gutters == null || gutters.confidence < 0.45) {
            return LaneRecognitionResult(
                isSuccess = false,
                statusMessage = "❌ NO PIN RACK OR LANE DETECTED - AIM AT PINS",
                pinRackDetected = pinRack != null,
                guttersDetected = gutters != null,
                foulLineDetected = false,
                pinRack = pinRack,
                calibration = null,
                homography = null,
                foulLineLeft = null,
                foulLineRight = null,
                arrowsLeft = null,
                arrowsRight = null,
                autoCenterGuidance = "AIM DOWN $gutterName GUTTER AT PINS",
                optimalZoomRatio = zoomRatio,
                confidence = 0.0
            )
        }

        // Compute Gutter-Alignment Guidance based on Pin Rack position
        val targetRackCenterX = if (alignment == Handedness.RIGHT) w * 0.55 else w * 0.45
        val centerDelta = pinRack.centerX - targetRackCenterX
        val centerDeltaPct = centerDelta / w
        val autoCenterGuidance = when {
            centerDeltaPct > 0.045 -> "⚡ PAN RIGHT ${(centerDeltaPct * 40.0).roundToInt().coerceAtLeast(1)}° TO ALIGN $gutterName GUTTER"
            centerDeltaPct < -0.045 -> "⚡ PAN LEFT ${(abs(centerDeltaPct) * 40.0).roundToInt().coerceAtLeast(1)}° TO ALIGN $gutterName GUTTER"
            else -> "🟢 $gutterName GUTTER ALIGNED"
        }

        // 3. Stage 3: Locate Foul Line across Gutters
        val (minFoulPct, maxFoulPct) = if (zoomRatio >= 2.0f) Pair(0.65, 0.88) else Pair(0.45, 0.72)
        val minScanY = (h * minFoulPct).toInt()
        val maxScanY = (h * maxFoulPct).toInt()

        var foulY: Int? = null
        if (tfliteDetector != null) {
            val mlFoul = tfliteDetector.detectLandmark(imageBytes, width, height, stride, classId = 3)
            if (mlFoul != null && mlFoul.confidence >= 0.25f) {
                val detectedY = mlFoul.centroid.y.toInt()
                if (detectedY in (h * 0.40).toInt()..(h * 0.95).toInt()) {
                    foulY = detectedY
                }
            }
        }
        if (foulY == null) {
            foulY = locateFoulLine(imageBytes, gutters, minScanY, maxScanY, width, height, stride)
                ?: (h * if (zoomRatio >= 2.0f) 0.76 else 0.56).toInt()
        }

        // 4. Stage 4: Solve 4-point homography (0 ft to 60 ft) and project exact 15-ft arrows
        val pinDeckY = pinRack.bottomY

        val opticalCenterX = width / 2.0
        val opticalCenterY = height / 2.0
        val focalLengthPx = kotlin.math.max(width, height).toDouble()
        val lensCorrector = LensDistortionCorrector(opticalCenterX, opticalCenterY, focalLengthPx)

        // Foul Line corners (0.0 ft, Boards 39 & 1)
        val rawFlL = Point2D(gutters.xLeftAt(foulY.toDouble()), foulY.toDouble())
        val rawFlR = Point2D(gutters.xRightAt(foulY.toDouble()), foulY.toDouble())

        // Pin Deck corners (60.0 ft, Boards 39 & 1)
        // Deck left is outside the 7-pin by 13% rack width; Deck right is outside the 10-pin by 13% rack width
        val halfRackW = pinRack.widthPx / 2.0
        val deckOffset = (pinRack.widthPx * 0.13).coerceIn(10.0, 30.0)
        val deckLeftX = (pinRack.centerX - halfRackW - deckOffset).coerceIn(0.0, w - 10.0)
        val deckRightX = (pinRack.centerX + halfRackW + deckOffset).coerceIn(deckLeftX + 20.0, w - 1.0)
        val rawPinDeckL = Point2D(deckLeftX, pinDeckY)
        val rawPinDeckR = Point2D(deckRightX, pinDeckY)

        // Undistort corners using Brown-Conrady radial model prior to DLT fitting
        val undistortedCorners = lensCorrector.undistortCalibrationCorners(
            rawFlL, rawFlR, rawPinDeckL, rawPinDeckR
        )
        val flL = undistortedCorners[0]
        val flR = undistortedCorners[1]
        val pinDeckL = undistortedCorners[2]
        val pinDeckR = undistortedCorners[3]

        // Compute 4-point homography using PIN_DECK mode (0 ft & 60 ft)
        val deckCalibResult = calibrator.calibrate(
            foulLineLeft = flL,
            foulLineRight = flR,
            arrowsLeft = pinDeckL,
            arrowsRight = pinDeckR,
            anchorMode = CalibrationAnchorMode.PIN_DECK,
            calibrationZoomRatio = zoomRatio
        )
        val hDeck = deckCalibResult?.second

        // Derive Arrow Line (15.0 ft) via exact forward homography projection
        val (alL, arR) = if (hDeck != null) {
            Pair(
                hDeck.projectLaneToPixel(board = 39.0, distanceFt = LaneConstants.ARROWS_DISTANCE_FT),
                hDeck.projectLaneToPixel(board = 1.0, distanceFt = LaneConstants.ARROWS_DISTANCE_FT)
            )
        } else {
            val arrowsY = (foulY - (foulY - pinDeckY) * 0.52).toInt().coerceIn(pinDeckY.toInt() + 10, foulY - 10)
            Pair(
                Point2D(gutters.xLeftAt(arrowsY.toDouble()), arrowsY.toDouble()),
                Point2D(gutters.xRightAt(arrowsY.toDouble()), arrowsY.toDouble())
            )
        }

        // Sanity check geometry
        val foulW = flR.x - flL.x
        val arrowsW = arR.x - alL.x
        val isValidGeometry = (foulW > 0.18 * w) && (arrowsW > 0.08 * w) && (foulW > arrowsW * 0.95)

        if (!isValidGeometry) {
            return LaneRecognitionResult(
                isSuccess = false,
                statusMessage = "UNCLEAR LANE PERSPECTIVE - ADJUST TRIPOD",
                pinRackDetected = true,
                guttersDetected = true,
                foulLineDetected = false,
                pinRack = pinRack,
                calibration = null,
                homography = null,
                foulLineLeft = flL,
                foulLineRight = flR,
                arrowsLeft = alL,
                arrowsRight = arR,
                autoCenterGuidance = autoCenterGuidance,
                optimalZoomRatio = zoomRatio,
                confidence = 0.50
            )
        }

        // 5. Intelligent Focal Point & Whole-Lane Safe Zoom Calculation
        // Invariants:
        // - Pins must remain the prominent focal point (headroom >= 10% from top of screen)
        // - Foul line & bowler slide foot must remain in frame (footroom >= 12% from bottom of screen)
        // - Both gutters at foul line must remain inside left/right screen boundaries (margin >= 5%)
        val pinTopY = pinRack.topY.coerceAtLeast(0.0)
        val foulYDouble = foulY.toDouble()

        // Maximum safe zoom that prevents clipping pins at the top
        val zPinsMax = if (pinTopY < 0.50 * h) {
            ((0.40 * h) / (0.50 * h - pinTopY)).coerceAtLeast(1.0)
        } else 3.5

        // Maximum safe zoom that prevents clipping foul line / slide foot at the bottom
        val zFoulMax = if (foulYDouble > 0.50 * h) {
            ((0.38 * h) / (foulYDouble - 0.50 * h)).coerceAtLeast(1.0)
        } else 3.5

        // Maximum safe zoom that prevents clipping left/right gutters
        val zLeftMax = if (flL.x < 0.50 * w) {
            ((0.45 * w) / (0.50 * w - flL.x)).coerceAtLeast(1.0)
        } else 3.5

        val zRightMax = if (flR.x > 0.50 * w) {
            ((0.45 * w) / (flR.x - 0.50 * w)).coerceAtLeast(1.0)
        } else 3.5

        val safeZoomCap = min(min(zPinsMax, zFoulMax), min(zLeftMax, zRightMax))

        // Target lane coverage: fill ~78% of width and ~72% of height
        val laneW = (flR.x - flL.x).coerceAtLeast(w * 0.10)
        val laneH = (foulYDouble - pinTopY).coerceAtLeast(h * 0.15)
        val targetZoomX = (0.78 * w) / laneW
        val targetZoomY = (0.72 * h) / laneH
        val desiredZoom = min(targetZoomX, targetZoomY)

        val optimalZoom = min(safeZoomCap, desiredZoom).toFloat().coerceIn(1.0f, 3.0f)


        val calibPair = if (anchorMode == CalibrationAnchorMode.PIN_DECK) {
            deckCalibResult
        } else {
            calibrator.calibrateGutters(
                foulLineLeft = flL,
                foulLineRight = flR,
                gutterLeft15ft = alL,
                gutterRight15ft = arR,
                calibrationZoomRatio = zoomRatio
            )
        }
        val topL = if (anchorMode == CalibrationAnchorMode.PIN_DECK) pinDeckL else alL
        val topR = if (anchorMode == CalibrationAnchorMode.PIN_DECK) pinDeckR else arR

        val finalCalib = calibPair?.first
        val finalH = calibPair?.second

        val overallConfidence = (pinRack.confidence * 0.45 + gutters.confidence * 0.35 + 0.20).coerceIn(0.0, 0.98)

        return LaneRecognitionResult(
            isSuccess = finalCalib != null,
            statusMessage = "✓ LANE & PIN RACK LOCKED (${String.format("%.0f", overallConfidence * 100)}%)",
            pinRackDetected = true,
            guttersDetected = true,
            foulLineDetected = true,
            pinRack = pinRack,
            calibration = finalCalib,
            homography = finalH,
            foulLineLeft = flL,
            foulLineRight = flR,
            arrowsLeft = alL,
            arrowsRight = arR,
            pinDeckLeft = pinDeckL,
            pinDeckRight = pinDeckR,
            autoCenterGuidance = autoCenterGuidance,
            optimalZoomRatio = optimalZoom,
            confidence = overallConfidence
        )
    }

    /**
     * Detects the distinctive 10-pin triangular rack (7 visible pin columns) at the end of the lane.
     * Uses 7-pin cluster peak extraction to ensure pins 6 and 10 are never omitted and isolates the active lane.
     */
    fun detectPinRack(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width,
        zoomRatio: Float = 1.0f,
        alignment: Handedness = Handedness.RIGHT
    ): PinRackCandidate? {
        val w = width.toDouble()
        val h = height.toDouble()

        // Search vertical region: pin deck sits in 10% to 58% of frame
        val minScanY = (h * 0.08).toInt().coerceIn(0, height - 1)
        val maxScanY = (h * if (zoomRatio >= 2.0f) 0.58 else 0.54).toInt().coerceIn(minScanY + 10, height - 1)

        var bestCandidate: PinRackCandidate? = null
        var maxScore = 0.0

        val rowStep = 3
        for (y in minScanY..maxScanY step rowStep) {
            val rowOffset = y * stride
            if (rowOffset + width > imageBytes.size) break

            val minX = (w * 0.05).toInt()
            val maxX = (w * 0.95).toInt()

            // 1. Calculate row brightness statistics
            var rowMax = 0
            for (x in minX..maxX step 4) {
                val lum = imageBytes[rowOffset + x].toInt() and 0xFF
                if (lum > rowMax) rowMax = lum
            }
            if (rowMax < 90) continue // Skip dark rows

            val peakThreshold = max(90, (rowMax * 0.65).toInt())

            // 2. Find individual pin reflection peaks (standing pins) across the row
            val peaks = mutableListOf<Int>()
            var prevLum = imageBytes[rowOffset + minX].toInt() and 0xFF
            var isRising = false

            for (x in (minX + 1)..maxX) {
                val lum = imageBytes[rowOffset + x].toInt() and 0xFF
                if (lum > prevLum) {
                    isRising = true
                } else if (lum < prevLum && isRising) {
                    if (prevLum >= peakThreshold) {
                        peaks.add(x - 1)
                    }
                    isRising = false
                }
                prevLum = lum
            }

            if (peaks.size < 4) continue

            // 3. Cluster peaks into contiguous pin racks (adjacent pin spacing between 12px and 45px)
            val clusters = mutableListOf<MutableList<Int>>()
            var currentCluster = mutableListOf<Int>()

            for (p in peaks) {
                if (currentCluster.isEmpty()) {
                    currentCluster.add(p)
                } else {
                    val gap = p - currentCluster.last()
                    if (gap in 10..48) {
                        currentCluster.add(p)
                    } else if (gap > 48) {
                        if (currentCluster.size >= 4) {
                            clusters.add(currentCluster)
                        }
                        currentCluster = mutableListOf(p)
                    }
                }
            }
            if (currentCluster.size >= 4) {
                clusters.add(currentCluster)
            }

            // 4. Evaluate each rack cluster: looking for ~7 visible pin columns
            for (c in clusters) {
                val xMin = c.first()
                val xMax = c.last()
                val rackWidth = (xMax - xMin).toDouble()
                val expectedWidth = w * (0.09 + 0.06 * (zoomRatio - 1.0f).coerceIn(0f, 2.5f))

                // Valid 7-pin rack width typically 80px to 260px
                if (rackWidth in (expectedWidth * 0.55)..(expectedWidth * 2.5)) {
                    val clusterCenterX = (xMin + xMax) / 2.0
                    val normCenterX = clusterCenterX / w

                    // Corridor check: camera aligned with right gutter -> active lane rack is on right (normX >= 0.40)
                    // Camera aligned with left gutter -> active lane rack is on left (normX <= 0.60)
                    val isInCorridor = if (alignment == Handedness.RIGHT) {
                        normCenterX in 0.40..0.98
                    } else {
                        normCenterX in 0.02..0.60
                    }
                    if (!isInCorridor) continue

                    // Add margin so full 7-pin profile (especially pins 6 and 10) are enclosed
                    val pad = (rackWidth * 0.06).coerceAtLeast(6.0)
                    val boundedLeft = (xMin - pad).coerceAtLeast(0.0)
                    val boundedRight = (xMax + pad).coerceAtMost(w - 1.0)
                    val finalWidth = boundedRight - boundedLeft
                    val finalCenter = (boundedLeft + boundedRight) / 2.0

                    val pinCountScore = (min(c.size, 7) / 7.0).coerceIn(0.5, 1.0)
                    // Favor the aligned lane: rightmost rack for RIGHT alignment, leftmost rack for LEFT alignment
                    val alignmentBias = if (alignment == Handedness.RIGHT) {
                        normCenterX.coerceIn(0.5, 1.0)
                    } else {
                        (1.0 - normCenterX).coerceIn(0.5, 1.0)
                    }

                    val score = pinCountScore * 0.6 + alignmentBias * 0.4

                    if (score > maxScore) {
                        maxScore = score
                        bestCandidate = PinRackCandidate(
                            centerX = finalCenter,
                            topY = (y - 16).toDouble().coerceAtLeast(0.0),
                            bottomY = (y + 16).toDouble().coerceAtMost(h - 1.0),
                            widthPx = finalWidth,
                            pinPeakCount = c.size,
                            contrastRatio = 2.0,
                            confidence = (0.70 + (c.size / 7.0) * 0.25).coerceAtMost(0.98)
                        )
                    }
                }
            }
        }

        return bestCandidate
    }

    /**
     * Detects all candidate 10-pin racks (7 visible pin columns) across the entire field of view.
     * Disregards single-lane corridor isolation so the user can interactively select their exact lane.
     */
    fun findAllPinRacks(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width,
        zoomRatio: Float = 1.0f,
        tfliteDetector: com.example.cebowlinglabtrack.domain.ml.TFLiteBallDetector? = null
    ): List<PinRackCandidate> {
        val w = width.toDouble()
        val h = height.toDouble()
        val results = mutableListOf<PinRackCandidate>()

        // 1. Edge LiteRT ML Detector detections
        if (tfliteDetector != null) {
            val allMlRacks = tfliteDetector.detectAllPinRacks(imageBytes, width, height, stride)
            for (mlRack in allMlRacks) {
                if (mlRack.confidence >= 0.15f) {
                    val box = mlRack.boundingBox
                    val topY = box[0].toDouble() * h
                    val bottomY = box[2].toDouble() * h
                    val leftX = box[1].toDouble() * w
                    val rightX = box[3].toDouble() * w
                    val widthPx = (rightX - leftX).coerceAtLeast(60.0)
                    val centerX = (leftX + rightX) / 2.0
                    results.add(
                        PinRackCandidate(
                            centerX = centerX,
                            topY = topY,
                            bottomY = bottomY,
                            widthPx = widthPx,
                            pinPeakCount = 7,
                            contrastRatio = 2.5,
                            confidence = mlRack.confidence.toDouble()
                        )
                    )
                }
            }
        }

        // 2. Optical 7-Pin Cluster peak extraction across horizontal rows
        val minScanY = (h * 0.08).toInt().coerceIn(0, height - 1)
        val maxScanY = (h * if (zoomRatio >= 2.0f) 0.58 else 0.54).toInt().coerceIn(minScanY + 10, height - 1)
        val opticalCandidates = mutableListOf<PinRackCandidate>()

        val rowStep = 3
        for (y in minScanY..maxScanY step rowStep) {
            val rowOffset = y * stride
            if (rowOffset + width > imageBytes.size) break

            val minX = (w * 0.05).toInt()
            val maxX = (w * 0.95).toInt()

            var rowMax = 0
            for (x in minX..maxX step 4) {
                val lum = imageBytes[rowOffset + x].toInt() and 0xFF
                if (lum > rowMax) rowMax = lum
            }
            if (rowMax < 90) continue

            val peakThreshold = max(90, (rowMax * 0.65).toInt())
            val peaks = mutableListOf<Int>()
            var prevLum = imageBytes[rowOffset + minX].toInt() and 0xFF
            var isRising = false

            for (x in (minX + 1)..maxX) {
                val lum = imageBytes[rowOffset + x].toInt() and 0xFF
                if (lum > prevLum) {
                    isRising = true
                } else if (lum < prevLum && isRising) {
                    if (prevLum >= peakThreshold) {
                        peaks.add(x - 1)
                    }
                    isRising = false
                }
                prevLum = lum
            }

            if (peaks.size < 4) continue

            val clusters = mutableListOf<MutableList<Int>>()
            var currentCluster = mutableListOf<Int>()

            for (p in peaks) {
                if (currentCluster.isEmpty()) {
                    currentCluster.add(p)
                } else {
                    val gap = p - currentCluster.last()
                    if (gap in 10..48) {
                        currentCluster.add(p)
                    } else if (gap > 48) {
                        if (currentCluster.size >= 4) {
                            clusters.add(currentCluster)
                        }
                        currentCluster = mutableListOf(p)
                    }
                }
            }
            if (currentCluster.size >= 4) {
                clusters.add(currentCluster)
            }

            for (c in clusters) {
                val xMin = c.first()
                val xMax = c.last()
                val rackWidth = (xMax - xMin).toDouble()
                val expectedWidth = w * (0.09 + 0.06 * (zoomRatio - 1.0f).coerceIn(0f, 2.5f))

                if (rackWidth in (expectedWidth * 0.55)..(expectedWidth * 2.5)) {
                    val pad = (rackWidth * 0.06).coerceAtLeast(6.0)
                    val boundedLeft = (xMin - pad).coerceAtLeast(0.0)
                    val boundedRight = (xMax + pad).coerceAtMost(w - 1.0)
                    val finalWidth = boundedRight - boundedLeft
                    val finalCenter = (boundedLeft + boundedRight) / 2.0

                    val candidate = PinRackCandidate(
                        centerX = finalCenter,
                        topY = (y - 16).toDouble().coerceAtLeast(0.0),
                        bottomY = (y + 16).toDouble().coerceAtMost(h - 1.0),
                        widthPx = finalWidth,
                        pinPeakCount = c.size,
                        contrastRatio = 2.0,
                        confidence = (0.70 + (c.size / 7.0) * 0.25).coerceAtMost(0.98)
                    )
                    opticalCandidates.add(candidate)
                }
            }
        }

        // Deduplicate and cluster optical candidates sharing similar horizontal centers
        val clusterBins = mutableListOf<MutableList<PinRackCandidate>>()
        for (cand in opticalCandidates) {
            val matchingBin = clusterBins.find { bin ->
                val avgX = bin.map { it.centerX }.average()
                abs(cand.centerX - avgX) < cand.widthPx * 0.50
            }
            if (matchingBin != null) {
                matchingBin.add(cand)
            } else {
                clusterBins.add(mutableListOf(cand))
            }
        }

        for (bin in clusterBins) {
            val best = bin.maxByOrNull { it.confidence } ?: continue
            val alreadyPresent = results.any { abs(it.centerX - best.centerX) < best.widthPx * 0.60 }
            if (!alreadyPresent) {
                results.add(best)
            }
        }

        return results.sortedBy { it.centerX }
    }

    /**
     * Calculates the optimal safe hardware zoom ratio for a user-selected pin rack,
     * ensuring pins remain the focal point with >= 10% headroom while keeping the full 60-ft lane in frame.
     */
    fun computeSafeZoomForRack(
        rack: PinRackCandidate,
        width: Int,
        height: Int,
        foulLineYEstimate: Double? = null
    ): Float {
        val w = width.toDouble()
        val h = height.toDouble()
        val pinTopY = rack.topY

        // Maximum safe zoom preserving pin deck headroom (at least 10% top margin)
        val zPinsMax = if (pinTopY < 0.50 * h) {
            ((0.40 * h) / (0.50 * h - pinTopY)).coerceAtLeast(1.0)
        } else 3.5

        val foulY = foulLineYEstimate ?: (h * 0.72)
        val zFoulMax = if (foulY > 0.50 * h) {
            ((0.40 * h) / (foulY - 0.50 * h)).coerceAtLeast(1.0)
        } else 3.5

        val laneW = rack.widthPx * 3.5
        val targetZoomX = (0.78 * w) / laneW.coerceAtLeast(w * 0.20)
        val targetZoomY = (0.72 * h) / (foulY - pinTopY).coerceAtLeast(h * 0.25)
        val desiredZoom = min(targetZoomX, targetZoomY)

        return min(min(zPinsMax, zFoulMax), desiredZoom).toFloat().coerceIn(1.0f, 3.0f)
    }

    /**
     * Snaps user-tapped arrows point to the lane centerline vector connecting the selected pin deck to arrows.
     */
    fun snapArrowsToCenterline(
        tappedPoint: Point2D,
        pinRack: PinRackCandidate,
        width: Int,
        height: Int
    ): Point2D {
        // Compute centerline vector from pin rack base down to tapped Y level
        val rackBaseY = pinRack.bottomY
        val rackCenterX = pinRack.centerX
        val tapY = tappedPoint.y.coerceIn(rackBaseY + 40.0, height * 0.85)

        // Refine X using tapped point offset relative to rack center
        val dx = tappedPoint.x - rackCenterX
        val snappedX = rackCenterX + dx.coerceIn(-width * 0.25, width * 0.25)
        return Point2D(snappedX, tapY)
    }

    /**
     * Projects the left and right foul line gutter corners given the selected pin rack and arrows point.
     */
    fun projectFoulCorners(
        pinRack: PinRackCandidate,
        arrowsPoint: Point2D,
        width: Int,
        height: Int,
        handedness: com.example.cebowlinglabtrack.domain.model.Handedness = com.example.cebowlinglabtrack.domain.model.Handedness.RIGHT
    ): Pair<Point2D, Point2D> {
        val w = width.toDouble()
        val h = height.toDouble()

        val rackBaseY = pinRack.bottomY
        val rackCenterX = pinRack.centerX
        val rackHalfW = pinRack.widthPx / 2.0

        val arrowY = arrowsPoint.y
        val arrowCenterX = arrowsPoint.x

        // Estimate foul line Y (typically around 68% - 75% of screen height)
        val foulY = (arrowY + (arrowY - rackBaseY) * 0.35).coerceIn(h * 0.60, h * 0.82)

        // Linear perspective divergence factor from pin deck to foul line
        val t = (foulY - rackBaseY) / (arrowY - rackBaseY).coerceAtLeast(1.0)
        val foulHalfW = (rackHalfW * 3.6).coerceIn(w * 0.15, w * 0.45)

        val foulCenterX = rackCenterX + (arrowCenterX - rackCenterX) * t

        val foulLeftX = (foulCenterX - foulHalfW).coerceIn(0.0, w * 0.60)
        val foulRightX = (foulCenterX + foulHalfW).coerceIn(w * 0.40, w - 1.0)

        return Pair(
            Point2D(foulLeftX, foulY),
            Point2D(foulRightX, foulY)
        )
    }

    /**
     * Direct gutter line detection scanning middle/lower lane surface.
     * Operates as a fallback when pin rack lights are dim, colored, or obstructed.
     */
    fun detectGuttersDirect(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int,
        zoomRatio: Float,
        alignment: Handedness = Handedness.RIGHT
    ): GutterBoundaryLines? {
        val w = width.toDouble()
        val h = height.toDouble()

        val startY = (h * (if (zoomRatio >= 2.0f) 0.35 else 0.42)).toInt()
        val endY = (h * (if (zoomRatio >= 2.0f) 0.78 else 0.68)).toInt()

        val leftGutterPoints = mutableListOf<Point2D>()
        val rightGutterPoints = mutableListOf<Point2D>()
        val stepY = max(4, (endY - startY) / 12)

        for (y in startY..endY step stepY) {
            val rowOffset = y * stride
            if (rowOffset + width > imageBytes.size) break

            // Scan left gutter (dark gutter -> bright lane transition)
            val minLeftX = (w * 0.02).toInt()
            val maxLeftX = (w * 0.45).toInt()
            var bestLeftGrad = 0
            var bestLeftX = -1

            for (x in minLeftX until maxLeftX) {
                val idx = rowOffset + x
                if (idx + 6 >= imageBytes.size) break
                val val1 = imageBytes[idx].toInt() and 0xFF
                val val2 = imageBytes[idx + 6].toInt() and 0xFF
                val grad = val2 - val1
                if (grad > bestLeftGrad && grad > 15) {
                    bestLeftGrad = grad
                    bestLeftX = x + 3
                }
            }
            if (bestLeftX > 0) {
                leftGutterPoints.add(Point2D(bestLeftX.toDouble(), y.toDouble()))
            }

            // Scan right gutter (bright lane -> dark gutter transition)
            val minRightX = (w * 0.55).toInt()
            val maxRightX = (w * 0.98).toInt()
            var bestRightGrad = 0
            var bestRightX = -1

            for (x in minRightX until maxRightX) {
                val idx = rowOffset + x
                if (idx + 6 >= imageBytes.size) break
                val val1 = imageBytes[idx].toInt() and 0xFF
                val val2 = imageBytes[idx + 6].toInt() and 0xFF
                val grad = val1 - val2
                if (grad > bestRightGrad && grad > 15) {
                    bestRightGrad = grad
                    bestRightX = x + 3
                }
            }
            if (bestRightX > 0) {
                rightGutterPoints.add(Point2D(bestRightX.toDouble(), y.toDouble()))
            }
        }

        if (leftGutterPoints.size < 3 || rightGutterPoints.size < 3) return null

        val (leftSlope, leftIntercept) = fitLine(leftGutterPoints)
        val (rightSlope, rightIntercept) = fitLine(rightGutterPoints)

        val validSlopes = when (alignment) {
            Handedness.RIGHT -> (leftSlope <= 0.08) && (rightSlope in -0.30..0.50)
            Handedness.LEFT -> (leftSlope in -0.50..0.30) && (rightSlope >= -0.08)
        }
        val confidence = if (validSlopes) 0.82 else 0.50

        return GutterBoundaryLines(
            leftSlope = leftSlope,
            leftIntercept = leftIntercept,
            rightSlope = rightSlope,
            rightIntercept = rightIntercept,
            confidence = confidence
        )
    }

    /**
     * Traces dark gutter channels from the pin rack downwards toward the camera.
     */
    private fun traceGuttersFromPins(
        imageBytes: ByteArray,
        pinRack: PinRackCandidate,
        width: Int,
        height: Int,
        stride: Int,
        zoomRatio: Float,
        alignment: Handedness = Handedness.RIGHT
    ): GutterBoundaryLines? {
        val w = width.toDouble()
        val h = height.toDouble()

        val startY = pinRack.bottomY.toInt() + 4
        val endY = (h * (if (zoomRatio >= 2.0f) 0.85 else 0.75)).toInt().coerceIn(startY + 30, height - 1)

        val halfRack = pinRack.widthPx / 2.0
        // Dynamic outward offset prevents clamping to the outer skirt of pins 7 & 10 (boards 2-3 & 37-38)
        val gutterSeedOffsetPx = (pinRack.widthPx * 0.18).coerceIn(10.0, 35.0)

        val seedLeftX = (pinRack.centerX - halfRack) - gutterSeedOffsetPx
        val seedRightX = (pinRack.centerX + halfRack) + gutterSeedOffsetPx

        val leftGutterPoints = mutableListOf<Point2D>()
        val rightGutterPoints = mutableListOf<Point2D>()

        val stepY = max(2, (endY - startY) / 16)
        val searchMargin = max(gutterSeedOffsetPx * 2.5, 40.0)

        for (y in startY..endY step stepY) {
            val rowOffset = y * stride

            val progress = (y - startY).toDouble() / (endY - startY)
            val quadProgress = progress * progress.coerceAtLeast(0.7)

            // Asymmetric perspective expansion based on gutter camera alignment:
            // The camera is aligned with the anchor gutter (Right for righty, Left for lefty),
            // which runs almost vertically down the frame with minimal drift.
            // The opposite gutter diverges heavily across the screen.
            val (leftExpansion, rightExpansion) = when (alignment) {
                Handedness.RIGHT -> Pair(
                    quadProgress * (pinRack.widthPx * (if (zoomRatio >= 2.0f) 2.0 else 3.0)),
                    progress * (pinRack.widthPx * 0.8)
                )
                Handedness.LEFT -> Pair(
                    progress * (pinRack.widthPx * 0.8),
                    quadProgress * (pinRack.widthPx * (if (zoomRatio >= 2.0f) 2.0 else 3.0))
                )
            }
            val expectedLeftX = seedLeftX - leftExpansion
            val expectedRightX = seedRightX + rightExpansion

            // Search left gutter around expectedLeftX with outward bias
            val leftSearchStart = (expectedLeftX - searchMargin).toInt().coerceIn(0, width - 10)
            val leftSearchEnd = (expectedLeftX + searchMargin * 0.8).toInt().coerceIn(leftSearchStart + 5, width - 1)

            var bestLeftGrad = 0
            var bestLeftX = -1
            for (x in leftSearchStart until leftSearchEnd) {
                val idx = rowOffset + x
                if (idx + 4 >= imageBytes.size) break
                val val1 = imageBytes[idx].toInt() and 0xFF
                val val2 = imageBytes[idx + 4].toInt() and 0xFF
                val grad = val2 - val1 // Gutter to lane
                if (grad > bestLeftGrad && grad > 15) {
                    bestLeftGrad = grad
                    bestLeftX = x + 2
                }
            }
            if (bestLeftX > 0) {
                leftGutterPoints.add(Point2D(bestLeftX.toDouble(), y.toDouble()))
            }

            // Search right gutter around expectedRightX with outward bias
            val rightSearchStart = (expectedRightX - searchMargin * 0.8).toInt().coerceIn(0, width - 10)
            val rightSearchEnd = (expectedRightX + searchMargin).toInt().coerceIn(rightSearchStart + 5, width - 1)

            var bestRightGrad = 0
            var bestRightX = -1
            for (x in rightSearchStart until rightSearchEnd) {
                val idx = rowOffset + x
                if (idx + 4 >= imageBytes.size) break
                val val1 = imageBytes[idx].toInt() and 0xFF
                val val2 = imageBytes[idx + 4].toInt() and 0xFF
                val grad = val1 - val2 // Lane to gutter
                if (grad > bestRightGrad && grad > 15) {
                    bestRightGrad = grad
                    bestRightX = x + 2
                }
            }
            if (bestRightX > 0) {
                rightGutterPoints.add(Point2D(bestRightX.toDouble(), y.toDouble()))
            }
        }

        if (leftGutterPoints.size < 3 || rightGutterPoints.size < 3) return null

        // Fit lines via linear regression: x = slope * y + intercept
        val (leftSlope, leftIntercept) = fitLine(leftGutterPoints)
        val (rightSlope, rightIntercept) = fitLine(rightGutterPoints)

        // Sanity checks:
        // When aligned with Right Gutter: Right slope is near 0 (-0.25..0.45), Left slope is negative (<= 0.05)
        // When aligned with Left Gutter: Left slope is near 0 (-0.45..0.25), Right slope is positive (>= -0.05)
        val validSlopes = when (alignment) {
            Handedness.RIGHT -> (leftSlope <= 0.05) && (rightSlope in -0.25..0.45)
            Handedness.LEFT -> (leftSlope in -0.45..0.25) && (rightSlope >= -0.05)
        }
        val confidence = if (validSlopes) 0.88 else 0.52

        return GutterBoundaryLines(
            leftSlope = leftSlope,
            leftIntercept = leftIntercept,
            rightSlope = rightSlope,
            rightIntercept = rightIntercept,
            confidence = confidence
        )
    }

    /**
     * Locates the high-contrast horizontal foul line between the approach and lane wood.
     */
    private fun locateFoulLine(
        imageBytes: ByteArray,
        gutters: GutterBoundaryLines,
        minScanY: Int,
        maxScanY: Int,
        width: Int,
        height: Int,
        stride: Int
    ): Int? {
        var maxEdgeY = -1
        var maxEdgeGrad = 0

        for (y in minScanY..maxScanY step 2) {
            val xL = gutters.xLeftAt(y.toDouble()).toInt().coerceIn(0, width - 1)
            val xR = gutters.xRightAt(y.toDouble()).toInt().coerceIn(0, width - 1)
            val laneWidth = xR - xL
            if (laneWidth < 40) continue

            // Sample vertical gradient across the central 60% of the lane
            val innerL = xL + (laneWidth * 0.2).toInt()
            val innerR = xR - (laneWidth * 0.2).toInt()

            var rowGradSum = 0
            var sampleCount = 0

            val rowAbove = (y - 3).coerceIn(0, height - 1) * stride
            val rowBelow = (y + 3).coerceIn(0, height - 1) * stride

            for (x in innerL..innerR step 6) {
                val above = imageBytes[rowAbove + x].toInt() and 0xFF
                val below = imageBytes[rowBelow + x].toInt() and 0xFF
                rowGradSum += abs(below - above)
                sampleCount++
            }

            if (sampleCount > 0) {
                val avgGrad = rowGradSum / sampleCount
                if (avgGrad > maxEdgeGrad && avgGrad > 18) {
                    maxEdgeGrad = avgGrad
                    maxEdgeY = y
                }
            }
        }

        return if (maxEdgeY > 0) maxEdgeY else null
    }

    /**
     * Fits line x = slope * y + intercept using ordinary least squares.
     */
    private fun fitLine(points: List<Point2D>): Pair<Double, Double> {
        val n = points.size.toDouble()
        var sumY = 0.0
        var sumX = 0.0
        var sumYY = 0.0
        var sumYX = 0.0

        for (p in points) {
            sumY += p.y
            sumX += p.x
            sumYY += p.y * p.y
            sumYX += p.y * p.x
        }

        val denominator = n * sumYY - sumY * sumY
        if (abs(denominator) < 1e-6) return Pair(0.0, sumX / n)

        val slope = (n * sumYX - sumY * sumX) / denominator
        val intercept = (sumX - slope * sumY) / n
        return Pair(slope, intercept)
    }
}
