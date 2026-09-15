package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.AutoLaneDetector
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLaneDetectorTest {

    @Test
    fun testAutoLaneDetectionWithSyntheticGutters() {
        val detector = AutoLaneDetector()
        val width = 800
        val height = 1200
        val stride = width
        val imageBytes = ByteArray(width * height)

        // Generate synthetic bowling lane:
        // - Dark gutters on edges (luminance ~ 30)
        // - Bright polished lane in center (luminance ~ 180)
        // Perspective convergence: lane is wider at bottom, narrower at top
        for (y in 0 until height) {
            val progress = y.toDouble() / height // 0 at top, 1 at bottom
            val laneHalfWidth = 100 + (progress * 220) // 100px at top, 320px at bottom
            val centerX = width / 2

            val leftGutterEdge = (centerX - laneHalfWidth).toInt()
            val rightGutterEdge = (centerX + laneHalfWidth).toInt()

            for (x in 0 until width) {
                val isLane = x in leftGutterEdge..rightGutterEdge
                imageBytes[y * stride + x] = if (isLane) 180.toByte() else 30.toByte()
            }
        }

        val result = detector.detectLaneFromFrame(imageBytes, width, height, stride)

        assertNotNull("Detection result should not be null", result)
        assertTrue("Detection should succeed on clear synthetic lane", result.isSuccess)
        assertTrue("Confidence should be high (> 0.8)", result.confidence >= 0.8)

        // Check geometry: foul line (bottom) should be wider than arrows (mid)
        val foulWidth = result.foulLineRight.x - result.foulLineLeft.x
        val arrowWidth = result.arrowsRight.x - result.arrowsLeft.x
        assertTrue("Foul line must be wider than arrows due to perspective", foulWidth > arrowWidth)

        // Check that homography matrix elements are valid 3x3
        assertEquals(9, result.calibration.homographyMatrixElements.size)
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
