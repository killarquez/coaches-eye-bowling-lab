package com.example.cebowlinglabtrack.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a discrete trajectory sample along the bowling lane.
 */
@Serializable
data class TrajectoryPoint(
    @SerialName("xBoard") val xBoard: Double,
    @SerialName("yFt") val yFt: Double,
    @SerialName("timeMs") val timeMs: Long,
    val vx: Double = 0.0,
    val vy: Double = 0.0,
    val isFiltered: Boolean = true
) {
    val board: Double get() = xBoard
    val timestampMs: Long get() = timeMs
}

/**
 * Specto Telemetry - Spatial Metrics (Boards and Longitudinal Distances).
 */
@Serializable
data class SpectoSpatialMetrics(
    @SerialName("laydown_board") val laydownBoard: Double,
    @SerialName("loft_distance_ft") val loftDistanceFt: Double,
    @SerialName("arrow_board") val arrowBoard: Double,
    @SerialName("pattern_exit_board") val patternExitBoard: Double,
    @SerialName("breakpoint_board") val breakpointBoard: Double,
    @SerialName("breakpoint_distance_ft") val breakpointDistanceFt: Double,
    @SerialName("entry_board") val entryBoard: Double,
    @SerialName("pindeck_exit_board") val pinDeckExitBoard: Double,
    @SerialName("pindeck_deflection") val pinDeckDeflection: Double
)

/**
 * Specto Telemetry - Angular Vectors (Relative to Lane Centerline X=20).
 */
@Serializable
data class SpectoAngleMetrics(
    @SerialName("launch_angle_deg") val launchAngleDeg: Double,
    @SerialName("breakpoint_angle_deg") val breakpointAngleDeg: Double,
    @SerialName("impact_angle_deg") val impactAngleDeg: Double
)

/**
 * Specto Telemetry - Speed & Velocity Decay.
 */
@Serializable
data class SpectoSpeedMetrics(
    @SerialName("launch_speed_mph") val launchSpeedMph: Double,
    @SerialName("entry_speed_mph") val entrySpeedMph: Double,
    @SerialName("speed_loss_mph") val speedLossMph: Double,
    @SerialName("avg_speed_mph") val avgSpeedMph: Double
)

/**
 * Specto Telemetry - Ball Motion Phases & Dynamics.
 */
@Serializable
data class SpectoDynamicsMetrics(
    @SerialName("rpm") val rpm: Int,
    @SerialName("power_score") val powerScore: Double,
    @SerialName("read_ft") val readFt: Double = 16.0,
    @SerialName("skid_ft") val skidFt: Double,
    @SerialName("hook_ft") val hookFt: Double,
    @SerialName("roll_ft") val rollFt: Double,
    @SerialName("accuracy_score") val accuracyScore: Double = 96.2,
    @SerialName("skill_tier") val skillTier: String = "PRO L1",
    @SerialName("axis_tilt_deg") val axisTiltDeg: Double = 14.0,
    @SerialName("axis_rotation_deg") val axisRotationDeg: Double = 55.0,
    @SerialName("total_rotations") val totalRotations: Double = 14.0,
    @SerialName("is_optical_rev_counted") val isOpticalRevCounted: Boolean = false
)

/**
 * Complete 22-parameter Specto Telemetry Model matching Kegel Specto reports.
 */
@Serializable
data class SpectoTelemetry(
    @SerialName("spatial") val spatial: SpectoSpatialMetrics,
    @SerialName("angles") val angles: SpectoAngleMetrics,
    @SerialName("speed") val speed: SpectoSpeedMetrics,
    @SerialName("dynamics") val dynamics: SpectoDynamicsMetrics
)

/**
 * Bowler skeletal kinematics captured during approach and release.
 */
