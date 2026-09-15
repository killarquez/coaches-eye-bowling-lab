package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.ml.PinDeckDetector
import com.example.cebowlinglabtrack.domain.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinDeckDetectorTest {

    private fun createCalibratedDetector(): PinDeckDetector {
        val calibrator = LaneCalibrator()
        val foulLeft = Point2D(100.0, 1800.0)
        val foulRight = Point2D(980.0, 1800.0)
        val arrowsLeft = Point2D(300.0, 1000.0)
        val arrowsRight = Point2D(780.0, 1000.0)

        val (_, homography) = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)!!
        val detector = PinDeckDetector()
        detector.updateHomography(homography)
        return detector
    }

    @Test
    fun testProjectedPinCoordinatesCountAndHeadpin() {
        val detector = createCalibratedDetector()
        val projectedPins = detector.getProjectedPinPositions()

        assertEquals("Should project exactly 10 pins", 10, projectedPins.size)

        // Headpin (Pin 1) is at Board 20, 60 ft
        val headpin = projectedPins[0]
        assertNotNull(headpin)
        assertTrue("Headpin Y should be near the top of the perspective trapezoid", headpin.y < 500.0)
        assertTrue("Headpin X should be centered between gutters", headpin.x in 450.0..630.0)
    }

    @Test
    fun testStrikeEvaluationOnBall1() {
        val detector = createCalibratedDetector()
        val width = 1080
        val height = 1920
        val size = width * height

        // 1. Capture baseline: All pins standing (bright white spots, luminance 220)
        val baseFrame = ByteArray(size) { 220.toByte() }
        detector.setBallNumber(1)
        detector.captureBaselinePins(baseFrame, width, height)

        // 2. Post-impact frame: All pins knocked down (luminance drops to 50)
        val postFrame = ByteArray(size) { 50.toByte() }
        val result = detector.evaluatePinfall(postFrame, width, height)

        assertTrue("Should detect Strike on Ball 1 when all pins down", result.isStrike)
        assertEquals("Fallen count should be 10", 10, result.fallenPinCount)
        assertTrue("Standing pin list should be empty", result.standingPins.isEmpty())
        assertEquals("Ball number should advance back to 1 after strike", 1, result.ballNumber)
    }

    @Test
    fun testSparePickupEvaluationOnBall2() {
        val detector = createCalibratedDetector()
        val width = 1080
        val height = 1920
        val size = width * height

        val baseFrame = ByteArray(size) { 220.toByte() }
        detector.setBallNumber(2)
        detector.captureBaselinePins(baseFrame, width, height)

        val postFrame = ByteArray(size) { 50.toByte() }
        val result = detector.evaluatePinfall(postFrame, width, height)

        assertTrue("Should detect Spare on Ball 2 when all remaining pins down", result.isSpare)
        assertEquals(10, result.fallenPinCount)
        assertTrue(result.standingPins.isEmpty())
    }

    @Test
    fun testSplitDetection() {
        val detector = createCalibratedDetector()
        val width = 1080
        val height = 1920
        val size = width * height
        val pins = detector.getProjectedPinPositions()

        val baseFrame = ByteArray(size) { 220.toByte() }
        detector.setBallNumber(1)
        detector.captureBaselinePins(baseFrame, width, height)

        // Post-frame: only pins 7 (index 6) and 10 (index 9) remain standing
        val postFrame = ByteArray(size) { 50.toByte() }
        for (idx in listOf(6, 9)) {
            val px = pins[idx].x.toInt().coerceIn(0, width - 1)
            val py = pins[idx].y.toInt().coerceIn(0, height - 1)
            for (dy in -3..3) {
                for (dx in -3..3) {
                    val y = (py + dy).coerceIn(0, height - 1)
                    val x = (px + dx).coerceIn(0, width - 1)
                    postFrame[y * width + x] = 220.toByte()
                }
            }
        }

        val result = detector.evaluatePinfall(postFrame, width, height)

        assertFalse("Not a strike", result.isStrike)
        assertTrue("Should detect 7-10 Split", result.isSplit)
        assertTrue("Standing should contain 7", result.standingPins.contains(7))
        assertTrue("Standing should contain 10", result.standingPins.contains(10))
    }
}
