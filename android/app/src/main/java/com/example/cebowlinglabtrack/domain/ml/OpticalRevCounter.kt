package com.example.cebowlinglabtrack.domain.ml

import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct Optical Rev Counter & 3D Spin Axis Analyzer.
 *
 * Tracks high-contrast tape (placed from ball COG to PAP or at the PAP) across 120 FPS
 * camera frames to directly measure:
 * 1. True optical RPM (direct rotation counting, not physics-inferred).
 * 2. Total ball revolutions from foul line to pins.
 * 3. Axis Tilt (0° to 30°) from projected ellipse minor-to-major ratio.
 * 4. Axis Rotation (0° to 90°) from spin vector heading relative to foul line.
 */
class OpticalRevCounter(
    private val contrastThreshold: Int = 180, // Fallback brightness threshold for white/neon tape (0..255)
    private val targetFps: Double = 120.0,
    private val useAdaptiveThreshold: Boolean = true
) {

    data class OpticalRevResult(
        val opticalRpm: Int,
        val totalRotations: Double,
        val axisTiltDeg: Double,
        val axisRotationDeg: Double,
        val isDetected: Boolean,
        val confidence: Float,
        val samplePointsCount: Int
    )

    data class TapeObservation(
        val timestampMs: Long,
        val centroidX: Double,
        val centroidY: Double,
        val orientationAngleRad: Double,
        val intensity: Double,
        val relX: Double = 0.0,
        val relY: Double = 0.0
    )

    private val observations = mutableListOf<TapeObservation>()
    private var lastObservedAngleRad = 0.0
    private var unrolledAngleRad = 0.0
    private var totalRotationsAcc = 0.0

    /**
     * Resets internal trackers for a new shot delivery.
     */
    fun reset() {
        observations.clear()
        lastObservedAngleRad = 0.0
        unrolledAngleRad = 0.0
        totalRotationsAcc = 0.0
    }

    /**
     * Processes a single camera frame given the detected ball center and radius.
     */
    fun processFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int,
        ballCenter: Point2D,
        ballRadiusPx: Int,
        timestampMs: Long
    ): TapeObservation? {
        val r = ballRadiusPx.coerceIn(8, 60)
        val cx = ballCenter.x.toInt()
        val cy = ballCenter.y.toInt()

        val minX = max(0, cx - r)
        val maxX = min(width - 1, cx + r)
        val minY = max(0, cy - r)
        val maxY = min(height - 1, cy + r)

        var sumBallLum = 0L
        var ballPixelCount = 0
        var maxBrightness = 0

        // 1. Evaluate ball surface luminance statistics and peak tape highlight
        for (y in minY..maxY) {
            val rowOffset = y * stride
            val dy = y - cy
            for (x in minX..maxX) {
                val dx = x - cx
                if (dx * dx + dy * dy > r * r) continue // Outside spherical ball mask

                val idx = rowOffset + x
                if (idx >= imageBytes.size) break
                val pixelVal = imageBytes[idx].toInt() and 0xFF
                sumBallLum += pixelVal
                ballPixelCount++
                if (pixelVal > maxBrightness) maxBrightness = pixelVal
            }
        }

        if (ballPixelCount < 16) {
            return null
        }

        val avgBallLum = (sumBallLum.toDouble() / ballPixelCount).toInt()
        val contrastDelta = maxBrightness - avgBallLum

        // If highlight contrast over the ball body is weak (< 25), no tape is facing the camera
        if (contrastDelta < 25) {
            return null
        }

        // Adaptive threshold: dynamically segments high-contrast tape across ball finishes
        // Dark balls (lum ~30-60) -> threshold ~110-150
        // Bright pearl balls (lum ~150-190) -> threshold ~190-240
        val dynamicThreshold = (avgBallLum + (contrastDelta * 0.55).toInt()).coerceIn(110, 245)
        val effectiveThreshold = if (useAdaptiveThreshold) dynamicThreshold else contrastThreshold

        var sumX = 0.0
        var sumY = 0.0
        var count = 0

        // 2. Locate brightest high-contrast cluster inside circular ball boundary
        for (y in minY..maxY) {
            val rowOffset = y * stride
            val dy = y - cy
            for (x in minX..maxX) {
                val dx = x - cx
                if (dx * dx + dy * dy > r * r) continue

                val idx = rowOffset + x
                if (idx >= imageBytes.size) break
                val pixelVal = imageBytes[idx].toInt() and 0xFF

                if (pixelVal >= effectiveThreshold) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        if (count < 4) {
            return null // No tape marker visible in this orientation
        }

        val tapeCenterX = sumX / count
        val tapeCenterY = sumY / count

        // 3. Compute 2D central image moments to calculate stripe orientation
        var mu20 = 0.0
        var mu02 = 0.0
        var mu11 = 0.0

        for (y in minY..maxY) {
            val rowOffset = y * stride
            val dy = y - cy
            for (x in minX..maxX) {
                val dx = x - cx
                if (dx * dx + dy * dy > r * r) continue
                val idx = rowOffset + x
                if (idx >= imageBytes.size) break
                val pixelVal = imageBytes[idx].toInt() and 0xFF

                if (pixelVal >= effectiveThreshold) {
                    val px = x - tapeCenterX
                    val py = y - tapeCenterY
                    mu20 += px * px
                    mu02 += py * py
                    mu11 += px * py
                }
            }
        }

        // Stripe / tape angle in 2D plane:
        // If tape is placed from COG to PAP or at the PAP, the displacement vector from ball center
        // to tape centroid gives direct 360-degree rotation tracking:
        val relX = tapeCenterX - cx
        val relY = tapeCenterY - cy
        val distFromCenter = sqrt(relX * relX + relY * relY)

        val angleRad = if (distFromCenter > 2.0) {
            atan2(relY, relX)
        } else {
            0.5 * atan2(2.0 * mu11, mu20 - mu02)
        }

        val obs = TapeObservation(
            timestampMs = timestampMs,
            centroidX = tapeCenterX,
            centroidY = tapeCenterY,
            orientationAngleRad = angleRad,
            intensity = maxBrightness.toDouble(),
            relX = relX,
            relY = relY
        )

        updateAngularKinematics(obs)
        observations.add(obs)
        return obs
    }

    /**
     * Unwraps angular changes across sequential frames to track full rotations.
     */
    private fun updateAngularKinematics(obs: TapeObservation) {
        if (observations.isEmpty()) {
            lastObservedAngleRad = obs.orientationAngleRad
            unrolledAngleRad = obs.orientationAngleRad
            return
        }

        var dAngle = obs.orientationAngleRad - lastObservedAngleRad
        // Normalize dAngle to [-PI, PI] to handle 360 wrap-around
        while (dAngle > PI) dAngle -= 2.0 * PI
        while (dAngle < -PI) dAngle += 2.0 * PI

        unrolledAngleRad += dAngle
        totalRotationsAcc = abs(unrolledAngleRad) / (2.0 * PI)
        lastObservedAngleRad = obs.orientationAngleRad
    }

    /**
     * Evaluates final optical rev count, RPM, Axis Tilt, and Axis Rotation for the completed shot.
     */
    fun evaluateShotRevRate(
        shotDurationMs: Long,
        fallbackRpm: Int = 400
    ): OpticalRevResult {
        if (observations.size < 6 || totalRotationsAcc < 0.5) {
            // Insufficient optical tape samples; fall back to physics estimate
            return OpticalRevResult(
                opticalRpm = fallbackRpm,
                totalRotations = (fallbackRpm * (shotDurationMs / 60000.0)).coerceAtLeast(1.0),
                axisTiltDeg = 14.0,
                axisRotationDeg = 55.0,
                isDetected = false,
                confidence = 0.0f,
                samplePointsCount = observations.size
            )
        }

        val dtSec = (observations.last().timestampMs - observations.first().timestampMs) / 1000.0
        val measuredRpm = if (dtSec > 0.1) {
            val revsInSample = abs(unrolledAngleRad) / (2.0 * PI)
            val revsPerSec = revsInSample / dtSec
            (revsPerSec * 60.0).roundToInt().coerceIn(150, 650)
        } else {
            fallbackRpm
        }

        // Extrapolate total rotations over the entire 60ft skid/hook/roll duration
        val totalShotRevs = (measuredRpm / 60.0) * (shotDurationMs / 1000.0)

        // Calculate Axis Tilt and Axis Rotation from relative trajectory of the tape marker
        val (tiltDeg, rotDeg) = estimateTiltAndRotation()

        return OpticalRevResult(
            opticalRpm = measuredRpm,
            totalRotations = (totalShotRevs * 10.0).roundToInt() / 10.0,
            axisTiltDeg = tiltDeg,
            axisRotationDeg = rotDeg,
            isDetected = true,
            confidence = (observations.size / 30.0f).coerceIn(0.5f, 1.0f),
            samplePointsCount = observations.size
        )
    }

    /**
     * Derives Axis Tilt (0..30°) and Axis Rotation (0..90°) from the precessing ellipse
     * described by the PAP tape marker on the ball's surface relative to ball center.
     */
    private fun estimateTiltAndRotation(): Pair<Double, Double> {
        if (observations.size < 4) return Pair(14.0, 55.0)

        val hasRel = observations.any { it.relX != 0.0 || it.relY != 0.0 }
        val xs = if (hasRel) observations.map { it.relX } else observations.map { it.centroidX }
        val ys = if (hasRel) observations.map { it.relY } else observations.map { it.centroidY }

        val spanX = xs.maxOrNull()!! - xs.minOrNull()!!
        val spanY = ys.maxOrNull()!! - ys.minOrNull()!!

        val major = max(spanX, spanY).coerceAtLeast(1.0)
        val minor = min(spanX, spanY)

        // Axis Tilt is the inclination of the spin axis: ratio of minor to major ellipse axis
        val ratio = (minor / major).coerceIn(0.0, 0.7)
        val tiltDeg = (Math.toDegrees(asin(ratio))).coerceIn(3.0, 28.0)

        // Axis Rotation is the direction the spin vector points relative to the foul line
        val rotAngle = Math.toDegrees(atan2(spanX, spanY.coerceAtLeast(0.01)))
        val rotDeg = (rotAngle * 1.5).coerceIn(20.0, 85.0)

        val roundedTilt = (tiltDeg * 10.0).roundToInt() / 10.0
        val roundedRot = (rotDeg * 10.0).roundToInt() / 10.0

        return Pair(roundedTilt, roundedRot)
    }

    /**
     * Helper for synthetic testing and simulation.
     */
    fun feedSyntheticRotations(rpm: Int, durationMs: Long, fps: Double = 120.0) {
        reset()
        val totalFrames = ((durationMs / 1000.0) * fps).toInt()
        val revsPerFrame = (rpm / 60.0) / fps
        var currentAngle = 0.0

        for (i in 0 until totalFrames) {
            val tMs = (i * (1000.0 / fps)).toLong()
            currentAngle += revsPerFrame * 2.0 * PI
            val normalized = currentAngle % (2.0 * PI)
            val rx = 15.0 * cos(currentAngle)
            val ry = 10.0 * sin(currentAngle)
            val obs = TapeObservation(
                timestampMs = tMs,
                centroidX = 500.0 + rx,
                centroidY = 500.0 + ry,
                orientationAngleRad = normalized,
                intensity = 240.0,
                relX = rx,
                relY = ry
            )
            updateAngularKinematics(obs)
            observations.add(obs)
        }
    }
}
