package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.kinematics.KinematicsCalculator
import com.example.cebowlinglabtrack.domain.kinematics.PoseFrame
import com.example.cebowlinglabtrack.domain.kinematics.PoseLandmark
import com.example.cebowlinglabtrack.domain.kinematics.PoseLandmarkIndex
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import com.example.cebowlinglabtrack.domain.model.Handedness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KinematicsCalculatorTest {

    @Test
    fun testSpineLateralTiltCalculation() {
        // Construct a synthetic pose with ~38 degree lateral tilt
        // Mid-hip at (0.5, 0.6), Mid-shoulder at (0.56, 0.4)
        // dx = 0.06, dy = 0.20 -> angle = atan(0.06 / 0.20) ~ 16.7 deg
        // Or dx = 0.12, dy = 0.15 -> atan(0.12 / 0.15) = atan(0.8) ~ 38.6 deg
        val landmarks = ArrayList<PoseLandmark>(33)
        for (i in 0..32) landmarks.add(PoseLandmark(0.5f, 0.5f))

        // Hips
        landmarks[PoseLandmarkIndex.LEFT_HIP] = PoseLandmark(0.44f, 0.60f)
        landmarks[PoseLandmarkIndex.RIGHT_HIP] = PoseLandmark(0.56f, 0.60f)

        // Shoulders tilted to the right
        landmarks[PoseLandmarkIndex.LEFT_SHOULDER] = PoseLandmark(0.54f, 0.45f)
        landmarks[PoseLandmarkIndex.RIGHT_SHOULDER] = PoseLandmark(0.70f, 0.45f)

        val frame = PoseFrame(timestampMs = 1000L, landmarks = landmarks)
        val tilt = KinematicsCalculator.calculateSpineLateralTilt(frame, Handedness.RIGHT)

        assertTrue("Spine lateral tilt should be ~38 deg, was: $tilt", tilt in 35.0..42.0)
    }

    @Test
    fun testLeadKneeFlexionCalculation() {
        // Construct lead leg with known angle (e.g. 128 degrees)
        val landmarks = ArrayList<PoseLandmark>(33)
        for (i in 0..32) landmarks.add(PoseLandmark(0.5f, 0.5f))

        // Left leg (lead slide leg for right-hander):
        // Knee at (0.5, 0.6, 0.0)
        // Hip at (0.5, 0.4, 0.0) -> Thigh vector: (0, -0.2, 0)
        // Ankle at (0.5 + 0.15, 0.75, 0.0) -> Shank vector: (0.15, 0.15, 0)
        landmarks[PoseLandmarkIndex.LEFT_HIP] = PoseLandmark(0.50f, 0.40f, 0f)
        landmarks[PoseLandmarkIndex.LEFT_KNEE] = PoseLandmark(0.50f, 0.60f, 0f)
        landmarks[PoseLandmarkIndex.LEFT_ANKLE] = PoseLandmark(0.65f, 0.75f, 0f)

        val frame = PoseFrame(timestampMs = 1000L, landmarks = landmarks)
        val kneeAngle = KinematicsCalculator.calculateLeadKneeFlexion(frame, Handedness.RIGHT)

        assertTrue("Knee flexion should be in leverage range (115-145 deg), was: $kneeAngle",
            kneeAngle in 115.0..145.0)
    }

    @Test
    fun testApproachSequenceProcessing() {
        val poses = SimulatedShotGenerator.generateApproachPoseSequence(
            totalDurationMs = 1500L,
            fps = 30,
            handedness = Handedness.RIGHT
        )
        val (_, homography) = LaneCalibrator().createDefaultCalibration(1080f, 1920f)

        val kinematics = KinematicsCalculator.processApproachSequence(
            frames = poses,
            homography = homography,
            handedness = Handedness.RIGHT
        )

        assertNotNull(kinematics)
        assertTrue("Spine tilt in range", kinematics.spineTiltReleaseDeg in 25.0..50.0)
        assertTrue("Slide knee flexion in range, was: ${kinematics.slideKneeFlexionDeg}", kinematics.slideKneeFlexionDeg in 95.0..145.0)
        assertTrue("Shoulder-hip separation in range", kinematics.shoulderHipSeparationDeg in 15.0..50.0)
        assertTrue("Apex timestamp before plant timestamp", kinematics.apexTimeMs <= kinematics.plantTimeMs)
        assertTrue("Plant timestamp before or equal to release timestamp", kinematics.plantTimeMs <= kinematics.releaseTimeMs)
        assertTrue("Apex-to-plant interval > 0", kinematics.apexToPlantMs >= 0)
    }
}