@Serializable
data class BowlerKinematics(
    @SerialName("spine_lateral_tilt_deg") val spineLateralTiltDeg: Double,
    @SerialName("forward_tilt_deg") val forwardTiltDeg: Double,
    @SerialName("knee_flexion_deg") val kneeFlexionDeg: Double,
    @SerialName("shoulder_hip_separation_deg") val shoulderHipSeparationDeg: Double,
    @SerialName("stance_board") val stanceBoard: Double = 22.0,
    @SerialName("slide_board") val slideBoard: Double = 21.0,
    @SerialName("drift_boards") val driftBoards: Double = -1.0,
    val apexTimeMs: Long = 0L,
    val plantTimeMs: Long = 0L,
    val releaseTimeMs: Long = 0L,
    val apexToPlantMs: Long = 0L,
    val plantToReleaseMs: Long = 0L
) {
    // Backward compatibility accessors
    val spineTiltReleaseDeg: Double get() = spineLateralTiltDeg
    val forwardTiltReleaseDeg: Double get() = forwardTiltDeg
    val slideKneeFlexionDeg: Double get() = kneeFlexionDeg
    val slideFootBoard: Double get() = slideBoard
    val stanceFootBoard: Double get() = stanceBoard
    val footDriftBoards: Double get() = driftBoards
}

/**
 * Unified Shot Data Model matching the exact JSON export schema.
 */
@Serializable
data class ShotData(
    @SerialName("shot_id") val shotId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("shot_number") val shotNumber: Int = 1,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("bowler_id") val bowlerId: String = "default-bowler",
    @SerialName("specto_telemetry") val spectoTelemetry: SpectoTelemetry,
    @SerialName("kinematics") val kinematics: BowlerKinematics,
    val trajectoryPoints: List<TrajectoryPoint> = emptyList()
) {
    // Backward compatibility helper for legacy UI access
    val ballMetrics: BallMetrics get() = BallMetrics(
        laydownBoard = spectoTelemetry.spatial.laydownBoard,
        arrowBoard = spectoTelemetry.spatial.arrowBoard,
        breakpointBoard = spectoTelemetry.spatial.breakpointBoard,
        breakpointDistanceFt = spectoTelemetry.spatial.breakpointDistanceFt,
        launchSpeedMph = spectoTelemetry.speed.launchSpeedMph,
        deckSpeedMph = spectoTelemetry.speed.entrySpeedMph,
        entryAngleDeg = spectoTelemetry.angles.impactAngleDeg,
        axisTiltDeg = spectoTelemetry.dynamics.axisTiltDeg,
        axisRotationDeg = spectoTelemetry.dynamics.axisRotationDeg,
        rpm = spectoTelemetry.dynamics.rpm,
        totalRotations = spectoTelemetry.dynamics.totalRotations,
        isOpticalRevCounted = spectoTelemetry.dynamics.isOpticalRevCounted
    )
}

/**
 * Compatibility wrapper for legacy UI cards while Specto full model is rendered.
 */
@Serializable
data class BallMetrics(
    val laydownBoard: Double,
    val arrowBoard: Double,
    val breakpointBoard: Double,
    val breakpointDistanceFt: Double,
    val launchSpeedMph: Double,
    val deckSpeedMph: Double,
    val entryAngleDeg: Double,
    val axisTiltDeg: Double = 14.0,
    val axisRotationDeg: Double = 55.0,
    val rpm: Int = 435,
    val totalRotations: Double = 14.0,
    val isOpticalRevCounted: Boolean = false
)

/**
 * The 6 Specto Skill Level Performance Tiers from the authentic Kegel benchmark.
 */
@Serializable
enum class SpectoSkillTier(
    val tierName: String,
    val averageScoreRange: String,
    val laydownTolerance: Double,
    val arrowTolerance: Double,
    val breakpointTolerance: Double,
    val entryTolerance: Double,
    val launchAngleTolerance: Double,
    val launchSpeedTolerance: Double,
    val rpmTolerancePct: Double
) {
    L1_UNDER_150("L1", "<150 avg", 4.0, 8.0, 15.0, 18.0, 1.5, 2.2, 35.0),
    L2_150_165("L2", "150–165", 3.6, 6.3, 11.9, 14.3, 1.2, 1.7, 30.0),
    L3_165_180("L3", "165–180", 2.7, 5.4, 10.2, 12.3, 1.0, 1.4, 25.0),
    L4_180_195("L4", "180–195", 1.9, 3.8, 7.0, 8.0, 0.8, 1.1, 20.0),
    L5_195_210("L5", "195–210", 1.5, 3.0, 5.6, 6.3, 0.7, 0.9, 15.0),
    L6_PRO("L6 (PRO)", "Pro Tour", 0.8, 1.5, 2.9, 2.5, 0.6, 0.6, 10.0)
}

