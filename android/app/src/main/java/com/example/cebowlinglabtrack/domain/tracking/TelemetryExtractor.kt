package com.example.cebowlinglabtrack.domain.tracking

import com.example.cebowlinglabtrack.domain.model.BallMetrics
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.model.SpectoAngleMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoDynamicsMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoSpatialMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoSpeedMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoTelemetry
import com.example.cebowlinglabtrack.domain.model.SpectoSessionStats
import com.example.cebowlinglabtrack.domain.model.SpectoSkillTier
import com.example.cebowlinglabtrack.domain.model.TrajectoryPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * High-precision Telemetry Extractor implementing the complete 22-parameter Specto Model.
 */
object TelemetryExtractor {

    /**
     * Extracts full 22-parameter Specto telemetry model from a completed ball trajectory.
     */
    fun extractSpectoTelemetry(
        trajectory: List<TrajectoryPoint>,
        oilPatternDistanceFt: Double = LaneConstants.DEFAULT_OIL_PATTERN_EXIT_FT,
        measuredRpm: Int? = null,
        detectedLoftFt: Double? = null,
        opticalRevResult: com.example.cebowlinglabtrack.domain.ml.OpticalRevCounter.OpticalRevResult? = null
    ): SpectoTelemetry {
        if (trajectory.size < 4) {
            return defaultSpectoTelemetry()
        }

        val sorted = trajectory.sortedBy { it.yFt }

        // 1. SPATIAL PARAMETERS
        val laydownBoard = interpolateBoardAtDistance(sorted, 0.0)

        // Loft distance: detected loft or first grounded point
        val loftDistanceFt = detectedLoftFt
            ?: (sorted.firstOrNull { it.yFt > 0.5 }?.yFt ?: 3.5).coerceIn(0.0, 15.0)

        val arrowBoard = interpolateBoardAtDistance(sorted, LaneConstants.ARROWS_DISTANCE_FT)
        val patternExitBoard = interpolateBoardAtDistance(sorted, oilPatternDistanceFt)

        val (breakpointBoard, breakpointDistanceFt) = findBreakpoint(sorted)

        val entryBoard = interpolateBoardAtDistance(sorted, LaneConstants.FOUL_LINE_TO_HEADPIN_FT)
        val pinDeckExitBoard = interpolateBoardAtDistance(sorted, LaneConstants.TOTAL_LANE_LENGTH_FT)
        val pinDeckDeflection = abs(pinDeckExitBoard - entryBoard)

        val spatial = SpectoSpatialMetrics(
            laydownBoard = round1(laydownBoard),
            loftDistanceFt = round1(loftDistanceFt),
            arrowBoard = round1(arrowBoard),
            patternExitBoard = round1(patternExitBoard),
            breakpointBoard = round1(breakpointBoard),
            breakpointDistanceFt = round1(breakpointDistanceFt),
            entryBoard = round1(entryBoard),
            pinDeckExitBoard = round1(pinDeckExitBoard),
            pinDeckDeflection = round1(pinDeckDeflection)
        )

        // 2. ANGULAR VECTORS
        // Launch Angle: trajectory vector over first 10-15 ft (negative playing towards right gutter)
        val launchAngleDeg = calculateLaunchAngle(sorted, 0.0, 15.0)

        // Impact Angle & Strike Probability via PocketAnalyzer (53 to 60 ft)
        val pocketPts = sorted.filter { it.yFt in 53.0..60.5 }.map { Point2D(it.xBoard, it.yFt) }
        val pocketTelemetry = PocketAnalyzer().evaluatePocketImpact(pocketPts)
        val impactAngleDeg = pocketTelemetry?.entryAngleDegrees ?: calculateEntryAngle(sorted, 53.0, 59.5)
        val strikeProb = pocketTelemetry?.strikeProbabilityPercent ?: 0.0

        // Breakpoint Angle: directional inflection at hook apex
        val breakpointAngleDeg = calculateBreakpointAngle(sorted, breakpointDistanceFt)

        val angles = SpectoAngleMetrics(
            launchAngleDeg = round1(launchAngleDeg),
            breakpointAngleDeg = round1(breakpointAngleDeg),
            impactAngleDeg = round1(impactAngleDeg),
            strikeProbabilityPercent = round1(strikeProb)
        )

        // 3. SPEED & VELOCITY DECAY
        val launchSpeedMph = calculateSegmentSpeedMph(sorted, 0.0, 15.0)
        val entrySpeedMph = calculateSegmentSpeedMph(sorted, 50.0, 60.0)
        val speedLossMph = (launchSpeedMph - entrySpeedMph).coerceAtLeast(0.0)

        val totalDistFt = (sorted.last().yFt - sorted.first().yFt).coerceAtLeast(10.0)
        val totalDtSec = ((sorted.last().timeMs - sorted.first().timeMs) / 1000.0).coerceAtLeast(0.1)
        val avgSpeedMph = (totalDistFt / totalDtSec) * LaneConstants.FT_PER_SEC_TO_MPH

        val speed = SpectoSpeedMetrics(
            launchSpeedMph = round1(launchSpeedMph),
            entrySpeedMph = round1(entrySpeedMph),
            speedLossMph = round1(speedLossMph),
            avgSpeedMph = round1(avgSpeedMph)
        )

        // 4. MOTION PHASES & DYNAMICS
        val finalRpm = opticalRevResult?.opticalRpm ?: measuredRpm ?: estimateRpm(launchSpeedMph, breakpointDistanceFt, impactAngleDeg)
        val powerScore = (launchSpeedMph * (finalRpm / 1000.0) * 100.0).roundToInt() / 100.0

        // Motion Phases: Skid, Hook, Roll
        // Read distance is where friction begins to take effect (typically ~55-60% of breakpoint distance)
        val readDistanceFt = (breakpointDistanceFt * 0.58).coerceIn(12.0, 28.0)
        val skidFt = (readDistanceFt + 7.0).coerceIn(16.0, 32.0)
        // Roll starts around 52-54 ft
        val rollStartFt = (LaneConstants.FOUL_LINE_TO_HEADPIN_FT - 6.0).coerceAtLeast(breakpointDistanceFt + 5.0)
        val rollFt = (LaneConstants.FOUL_LINE_TO_HEADPIN_FT - rollStartFt).coerceAtLeast(0.0)
        val hookFt = (rollStartFt - skidFt).coerceAtLeast(10.0)

        val dynamics = SpectoDynamicsMetrics(
            rpm = finalRpm,
            powerScore = powerScore,
            readFt = round1(readDistanceFt),
            skidFt = round1(skidFt),
            hookFt = round1(hookFt),
            rollFt = round1(rollFt),
            axisTiltDeg = opticalRevResult?.axisTiltDeg ?: 14.0,
            axisRotationDeg = opticalRevResult?.axisRotationDeg ?: 55.0,
            totalRotations = opticalRevResult?.totalRotations ?: ((finalRpm / 60.0) * totalDtSec),
            isOpticalRevCounted = opticalRevResult?.isDetected ?: false
        )

        return SpectoTelemetry(
            spatial = spatial,
            angles = angles,
            speed = speed,
            dynamics = dynamics
        )
    }

