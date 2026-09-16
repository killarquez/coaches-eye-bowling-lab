package com.example.cebowlinglabtrack.domain.ml

import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.model.LanePoint
import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.abs

/**
 * 10-Pin Optical Detector and Pinfall Scoring Engine.
 *
 * Projects USBC standard pin coordinates onto the camera frame via Homography H,
 * monitors pre-shot standing pins, and measures post-impact pinfall to detect
 * Strikes, Spares, Splits, and 1st vs 2nd ball status (matching LaneTrax indicators).
 */
class PinDeckDetector(
    private var homography: HomographyMatrix? = null
) {

    data class PinState(
        val pinNumber: Int,
        val board: Double,
        val distanceFt: Double,
        val screenPos: Point2D,
        val isStanding: Boolean
    )

    data class PinfallResult(
        val standingPins: List<Int>,
        val fallenPinCount: Int,
        val isStrike: Boolean,
        val isSpare: Boolean,
        val isSplit: Boolean,
        val ballNumber: Int, // 1 for first ball (1 dot), 2 for second ball (2 dots)
        val description: String
    )

    companion object {
        // Exact USBC Standard 10-Pin Coordinates (Board across 39 boards, Distance Ft)
        val STANDARD_PIN_COORDS = listOf(
            LanePoint(20.00, 60.00), // Pin 1 (Headpin)
            LanePoint(14.36, 60.87), // Pin 2
            LanePoint(25.64, 60.87), // Pin 3
            LanePoint(8.72, 61.73),  // Pin 4
            LanePoint(20.00, 61.73), // Pin 5
            LanePoint(31.28, 61.73), // Pin 6
            LanePoint(3.09, 62.60),  // Pin 7
            LanePoint(14.36, 62.60), // Pin 8
            LanePoint(25.64, 62.60), // Pin 9
            LanePoint(36.91, 62.60)  // Pin 10
        )
    }

    private var baselinePinLuminance = IntArray(10) { 180 }
    private var currentBallNumber = 1

    fun updateHomography(newH: HomographyMatrix) {
        this.homography = newH
    }

    fun setBallNumber(ballNum: Int) {
        this.currentBallNumber = ballNum.coerceIn(1, 2)
    }

    /**
     * Gets projected screen pixel locations for all 10 pins.
     */
    fun getProjectedPinPositions(): List<Point2D> {
        val h = homography ?: return emptyList()
        return STANDARD_PIN_COORDS.map { h.forward(it) }
    }

    /**
     * Captures baseline luminance of standing pins prior to ball delivery.
     */
    fun captureBaselinePins(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width
    ) {
        val pinPositions = getProjectedPinPositions()
        if (pinPositions.size != 10) return

        for (i in 0 until 10) {
            baselinePinLuminance[i] = samplePinLuminance(
                imageBytes,
                pinPositions[i].x.toInt(),
                pinPositions[i].y.toInt(),
                width,
                height,
                stride
            )
        }
    }

    /**
     * Real-time standing pin detector evaluating current frame against local pin deck background.
     * Can be invoked continuously in IDLE to give instant coach feedback before and during shots.
     */
    fun detectStandingPinsNow(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width
    ): List<Int> {
        val pinPositions = getProjectedPinPositions()
        if (pinPositions.size != 10) return (1..10).toList()

        val pitLum = samplePitBackgroundLuminance(imageBytes, pinPositions, width, height, stride)
        val standing = mutableListOf<Int>()

        for (i in 0 until 10) {
            val lum = samplePinLuminance(
                imageBytes,
                pinPositions[i].x.toInt(),
                pinPositions[i].y.toInt(),
                width,
                height,
                stride
            )
            val baseLum = baselinePinLuminance[i]
            val isBrighterThanPit = (lum - pitLum) > 16
            val matchesBaseline = (baseLum - lum) < 22 && lum > 70
            if (isBrighterThanPit || matchesBaseline) {
                standing.add(i + 1)
            }
        }
        return standing
    }

    /**
     * Evaluates pinfall post-impact and determines standing pins, strike, spare, and split.
     */
    fun evaluatePinfall(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width
    ): PinfallResult {
        val pinPositions = getProjectedPinPositions()
        val standing = mutableListOf<Int>()
        val pitLum = samplePitBackgroundLuminance(imageBytes, pinPositions, width, height, stride)

        for (i in 0 until 10) {
            val pinNum = i + 1
            if (pinPositions.size == 10) {
                val currentLum = samplePinLuminance(
                    imageBytes,
                    pinPositions[i].x.toInt(),
                    pinPositions[i].y.toInt(),
                    width,
                    height,
                    stride
                )
                val baseLum = baselinePinLuminance[i]
                // Pin knocked down leaves a darker spot on the pin deck
                val dropFromBase = baseLum - currentLum
                val isStanding = (dropFromBase < 20) && (currentLum - pitLum > 14 || currentLum > 110)
                if (isStanding) {
                    standing.add(pinNum)
                }
            }
        }

        val fallenCount = 10 - standing.size
        val isStrike = (currentBallNumber == 1 && standing.isEmpty())
        val isSpare = (currentBallNumber == 2 && standing.isEmpty())
        val isSplit = isSplitLeave(standing)

        val desc = when {
            isStrike -> "STRIKE! (10 PINS)"
            isSpare -> "SPARE PICKUP!"
            isSplit -> "SPLIT LEAVE (${standing.joinToString("-")})"
            standing.isEmpty() -> "ALL PINS DOWN"
            else -> "${fallenCount} PINS (LEAVING ${standing.joinToString(",")})"
        }

        val result = PinfallResult(
            standingPins = standing,
            fallenPinCount = fallenCount,
            isStrike = isStrike,
            isSpare = isSpare,
            isSplit = isSplit,
            ballNumber = currentBallNumber,
            description = desc
        )

        // Advance ball state machine: strike resets to 1; otherwise toggles 1 -> 2 -> 1
        if (isStrike || currentBallNumber == 2) {
            currentBallNumber = 1
        } else {
            currentBallNumber = 2
        }

        return result
    }

    /**
     * Checks if a leave represents a bowling split (Headpin down, gap between non-adjacent pins).
     */
    private fun isSplitLeave(standing: List<Int>): Boolean {
        if (standing.contains(1) || standing.size < 2) return false
        // Notable splits: 7-10, 4-6, 4-6-7-10, 3-10, 2-7
        if (standing.contains(7) && standing.contains(10)) return true
        if (standing.contains(4) && standing.contains(6)) return true
        if (standing.contains(2) && standing.contains(7)) return true
        if (standing.contains(3) && standing.contains(10)) return true
        return false
    }

    /**
     * Samples the dark pit / pinsetter backdrop immediately behind the pins.
     */
    private fun samplePitBackgroundLuminance(
        imageBytes: ByteArray,
        pins: List<Point2D>,
        width: Int,
        height: Int,
        stride: Int
    ): Int {
        if (pins.isEmpty()) return 50
        val topPinY = pins.minOf { it.y }.toInt()
        val sampleY = (topPinY - 8).coerceIn(0, height - 1)
        val rowOffset = sampleY * stride

        var sum = 0
        var count = 0
        val minX = pins.minOf { it.x }.toInt().coerceIn(0, width - 1)
        val maxX = pins.maxOf { it.x }.toInt().coerceIn(minX, width - 1)

        for (x in minX..maxX step 6) {
            val idx = rowOffset + x
            if (idx in imageBytes.indices) {
                sum += (imageBytes[idx].toInt() and 0xFF)
                count++
            }
        }
        return if (count > 0) sum / count else 50
    }

    /**
     * Samples average 5x5 luminance neighborhood around pin centroid.
     */
    private fun samplePinLuminance(
        imageBytes: ByteArray,
        cx: Int,
        cy: Int,
        width: Int,
        height: Int,
        stride: Int
    ): Int {
        var sum = 0
        var count = 0
        val r = 2

        for (dy in -r..r) {
            val y = cy + dy
            if (y !in 0 until height) continue
            val rowOffset = y * stride

            for (dx in -r..r) {
                val x = cx + dx
                if (x !in 0 until width) continue
                val idx = rowOffset + x
                if (idx in imageBytes.indices) {
                    sum += (imageBytes[idx].toInt() and 0xFF)
                    count++
                }
            }
        }

        return if (count > 0) sum / count else 128
    }
}
