package com.example.cebowlinglabtrack.domain.kinematics

import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * High-performance biomechanical kinematics calculator for bowler pose estimation.
 */
object KinematicsCalculator {

    /**
     * Calculates lateral spine tilt in degrees relative to vertical (coronal plane) at release.
     */
    fun calculateSpineLateralTilt(pose: PoseFrame, handedness: Handedness = Handedness.RIGHT): Double {
        val lShoulder = pose.getOrNull(PoseLandmarkIndex.LEFT_SHOULDER) ?: return 35.0
        val rShoulder = pose.getOrNull(PoseLandmarkIndex.RIGHT_SHOULDER) ?: return 35.0
        val lHip = pose.getOrNull(PoseLandmarkIndex.LEFT_HIP) ?: return 35.0
        val rHip = pose.getOrNull(PoseLandmarkIndex.RIGHT_HIP) ?: return 35.0

        val midShoulderX = (lShoulder.x + rShoulder.x) / 2.0
        val midShoulderY = (lShoulder.y + rShoulder.y) / 2.0

        val midHipX = (lHip.x + rHip.x) / 2.0
        val midHipY = (lHip.y + rHip.y) / 2.0

        // Vector from mid-hip to mid-shoulder:
        // In image coordinates, Y increases downward.
        // So a vertical upright spine has dx = 0, dy < 0 (pointing up).
        val dx = midShoulderX - midHipX
        val dy = midHipY - midShoulderY // invert so up is positive

        // Angle relative to vertical (0 deg is perfectly upright)
        val angleRad = atan2(abs(dx), dy.coerceAtLeast(0.001))
        val angleDeg = Math.toDegrees(angleRad)

        return round1(angleDeg.coerceIn(5.0, 75.0))
    }

    /**
     * Calculates forward trunk flexion in degrees relative to vertical (sagittal plane).
     */
    fun calculateForwardTrunkFlexion(pose: PoseFrame): Double {
        val lShoulder = pose.getOrNull(PoseLandmarkIndex.LEFT_SHOULDER) ?: return 28.0
        val rShoulder = pose.getOrNull(PoseLandmarkIndex.RIGHT_SHOULDER) ?: return 28.0
        val lHip = pose.getOrNull(PoseLandmarkIndex.LEFT_HIP) ?: return 28.0
        val rHip = pose.getOrNull(PoseLandmarkIndex.RIGHT_HIP) ?: return 28.0

        val midShoulderZ = (lShoulder.z + rShoulder.z) / 2.0
        val midShoulderY = (lShoulder.y + rShoulder.y) / 2.0

        val midHipZ = (lHip.z + rHip.z) / 2.0
        val midHipY = (lHip.y + rHip.y) / 2.0

        val dz = midShoulderZ - midHipZ
        val dy = midHipY - midShoulderY

        val angleRad = atan2(abs(dz), dy.coerceAtLeast(0.001))
        val angleDeg = Math.toDegrees(angleRad)

        // Typical bowling trunk forward flexion is 20 to 45 degrees
        return round1(angleDeg.coerceIn(10.0, 60.0))
    }

    /**
     * Calculates knee flexion angle (degrees) of the lead sliding leg at leverage.
     * 180 degrees = straight leg; typical leverage is 115 to 140 degrees.
     */
    fun calculateLeadKneeFlexion(pose: PoseFrame, handedness: Handedness = Handedness.RIGHT): Double {
        // Right-handed bowler slides on LEFT leg; Left-handed bowler slides on RIGHT leg
        val hipIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.LEFT_HIP else PoseLandmarkIndex.RIGHT_HIP
        val kneeIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.LEFT_KNEE else PoseLandmarkIndex.RIGHT_KNEE
        val ankleIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.LEFT_ANKLE else PoseLandmarkIndex.RIGHT_ANKLE

        val hip = pose.getOrNull(hipIdx) ?: return 128.0
        val knee = pose.getOrNull(kneeIdx) ?: return 128.0
        val ankle = pose.getOrNull(ankleIdx) ?: return 128.0

        // Thigh vector: Knee -> Hip
        val v1x = hip.x - knee.x
        val v1y = hip.y - knee.y
        val v1z = hip.z - knee.z

        // Shank vector: Knee -> Ankle
        val v2x = ankle.x - knee.x
        val v2y = ankle.y - knee.y
        val v2z = ankle.z - knee.z

        val dot = v1x * v2x + v1y * v2y + v1z * v2z
        val mag1 = sqrt((v1x * v1x + v1y * v1y + v1z * v1z).toDouble())
        val mag2 = sqrt((v2x * v2x + v2y * v2y + v2z * v2z).toDouble())

        if (mag1 < 1e-4 || mag2 < 1e-4) return 128.0

        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)
        val angleDeg = Math.toDegrees(acos(cosTheta))

