package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.LaneStabilityVerifier
import com.example.cebowlinglabtrack.domain.model.Point2D
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class LaneStabilityVerifierTest {

    private lateinit var verifier: LaneStabilityVerifier
    private val width = 640
    private val height = 480
    private val stride = 640

    @Before
    fun setUp() {
        verifier = LaneStabilityVerifier(patchSize = 32, searchRadius = 10, minStableThreshold = 0.80f)
    }

    private fun generateSyntheticLaneFrame(): ByteArray {
        val bytes = ByteArray(width * height)
        // Background wood gradient
        for (y in 0 until height) {
            for (x in 0 until width) {
                bytes[y * stride + x] = ((x * 0.2 + y * 0.3) % 180 + 30).toInt().toByte()
            }
        }
        // Draw sharp high-contrast foul line corners
        drawPatch(bytes, cx = 150, cy = 350, pattern = 1)
        drawPatch(bytes, cx = 490, cy = 350, pattern = 2)
        // Draw sharp arrows chevron
        drawPatch(bytes, cx = 320, cy = 200, pattern = 3)
        return bytes
    }

    private fun drawPatch(frame: ByteArray, cx: Int, cy: Int, pattern: Int) {
        val half = 16
        for (dy in -half until half) {
            for (dx in -half until half) {
                val valLum = when (pattern) {
                    1 -> if ((dx + dy) % 6 == 0) 240 else 20
                    2 -> if ((dx - dy) % 6 == 0) 230 else 30
                    else -> if (dx * dx + dy * dy < 64) 250 else 40
                }
                val y = cy + dy
                val x = cx + dx
                if (x in 0 until width && y in 0 until height) {
                    frame[y * stride + x] = valLum.toByte()
                }
            }
        }
    }

    @Test
    fun testIdenticalFrameReports100PercentStability() {
        val baseFrame = generateSyntheticLaneFrame()
        val foulL = Point2D(150.0, 350.0)
        val foulR = Point2D(490.0, 350.0)
        val arrow = Point2D(320.0, 200.0)

        val captured = verifier.captureReferenceLandmarks(baseFrame, width, height, stride, foulL, foulR, arrow)
        assertTrue("Reference landmarks should be captured successfully", captured)
        assertTrue("Verifier should be initialized", verifier.isInitialized)

        val report = verifier.verifyStability(baseFrame, width, height, stride)
        assertTrue("Identical frame should be stable", report.isStable)
        assertTrue("Score should be >= 95%", report.scorePercent >= 95)
        assertEquals(0f, report.offsetDx, 0.5f)
        assertEquals(0f, report.offsetDy, 0.5f)
    }

    @Test
    fun testSlightlyShiftedFrameDetectsOffsetAndMaintainsStability() {
        val baseFrame = generateSyntheticLaneFrame()
        val foulL = Point2D(150.0, 350.0)
        val foulR = Point2D(490.0, 350.0)
        val arrow = Point2D(320.0, 200.0)

        verifier.captureReferenceLandmarks(baseFrame, width, height, stride, foulL, foulR, arrow)

        // Shift frame by dx = +2, dy = +2
        val shiftedFrame = ByteArray(width * height)
        val shiftX = 2
        val shiftY = 2
        for (y in 0 until height - shiftY) {
            for (x in 0 until width - shiftX) {
                shiftedFrame[(y + shiftY) * stride + (x + shiftX)] = baseFrame[y * stride + x]
            }
        }

        val report = verifier.verifyStability(shiftedFrame, width, height, stride)
        assertTrue("Small translation should still match >= 80%", report.isStable)
        assertTrue("Report score should be >= 80%", report.scorePercent >= 80)
        assertEquals(2.0f, report.offsetDx, 1.0f)
        assertEquals(2.0f, report.offsetDy, 1.0f)
    }

    @Test
    fun testHeavilyDistortedFrameFlagsCameraDrift() {
        val baseFrame = generateSyntheticLaneFrame()
        val foulL = Point2D(150.0, 350.0)
        val foulR = Point2D(490.0, 350.0)
        val arrow = Point2D(320.0, 200.0)

        verifier.captureReferenceLandmarks(baseFrame, width, height, stride, foulL, foulR, arrow)

        // Generate totally random noise frame (camera knocked to floor or covered)
        val corruptedFrame = ByteArray(width * height)
        Random(42).nextBytes(corruptedFrame)

        val report = verifier.verifyStability(corruptedFrame, width, height, stride)
        assertFalse("Corrupted frame should flag camera drift", report.isStable)
        assertTrue("Score should be < 70%", report.scorePercent < 70)
        assertTrue("Status message should indicate drift", report.statusMessage.contains("DRIFT"))
    }
}
