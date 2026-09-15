package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.tracking.TelemetryExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the full 22-parameter Specto telemetry model against authentic data
 * from the reference session report (DOC-20260119-WA0006.pdf).
 */
class SpectoTelemetryTest {

    @Test
    fun testAll22ParametersShot1AuthenticPdf() {
        val shot = SimulatedShotGenerator.generateShot(
            preset = ShotStylePreset.SPECTO_SHOT_1,
            sessionId = "session_20260119_alfredo"
        )
        val telemetry = shot.spectoTelemetry

        // --- Category A: Spatial Metrics (Boards & Longitudinal Distances) ---
        // 1. Laydown Board: 17.3
        assertEquals("Laydown board must be 17.3", 17.3, telemetry.spatial.laydownBoard, 0.05)
        // 2. Loft Distance: 6.2 ft
        assertEquals("Loft distance must be 6.2 ft", 6.2, telemetry.spatial.loftDistanceFt, 0.05)
        // 3. Arrow Board: 8.8
        assertEquals("Arrow board must be 8.8", 8.8, telemetry.spatial.arrowBoard, 0.05)
        // 4. Pattern Exit Board: 1.9 (at 40 ft)
        assertEquals("Pattern exit board must be 1.9", 1.9, telemetry.spatial.patternExitBoard, 0.05)
        // 5. Breakpoint Board: 1.7
        assertEquals("Breakpoint board must be 1.7", 1.7, telemetry.spatial.breakpointBoard, 0.05)
        // 6. Breakpoint Distance: 37.0 ft
        assertEquals("Breakpoint distance must be 37.0 ft", 37.0, telemetry.spatial.breakpointDistanceFt, 0.05)
        // 7. Entry Board: 17.9 (at 60 ft)
        assertEquals("Entry board must be 17.9", 17.9, telemetry.spatial.entryBoard, 0.05)
        // 8. Pin Deck Exit Board: 23.3 (at 62.8 ft)
        assertEquals("Pin deck exit board must be 23.3", 23.3, telemetry.spatial.pinDeckExitBoard, 0.05)
        // 9. Pin Deck Deflection: 5.4 boards (23.3 - 17.9)
        assertEquals("Deflection must be ~5.3-5.4 boards", 5.4, telemetry.spatial.pinDeckDeflection, 0.15)

        // --- Category B: Angular Vectors ---
        // 10. Launch Angle: -2.9 deg (negative playing out to right gutter)
        assertEquals("Launch angle must be -2.9°", -2.9, telemetry.angles.launchAngleDeg, 0.05)
        // 11. Breakpoint Angle: 8.5 deg
        assertEquals("Breakpoint angle must be 8.5°", 8.5, telemetry.angles.breakpointAngleDeg, 0.05)
        // 12. Impact Angle: 5.6 deg
        assertEquals("Impact angle must be 5.6°", 5.6, telemetry.angles.impactAngleDeg, 0.05)

        // --- Category C: Speed & Velocity Decay ---
        // 13. Launch Speed: 17.5 mph
        assertEquals("Launch speed must be 17.5 mph", 17.5, telemetry.speed.launchSpeedMph, 0.05)
        // 14. Entry Speed: 14.3 mph
        assertEquals("Entry speed must be 14.3 mph", 14.3, telemetry.speed.entrySpeedMph, 0.05)
        // 15. Speed Loss: 3.2 mph (17.5 - 14.3)
        assertEquals("Speed loss must be 3.2 mph", 3.2, telemetry.speed.speedLossMph, 0.05)
        // 16. Average Speed: 16.2 mph
        assertEquals("Average speed must be 16.2 mph", 16.2, telemetry.speed.avgSpeedMph, 0.05)

        // --- Category D: Motion Phases & Dynamics ---
        // 17. RPM: 391
        assertEquals("RPM must be 391", 391, telemetry.dynamics.rpm)
        // 18. Skid Distance: 23.0 ft
        assertEquals("Skid distance must be 23.0 ft", 23.0, telemetry.dynamics.skidFt, 0.05)
        // 19. Hook Distance: 31.0 ft
        assertEquals("Hook distance must be 31.0 ft", 31.0, telemetry.dynamics.hookFt, 0.05)
        // 20. Roll Distance: 6.0 ft
        assertEquals("Roll distance must be 6.0 ft", 6.0, telemetry.dynamics.rollFt, 0.05)
        // 21. Power Score: 17.5 * 0.391 = 6.84
        assertEquals("Power score must be 6.84", 6.84, telemetry.dynamics.powerScore, 0.05)
        // 22. Accuracy Score & Skill Tier
        assertEquals("Accuracy score must be 96.2", 96.2, telemetry.dynamics.accuracyScore, 0.1)
        assertEquals("Skill tier must be PRO L1", "PRO L1", telemetry.dynamics.skillTier)
    }

    @Test
    fun testExtractionFromTrajectoryMatchesDirectCalculations() {
        val shot = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.SPECTO_SHOT_1)
        val extracted = TelemetryExtractor.extractSpectoTelemetry(
            trajectory = shot.trajectoryPoints,
            measuredRpm = 391
        )

        // Verify that extraction from the generated 60-sample trajectory recovers the 22 parameters
        assertEquals(17.3, extracted.spatial.laydownBoard, 0.5)
        assertEquals(8.8, extracted.spatial.arrowBoard, 0.5)
        assertEquals(1.7, extracted.spatial.breakpointBoard, 0.5)
        assertEquals(37.0, extracted.spatial.breakpointDistanceFt, 2.0)
        assertEquals(17.9, extracted.spatial.entryBoard, 0.5)

        // Angular vector directions
        assertTrue("Launch angle must be negative (playing right)", extracted.angles.launchAngleDeg < 0)
        assertTrue("Breakpoint angle must be positive directional change", extracted.angles.breakpointAngleDeg > 0)
        assertTrue("Impact angle in standard pocket range", extracted.angles.impactAngleDeg in 4.0..7.0)

        // Speed decay: entry speed < launch speed
        assertTrue(extracted.speed.entrySpeedMph < extracted.speed.launchSpeedMph)
        assertEquals(extracted.speed.launchSpeedMph - extracted.speed.entrySpeedMph, extracted.speed.speedLossMph, 0.1)

        // Power score formula
        val expectedPower = Math.round(extracted.speed.launchSpeedMph * (391 / 1000.0) * 100.0) / 100.0
        assertEquals(expectedPower, extracted.dynamics.powerScore, 0.01)
    }
}