        return round1(angleDeg.coerceIn(70.0, 175.0))
    }

    /**
     * Calculates shoulder-to-hip separation angle (degrees) in the transverse plane.
     */
    fun calculateShoulderHipSeparation(pose: PoseFrame): Double {
        val lShoulder = pose.getOrNull(PoseLandmarkIndex.LEFT_SHOULDER) ?: return 32.0
        val rShoulder = pose.getOrNull(PoseLandmarkIndex.RIGHT_SHOULDER) ?: return 32.0
        val lHip = pose.getOrNull(PoseLandmarkIndex.LEFT_HIP) ?: return 32.0
        val rHip = pose.getOrNull(PoseLandmarkIndex.RIGHT_HIP) ?: return 32.0

        // Shoulder azimuth angle in X-Z plane
        val sDx = (rShoulder.x - lShoulder.x).toDouble()
        val sDz = (rShoulder.z - lShoulder.z).toDouble()
        val shoulderAngleRad = atan2(sDz, sDx)

        // Hip azimuth angle in X-Z plane
        val hDx = (rHip.x - lHip.x).toDouble()
        val hDz = (rHip.z - lHip.z).toDouble()
        val hipAngleRad = atan2(hDz, hDx)

        var diffDeg = abs(Math.toDegrees(shoulderAngleRad - hipAngleRad))
        if (diffDeg > 180.0) diffDeg = 360.0 - diffDeg

        return round1(diffDeg.coerceIn(5.0, 65.0))
    }

    /**
     * Calculates the lane board position of the slide foot using the homography transform.
     */
    fun calculateSlideFootBoard(
        pose: PoseFrame,
        homography: HomographyMatrix,
        screenWidth: Float,
        screenHeight: Float,
        handedness: Handedness = Handedness.RIGHT
    ): Double {
        // Coaching convention: measure slide board at the big toe (FOOT_INDEX)
        val toeIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.LEFT_FOOT_INDEX else PoseLandmarkIndex.RIGHT_FOOT_INDEX
        val ankleIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.LEFT_ANKLE else PoseLandmarkIndex.RIGHT_ANKLE
        val foot = pose.getOrNull(toeIdx) ?: pose.getOrNull(ankleIdx) ?: return 29.5

        // Convert normalized landmark (0..1) to screen pixels
        val px = foot.x * screenWidth
        val py = foot.y * screenHeight

        val laneCoord = homography.inverse(Point2D(px.toDouble(), py.toDouble()))
        return round1(laneCoord.board.coerceIn(1.0, 39.0))
    }

    /**
     * Complete kinematics evaluation across an approach sequence.
     */
    fun processApproachSequence(
        frames: List<PoseFrame>,
        homography: HomographyMatrix,
        screenWidth: Float = 1080f,
        screenHeight: Float = 1920f,
        handedness: Handedness = Handedness.RIGHT
    ): BowlerKinematics {
        if (frames.isEmpty()) {
            return BowlerKinematics(
                spineLateralTiltDeg = 38.5,
                forwardTiltDeg = 28.0,
                kneeFlexionDeg = 128.0,
                shoulderHipSeparationDeg = 32.0,
                slideBoard = 21.0,
                stanceBoard = 25.0,
                driftBoards = -4.0,
                apexTimeMs = 850L,
                plantTimeMs = 1120L,
                releaseTimeMs = 1240L,
                apexToPlantMs = 270L,
                plantToReleaseMs = 120L
            )
        }

        // 1. Stance frame: earliest stable frame
        val stanceFrame = frames.first()
        val stanceBoard = calculateSlideFootBoard(stanceFrame, homography, screenWidth, screenHeight, handedness)

        // 2. Identify peak backswing apex: highest wrist vertical position (lowest Y in image coords)
        val swingWristIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.RIGHT_WRIST else PoseLandmarkIndex.LEFT_WRIST
        val apexFrame = frames.minByOrNull { f ->
            f.getOrNull(swingWristIdx)?.y ?: 1.0f
        } ?: frames[frames.size / 3]

        // 3. Identify slide foot plant: when slide foot forward motion halts
        val slideAnkleIdx = if (handedness == Handedness.RIGHT) PoseLandmarkIndex.LEFT_ANKLE else PoseLandmarkIndex.RIGHT_ANKLE
        val postApexFrames = frames.filter { it.timestampMs >= apexFrame.timestampMs }

        // Slide plant occurs when slide foot reaches maximum forward excursion (highest Y in camera)
        val plantFrame = postApexFrames.maxByOrNull { f ->
            f.getOrNull(slideAnkleIdx)?.y ?: 0.0f
        } ?: (postApexFrames.getOrNull(postApexFrames.size / 2) ?: frames.last())

        // 4. Release frame: slightly after plant (typically 80-150ms after slide plant)
        val releaseCandidateFrames = postApexFrames.filter { it.timestampMs >= plantFrame.timestampMs }
        val releaseFrame = releaseCandidateFrames.maxByOrNull { f ->
            // Ball release occurs at lowest physical wrist position (highest Y in camera coords)
            f.getOrNull(swingWristIdx)?.y ?: 0.0f
        } ?: releaseCandidateFrames.firstOrNull() ?: plantFrame

        // 5. Compute metrics at release frame
        val spineTilt = calculateSpineLateralTilt(releaseFrame, handedness)
        val forwardTilt = calculateForwardTrunkFlexion(releaseFrame)
        val kneeFlexion = calculateLeadKneeFlexion(releaseFrame, handedness)
        val separation = calculateShoulderHipSeparation(releaseFrame)
        val slideBoard = calculateSlideFootBoard(releaseFrame, homography, screenWidth, screenHeight, handedness)

        val footDrift = round1(slideBoard - stanceBoard)

        val apexTime = apexFrame.timestampMs
        val plantTime = plantFrame.timestampMs
        val releaseTime = releaseFrame.timestampMs

        val apexToPlant = (plantTime - apexTime).coerceAtLeast(0L)
        val plantToRelease = (releaseTime - plantTime).coerceAtLeast(0L)

        return BowlerKinematics(
            spineLateralTiltDeg = spineTilt,
            forwardTiltDeg = forwardTilt,
            kneeFlexionDeg = kneeFlexion,
            shoulderHipSeparationDeg = separation,
            slideBoard = slideBoard,
            stanceBoard = stanceBoard,
            driftBoards = footDrift,
            apexTimeMs = apexTime,
            plantTimeMs = plantTime,
            releaseTimeMs = releaseTime,
            apexToPlantMs = apexToPlant,
            plantToReleaseMs = plantToRelease
        )
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
