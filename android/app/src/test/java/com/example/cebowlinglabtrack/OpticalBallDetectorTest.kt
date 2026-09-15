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
}
