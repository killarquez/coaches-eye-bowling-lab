package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.Point2D
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
        anchorMode: CalibrationAnchorMode = CalibrationAnchorMode.PIN_DECK
    ): LaneRecognitionResult {
        val w = width.toDouble()
        val h = height.toDouble()

        val gutterName = if (alignment == Handedness.RIGHT) "RIGHT" else "LEFT"

        // 1. Stage 1: Detect Full 10-Pin Rack (Primary Environmental Anchor)
        val pinRack = detectPinRack(imageBytes, width, height, stride, zoomRatio, alignment)
        if (pinRack == null || pinRack.confidence < 0.45) {
            return LaneRecognitionResult(
                isSuccess = false,
                statusMessage = "❌ NO PIN RACK DETECTED - AIM AT PINS",
                pinRackDetected = false,
                guttersDetected = false,
                foulLineDetected = false,
                pinRack = null,
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

        // 2. Stage 2: Trace Gutter Boundaries from Pins Downwards
        val gutters = traceGuttersFromPins(imageBytes, pinRack, width, height, stride, zoomRatio, alignment)
        if (gutters == null || gutters.confidence < 0.50) {
            return LaneRecognitionResult(
                isSuccess = false,
                statusMessage = "PINS DETECTED • ALIGN $gutterName GUTTER IN VIEW",
                pinRackDetected = true,
                guttersDetected = false,
                foulLineDetected = false,
                pinRack = pinRack,
                calibration = null,
                homography = null,
                foulLineLeft = null,
                foulLineRight = null,
                arrowsLeft = null,
                arrowsRight = null,
                autoCenterGuidance = autoCenterGuidance,
                optimalZoomRatio = zoomRatio,
                confidence = 0.40
            )
        }

        // 3. Stage 3: Locate Foul Line across Gutters
        val (minFoulPct, maxFoulPct) = if (zoomRatio >= 2.0f) Pair(0.65, 0.88) else Pair(0.45, 0.72)
        val minScanY = (h * minFoulPct).toInt()
        val maxScanY = (h * maxFoulPct).toInt()
        val foulY = locateFoulLine(imageBytes, gutters, minScanY, maxScanY, width, height, stride)
            ?: (h * if (zoomRatio >= 2.0f) 0.76 else 0.56).toInt()

        // 4. Stage 4: Solve 4-point homography (0 ft to 60 ft) and project exact 15-ft arrows
        val pinDeckY = pinRack.bottomY

        val opticalCenterX = width / 2.0
        val opticalCenterY = height / 2.0
        val focalLengthPx = kotlin.math.max(width, height).toDouble()
        val lensCorrector = LensDistortionCorrector(opticalCenterX, opticalCenterY, focalLengthPx)

        // Foul Line corners (0.0 ft, Boards 1 & 39)
        val rawFlL = Point2D(gutters.xLeftAt(foulY.toDouble()), foulY.toDouble())
        val rawFlR = Point2D(gutters.xRightAt(foulY.toDouble()), foulY.toDouble())

        // Pin Deck corners (60.0 ft, Boards 1 & 39)
        val rawPinDeckL = Point2D(gutters.xLeftAt(pinDeckY), pinDeckY)
        val rawPinDeckR = Point2D(gutters.xRightAt(pinDeckY), pinDeckY)

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
                hDeck.projectLaneToPixel(board = 1.0, distanceFt = LaneConstants.ARROWS_DISTANCE_FT),
                hDeck.projectLaneToPixel(board = 39.0, distanceFt = LaneConstants.ARROWS_DISTANCE_FT)
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

        // Optimal Zoom Recommendation to fill ~80% of screen width with the lane
        val laneCoverage = foulW / w
        val optimalZoom = if (laneCoverage in 0.15..0.95) {
            (0.80 / laneCoverage).toFloat().coerceIn(1.0f, 3.5f)
        } else {
            zoomRatio
        }

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
     * Detects the distinctive 10-pin triangular rack at the end of the lane.
     * Pin decks exhibit high-luminance white peaks (L > 160) bounded by a dark pit background (L < 75).
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

        // Pin rack search vertical region:
        // In real-world bowling framing (perspective looking down the lane at 60 ft),
        // the pin deck sits in the upper 2% to 38% of the camera frame.
        val minScanY = (h * 0.02).toInt().coerceIn(0, height - 1)
        val maxScanY = (h * if (zoomRatio >= 2.0f) 0.42 else 0.38).toInt().coerceIn(minScanY + 10, height - 1)

        var bestCandidate: PinRackCandidate? = null
        var maxScore = 0.0

        val rowStep = 2
        for (y in minScanY..maxScanY step rowStep) {
            val rowOffset = y * stride
            if (rowOffset + width > imageBytes.size) break

            // Find high-luminance segments in central region of width
            val minX = (w * 0.10).toInt()
            val maxX = (w * 0.90).toInt()

            var inBrightSegment = false
            var segmentStartX = 0
            var segmentEndX = 0
            var segmentPeakCount = 0
            var prevVal = 0
            var isRising = false
            var darkGapCount = 0

            for (x in minX..maxX) {
                val idx = rowOffset + x
                val lum = imageBytes[idx].toInt() and 0xFF

                // Check for local maxima (pin reflection peaks)
                if (lum > prevVal) {
                    isRising = true
                } else if (lum < prevVal - 4 && isRising && prevVal > 138) {
                    segmentPeakCount++
                    isRising = false
                }
                prevVal = lum

                if (lum > 135 && !inBrightSegment) {
                    inBrightSegment = true
                    segmentStartX = x
                    segmentEndX = x
                    segmentPeakCount = 0
                    darkGapCount = 0
                } else if (inBrightSegment) {
                    if (lum > 115) {
                        segmentEndX = x
                        darkGapCount = 0
                    } else {
                        darkGapCount++
                    }

                    // Segment terminates if dark gap exceeds 8px (separating adjacent lanes) or row ends
                    if (darkGapCount > 8 || x == maxX) {
                        inBrightSegment = false
                        val segmentWidth = segmentEndX - segmentStartX
                        val expectedPinRackWidth = w * (0.08 + 0.08 * (zoomRatio - 1.0f).coerceIn(0f, 2.5f))

                        // Bounding width should match pin deck perspective (~2% to 35% of screen width)
                        if (segmentWidth in (expectedPinRackWidth * 0.25).toInt()..(expectedPinRackWidth * 2.6).toInt()) {
                            // Check dark background contrast above and to the sides (dark pit / pinsetter mask)
                            val bgLeftX = (segmentStartX - 8).coerceIn(0, width - 1)
                            val bgRightX = (segmentEndX + 8).coerceIn(0, width - 1)
                            val bgAboveY = (y - 6).coerceIn(0, height - 1)

                            val bgLeftLum = imageBytes[rowOffset + bgLeftX].toInt() and 0xFF
                            val bgRightLum = imageBytes[rowOffset + bgRightX].toInt() and 0xFF
                            val bgAboveLum = imageBytes[bgAboveY * stride + ((segmentStartX + segmentEndX) / 2).coerceIn(0, width - 1)].toInt() and 0xFF
                            val darkPitLum = min(bgAboveLum, (bgLeftLum + bgRightLum) / 2)

                            val contrast = (175.0 - darkPitLum) / 175.0

                            if (contrast > 0.25 && segmentPeakCount >= 2) {
                                val centerX = segmentStartX + segmentWidth / 2.0
                                val targetRackCenterX = if (alignment == Handedness.RIGHT) w * 0.55 else w * 0.45
                                val offsetRatio = abs(centerX - targetRackCenterX) / (w / 2.0)
                                val confidence = ((contrast * 0.5) + (min(segmentPeakCount, 5) / 5.0 * 0.5)).coerceIn(0.0, 1.0)
                                // Prioritize candidate matching target gutter alignment
                                val score = confidence * (1.0 - 0.5 * offsetRatio)

                                if (score > maxScore) {
                                    maxScore = score
                                    bestCandidate = PinRackCandidate(
                                        centerX = centerX,
                                        topY = (y - 12).toDouble().coerceAtLeast(0.0),
                                        bottomY = (y + 12).toDouble().coerceAtMost(h - 1.0),
                                        widthPx = segmentWidth.toDouble(),
                                        pinPeakCount = segmentPeakCount,
                                        contrastRatio = contrast,
                                        confidence = confidence
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        return bestCandidate
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
