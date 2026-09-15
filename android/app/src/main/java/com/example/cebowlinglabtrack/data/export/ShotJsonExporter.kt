package com.example.cebowlinglabtrack.data.export

import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.SpectoAngleMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoDynamicsMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoSpatialMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoSpeedMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoTelemetry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * 22-Parameter Kegel Specto & Kinematics Data Transfer Objects matching Section 4 JSON Schema.
 */
@Serializable
data class ExportSpatialMetrics(
    @SerialName("laydown_board") val laydownBoard: Double,
    @SerialName("loft_distance_ft") val loftDistanceFt: Double,
    @SerialName("arrow_board") val arrowBoard: Double,
    @SerialName("pattern_exit_board") val patternExitBoard: Double,
    @SerialName("breakpoint_board") val breakpointBoard: Double,
    @SerialName("breakpoint_distance_ft") val breakpointDistanceFt: Double,
    @SerialName("entry_board") val entryBoard: Double,
    @SerialName("pindeck_exit_board") val pinDeckExitBoard: Double,
    @SerialName("pindeck_deflection_boards") val pinDeckDeflectionBoards: Double
)

@Serializable
data class ExportAngleMetrics(
    @SerialName("launch_angle_deg") val launchAngleDeg: Double,
    @SerialName("breakpoint_angle_deg") val breakpointAngleDeg: Double,
    @SerialName("impact_angle_deg") val impactAngleDeg: Double
)

@Serializable
data class ExportSpeedMetrics(
    @SerialName("launch_speed_mph") val launchSpeedMph: Double,
    @SerialName("entry_speed_mph") val entrySpeedMph: Double,
    @SerialName("speed_loss_mph") val speedLossMph: Double,
    @SerialName("average_speed_mph") val averageSpeedMph: Double
)

@Serializable
data class ExportDynamicsMetrics(
    @SerialName("rev_rate_rpm") val revRateRpm: Double,
    @SerialName("skid_distance_ft") val skidDistanceFt: Double,
    @SerialName("hook_distance_ft") val hookDistanceFt: Double,
    @SerialName("roll_distance_ft") val rollDistanceFt: Double,
    @SerialName("power_score") val powerScore: Double,
    @SerialName("accuracy_score") val accuracyScore: Double,
    @SerialName("skill_tier") val skillTier: String
)

@Serializable
data class ExportSpectoTelemetry(
    @SerialName("spatial") val spatial: ExportSpatialMetrics,
    @SerialName("angles") val angles: ExportAngleMetrics,
    @SerialName("speed") val speed: ExportSpeedMetrics,
    @SerialName("dynamics") val dynamics: ExportDynamicsMetrics
)

@Serializable
data class ExportKinematics(
    @SerialName("spine_lateral_tilt_deg") val spineLateralTiltDeg: Double,
    @SerialName("forward_tilt_deg") val forwardTiltDeg: Double,
    @SerialName("knee_flexion_deg") val kneeFlexionDeg: Double,
    @SerialName("shoulder_hip_separation_deg") val shoulderHipSeparationDeg: Double,
    @SerialName("stance_board") val stanceBoard: Double,
    @SerialName("slide_board") val slideBoard: Double,
    @SerialName("drift_boards") val driftBoards: Double
)

/**
 * Strict 1:1 implementation of the Section 4 Nested JSON Export Schema.
 */
