package com.example.cebowlinglabtrack.domain.ml

import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-speed optical ball detector operating on camera luminance (Y-plane) frames.
 *
 * Employs temporal frame differencing, lane polygon ROI masking, and connected-component
 * centroid analysis to detect bowling ball motion at 60-120 FPS with <3ms latency.
 */
class OpticalBallDetector(
    private var homography: HomographyMatrix? = null,
    private val motionThreshold: Int = 28,
    private val minClusterPixels: Int = 12,
    private val maxClusterPixels: Int = 3000
) : BallDetectorEngine {

    private var prevFrameBytes: ByteArray? = null
    private var prevWidth = 0
    private var prevHeight = 0
    private var lastLatencyMs: Double = 0.0

    // Calibrated lane polygon in screen coordinates (FlL, FlR, DeckR, DeckL)
    private var lanePolygon: List<Point2D>? = null
    private var laneMinY = 0.0
    private var laneMaxY = 0.0

    private var lastKnownBallPos: Point2D? = null
    private var consecutiveLostFrames = 0
    private var lastBallRadiusPx: Int = 20

    private class MotionCluster {
        var sumX: Long = 0L
        var sumY: Long = 0L
        var count: Int = 0
        var minX: Int = Int.MAX_VALUE
        var maxX: Int = Int.MIN_VALUE
        var minY: Int = Int.MAX_VALUE
        var maxY: Int = Int.MIN_VALUE

        fun add(x: Int, y: Int) {
            sumX += x
            sumY += y
            count++
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        fun width(): Int = if (count > 0) maxX - minX + 1 else 0
        fun height(): Int = if (count > 0) maxY - minY + 1 else 0
        fun cx(): Double = if (count > 0) sumX.toDouble() / count else 0.0
        fun cy(): Double = if (count > 0) sumY.toDouble() / count else 0.0
        fun aspectRatio(): Double = width().toDouble() / max(1, height())

        fun reset() {
            sumX = 0L
            sumY = 0L
            count = 0
            minX = Int.MAX_VALUE
            maxX = Int.MIN_VALUE
            minY = Int.MAX_VALUE
            maxY = Int.MIN_VALUE
        }
    }

    private val clusterPool = Array(8) { MotionCluster() }

    fun updateHomography(newH: HomographyMatrix) {
        this.homography = newH
        // Precompute screen coordinates of the 4 lane corners to enable fast trapezoid bounds
        val flL = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.0, 0.0))
        val flR = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.0, 0.0))
        val deckR = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.0, 60.0))
        val deckL = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.0, 60.0))
        lanePolygon = listOf(flL, flR, deckR, deckL)
        laneMinY = minOf(flL.y, flR.y, deckL.y, deckR.y)
        laneMaxY = maxOf(flL.y, flR.y, deckL.y, deckR.y)
        lastKnownBallPos = null
        consecutiveLostFrames = 0
    }

    override fun detectBall(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int
    ): Point2D? {
        val startNs = System.nanoTime()

        val prev = prevFrameBytes
        if (prev == null || prevWidth != width || prevHeight != height || prev.size != imageBytes.size) {
            // Store baseline frame
            prevFrameBytes = imageBytes.copyOf()
            prevWidth = width
            prevHeight = height
            lastLatencyMs = (System.nanoTime() - startNs) / 1_000_000.0
            return null
        }

        // Bounding box of lane polygon if available to minimize vertical search space
        val poly = lanePolygon
        var roiMinY = 0
        var roiMaxY = height

        if (poly != null && poly.size == 4) {
            roiMinY = max(0, laneMinY.toInt() - 15)
            roiMaxY = min(height - 1, laneMaxY.toInt() + 15)
        }

        // Adaptive ambient compensation: sample lane midpoint to detect global exposure shift
        var ambientSum = 0
        var sampleCount = 0
        val sampleY = ((roiMinY + roiMaxY) / 2).coerceIn(0, height - 1)
        val sampleRow = sampleY * stride
        val (sLeft, sRight) = getLaneSpanAtY(sampleY, width)
        if (sRight > sLeft) {
            val stepSample = max(1, (sRight - sLeft) / 8)
            for (sx in sLeft..sRight step stepSample) {
                val idx = sampleRow + sx
                if (idx < imageBytes.size) {
                    val c = imageBytes[idx].toInt() and 0xFF
                    val p = prev[idx].toInt() and 0xFF
                    ambientSum += (c - p)
                    sampleCount++
                }
            }
        }
        val ambientShift = if (sampleCount > 0) ambientSum / sampleCount else 0
        val effectiveMotionThreshold = max(motionThreshold, abs(ambientShift) + 16)

        // Subsample step for speed (step=2 checks 1 in 4 pixels; sub-millisecond on mobile CPU)
        val step = 2
        var activeClusterIdx = 0
        clusterPool[0].reset()

        var lastMotionY = -1

        for (y in roiMinY until roiMaxY step step) {
            val (scanLeft, scanRight) = getLaneSpanAtY(y, width)
            if (scanLeft >= scanRight) continue

            val rowOffset = y * stride
            var rowHadMotion = false

            for (x in scanLeft until scanRight step step) {
                val idx = rowOffset + x
                if (idx >= imageBytes.size) break

                val currVal = imageBytes[idx].toInt() and 0xFF
                val prevVal = prev[idx].toInt() and 0xFF
                val diff = abs(currVal - prevVal)

                if (diff > effectiveMotionThreshold) {
                    // Split into new cluster if there is a vertical gap > 40px
                    if (lastMotionY >= 0 && (y - lastMotionY) > 40 && clusterPool[activeClusterIdx].count > 0) {
                        if (activeClusterIdx < clusterPool.size - 1) {
                            activeClusterIdx++
                            clusterPool[activeClusterIdx].reset()
                        }
                    }

                    clusterPool[activeClusterIdx].add(x, y)
                    rowHadMotion = true
                }
            }

            if (rowHadMotion) {
                lastMotionY = y
            }
        }

        // Copy current frame to prev for next differencing cycle
        System.arraycopy(imageBytes, 0, prev, 0, imageBytes.size)
        lastLatencyMs = (System.nanoTime() - startNs) / 1_000_000.0

        // Select best candidate cluster
        var bestCandidate: MotionCluster? = null
        var bestScore = Double.MAX_VALUE

        for (i in 0..activeClusterIdx) {
            val c = clusterPool[i]
            if (c.count in minClusterPixels..maxClusterPixels) {
                val aspect = c.aspectRatio()
                if (aspect in 0.25..4.0 && c.width() in 6..140 && c.height() in 6..140) {
                    val cx = c.cx()
                    val cy = c.cy()

                    val score = if (lastKnownBallPos != null) {
                        val dx = cx - lastKnownBallPos!!.x
                        val dy = cy - lastKnownBallPos!!.y
                        val dist = sqrt(dx * dx + dy * dy)
                        // Penalize moving backwards towards foul line in y-down coords
                        dist + (if (dy > 15.0) 200.0 else 0.0)
                    } else {
                        // At ball release, favor cluster closest to foul line (largest Y)
                        val aspectPenalty = abs(aspect - 1.0) * 40.0
                        (roiMaxY - cy) + aspectPenalty
                    }

                    if (score < bestScore) {
                        bestScore = score
                        bestCandidate = c
                    }
                }
            }
        }

        if (bestCandidate != null) {
            val cx = bestCandidate.cx()
            val cy = bestCandidate.cy()
            lastKnownBallPos = Point2D(cx, cy)
            consecutiveLostFrames = 0
            lastBallRadiusPx = ((max(bestCandidate.width(), bestCandidate.height()) / 2.0).toInt()).coerceIn(8, 60)
            return Point2D(cx, cy)
        } else {
            consecutiveLostFrames++
            if (consecutiveLostFrames > 12) {
                lastKnownBallPos = null
            }
            return null
        }
    }

    private fun getLaneSpanAtY(y: Int, width: Int): Pair<Int, Int> {
        val poly = lanePolygon
        if (poly == null || poly.size != 4) {
            return Pair(0, width - 1)
        }

        val flL = poly[0]
        val flR = poly[1]
        val deckR = poly[2]
        val deckL = poly[3]

        if (y < laneMinY - 10 || y > laneMaxY + 10) {
            return Pair(0, -1)
        }

        val tL = ((y - deckL.y) / (flL.y - deckL.y + 1e-9)).coerceIn(0.0, 1.0)
        val tR = ((y - deckR.y) / (flR.y - deckR.y + 1e-9)).coerceIn(0.0, 1.0)

        val xL = deckL.x + tL * (flL.x - deckL.x)
        val xR = deckR.x + tR * (flR.x - deckR.x)

        val minX = max(0, (min(xL, xR) - 6).toInt())
        val maxX = min(width - 1, (max(xL, xR) + 6).toInt())

        return Pair(minX, maxX)
    }

    fun getLastBallRadiusPx(): Int = lastBallRadiusPx

    override fun getLastInferenceLatencyMs(): Double = lastLatencyMs

    /**
     * Resets baseline frame buffer (e.g. after camera recalibration).
     */
    fun resetBaseline() {
        prevFrameBytes = null
        lastKnownBallPos = null
        consecutiveLostFrames = 0
    }
}
