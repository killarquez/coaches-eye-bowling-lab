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

    fun updateHomography(newH: HomographyMatrix) {
        this.homography = newH
        // Precompute screen coordinates of the 4 lane corners to enable fast 2D point-in-polygon tests
        val flL = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.0, 0.0))
        val flR = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.0, 0.0))
        val deckR = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.0, 60.0))
        val deckL = newH.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.0, 60.0))
        lanePolygon = listOf(flL, flR, deckR, deckL)
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

        // Subsample step for speed (step=2 checks 1 in 4 pixels; fast on mobile CPU)
        val step = 2
        var sumX = 0L
        var sumY = 0L
        var motionCount = 0

        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0

        // Bounding box of lane polygon if available to minimize search space
        val poly = lanePolygon
        var roiMinX = 0
        var roiMaxX = width
        var roiMinY = 0
        var roiMaxY = height

        if (poly != null && poly.size == 4) {
            roiMinX = max(0, poly.minOf { it.x }.toInt() - 20)
            roiMaxX = min(width - 1, poly.maxOf { it.x }.toInt() + 20)
            roiMinY = max(0, poly.minOf { it.y }.toInt() - 20)
            roiMaxY = min(height - 1, poly.maxOf { it.y }.toInt() + 20)
        }

        for (y in roiMinY until roiMaxY step step) {
            val rowOffset = y * stride
            for (x in roiMinX until roiMaxX step step) {
                val idx = rowOffset + x
                if (idx >= imageBytes.size) break

                val currVal = imageBytes[idx].toInt() and 0xFF
                val prevVal = prev[idx].toInt() and 0xFF
                val diff = abs(currVal - prevVal)

                if (diff > motionThreshold) {
                    // Check if inside lane polygon
                    val pt = Point2D(x.toDouble(), y.toDouble())
                    if (poly == null || isPointInPolygon(pt, poly)) {
                        sumX += x
                        sumY += y
                        motionCount++

                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
        }

        // Copy current frame to prev for next differencing cycle
        System.arraycopy(imageBytes, 0, prev, 0, imageBytes.size)

        lastLatencyMs = (System.nanoTime() - startNs) / 1_000_000.0

        if (motionCount in minClusterPixels..maxClusterPixels) {
            val cx = sumX.toDouble() / motionCount
            val cy = sumY.toDouble() / motionCount

            // Validate aspect ratio of motion cluster (spherical ball should have aspect ratio between 0.25 and 4.0)
            val clusterW = (maxX - minX + 1).toDouble()
            val clusterH = (maxY - minY + 1).toDouble()
            val aspect = clusterW / max(1.0, clusterH)

            if (aspect in 0.25..4.0) {
                lastBallRadiusPx = ((max(clusterW, clusterH) / 2.0).toInt()).coerceIn(8, 60)
                return Point2D(cx, cy)
            }
        }

        return null
    }

    private var lastBallRadiusPx: Int = 20
    fun getLastBallRadiusPx(): Int = lastBallRadiusPx

    override fun getLastInferenceLatencyMs(): Double = lastLatencyMs

    /**
     * Resets baseline frame buffer (e.g. after camera recalibration).
     */
    fun resetBaseline() {
        prevFrameBytes = null
    }

    /**
     * Point-in-polygon ray-casting algorithm.
     */
    private fun isPointInPolygon(pt: Point2D, poly: List<Point2D>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val xi = poly[i].x
            val yi = poly[i].y
            val xj = poly[j].x
            val yj = poly[j].y

            val intersect = ((yi > pt.y) != (yj > pt.y)) &&
                    (pt.x < (xj - xi) * (pt.y - yi) / (yj - yi + 1e-9) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
