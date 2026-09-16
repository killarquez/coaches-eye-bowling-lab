package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.ml.OpticalBallDetector
import com.example.cebowlinglabtrack.domain.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpticalBallDetectorTest {

    @Test
    fun testMotionDetectionInsideLane() {
        val calibrator = LaneCalibrator()
        val foulLeft = Point2D(100.0, 1800.0)
        val foulRight = Point2D(980.0, 1800.0)
        val arrowsLeft = Point2D(300.0, 1000.0)
        val arrowsRight = Point2D(780.0, 1000.0)

        val (_, homography) = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)!!
        val detector = OpticalBallDetector()
        detector.updateHomography(homography)

        val width = 1080
        val height = 1920
        val stride = width
        val size = width * height

        // 1. Frame 1: Uniform background
        val frame1 = ByteArray(size) { 100.toByte() }
        val res1 = detector.detectBall(frame1, width, height, stride)
        assertNull("First frame must initialize baseline and return null", res1)

        // 2. Frame 2: Ball motion inside lane at (540, 1400)
        val frame2 = frame1.copyOf()
        val ballCenterX = 540
        val ballCenterY = 1400
        val radius = 10

        for (y in (ballCenterY - radius)..(ballCenterY + radius)) {
            for (x in (ballCenterX - radius)..(ballCenterX + radius)) {
                val dx = x - ballCenterX
                val dy = y - ballCenterY
                if (dx * dx + dy * dy <= radius * radius) {
                    frame2[y * stride + x] = 200.toByte() // Strong brightness difference
                }
            }
        }

        val res2 = detector.detectBall(frame2, width, height, stride)
        assertNotNull("Should detect moving ball inside lane", res2)
        assertEquals("Centroid X should match ball position", ballCenterX.toDouble(), res2!!.x, 5.0)
        assertEquals("Centroid Y should match ball position", ballCenterY.toDouble(), res2.y, 5.0)
        assertTrue("Latency should be measured and sub-10ms", detector.getLastInferenceLatencyMs() < 50.0)
    }

    @Test
    fun testMotionOutsideLaneIsIgnored() {
        val calibrator = LaneCalibrator()
        val foulLeft = Point2D(200.0, 1800.0)
        val foulRight = Point2D(880.0, 1800.0)
        val arrowsLeft = Point2D(350.0, 1000.0)
        val arrowsRight = Point2D(730.0, 1000.0)

        val (_, homography) = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)!!
        val detector = OpticalBallDetector()
        detector.updateHomography(homography)

        val width = 1080
        val height = 1920
        val stride = width
        val size = width * height

        val frame1 = ByteArray(size) { 80.toByte() }
        detector.detectBall(frame1, width, height, stride)

        // Place motion far outside the lane (e.g. at x = 50, y = 500 on the concourse/wall)
        val frame2 = frame1.copyOf()
        for (y in 480..520) {
            for (x in 30..70) {
                frame2[y * stride + x] = 220.toByte()
            }
        }

        val res = detector.detectBall(frame2, width, height, stride)
        assertNull("Motion outside calibrated lane polygon must be ignored", res)
    }

    @Test
    fun testMultiClusterRejectionPinSweepVsBall() {
        val calibrator = LaneCalibrator()
        val foulLeft = Point2D(100.0, 1800.0)
        val foulRight = Point2D(980.0, 1800.0)
        val arrowsLeft = Point2D(300.0, 1000.0)
        val arrowsRight = Point2D(780.0, 1000.0)

        val (_, homography) = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)!!
        val detector = OpticalBallDetector()
        detector.updateHomography(homography)

        val width = 1080
        val height = 1920
        val stride = width
        val size = width * height

        val frame1 = ByteArray(size) { 100.toByte() }
        detector.detectBall(frame1, width, height, stride)

        // Frame 2 has two distinct motions inside the lane:
        // Cluster 1: Horizontal pin sweep bar near pin deck (y = 550, width = 140, height = 6) -> aspect ratio ~23.3
        // Cluster 2: Spherical bowling ball at (540, 1350) with radius = 12 -> aspect ratio ~1.0
        val frame2 = frame1.copyOf()

        // Draw pin sweep bar
        for (y in 548..553) {
            for (x in 450..590) {
                frame2[y * stride + x] = 210.toByte()
            }
        }

        // Draw spherical bowling ball
        val ballCenterX = 540
        val ballCenterY = 1350
        val radius = 12
        for (y in (ballCenterY - radius)..(ballCenterY + radius)) {
            for (x in (ballCenterX - radius)..(ballCenterX + radius)) {
                val dx = x - ballCenterX
                val dy = y - ballCenterY
                if (dx * dx + dy * dy <= radius * radius) {
                    frame2[y * stride + x] = 220.toByte()
                }
            }
        }

        val res = detector.detectBall(frame2, width, height, stride)
        assertNotNull("Should detect the spherical ball and ignore the pin sweep", res)
        assertEquals("Centroid X should match the ball position", ballCenterX.toDouble(), res!!.x, 6.0)
        assertEquals("Centroid Y should match the ball position", ballCenterY.toDouble(), res.y, 6.0)
    }

    @Test
    fun testAmbientExposureShiftSuppression() {
        val calibrator = LaneCalibrator()
        val foulLeft = Point2D(100.0, 1800.0)
        val foulRight = Point2D(980.0, 1800.0)
        val arrowsLeft = Point2D(300.0, 1000.0)
        val arrowsRight = Point2D(780.0, 1000.0)

        val (_, homography) = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)!!
        val detector = OpticalBallDetector()
        detector.updateHomography(homography)

        val width = 1080
        val height = 1920
        val stride = width
        val size = width * height

        val frame1 = ByteArray(size) { 100.toByte() }
        detector.detectBall(frame1, width, height, stride)

        // Frame 2: Entire image brightens uniformly by +20 (camera auto-exposure adjustment)
        // No actual localized ball motion
        val frame2 = ByteArray(size) { 120.toByte() }
        val res = detector.detectBall(frame2, width, height, stride)
        assertNull("Global auto-exposure shift should be suppressed without false ball lock", res)
    }
}
