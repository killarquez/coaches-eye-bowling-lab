package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import com.example.cebowlinglabtrack.domain.tracking.TelemetryExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryExtractorTest {

    @Test
    fun testExtractionFromKnownTrajectory() {
        val shot = SimulatedShotGenerator.generateShot(
            preset = ShotStylePreset.POWER_CRANKER
        )

        val extracted = TelemetryExtractor.extractMetrics(shot.trajectoryPoints)

        // Validate Laydown Board ~ 24.6 (Session average cranker track)
        assertEquals(24.6, extracted.laydownBoard, 0.5)

        // Validate Arrow Board ~ 16.0
        assertEquals(16.0, extracted.arrowBoard, 0.5)

        // Validate Breakpoint Board ~ 6.6 at distance ~ 42 ft
        assertEquals(6.6, extracted.breakpointBoard, 0.5)
        assertTrue("Breakpoint distance should be in 38-44 ft range",
            extracted.breakpointDistanceFt in 38.0..44.0)

        // Validate Launch Speed vs Deck Speed
        assertTrue("Launch speed should be ~17.5-18.5 mph", extracted.launchSpeedMph in 17.0..19.0)
        assertTrue("Deck speed should be ~14.0-16.0 mph", extracted.deckSpeedMph in 13.5..16.5)
        assertTrue("Deck speed should be slower than launch speed due to friction",
            extracted.deckSpeedMph < extracted.launchSpeedMph)

        // Validate Entry Angle ~ 4.5 - 6.0 deg
        assertTrue("Entry angle should be in 4.0-6.2 deg range", extracted.entryAngleDeg in 4.0..6.5)
    }

    @Test
    fun testInterpolationAtExactAndMidPoints() {
        val shot = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.SMOOTH_TWEENER)
        val pts = shot.trajectoryPoints

        val b0 = TelemetryExtractor.interpolateBoardAtDistance(pts, 0.0)
        assertEquals(15.0, b0, 0.5)

        val b15 = TelemetryExtractor.interpolateBoardAtDistance(pts, 15.0)
        assertEquals(11.0, b15, 0.5)
    }
}
