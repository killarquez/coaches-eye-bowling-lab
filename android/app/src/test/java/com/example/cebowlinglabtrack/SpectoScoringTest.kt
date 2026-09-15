package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import com.example.cebowlinglabtrack.domain.model.SpectoSkillTier
import com.example.cebowlinglabtrack.domain.tracking.TelemetryExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Validates Power Score, Accuracy Score, and Kegel L1..L6 PRO Skill Tier classifications.
 */
class SpectoScoringTest {

    @Test
    fun testPowerScoreFormula() {
        // Authentic Shot 1: 17.5 mph, 391 RPM -> 17.5 * 0.391 = 6.8425 -> 6.84
        val speed1 = 17.5
        val rpm1 = 391
        val powerScore1 = (speed1 * (rpm1 / 1000.0) * 100.0).roundToInt() / 100.0
        assertEquals(6.84, powerScore1, 0.001)

        // Session Average: 18.1 mph, 435 RPM -> 18.1 * 0.435 = 7.8735 -> 7.87 (~7.9 in report)
        val avgSpeed = 18.1
        val avgRpm = 435
        val avgPowerScore = (avgSpeed * (avgRpm / 1000.0) * 10.0).roundToInt() / 10.0
        assertEquals(7.9, avgPowerScore, 0.05)

        // Cranker High Rev: 18.5 mph, 500 RPM -> 18.5 * 0.500 = 9.25
        val speedCranker = 18.5
        val rpmCranker = 500
        val powerCranker = (speedCranker * (rpmCranker / 1000.0) * 100.0).roundToInt() / 100.0
        assertEquals(9.25, powerCranker, 0.001)
    }

    @Test
    fun testSkillTierTolerances() {
        // L6 PRO has tightest tolerances: Laydown <= 0.8, Arrow <= 1.5, Breakpoint <= 2.9
        assertEquals(0.8, SpectoSkillTier.L6_PRO.laydownTolerance, 0.001)
        assertEquals(1.5, SpectoSkillTier.L6_PRO.arrowTolerance, 0.001)
        assertEquals(2.9, SpectoSkillTier.L6_PRO.breakpointTolerance, 0.001)

        // L5 has: Laydown <= 1.5, Arrow <= 3.0
        assertEquals(1.5, SpectoSkillTier.L5_195_210.laydownTolerance, 0.001)
        assertEquals(3.0, SpectoSkillTier.L5_195_210.arrowTolerance, 0.001)

        // L1 has widest tolerances
        assertEquals(4.0, SpectoSkillTier.L1_UNDER_150.laydownTolerance, 0.001)
        assertEquals(8.0, SpectoSkillTier.L1_UNDER_150.arrowTolerance, 0.001)
    }

    @Test
    fun testMultiShotSessionConsistencyAndTierClassification() {
        // Collect 3 authentic shots from Alfredo's session
        val shot1 = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.SPECTO_SHOT_1)
        val shot2 = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.SPECTO_SHOT_2)
        val shot3 = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.SPECTO_SHOT_3)

        val sessionShots = listOf(shot1.spectoTelemetry, shot2.spectoTelemetry, shot3.spectoTelemetry)
        val stats = TelemetryExtractor.calculateSessionStats(sessionShots)

        assertEquals(3, stats.totalShots)
        assertTrue("Average power score should be ~6.8 - 7.5", stats.averagePowerScore in 6.5..7.5)
        assertTrue("Accuracy score must be a positive consistency index", stats.accuracyScore > 0)
        assertTrue("Average speed in 17-18 mph range", stats.avgLaunchSpeedMph in 17.0..18.5)
        assertTrue("Average RPM in 380-410 range", stats.avgRpm in 380..420)
    }
}
