package com.example.cebowlinglabtrack.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Visual Target Line model for lane planning and real-time shot comparison (Strike.app style).
 * Defines target trajectory keypoints at the Foul Line, 15ft Arrows, Breakpoint, and Pocket Entry.
 */
data class VisualTargetLine(
    val id: String,
    val name: String,
    val description: String = "",
    val laydownBoard: Float,
    val arrowBoard: Float,
    val breakpointBoard: Float,
    val breakpointDistanceFt: Float = 42.0f,
    val pocketBoard: Float = 17.5f
) {
    val breakpointDistFt: Float get() = breakpointDistanceFt

    /**
     * Generates an interpolated target path down the 60-foot lane.
     */
    fun generateTrajectory(numPoints: Int = 60): List<LanePoint> {
        val points = mutableListOf<LanePoint>()
        // Key knots: (Y, Board)
        val k0 = Pair(0.0f, laydownBoard)
        val k1 = Pair(15.0f, arrowBoard)
        val k2 = Pair(breakpointDistanceFt, breakpointBoard)
        val k3 = Pair(60.0f, pocketBoard)

        for (i in 0..numPoints) {
            val y = (i.toFloat() / numPoints) * 60.0f
            val x = evaluateBoardAtY(y, k0, k1, k2, k3)
            points.add(LanePoint(board = x.toDouble(), distanceFt = y.toDouble()))
        }
        return points
    }

    fun generateTargetTrajectory(numPoints: Int = 60): List<LanePoint> = generateTrajectory(numPoints)

    private fun evaluateBoardAtY(
        y: Float,
        k0: Pair<Float, Float>,
        k1: Pair<Float, Float>,
        k2: Pair<Float, Float>,
        k3: Pair<Float, Float>
    ): Float {
        return when {
            y <= k1.first -> {
                val t = (y - k0.first) / (k1.first - k0.first)
                k0.second + t * (k1.second - k0.second)
            }
            y <= k2.first -> {
                // Skid-to-hook phase: smooth transition from arrows to breakpoint
                val t = (y - k1.first) / (k2.first - k1.first)
                // Smooth hermite ease
                val t2 = t * t * (3 - 2 * t)
                k1.second + t2 * (k2.second - k1.second)
            }
            else -> {
                // Roll phase: strong continuous arc from breakpoint to pocket
                val t = (y - k2.first) / (k3.first - k2.first)
                val t2 = t * (2 - t) // Decelerating arc into pocket
                k2.second + t2 * (k3.second - k2.second)
            }
        }
    }

    /**
     * Compares the actual ball trajectory against this target line.
     */
    fun compareShot(actualTrajectory: List<TrajectoryPoint>): TargetComparisonResult {
        if (actualTrajectory.isEmpty()) {
            return TargetComparisonResult(0f, 0f, 0f, 0f, 0, "NO DATA", name)
        }

        // 1. Find actual laydown (near Y = 0..5 ft)
        val actualLaydown = actualTrajectory.minByOrNull { it.yFt }?.xBoard?.toFloat() ?: laydownBoard
        val laydownDelta = actualLaydown - laydownBoard

        // 2. Find actual arrow board (near Y = 15 ft)
        val actualArrow = actualTrajectory.minByOrNull { abs(it.yFt - 15.0) }?.xBoard?.toFloat() ?: arrowBoard
        val arrowDelta = actualArrow - arrowBoard

        // 3. Find actual breakpoint (max/min board in hook phase or near breakpointDistanceFt)
        val actualBreakpoint = actualTrajectory.minByOrNull { abs(it.yFt - breakpointDistanceFt.toDouble()) }?.xBoard?.toFloat() ?: breakpointBoard
        val breakpointDelta = actualBreakpoint - breakpointBoard

        // 4. Find actual pocket entry (near Y = 60 ft)
        val actualPocket = actualTrajectory.maxByOrNull { it.yFt }?.xBoard?.toFloat() ?: pocketBoard
        val pocketDelta = actualPocket - pocketBoard

        // Accuracy score: 100 - realistic coaching penalties for board deviations (USBC/Specto standard)
        val totalPenalty = abs(arrowDelta) * 5.0f + abs(pocketDelta) * 5.0f + abs(breakpointDelta) * 3.0f + abs(laydownDelta) * 2.0f
        val accuracyScore = (100f - totalPenalty).coerceIn(0f, 100f).roundToInt()

        val rating = when {
            accuracyScore >= 92 -> "PERFECT EXECUTION"
            accuracyScore >= 80 -> "GREAT SHOT"
            accuracyScore >= 65 -> "SLIGHT MISS"
            else -> "WIDE"
        }

        return TargetComparisonResult(
            laydownDelta = laydownDelta,
            arrowDelta = arrowDelta,
            breakpointDelta = breakpointDelta,
            pocketDelta = pocketDelta,
            overallAccuracyScore = accuracyScore,
            accuracyRating = rating,
            targetLineName = name
        )
    }

    companion object {
        val POWER_CRANKER = VisualTargetLine(
            id = "power_cranker",
            name = "Power Cranker (15→6)",
            description = "High rev deep hook: 20 at foul line out to 6th board breakpoint",
            laydownBoard = 20.0f,
            arrowBoard = 15.0f,
            breakpointBoard = 6.0f,
            breakpointDistanceFt = 42.0f,
            pocketBoard = 17.5f
        )

        val DOWN_10 = VisualTargetLine(
            id = "down_10",
            name = "Down 10 (Direct)",
            description = "Direct stiff line down 2nd arrow straight into pocket",
            laydownBoard = 10.0f,
            arrowBoard = 10.0f,
            breakpointBoard = 10.0f,
            breakpointDistanceFt = 40.0f,
            pocketBoard = 17.5f
        )

        val INSIDE_STROKER = VisualTargetLine(
            id = "inside_stroker",
            name = "Inside Stroker (11→8)",
            description = "Classic smooth arc: 14 at foul line out to 8 at breakpoint",
            laydownBoard = 14.0f,
            arrowBoard = 11.0f,
            breakpointBoard = 8.0f,
            breakpointDistanceFt = 41.0f,
            pocketBoard = 17.5f
        )

        val DEEP_INSIDE = VisualTargetLine(
            id = "deep_inside",
            name = "Deep Inside (18→9)",
            description = "5th arrow power swing inside out for heavy oil patterns",
            laydownBoard = 26.0f,
            arrowBoard = 18.0f,
            breakpointBoard = 9.0f,
            breakpointDistanceFt = 44.0f,
            pocketBoard = 17.5f
        )

        val TEN_PIN_SPARE = VisualTargetLine(
            id = "ten_pin_spare",
            name = "10-Pin Spare Cross-Lane",
            description = "Cross-lane flat line for right corner 10-pin conversion",
            laydownBoard = 32.0f,
            arrowBoard = 22.0f,
            breakpointBoard = 12.0f,
            breakpointDistanceFt = 45.0f,
            pocketBoard = 35.0f
        )

        val SEVEN_PIN_SPARE = VisualTargetLine(
            id = "seven_pin_spare",
            name = "7-Pin Spare Cross-Lane",
            description = "Cross-lane flat line for left corner 7-pin conversion",
            laydownBoard = 10.0f,
            arrowBoard = 15.0f,
            breakpointBoard = 10.0f,
            breakpointDistanceFt = 45.0f,
            pocketBoard = 5.0f
        )

        val PRESETS = listOf(
            DOWN_10,
            POWER_CRANKER,
            INSIDE_STROKER,
            DEEP_INSIDE,
            TEN_PIN_SPARE,
            SEVEN_PIN_SPARE
        )

        val DEFAULT = POWER_CRANKER
    }
}

/**
 * Result of comparing an actual shot against the visual target line.
 */
data class TargetComparisonResult(
    val laydownDelta: Float,
    val arrowDelta: Float,
    val breakpointDelta: Float,
    val pocketDelta: Float,
    val overallAccuracyScore: Int,
    val accuracyRating: String,
    val targetLineName: String = ""
) {
    val laydownDeltaBoards: Float get() = laydownDelta
    val arrowDeltaBoards: Float get() = arrowDelta
    val breakpointDeltaBoards: Float get() = breakpointDelta
    val pocketDeltaBoards: Float get() = pocketDelta
}

