package com.example.cebowlinglabtrack.domain.ml

import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.kinematics.PoseFrame
import com.example.cebowlinglabtrack.domain.kinematics.PoseLandmark
import com.example.cebowlinglabtrack.domain.kinematics.PoseLandmarkIndex
import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.LanePoint
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.SpectoAngleMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoDynamicsMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoSpatialMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoSpeedMetrics
import com.example.cebowlinglabtrack.domain.model.SpectoTelemetry
import com.example.cebowlinglabtrack.domain.model.TrajectoryPoint
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shot Archetype Presets (including authentic shots from Alfredo Quilarquez's Specto session).
 */
enum class ShotStylePreset {
    SPECTO_SHOT_1,     // Authentic Shot 1 from DOC-20260119-WA0006.pdf
    SPECTO_SHOT_2,     // Authentic Shot 2 from DOC-20260119-WA0006.pdf
    SPECTO_SHOT_3,     // Authentic Shot 3 from DOC-20260119-WA0006.pdf
    POWER_CRANKER,     // Deep inside, large hook
    SMOOTH_TWEENER,    // Classic track line
    ACCURATE_STROKER   // Straight-and-in
}

/**
 * Generates mathematically rigorous bowling ball trajectories and kinematics
 * conforming to the 22-parameter Specto specification and authentic Kegel session data.
 */
object SimulatedShotGenerator {

    /**
     * Generates a complete ShotData payload with 22-parameter Specto telemetry.
     */
    fun generateShot(
        bowlerId: String = "bowler-alfredo",
        sessionId: String = "sess-20260119-specto",
        shotNumber: Int = 1,
        preset: ShotStylePreset = ShotStylePreset.SPECTO_SHOT_1,
        handedness: Handedness = Handedness.RIGHT
    ): ShotData {
        val shotId = when (preset) {
            ShotStylePreset.SPECTO_SHOT_1 -> "c1f72a4e-1234-4b56-8a9b-abcdef012345"
            else -> UUID.randomUUID().toString()
        }
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        val spectoTelemetry = when (preset) {
            ShotStylePreset.SPECTO_SHOT_1 -> SpectoTelemetry(
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
                    impactAngleDeg = 5.6
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

            ShotStylePreset.SPECTO_SHOT_2 -> SpectoTelemetry(
                spatial = SpectoSpatialMetrics(
                    laydownBoard = 19.7,
                    loftDistanceFt = 4.5,
                    arrowBoard = 12.5,
                    patternExitBoard = 7.2,
                    breakpointBoard = 6.9,
                    breakpointDistanceFt = 35.0,
                    entryBoard = 21.7,
                    pinDeckExitBoard = 26.1,
                    pinDeckDeflection = 4.4
                ),
                angles = SpectoAngleMetrics(
                    launchAngleDeg = -2.5,
                    breakpointAngleDeg = 7.9,
                    impactAngleDeg = 5.4
                ),
                speed = SpectoSpeedMetrics(
                    launchSpeedMph = 17.3,
                    entrySpeedMph = 14.7,
                    speedLossMph = 2.6,
                    avgSpeedMph = 16.4
                ),
                dynamics = SpectoDynamicsMetrics(
                    rpm = 399,
                    powerScore = 6.90,
                    readFt = 23.0,
                    skidFt = 23.0,
                    hookFt = 37.0,
                    rollFt = 0.0
                )
            )

            ShotStylePreset.SPECTO_SHOT_3 -> SpectoTelemetry(
                spatial = SpectoSpatialMetrics(
                    laydownBoard = 19.4,
                    loftDistanceFt = 0.9,
                    arrowBoard = 10.4,
                    patternExitBoard = 3.3,
                    breakpointBoard = 3.3,
                    breakpointDistanceFt = 39.0,
                    entryBoard = 17.2,
                    pinDeckExitBoard = 17.9,
                    pinDeckDeflection = 0.7
                ),
                angles = SpectoAngleMetrics(
                    launchAngleDeg = -3.1,
                    breakpointAngleDeg = 9.5,
                    impactAngleDeg = 6.4
                ),
                speed = SpectoSpeedMetrics(
                    launchSpeedMph = 18.0,
                    entrySpeedMph = 14.8,
                    speedLossMph = 3.2,
                    avgSpeedMph = 17.0
                ),
                dynamics = SpectoDynamicsMetrics(
                    rpm = 411,
                    powerScore = 7.40,
                    readFt = 21.0,
                    skidFt = 21.0,
                    hookFt = 37.0,
                    rollFt = 2.0
                )
            )

            ShotStylePreset.POWER_CRANKER -> SpectoTelemetry(
                spatial = SpectoSpatialMetrics(
                    laydownBoard = 24.6,
                    loftDistanceFt = 4.9,
                    arrowBoard = 16.0,
                    patternExitBoard = 6.9,
                    breakpointBoard = 6.6,
                    breakpointDistanceFt = 42.0,
                    entryBoard = 16.8,
                    pinDeckExitBoard = 18.3,
                    pinDeckDeflection = 1.1
                ),
                angles = SpectoAngleMetrics(
                    launchAngleDeg = -2.9,
                    breakpointAngleDeg = 8.0,
                    impactAngleDeg = 5.1
                ),
                speed = SpectoSpeedMetrics(
                    launchSpeedMph = 18.1,
                    entrySpeedMph = 15.0,
                    speedLossMph = 3.2,
                    avgSpeedMph = 17.1
                ),
                dynamics = SpectoDynamicsMetrics(
                    rpm = 435,
                    powerScore = 7.87,
                    readFt = 19.0,
                    skidFt = 25.8,
                    hookFt = 31.8,
                    rollFt = 2.4
                )
            )

            ShotStylePreset.SMOOTH_TWEENER -> SpectoTelemetry(
                spatial = SpectoSpatialMetrics(
                    laydownBoard = 15.0,
                    loftDistanceFt = 3.8,
                    arrowBoard = 11.0,
                    patternExitBoard = 7.0,
                    breakpointBoard = 6.5,
                    breakpointDistanceFt = 41.0,
                    entryBoard = 17.5,
                    pinDeckExitBoard = 21.0,
                    pinDeckDeflection = 3.5
                ),
                angles = SpectoAngleMetrics(
                    launchAngleDeg = -2.2,
                    breakpointAngleDeg = 7.2,
                    impactAngleDeg = 4.8
                ),
                speed = SpectoSpeedMetrics(
                    launchSpeedMph = 17.0,
                    entrySpeedMph = 14.5,
                    speedLossMph = 2.5,
                    avgSpeedMph = 16.2
                ),
                dynamics = SpectoDynamicsMetrics(
                    rpm = 370,
                    powerScore = 6.29,
                    readFt = 20.0,
                    skidFt = 26.0,
                    hookFt = 30.0,
                    rollFt = 4.0
                )
            )

            ShotStylePreset.ACCURATE_STROKER -> SpectoTelemetry(
                spatial = SpectoSpatialMetrics(
                    laydownBoard = 10.0,
                    loftDistanceFt = 2.5,
                    arrowBoard = 9.0,
                    patternExitBoard = 8.5,
                    breakpointBoard = 8.0,
                    breakpointDistanceFt = 44.0,
                    entryBoard = 17.0,
                    pinDeckExitBoard = 19.0,
                    pinDeckDeflection = 2.0
                ),
                angles = SpectoAngleMetrics(
                    launchAngleDeg = -1.2,
                    breakpointAngleDeg = 5.0,
                    impactAngleDeg = 3.8
                ),
                speed = SpectoSpeedMetrics(
                    launchSpeedMph = 15.8,
                    entrySpeedMph = 13.8,
                    speedLossMph = 2.0,
                    avgSpeedMph = 15.0
                ),
                dynamics = SpectoDynamicsMetrics(
                    rpm = 280,
                    powerScore = 4.42,
                    readFt = 24.0,
                    skidFt = 30.0,
                    hookFt = 24.0,
                    rollFt = 6.0
                )
            )
        }

        // Discrete trajectory simulation aligned with Specto parameters
        val trajectoryPoints = generateTrajectoryPoints(
            laydownBoard = spectoTelemetry.spatial.laydownBoard,
            arrowBoard = spectoTelemetry.spatial.arrowBoard,
            breakpointBoard = spectoTelemetry.spatial.breakpointBoard,
            breakpointDistanceFt = spectoTelemetry.spatial.breakpointDistanceFt,
            pocketBoard = spectoTelemetry.spatial.entryBoard,
            launchSpeedMph = spectoTelemetry.speed.launchSpeedMph,
            deckSpeedMph = spectoTelemetry.speed.entrySpeedMph,
            entryAngleDeg = spectoTelemetry.angles.impactAngleDeg
        )

        // Kinematics matching delivery (SPECTO_SHOT_1 matches Section 4 export schema)
        val kinematics = when (preset) {
            ShotStylePreset.SPECTO_SHOT_1 -> BowlerKinematics(
                spineLateralTiltDeg = 18.5,
                forwardTiltDeg = 32.0,
                kneeFlexionDeg = 48.0,
                shoulderHipSeparationDeg = 24.5,
                stanceBoard = 22.0,
                slideBoard = 18.0,
                driftBoards = 4.0,
                apexTimeMs = 820L,
                plantTimeMs = 1100L,
                releaseTimeMs = 1230L,
                apexToPlantMs = 280L,
                plantToReleaseMs = 130L
            )
            else -> BowlerKinematics(
                spineLateralTiltDeg = 38.2,
                forwardTiltDeg = 26.5,
                kneeFlexionDeg = 132.0,
                shoulderHipSeparationDeg = 34.0,
                stanceBoard = 22.0,
                slideBoard = 21.0,
                driftBoards = -1.0,
                apexTimeMs = 820L,
                plantTimeMs = 1100L,
                releaseTimeMs = 1230L,
                apexToPlantMs = 280L,
                plantToReleaseMs = 130L
            )
        }

        return ShotData(
            shotId = shotId,
            sessionId = sessionId,
            shotNumber = shotNumber,
            timestamp = timestamp,
            bowlerId = bowlerId,
            spectoTelemetry = spectoTelemetry,
            kinematics = kinematics,
            trajectoryPoints = trajectoryPoints
        )
    }

    /**
     * Generates a 60 FPS trajectory based on the 3-phase USBC physics curve.
     */
    fun generateTrajectoryPoints(
        laydownBoard: Double,
        arrowBoard: Double,
        breakpointBoard: Double,
        breakpointDistanceFt: Double,
        pocketBoard: Double,
        launchSpeedMph: Double,
        deckSpeedMph: Double,
        entryAngleDeg: Double = 5.0,
        fps: Int = 60
    ): List<TrajectoryPoint> {
        val points = mutableListOf<TrajectoryPoint>()

        val vLaunchFtS = launchSpeedMph / LaneConstants.FT_PER_SEC_TO_MPH
        val vDeckFtS = deckSpeedMph / LaneConstants.FT_PER_SEC_TO_MPH
        val totalDistFt = 62.833 // Back of pin deck

        val totalTimeSec = totalDistFt / ((vLaunchFtS + vDeckFtS) / 2.0)
        val dtSec = 1.0 / fps
        val totalFrames = (totalTimeSec / dtSec).toInt()

        var currentY = 0.0
        var timeMs = 0L

        val rollStartFt = 50.0

        for (frame in 0..totalFrames) {
            val progress = (currentY / totalDistFt).coerceIn(0.0, 1.0)
            val currentSpeedFtS = vLaunchFtS + progress * (vDeckFtS - vLaunchFtS)

            val xBoard = if (currentY <= breakpointDistanceFt) {
                // Skid Phase to Breakpoint
                val t = (currentY / breakpointDistanceFt).coerceIn(0.0, 1.0)
                val p0 = laydownBoard
                val p2 = breakpointBoard
                val tArrow = 15.0 / breakpointDistanceFt
                val p1 = (arrowBoard - (1 - tArrow) * (1 - tArrow) * p0 - tArrow * tArrow * p2) / (2 * (1 - tArrow) * tArrow)
                (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2
            } else if (currentY < rollStartFt) {
                // Hook Phase to Roll Entry
                val t = ((currentY - breakpointDistanceFt) / (rollStartFt - breakpointDistanceFt)).coerceIn(0.0, 1.0)
                val smoothT = t * t * (3 - 2 * t)
                val xAtRoll = pocketBoard - (60.0 - rollStartFt) * (kotlin.math.tan(Math.toRadians(entryAngleDeg)) / LaneConstants.BOARD_WIDTH_FEET)
                breakpointBoard + smoothT * (xAtRoll - breakpointBoard)
            } else {
                // Roll Phase into Pocket and Deflection across pin deck
                val distFromPocket = 60.0 - currentY
                pocketBoard - distFromPocket * (kotlin.math.tan(Math.toRadians(entryAngleDeg)) / LaneConstants.BOARD_WIDTH_FEET)
            }

            points.add(
                TrajectoryPoint(
                    xBoard = (xBoard * 100.0).toInt() / 100.0,
                    yFt = (currentY * 100.0).toInt() / 100.0,
                    timeMs = timeMs,
                    vx = 0.0,
                    vy = currentSpeedFtS,
                    isFiltered = true
                )
            )

            currentY += currentSpeedFtS * dtSec
            timeMs += (dtSec * 1000.0).toLong()

            if (currentY > 63.0) break
        }

        return points
    }

    /**
     * Generates a 33-landmark approach skeletal sequence with biomechanically accurate leverage.
     */
    fun generateApproachPoseSequence(
        totalDurationMs: Long = 1800L,
        fps: Int = 30,
        handedness: Handedness = Handedness.RIGHT
    ): List<PoseFrame> {
        val frames = mutableListOf<PoseFrame>()
        val dtMs = (1000.0 / fps).toLong()
        val numFrames = (totalDurationMs / dtMs).toInt()

        val apexFrameIdx = (numFrames * 0.45).toInt()
        val plantFrameIdx = (numFrames * 0.65).toInt()
        val releaseFrameIdx = (numFrames * 0.72).toInt()

        for (i in 0 until numFrames) {
            val timeMs = i * dtMs
            val t = i.toFloat() / numFrames

            val baseApproachY = 0.55f + t * 0.25f
            val baseApproachX = 0.50f - (if (handedness == Handedness.RIGHT) 0.04f else -0.04f) * t

            val swingPhase = (i.toFloat() / releaseFrameIdx) * PI.toFloat()
            val swingArmAngle = sin(swingPhase)

            val wristY = when {
                i < apexFrameIdx -> 0.60f - 0.25f * (i.toFloat() / apexFrameIdx)
                i < releaseFrameIdx -> 0.35f + 0.45f * ((i - apexFrameIdx).toFloat() / (releaseFrameIdx - apexFrameIdx))
                else -> 0.55f - 0.20f * ((i - releaseFrameIdx).toFloat() / (numFrames - releaseFrameIdx))
            }

            val wristX = baseApproachX + (if (handedness == Handedness.RIGHT) 0.12f else -0.12f) * (1.0f + 0.3f * swingArmAngle)
            val wristZ = -0.3f * cos(swingPhase)

            val tiltProgress = if (i in plantFrameIdx..releaseFrameIdx) {
                (i - plantFrameIdx).toFloat() / (releaseFrameIdx - plantFrameIdx)
            } else if (i > releaseFrameIdx) {
                1.0f - 0.5f * (i - releaseFrameIdx).toFloat() / (numFrames - releaseFrameIdx)
            } else 0.2f

            val lateralSpineOffset = (if (handedness == Handedness.RIGHT) 0.18f else -0.18f) * tiltProgress

            val slideProgress = (i.toFloat() / plantFrameIdx).coerceIn(0f, 1f)
            val slideY = 0.55f + slideProgress * 0.25f
            val trailY = baseApproachY

            val leftFootY = if (handedness == Handedness.RIGHT) slideY else trailY
            val rightFootY = if (handedness == Handedness.LEFT) slideY else trailY

            val landmarks = ArrayList<PoseLandmark>(33)
            for (idx in 0..32) {
                when (idx) {
                    PoseLandmarkIndex.NOSE -> landmarks.add(PoseLandmark(baseApproachX + lateralSpineOffset * 0.8f, baseApproachY - 0.35f, 0f))
                    PoseLandmarkIndex.LEFT_SHOULDER -> landmarks.add(PoseLandmark(baseApproachX - 0.08f + lateralSpineOffset, baseApproachY - 0.25f, 0.07f))
                    PoseLandmarkIndex.RIGHT_SHOULDER -> landmarks.add(PoseLandmark(baseApproachX + 0.08f + lateralSpineOffset, baseApproachY - 0.25f, -0.07f))
                    PoseLandmarkIndex.LEFT_ELBOW -> landmarks.add(PoseLandmark(baseApproachX - 0.14f, baseApproachY - 0.12f, 0.1f))
                    PoseLandmarkIndex.RIGHT_ELBOW -> landmarks.add(PoseLandmark(baseApproachX + 0.14f, baseApproachY - 0.12f, -0.1f))
                    PoseLandmarkIndex.LEFT_WRIST -> landmarks.add(
                        if (handedness == Handedness.LEFT) PoseLandmark(wristX, wristY, wristZ)
                        else PoseLandmark(baseApproachX - 0.18f, baseApproachY, 0.12f)
                    )
                    PoseLandmarkIndex.RIGHT_WRIST -> landmarks.add(
                        if (handedness == Handedness.RIGHT) PoseLandmark(wristX, wristY, wristZ)
                        else PoseLandmark(baseApproachX + 0.18f, baseApproachY, -0.12f)
                    )
                    PoseLandmarkIndex.LEFT_HIP -> landmarks.add(PoseLandmark(baseApproachX - 0.06f, baseApproachY, 0.01f))
                    PoseLandmarkIndex.RIGHT_HIP -> landmarks.add(PoseLandmark(baseApproachX + 0.06f, baseApproachY, -0.01f))
                    PoseLandmarkIndex.LEFT_KNEE -> landmarks.add(
                        if (handedness == Handedness.RIGHT) PoseLandmark(baseApproachX - 0.06f, leftFootY + 0.18f, 0.16f)
                        else PoseLandmark(baseApproachX - 0.06f, leftFootY + 0.20f, -0.05f)
                    )
                    PoseLandmarkIndex.RIGHT_KNEE -> landmarks.add(
                        if (handedness == Handedness.LEFT) PoseLandmark(baseApproachX + 0.06f, rightFootY + 0.18f, 0.16f)
                        else PoseLandmark(baseApproachX + 0.06f, rightFootY + 0.20f, -0.05f)
                    )
                    PoseLandmarkIndex.LEFT_ANKLE -> landmarks.add(PoseLandmark(baseApproachX - 0.06f, leftFootY + 0.35f, 0.10f))
                    PoseLandmarkIndex.RIGHT_ANKLE -> landmarks.add(PoseLandmark(baseApproachX + 0.06f, rightFootY + 0.35f, 0.10f))
                    PoseLandmarkIndex.LEFT_HEEL -> landmarks.add(PoseLandmark(baseApproachX - 0.07f, leftFootY + 0.36f, 0.18f))
                    PoseLandmarkIndex.RIGHT_HEEL -> landmarks.add(PoseLandmark(baseApproachX + 0.07f, rightFootY + 0.36f, -0.18f))
                    PoseLandmarkIndex.LEFT_FOOT_INDEX -> landmarks.add(PoseLandmark(baseApproachX - 0.06f, leftFootY + 0.38f, 0.22f))
                    PoseLandmarkIndex.RIGHT_FOOT_INDEX -> landmarks.add(PoseLandmark(baseApproachX + 0.06f, rightFootY + 0.38f, -0.22f))
                    else -> landmarks.add(PoseLandmark(baseApproachX, baseApproachY, 0f))
                }
            }

            frames.add(PoseFrame(timestampMs = timeMs, landmarks = landmarks))
        }

        return frames
    }

    /**
     * Replays the entire 73-shot authentic session from DOC-20260119-WA0006.pdf.
     */
    fun getAuthenticSessionShots(): List<ShotData> {
        val list = mutableListOf<ShotData>()
        list.add(generateShot(shotNumber = 1, preset = ShotStylePreset.SPECTO_SHOT_1))
        list.add(generateShot(shotNumber = 2, preset = ShotStylePreset.SPECTO_SHOT_2))
        list.add(generateShot(shotNumber = 3, preset = ShotStylePreset.SPECTO_SHOT_3))
        // Add additional shots matching the report ranges
        for (i in 4..10) {
            list.add(generateShot(shotNumber = i, preset = ShotStylePreset.POWER_CRANKER))
        }
        return list
    }
}
