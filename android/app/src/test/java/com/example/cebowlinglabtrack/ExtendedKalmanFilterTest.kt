package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.tracking.ExtendedKalmanFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExtendedKalmanFilterTest {

    @Test
    fun testPredictionStep() {
        // Initial state at Board 15, Y = 0 ft, Vy = 25 ft/s (~17 mph)
        val ekf = ExtendedKalmanFilter(
            initialX = 15.0,
            initialY = 0.0,
            initialVx = 0.0,
            initialVy = 25.0,
            initialAx = 0.0
        )

        // Predict 0.1 seconds ahead
        ekf.predict(0.1)

        assertEquals("Board X should remain ~15", 15.0, ekf.xBoard, 0.01)
        assertEquals("Distance Y should advance by 2.5 ft (25 * 0.1)", 2.5, ekf.yFt, 0.01)
    }

    @Test
    fun testMeasurementUpdateAndNoiseSmoothing() {
        val ekf = ExtendedKalmanFilter(
            initialX = 15.0,
            initialY = 0.0,
            initialVx = 0.0,
            initialVy = 25.0,
            initialAx = 0.0
        )

        // Simulate 20 frames of noisy measurements along a known line
        var trueY = 0.0
        val dt = 0.016 // ~60 FPS
        val vy = 25.0

        for (i in 1..20) {
            trueY += vy * dt
            ekf.predict(dt)

            // Add simulated sensor noise +/- 0.3 boards and +/- 0.4 ft
            val noiseX = if (i % 2 == 0) 0.2 else -0.2
            val noiseY = if (i % 2 == 0) -0.3 else 0.3

            val accepted = ekf.update(15.0 + noiseX, trueY + noiseY)
            assertTrue("Measurement should be accepted within gate", accepted)
        }

        // Filter should track true position without chasing the noise
        assertEquals(15.0, ekf.xBoard, 0.35)
        assertEquals(trueY, ekf.yFt, 0.45)
    }

    @Test
    fun testOutlierRejectionViaMahalanobisGating() {
        val ekf = ExtendedKalmanFilter(
            initialX = 15.0,
            initialY = 10.0,
            initialVx = 0.0,
            initialVy = 25.0,
            initialAx = 0.0
        )

        ekf.predict(0.016)

        // Attempt impossible false positive detection far away (e.g. glare on Board 38, Y=45ft)
        val accepted = ekf.update(38.0, 45.0)
        assertFalse("Extreme outlier should be rejected by gating", accepted)
        assertEquals(1, ekf.consecutiveCoastFrames)
    }

    @Test
    fun testCoastingThroughOcclusion() {
        val ekf = ExtendedKalmanFilter(
            initialX = 15.0,
            initialY = 10.0,
            initialVx = -1.0, // drifting right 1 board/s
            initialVy = 25.0,
            initialAx = 0.0
        )

        val dt = 0.016

        // Ball passes through an oil reflection: 4 frames of occlusion
        for (i in 1..4) {
            ekf.coast(dt)
        }

        assertEquals(4, ekf.consecutiveCoastFrames)
        // Trajectory should continue moving predictably
        assertTrue(ekf.yFt > 10.0 + 4 * dt * 24.0)
        assertTrue(ekf.xBoard < 15.0)
    }
}
