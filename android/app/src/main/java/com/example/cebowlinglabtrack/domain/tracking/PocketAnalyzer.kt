package com.example.cebowlinglabtrack.domain.tracking

import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.pow

data class PocketTelemetry(
    val impactBoard: Double,
    val entryAngleDegrees: Double,
    val strikeProbabilityPercent: Double
)

class PocketAnalyzer {

    /**
     * Computes the entry angle and strike probability at the pin deck.
     * 
     * Target strike pocket center for a right-handed shot is Board 17.5 (between 1-3 pins).
     * Ideal entry angle is between 4.0° and 6.0°.
     *
     * @param finalTrajectoryPoints Recent smoothed (Board, DistanceFt) points between 54 ft and 60 ft.
     */
    fun evaluatePocketImpact(finalTrajectoryPoints: List<Point2D>): PocketTelemetry? {
        if (finalTrajectoryPoints.size < 3) return null

        val pFirst = finalTrajectoryPoints.first() // e.g. at 54 ft
        val pLast = finalTrajectoryPoints.last()   // e.g. at 60 ft

        val deltaBoards = pLast.x - pFirst.x
        val deltaFeet = pLast.y - pFirst.y

        // Standard USBC dimensions:
        // Lane width = 41.5 inches over 39 boards => ~1.064 inches per board
        // 1 foot downlane = 12.0 inches
        val deltaXInches = deltaBoards * 1.0641
        val deltaYInches = deltaFeet * 12.0

        // Entry angle relative to parallel lane boards (degrees)
        val angleRad = atan2(deltaXInches, deltaYInches)
        val angleDeg = Math.toDegrees(angleRad)

        val impactBoard = pLast.x

        // Strike probability model based on pocket board entry and angle:
        // Peak probability is at Board 17.5 with a 6.0° entry angle.
        val boardError = impactBoard - 17.5
        val angleError = angleDeg - 6.0

        // Gaussian bivariate decay model
        val strikeProb = 95.0 * exp(-0.5 * ((boardError / 0.75).pow(2) + (angleError / 1.5).pow(2)))

        return PocketTelemetry(
            impactBoard = impactBoard,
            entryAngleDegrees = angleDeg,
            strikeProbabilityPercent = strikeProb.coerceIn(0.0, 99.9)
        )
    }
}