    /**
     * Backward-compatible extractor returning BallMetrics.
     */
    fun extractMetrics(trajectory: List<TrajectoryPoint>): BallMetrics {
        val s = extractSpectoTelemetry(trajectory)
        return BallMetrics(
            laydownBoard = s.spatial.laydownBoard,
            arrowBoard = s.spatial.arrowBoard,
            breakpointBoard = s.spatial.breakpointBoard,
            breakpointDistanceFt = s.spatial.breakpointDistanceFt,
            launchSpeedMph = s.speed.launchSpeedMph,
            deckSpeedMph = s.speed.entrySpeedMph,
            entryAngleDeg = s.angles.impactAngleDeg,
            axisTiltDeg = s.dynamics.axisTiltDeg,
            axisRotationDeg = s.dynamics.axisRotationDeg,
            rpm = s.dynamics.rpm,
            totalRotations = s.dynamics.totalRotations,
            isOpticalRevCounted = s.dynamics.isOpticalRevCounted
        )
    }

    /**
     * Interpolates lateral board position X at down-lane distance targetY (feet).
     */
    fun interpolateBoardAtDistance(points: List<TrajectoryPoint>, targetY: Double): Double {
        if (points.isEmpty()) return 20.0
        if (targetY <= points.first().yFt) return points.first().xBoard
        if (targetY >= points.last().yFt) return points.last().xBoard

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (p1.yFt <= targetY && p2.yFt >= targetY) {
                val dy = p2.yFt - p1.yFt
                if (abs(dy) < 1e-6) return p1.xBoard
                val t = (targetY - p1.yFt) / dy
                return p1.xBoard + t * (p2.xBoard - p1.xBoard)
            }
        }
        return points.last().xBoard
    }

    /**
     * Calculates Launch Angle (degrees) over [startY, endY] relative to lane centerline.
     * Negative means playing out towards right gutter (standard Specto convention).
     */
    fun calculateLaunchAngle(points: List<TrajectoryPoint>, startY: Double, endY: Double): Double {
        val p1X = interpolateBoardAtDistance(points, startY)
        val p2X = interpolateBoardAtDistance(points, endY)

        val deltaBoards = p2X - p1X
        val deltaXFt = deltaBoards * LaneConstants.BOARD_WIDTH_FEET
        val deltaYFt = endY - startY

        if (deltaYFt <= 0.1) return 0.0

        // In standard right-handed play, laydown is ~17, arrows is ~9:
        // deltaBoards = 9 - 17 = -8 boards (heading right toward gutter).
        // Specto convention: negative degrees = playing out to the right gutter (e.g. -2.9 deg).
        val rad = atan2(deltaXFt, deltaYFt)
        return Math.toDegrees(rad)
    }

    /**
     * Calculates Breakpoint Directional Change Angle (degrees) at hook apex.
     */
    fun calculateBreakpointAngle(points: List<TrajectoryPoint>, bpDistanceFt: Double): Double {
        val beforeX = interpolateBoardAtDistance(points, (bpDistanceFt - 8.0).coerceAtLeast(0.0))
        val bpX = interpolateBoardAtDistance(points, bpDistanceFt)
        val afterX = interpolateBoardAtDistance(points, (bpDistanceFt + 8.0).coerceAtMost(60.0))

        val inAngle = atan2((bpX - beforeX) * LaneConstants.BOARD_WIDTH_FEET, 8.0)
        val outAngle = atan2((afterX - bpX) * LaneConstants.BOARD_WIDTH_FEET, 8.0)

        val diffDeg = abs(Math.toDegrees(outAngle - inAngle))
        return diffDeg.coerceIn(2.0, 15.0)
    }

    /**
     * Finds Breakpoint: extrema board (minimum or maximum) and down-lane distance.
     */
    fun findBreakpoint(points: List<TrajectoryPoint>): Pair<Double, Double> {
        val hookZone = points.filter { it.yFt in 25.0..55.0 }
        if (hookZone.isEmpty()) {
            val mid = points[points.size / 2]
            return Pair(mid.xBoard, mid.yFt)
        }

        val firstX = points.first().xBoard
        val minPoint = hookZone.minByOrNull { it.xBoard } ?: hookZone.first()
        val maxPoint = hookZone.maxByOrNull { it.xBoard } ?: hookZone.first()

        val minDiff = abs(minPoint.xBoard - firstX)
        val maxDiff = abs(maxPoint.xBoard - firstX)

        val bp = if (minDiff >= maxDiff) minPoint else maxPoint
        return Pair(bp.xBoard, bp.yFt)
    }

    /**
     * Calculates speed in mph over distance segment [startY, endY].
     */
    fun calculateSegmentSpeedMph(points: List<TrajectoryPoint>, startY: Double, endY: Double): Double {
        val segment = points.filter { it.yFt in (startY - 2.0)..(endY + 2.0) }
        if (segment.size < 2) {
            val totalDy = points.last().yFt - points.first().yFt
            val totalDt = (points.last().timeMs - points.first().timeMs) / 1000.0
            return if (totalDt > 0.05) (totalDy / totalDt) * LaneConstants.FT_PER_SEC_TO_MPH else 17.5
        }

        val pStart = segment.first()
        val pEnd = segment.last()
        val dy = pEnd.yFt - pStart.yFt
        val dtSec = (pEnd.timeMs - pStart.timeMs) / 1000.0

        if (dtSec <= 0.01 || dy <= 0.5) return 17.5
        return (dy / dtSec) * LaneConstants.FT_PER_SEC_TO_MPH
    }

    /**
     * Calculates pocket entry attack angle in degrees over [startY, endY].
     */
    fun calculateEntryAngle(points: List<TrajectoryPoint>, startY: Double, endY: Double): Double {
        val p1X = interpolateBoardAtDistance(points, startY)
        val p2X = interpolateBoardAtDistance(points, endY)

        val deltaBoards = p2X - p1X
        val deltaXFt = deltaBoards * LaneConstants.BOARD_WIDTH_FEET
        val deltaYFt = endY - startY

        if (deltaYFt <= 0.1) return 5.6
        val rad = atan2(abs(deltaXFt), deltaYFt)
        return Math.toDegrees(rad)
    }

    /**
     * Estimates RPM based on ball launch speed, hook distance, and entry angle when optical marker tracking is unavailable.
     */
    private fun estimateRpm(launchSpeedMph: Double, bpDistFt: Double, entryAngleDeg: Double): Int {
        val baseRpm = 350.0
        val speedFactor = (launchSpeedMph - 15.0) * 15.0
        val angleFactor = (entryAngleDeg - 3.0) * 35.0
        val distFactor = (45.0 - bpDistFt) * 8.0
        val est = baseRpm + speedFactor + angleFactor + distFactor
        return est.roundToInt().coerceIn(200, 600)
    }

    /**
     * Calculates Specto Session Consistency Statistics and classifies into Skill Tiers (L1..L6 PRO).
     */
    fun calculateSessionStats(telemetryList: List<SpectoTelemetry>): SpectoSessionStats {
        if (telemetryList.isEmpty()) {
            return SpectoSessionStats(
                totalShots = 0,
                accuracyScore = 49.0,
                averagePowerScore = 7.9,
                skillTier = SpectoSkillTier.L5_195_210,
                avgLaunchSpeedMph = 18.1,
                launchSpeedRangeMph = 1.5,
                avgRpm = 435,
                rpmRange = 104,
                avgLaunchAngleDeg = -2.9,
                launchAngleRangeDeg = 2.0,
                avgLaydownBoard = 24.6,
                laydownBoardRange = 12.2,
                avgArrowBoard = 16.0,
                arrowBoardRange = 12.5,
                avgBreakpointBoard = 6.6,
                breakpointBoardRange = 10.4,
                avgEntryBoard = 16.8,
                entryBoardRange = 13.9,
                avgImpactAngleDeg = 5.1,
                impactAngleRangeDeg = 4.7
            )
        }

        val n = telemetryList.size
        val laydowns = telemetryList.map { it.spatial.laydownBoard }
        val arrows = telemetryList.map { it.spatial.arrowBoard }
        val breakpoints = telemetryList.map { it.spatial.breakpointBoard }
        val entries = telemetryList.map { it.spatial.entryBoard }
        val speeds = telemetryList.map { it.speed.launchSpeedMph }
        val rpms = telemetryList.map { it.dynamics.rpm }
        val launchAngles = telemetryList.map { it.angles.launchAngleDeg }
        val impactAngles = telemetryList.map { it.angles.impactAngleDeg }
        val powerScores = telemetryList.map { it.dynamics.powerScore }

        val laydownRange = (laydowns.maxOrNull() ?: 0.0) - (laydowns.minOrNull() ?: 0.0)
        val arrowRange = (arrows.maxOrNull() ?: 0.0) - (arrows.minOrNull() ?: 0.0)
        val breakpointRange = (breakpoints.maxOrNull() ?: 0.0) - (breakpoints.minOrNull() ?: 0.0)
        val entryRange = (entries.maxOrNull() ?: 0.0) - (entries.minOrNull() ?: 0.0)
        val speedRange = (speeds.maxOrNull() ?: 0.0) - (speeds.minOrNull() ?: 0.0)
        val rpmRange = (rpms.maxOrNull() ?: 0) - (rpms.minOrNull() ?: 0)
        val launchAngleRange = (launchAngles.maxOrNull() ?: 0.0) - (launchAngles.minOrNull() ?: 0.0)
        val impactAngleRange = (impactAngles.maxOrNull() ?: 0.0) - (impactAngles.minOrNull() ?: 0.0)

        // Specto Accuracy Score (lower is better, range-weighted index)
        val accuracyScore = round1(
            laydownRange * 1.0 +
                    arrowRange * 1.0 +
                    breakpointRange * 1.0 +
                    entryRange * 1.0
        )

        val avgPower = round1(powerScores.sum() / n)

        // Classify Skill Tier based on tolerances
        val tier = when {
            laydownRange <= SpectoSkillTier.L6_PRO.laydownTolerance &&
                    arrowRange <= SpectoSkillTier.L6_PRO.arrowTolerance &&
                    breakpointRange <= SpectoSkillTier.L6_PRO.breakpointTolerance -> SpectoSkillTier.L6_PRO
            laydownRange <= SpectoSkillTier.L5_195_210.laydownTolerance &&
                    arrowRange <= SpectoSkillTier.L5_195_210.arrowTolerance -> SpectoSkillTier.L5_195_210
            laydownRange <= SpectoSkillTier.L4_180_195.laydownTolerance &&
                    arrowRange <= SpectoSkillTier.L4_180_195.arrowTolerance -> SpectoSkillTier.L4_180_195
            laydownRange <= SpectoSkillTier.L3_165_180.laydownTolerance &&
                    arrowRange <= SpectoSkillTier.L3_165_180.arrowTolerance -> SpectoSkillTier.L3_165_180
            laydownRange <= SpectoSkillTier.L2_150_165.laydownTolerance -> SpectoSkillTier.L2_150_165
            else -> SpectoSkillTier.L1_UNDER_150
        }

        return SpectoSessionStats(
            totalShots = n,
            accuracyScore = accuracyScore,
            averagePowerScore = avgPower,
            skillTier = tier,
            avgLaunchSpeedMph = round1(speeds.sum() / n),
            launchSpeedRangeMph = round1(speedRange),
            avgRpm = (rpms.sum() / n),
            rpmRange = rpmRange,
            avgLaunchAngleDeg = round1(launchAngles.sum() / n),
            launchAngleRangeDeg = round1(launchAngleRange),
            avgLaydownBoard = round1(laydowns.sum() / n),
            laydownBoardRange = round1(laydownRange),
            avgArrowBoard = round1(arrows.sum() / n),
            arrowBoardRange = round1(arrowRange),
            avgBreakpointBoard = round1(breakpoints.sum() / n),
            breakpointBoardRange = round1(breakpointRange),
            avgEntryBoard = round1(entries.sum() / n),
            entryBoardRange = round1(entryRange),
            avgImpactAngleDeg = round1(impactAngles.sum() / n),
            impactAngleRangeDeg = round1(impactAngleRange)
        )
    }

    private fun defaultSpectoTelemetry(): SpectoTelemetry {
        return SpectoTelemetry(
            spatial = SpectoSpatialMetrics(
                laydownBoard = 17.3,
                loftDistanceFt = 6.2,
                arrowBoard = 8.8,
                patternExitBoard = 1.9,
                breakpointBoard = 1.7,
                breakpointDistanceFt = 37.0,
                entryBoard = 17.9,
                pinDeckExitBoard = 23.3,
                pinDeckDeflection = 5.3
            ),
            angles = SpectoAngleMetrics(
                launchAngleDeg = -2.9,
                breakpointAngleDeg = 8.5,
                impactAngleDeg = 5.6,
                strikeProbabilityPercent = 88.5
            ),
            speed = SpectoSpeedMetrics(
                launchSpeedMph = 17.5,
                entrySpeedMph = 14.3,
                speedLossMph = 3.2,
                avgSpeedMph = 16.2
            ),
            dynamics = SpectoDynamicsMetrics(
                rpm = 391,
                powerScore = 6.84,
                readFt = 16.0,
                skidFt = 23.0,
                hookFt = 31.0,
                rollFt = 6.0
            )
        )
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