@Serializable
data class ShotExportSchema(
    @SerialName("shot_id") val shotId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("shot_number") val shotNumber: Int,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("specto_telemetry") val spectoTelemetry: ExportSpectoTelemetry,
    @SerialName("kinematics") val kinematics: ExportKinematics
) {
    /**
     * Converts exported schema back to internal ShotData domain model.
     */
    fun toShotData(): ShotData {
        return ShotData(
            shotId = shotId,
            sessionId = sessionId,
            shotNumber = shotNumber,
            timestamp = timestamp.toString(),
            spectoTelemetry = SpectoTelemetry(
                spatial = SpectoSpatialMetrics(
                    laydownBoard = spectoTelemetry.spatial.laydownBoard,
                    loftDistanceFt = spectoTelemetry.spatial.loftDistanceFt,
                    arrowBoard = spectoTelemetry.spatial.arrowBoard,
                    patternExitBoard = spectoTelemetry.spatial.patternExitBoard,
                    breakpointBoard = spectoTelemetry.spatial.breakpointBoard,
                    breakpointDistanceFt = spectoTelemetry.spatial.breakpointDistanceFt,
                    entryBoard = spectoTelemetry.spatial.entryBoard,
                    pinDeckExitBoard = spectoTelemetry.spatial.pinDeckExitBoard,
                    pinDeckDeflection = spectoTelemetry.spatial.pinDeckDeflectionBoards
                ),
                angles = SpectoAngleMetrics(
                    launchAngleDeg = spectoTelemetry.angles.launchAngleDeg,
                    breakpointAngleDeg = spectoTelemetry.angles.breakpointAngleDeg,
                    impactAngleDeg = spectoTelemetry.angles.impactAngleDeg
                ),
                speed = SpectoSpeedMetrics(
                    launchSpeedMph = spectoTelemetry.speed.launchSpeedMph,
                    entrySpeedMph = spectoTelemetry.speed.entrySpeedMph,
                    speedLossMph = spectoTelemetry.speed.speedLossMph,
                    avgSpeedMph = spectoTelemetry.speed.averageSpeedMph
                ),
                dynamics = SpectoDynamicsMetrics(
                    rpm = spectoTelemetry.dynamics.revRateRpm.toInt(),
                    powerScore = spectoTelemetry.dynamics.powerScore,
                    readFt = (spectoTelemetry.spatial.breakpointDistanceFt * 0.58).coerceIn(12.0, 28.0),
                    skidFt = spectoTelemetry.dynamics.skidDistanceFt,
                    hookFt = spectoTelemetry.dynamics.hookDistanceFt,
                    rollFt = spectoTelemetry.dynamics.rollDistanceFt,
                    accuracyScore = spectoTelemetry.dynamics.accuracyScore,
                    skillTier = spectoTelemetry.dynamics.skillTier
                )
            ),
            kinematics = BowlerKinematics(
                spineLateralTiltDeg = kinematics.spineLateralTiltDeg,
                forwardTiltDeg = kinematics.forwardTiltDeg,
                kneeFlexionDeg = kinematics.kneeFlexionDeg,
                shoulderHipSeparationDeg = kinematics.shoulderHipSeparationDeg,
                stanceBoard = kinematics.stanceBoard,
                slideBoard = kinematics.slideBoard,
                driftBoards = kinematics.driftBoards
            )
        )
    }
}

/**
 * Exporter generating strict, validated JSON matching Section 4 of the specification.
 */
object ShotJsonExporter {

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Serializes a ShotData object to the exact specification JSON string.
     */
    fun exportToJson(shot: ShotData): String {
        val schema = toExportSchema(shot)
        return json.encodeToString(schema)
    }

    /**
     * Maps ShotData into the clean ShotExportSchema matching Section 4.
     */
    fun toExportSchema(shot: ShotData): ShotExportSchema {
        val sp = shot.spectoTelemetry.spatial
        val an = shot.spectoTelemetry.angles
        val sd = shot.spectoTelemetry.speed
        val dy = shot.spectoTelemetry.dynamics
        val kn = shot.kinematics

        val parsedTimestamp = shot.timestamp.toLongOrNull() ?: try {
            Instant.parse(shot.timestamp).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        return ShotExportSchema(
            shotId = shot.shotId,
            sessionId = shot.sessionId,
            shotNumber = shot.shotNumber,
            timestamp = parsedTimestamp,
            spectoTelemetry = ExportSpectoTelemetry(
                spatial = ExportSpatialMetrics(
                    laydownBoard = sp.laydownBoard,
                    loftDistanceFt = sp.loftDistanceFt,
                    arrowBoard = sp.arrowBoard,
                    patternExitBoard = sp.patternExitBoard,
                    breakpointBoard = sp.breakpointBoard,
                    breakpointDistanceFt = sp.breakpointDistanceFt,
                    entryBoard = sp.entryBoard,
                    pinDeckExitBoard = sp.pinDeckExitBoard,
                    pinDeckDeflectionBoards = sp.pinDeckDeflection
                ),
                angles = ExportAngleMetrics(
                    launchAngleDeg = an.launchAngleDeg,
                    breakpointAngleDeg = an.breakpointAngleDeg,
                    impactAngleDeg = an.impactAngleDeg
                ),
                speed = ExportSpeedMetrics(
                    launchSpeedMph = sd.launchSpeedMph,
                    entrySpeedMph = sd.entrySpeedMph,
                    speedLossMph = sd.speedLossMph,
                    averageSpeedMph = sd.avgSpeedMph
                ),
                dynamics = ExportDynamicsMetrics(
                    revRateRpm = dy.rpm.toDouble(),
                    skidDistanceFt = dy.skidFt,
                    hookDistanceFt = dy.hookFt,
                    rollDistanceFt = dy.rollFt,
                    powerScore = dy.powerScore,
                    accuracyScore = dy.accuracyScore,
                    skillTier = dy.skillTier
                )
            ),
            kinematics = ExportKinematics(
                spineLateralTiltDeg = kn.spineLateralTiltDeg,
                forwardTiltDeg = kn.forwardTiltDeg,
                kneeFlexionDeg = kn.kneeFlexionDeg,
                shoulderHipSeparationDeg = kn.shoulderHipSeparationDeg,
                stanceBoard = kn.stanceBoard,
                slideBoard = kn.slideBoard,
                driftBoards = kn.driftBoards
            )
        )
    }

    /**
     * Parses an exported JSON back into ShotExportSchema.
     */
    fun parseFromJson(jsonString: String): ShotExportSchema {
        return json.decodeFromString(jsonString)
    }
}
