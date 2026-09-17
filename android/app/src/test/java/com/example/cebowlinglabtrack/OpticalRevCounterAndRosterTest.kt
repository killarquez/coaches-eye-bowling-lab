package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.ml.OpticalRevCounter
import com.example.cebowlinglabtrack.domain.model.BowlerProfile
import com.example.cebowlinglabtrack.domain.model.BowlingStyle
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.model.RevTrackingMethod
import com.example.cebowlinglabtrack.domain.model.TapeColor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OpticalRevCounterAndRosterTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testOpticalRevCounterWithRotatingTape() {
        val counter = OpticalRevCounter(contrastThreshold = 180, targetFps = 120.0)
        counter.reset()

        val width = 200
        val height = 200
        val stride = width
        val ballCenter = Point2D(100.0, 100.0)
        val ballRadius = 25

        // Simulate 40 frames at 120 FPS (~333 ms total duration)
        // Spinning at ~450 RPM = 7.5 rev/sec -> 7.5 * 2*PI rad/sec = 47.12 rad/sec
        // In 333 ms, that is ~2.5 full revolutions
        val targetRpm = 450.0
        val revsPerSec = targetRpm / 60.0
        val angularVelocityRadPerSec = revsPerSec * 2.0 * PI

        var frameTimeMs = 0L
        val dtMs = 8L // ~120 FPS = 8.33 ms per frame

        for (frame in 0 until 40) {
            frameTimeMs = frame * dtMs
            val currentAngle = (frameTimeMs / 1000.0) * angularVelocityRadPerSec

            // Create frame buffer (black ball background = 30)
            val buffer = ByteArray(width * height) { 30 }

            // Draw a white tape marker line (value = 240) from center outward at currentAngle
            val tapeLength = 15
            for (dist in 4..tapeLength) {
                val px = (ballCenter.x + dist * cos(currentAngle)).toInt()
                val py = (ballCenter.y + dist * sin(currentAngle)).toInt()
                if (px in 0 until width && py in 0 until height) {
                    buffer[py * stride + px] = 240.toByte()
                    // 3x3 kernel around tape pixel for realistic blob
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = px + dx
                            val ny = py + dy
                            if (nx in 0 until width && ny in 0 until height) {
                                buffer[ny * stride + nx] = 220.toByte()
                            }
                        }
                    }
                }
            }

            counter.processFrame(
                imageBytes = buffer,
                width = width,
                height = height,
                stride = stride,
                ballCenter = ballCenter,
                ballRadiusPx = ballRadius,
                timestampMs = frameTimeMs
            )
        }

        // Shot delivery total duration: 1800 ms (1.8 sec from foul to pins)
        val result = counter.evaluateShotRevRate(
            shotDurationMs = 1800L,
            fallbackRpm = 400
        )

        assertTrue("Optical tape should be detected", result.isDetected)
        assertTrue("Measured RPM should be near 450 RPM (actual: ${result.opticalRpm})", result.opticalRpm in 350..550)
        assertTrue("Total rotations should be >= 10 revs (actual: ${result.totalRotations})", result.totalRotations >= 10.0)
        assertTrue("Axis tilt should be within normal bowling range 0-30 deg (actual: ${result.axisTiltDeg})", result.axisTiltDeg in 0.0..30.0)
        assertTrue("Axis rotation should be within normal bowling range 0-90 deg (actual: ${result.axisRotationDeg})", result.axisRotationDeg in 0.0..90.0)
    }

    @Test
    fun testOpticalRevCounterFallbackWhenNoTape() {
        val counter = OpticalRevCounter()
        counter.reset()

        // Insufficient frames / no tape
        val result = counter.evaluateShotRevRate(shotDurationMs = 1500L, fallbackRpm = 425)

        assertFalse("Should not be detected when no tape", result.isDetected)
        assertEquals("Should fall back to provided benchmark RPM", 425, result.opticalRpm)
        assertEquals("Confidence should be 0.0", 0.0f, result.confidence, 0.01f)
    }

    @Test
    fun testBowlerProfileSerializationAndCRMFields() {
        val profile = BowlerProfile(
            id = "CEB-101",
            name = "Marcus Turner",
            email = "marcus@cebowlinglab.com",
            phone = "(555) 234-5678",
            heightInches = 71.0,
            handedness = Handedness.RIGHT,
            style = BowlingStyle.TWO_HANDED,
            bookAverage = 194,
            careerHighGame = 279,
            careerHighSeries = 698,
            papCoordinates = "4 3/4\" over by 1/2\" up",
            benchmarkSpeedMph = 16.2,
            benchmarkRpm = 460,
            benchmarkAxisTiltDeg = 14.0,
            benchmarkAxisRotationDeg = 55.0,
            totalSessionsCoached = 2,
            lastSessionDate = "2026-09-02",
            primaryGoal = "Rev Rate & Ball Speed Synchronization"
        )

        val serialized = json.encodeToString(profile)
        val deserialized = json.decodeFromString<BowlerProfile>(serialized)

        assertEquals("CEB-101", deserialized.id)
        assertEquals("Marcus Turner", deserialized.name)
        assertEquals(BowlingStyle.TWO_HANDED, deserialized.style)
        assertEquals("2-Handed", deserialized.style.displayName())
        assertEquals(71.0, deserialized.heightInches, 0.01)
        assertEquals(194, deserialized.bookAverage)
        assertEquals("4 3/4\" over by 1/2\" up", deserialized.papCoordinates)
        assertEquals(460, deserialized.benchmarkRpm)
        assertEquals(16.2, deserialized.benchmarkSpeedMph, 0.01)
    }

    @Test
    fun testBowlingStyleDisplayNames() {
        assertEquals("1-Handed (Thumb In)", BowlingStyle.ONE_HANDED_THUMB.displayName())
        assertEquals("1-Handed (Thumb In)", BowlingStyle.ONE_HANDED.displayName())
        assertEquals("1-Handed (No Thumb)", BowlingStyle.ONE_HANDED_NO_THUMB.displayName())
        assertEquals("2-Handed", BowlingStyle.TWO_HANDED.displayName())
    }

    @Test
    fun testAdaptiveThresholdDarkBallInDimAlley() {
        // In dim bowling alley lighting, tape may only reach luminance 150 (below legacy static 180)
        val counter = OpticalRevCounter(contrastThreshold = 180, targetFps = 120.0, useAdaptiveThreshold = true)
        counter.reset()

        val width = 200
        val height = 200
        val stride = width
        val ballCenter = Point2D(100.0, 100.0)
        val ballRadius = 25

        val targetRpm = 420.0
        val revsPerSec = targetRpm / 60.0
        val angularVelocityRadPerSec = revsPerSec * 2.0 * PI
        val dtMs = 8L

        for (frame in 0 until 40) {
            val frameTimeMs = frame * dtMs
            val currentAngle = (frameTimeMs / 1000.0) * angularVelocityRadPerSec

            // Dark ball surface: luminance 35
            val buffer = ByteArray(width * height) { 35.toByte() }

            // Dim tape highlight: luminance 150
            val tapeLength = 15
            for (dist in 4..tapeLength) {
                val px = (ballCenter.x + dist * cos(currentAngle)).toInt()
                val py = (ballCenter.y + dist * sin(currentAngle)).toInt()
                if (px in 0 until width && py in 0 until height) {
                    buffer[py * stride + px] = 150.toByte()
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = px + dx
                            val ny = py + dy
                            if (nx in 0 until width && ny in 0 until height) {
                                buffer[ny * stride + nx] = 140.toByte()
                            }
                        }
                    }
                }
            }

            counter.processFrame(
                imageBytes = buffer,
                width = width,
                height = height,
                stride = stride,
                ballCenter = ballCenter,
                ballRadiusPx = ballRadius,
                timestampMs = frameTimeMs
            )
        }

        val result = counter.evaluateShotRevRate(shotDurationMs = 1800L, fallbackRpm = 400)
        assertTrue("Adaptive threshold must detect tape on dark ball in dim lighting", result.isDetected)
        assertTrue("RPM should be measured near 420 (actual: ${result.opticalRpm})", result.opticalRpm in 340..520)
    }

    @Test
    fun testAdaptiveThresholdPearlBall() {
        // Bright pearl ball has background luminance ~185 (which would swamp a static 180 threshold)
        val counter = OpticalRevCounter(contrastThreshold = 180, targetFps = 120.0, useAdaptiveThreshold = true)
        counter.reset()

        val width = 200
        val height = 200
        val stride = width
        val ballCenter = Point2D(100.0, 100.0)
        val ballRadius = 25

        val targetRpm = 480.0
        val revsPerSec = targetRpm / 60.0
        val angularVelocityRadPerSec = revsPerSec * 2.0 * PI
        val dtMs = 8L

        for (frame in 0 until 40) {
            val frameTimeMs = frame * dtMs
            val currentAngle = (frameTimeMs / 1000.0) * angularVelocityRadPerSec

            // Pearl ball background: luminance 185
            val buffer = ByteArray(width * height) { 185.toByte() }

            // Bright white tape: luminance 250
            val tapeLength = 15
            for (dist in 4..tapeLength) {
                val px = (ballCenter.x + dist * cos(currentAngle)).toInt()
                val py = (ballCenter.y + dist * sin(currentAngle)).toInt()
                if (px in 0 until width && py in 0 until height) {
                    buffer[py * stride + px] = 250.toByte()
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = px + dx
                            val ny = py + dy
                            if (nx in 0 until width && ny in 0 until height) {
                                buffer[ny * stride + nx] = 245.toByte()
                            }
                        }
                    }
                }
            }

            counter.processFrame(
                imageBytes = buffer,
                width = width,
                height = height,
                stride = stride,
                ballCenter = ballCenter,
                ballRadiusPx = ballRadius,
                timestampMs = frameTimeMs
            )
        }

        val result = counter.evaluateShotRevRate(shotDurationMs = 1800L, fallbackRpm = 400)
        assertTrue("Adaptive threshold must cleanly isolate tape from bright pearl ball body", result.isDetected)
        assertTrue("RPM should be measured near 480 (actual: ${result.opticalRpm})", result.opticalRpm in 380..580)
    }

    @Test
    fun testTapeColorSelectionAndTrackingMethod() {
        val counter = OpticalRevCounter(contrastThreshold = 180, targetFps = 120.0, useAdaptiveThreshold = true)
        counter.reset()

        val width = 200
        val height = 200
        val stride = width
        val ballCenter = Point2D(100.0, 100.0)
        val ballRadius = 25

        val targetRpm = 450.0
        val revsPerSec = targetRpm / 60.0
        val angularVelocityRadPerSec = revsPerSec * 2.0 * PI
        val dtMs = 8L

        for (frame in 0 until 40) {
            val frameTimeMs = frame * dtMs
            val currentAngle = (frameTimeMs / 1000.0) * angularVelocityRadPerSec
            val buffer = ByteArray(width * height) { 35.toByte() }

            val tapeLength = 15
            for (dist in 4..tapeLength) {
                val px = (ballCenter.x + dist * cos(currentAngle)).toInt()
                val py = (ballCenter.y + dist * sin(currentAngle)).toInt()
                if (px in 0 until width && py in 0 until height) {
                    buffer[py * stride + px] = 200.toByte()
                }
            }

            counter.processFrame(
                imageBytes = buffer,
                width = width,
                height = height,
                stride = stride,
                ballCenter = ballCenter,
                ballRadiusPx = ballRadius,
                timestampMs = frameTimeMs,
                tapeColor = TapeColor.NEON_GREEN
            )
        }

        val result = counter.evaluateShotRevRate(shotDurationMs = 1800L, fallbackRpm = 400, tapeColor = TapeColor.NEON_GREEN)
        assertTrue("Neon green tape should be detected", result.isDetected)
        assertEquals("Tracking method should be OPTICAL_TAPE", RevTrackingMethod.OPTICAL_TAPE, result.revTrackingMethod)
        assertTrue("RPM should be near 450 (actual: ${result.opticalRpm})", result.opticalRpm in 350..550)
    }

    @Test
    fun testUntapedBallNaturalFeatureDetection() {
        val counter = OpticalRevCounter(contrastThreshold = 180, targetFps = 120.0, useAdaptiveThreshold = true)
        counter.reset()

        val width = 200
        val height = 200
        val stride = width
        val ballCenter = Point2D(100.0, 100.0)
        val ballRadius = 25

        // Natural feature: contrasting finger insert or pearl swirl orbiting ball
        val targetRpm = 430.0
        val revsPerSec = targetRpm / 60.0
        val angularVelocityRadPerSec = revsPerSec * 2.0 * PI
        val dtMs = 8L

        for (frame in 0 until 40) {
            val frameTimeMs = frame * dtMs
            val currentAngle = (frameTimeMs / 1000.0) * angularVelocityRadPerSec
            // Dark solid coverstock = 40
            val buffer = ByteArray(width * height) { 40.toByte() }

            // Feature spot (e.g. bright finger insert or engraving) at radius 12
            val fx = (ballCenter.x + 12 * cos(currentAngle)).toInt()
            val fy = (ballCenter.y + 12 * sin(currentAngle)).toInt()
            for (dy in -2..2) {
                for (dx in -2..2) {
                    val px = fx + dx
                    val py = fy + dy
                    if (px in 0 until width && py in 0 until height) {
                        buffer[py * stride + px] = 120.toByte() // Distinct contrast from 40
                    }
                }
            }

            counter.processFrame(
                imageBytes = buffer,
                width = width,
                height = height,
                stride = stride,
                ballCenter = ballCenter,
                ballRadiusPx = ballRadius,
                timestampMs = frameTimeMs,
                tapeColor = TapeColor.NO_TAPE
            )
        }

        val result = counter.evaluateShotRevRate(shotDurationMs = 1800L, fallbackRpm = 400, tapeColor = TapeColor.NO_TAPE)
        assertTrue("Natural feature should be detected when untaped", result.isDetected)
        assertEquals("Tracking method should be NATURAL_FEATURE", RevTrackingMethod.NATURAL_FEATURE, result.revTrackingMethod)
        assertTrue("RPM should be near 430 (actual: ${result.opticalRpm})", result.opticalRpm in 320..540)
    }

    @Test
    fun testUntapedBallSolidMatteFallbackToTrajectory() {
        val counter = OpticalRevCounter(contrastThreshold = 180, targetFps = 120.0, useAdaptiveThreshold = true)
        counter.reset()

        val width = 200
        val height = 200
        val stride = width
        val ballCenter = Point2D(100.0, 100.0)
        val ballRadius = 25
        val dtMs = 8L

        // Uniform matte black ball with zero surface contrast
        for (frame in 0 until 40) {
            val frameTimeMs = frame * dtMs
            val buffer = ByteArray(width * height) { 40.toByte() }

            counter.processFrame(
                imageBytes = buffer,
                width = width,
                height = height,
                stride = stride,
                ballCenter = ballCenter,
                ballRadiusPx = ballRadius,
                timestampMs = frameTimeMs,
                tapeColor = TapeColor.NO_TAPE
            )
        }

        val result = counter.evaluateShotRevRate(shotDurationMs = 1800L, fallbackRpm = 415, tapeColor = TapeColor.NO_TAPE)
        assertFalse("Featureless matte ball should not report optical lock", result.isDetected)
        assertEquals("Should fall back to TRAJECTORY_ESTIMATE method", RevTrackingMethod.TRAJECTORY_ESTIMATE, result.revTrackingMethod)
        assertEquals("Should fall back to calculated/benchmark RPM", 415, result.opticalRpm)
    }

    @Test
    fun testBowlerProfileWithTapeColorSerialization() {
        val profile = BowlerProfile(
            id = "CEB-105",
            name = "Sarah Jenkins",
            heightInches = 66.0,
            handedness = Handedness.RIGHT,
            style = BowlingStyle.TWO_HANDED,
            tapeColor = TapeColor.HOT_PINK
        )

        val serialized = json.encodeToString(profile)
        val deserialized = json.decodeFromString<BowlerProfile>(serialized)

        assertEquals(TapeColor.HOT_PINK, deserialized.tapeColor)

        // Default should be WHITE
        val defaultProfile = BowlerProfile(id = "CEB-106", name = "Dan")
        assertEquals(TapeColor.WHITE, defaultProfile.tapeColor)
    }
}