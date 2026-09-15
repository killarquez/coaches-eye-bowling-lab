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
        // Standard USBC 10-Pin Coordinates (Board, Distance Ft)
        val STANDARD_PIN_COORDS = listOf(
            LanePoint(20.0, 60.00), // Pin 1 (Headpin)
            LanePoint(16.8, 60.89), // Pin 2
            LanePoint(23.2, 60.89), // Pin 3
            LanePoint(13.6, 61.78), // Pin 4
            LanePoint(20.0, 61.78), // Pin 5
            LanePoint(26.4, 61.78), // Pin 6
            LanePoint(10.4, 62.67), // Pin 7
            LanePoint(16.8, 62.67), // Pin 8
            LanePoint(23.2, 62.67), // Pin 9
            LanePoint(29.6, 62.67)  // Pin 10
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
                val isStanding = (baseLum - currentLum) < 18
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