/**
 * Multi-shot session consistency statistics.
 */
@Serializable
data class SpectoSessionStats(
    val totalShots: Int,
    val accuracyScore: Double,       // Overall Accuracy Index (e.g. 49.0, lower is better)
    val averagePowerScore: Double,   // Average Power Score (e.g. 7.9)
    val skillTier: SpectoSkillTier,  // Classified Tier (L1..L6 PRO)
    val avgLaunchSpeedMph: Double,
    val launchSpeedRangeMph: Double,
    val avgRpm: Int,
    val rpmRange: Int,
    val avgLaunchAngleDeg: Double,
    val launchAngleRangeDeg: Double,
    val avgLaydownBoard: Double,
    val laydownBoardRange: Double,
    val avgArrowBoard: Double,
    val arrowBoardRange: Double,
    val avgBreakpointBoard: Double,
    val breakpointBoardRange: Double,
    val avgEntryBoard: Double,
    val entryBoardRange: Double,
    val avgImpactAngleDeg: Double,
    val impactAngleRangeDeg: Double
)

@Serializable
enum class Handedness { RIGHT, LEFT }

@Serializable
enum class BowlingStyle {
    ONE_HANDED,           // Legacy compatibility
    ONE_HANDED_THUMB,     // 1-Handed (Thumb In)
    ONE_HANDED_NO_THUMB,  // 1-Handed (No Thumb)
    TWO_HANDED;           // 2-Handed

    fun displayName(): String = when (this) {
        ONE_HANDED, ONE_HANDED_THUMB -> "1-Handed (Thumb In)"
        ONE_HANDED_NO_THUMB -> "1-Handed (No Thumb)"
        TWO_HANDED -> "2-Handed"
    }
}

@Serializable
data class BowlerProfile(
    val id: String, // e.g. "CEB-101"
    val name: String, // e.g. "Marcus Turner"
    val email: String = "",
    val phone: String = "",
    val heightInches: Double = 70.0,
    val handedness: Handedness = Handedness.RIGHT,
    val style: BowlingStyle = BowlingStyle.TWO_HANDED,
    val bookAverage: Int = 194,
    val careerHighGame: Int = 279,
    val careerHighSeries: Int = 698,
    val papCoordinates: String = "4 3/4\" over by 1/2\" up",
    val benchmarkSpeedMph: Double = 16.2,
    val benchmarkRpm: Int = 460,
    val benchmarkAxisTiltDeg: Double = 14.0,
    val benchmarkAxisRotationDeg: Double = 55.0,
    val totalSessionsCoached: Int = 2,
    val lastSessionDate: String = "2026-09-02",
    val primaryGoal: String = "Rev Rate & Ball Speed Synchronization",
    val notes: String = ""
)

object LaneConstants {
    const val LANE_WIDTH_INCHES = 41.5
    const val TOTAL_BOARDS = 39
    const val BOARD_WIDTH_INCHES = LANE_WIDTH_INCHES / TOTAL_BOARDS
    const val BOARD_WIDTH_FEET = BOARD_WIDTH_INCHES / 12.0
    const val FOUL_LINE_TO_HEADPIN_FT = 60.0
    const val TOTAL_LANE_LENGTH_FT = 62.833 // Back of pin deck into the pit
    const val ARROWS_DISTANCE_FT = 15.0
    const val DEFAULT_OIL_PATTERN_EXIT_FT = 40.0
    const val RANGE_FINDERS_DISTANCE_FT = 40.0
    const val FT_PER_SEC_TO_MPH = 0.68181818
}

@Serializable
data class Point2D(val x: Double, val y: Double)

@Serializable
data class LanePoint(val board: Double, val distanceFt: Double)

@Serializable
data class LaneCalibration(
    val id: String,
    val laneName: String = "Lane 1",
    val foulLineLeftScreen: Point2D,
    val foulLineRightScreen: Point2D,
    val arrowsLeftScreen: Point2D,
    val arrowsRightScreen: Point2D,
    val homographyMatrixElements: List<Double> = emptyList(),
    val reprojectionErrorRmse: Double = 0.0,
    val calibrationZoomRatio: Float = 1.0f,
    val anchorMode: String = "GUTTERS_AT_ARROWS"
)
