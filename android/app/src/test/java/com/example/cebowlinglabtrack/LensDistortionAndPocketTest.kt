package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.LensDistortionCorrector
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.tracking.PocketAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class LensDistortionAndPocketTest {

    @Test
    fun testLensDistortionOpticalCenterIsIdentity() {
        val cx = 540.0
        val cy = 960.0
        val focalLength = 1000.0
        val corrector = LensDistortionCorrector(cx, cy, focalLength, k1 = -0.08, k2 = 0.01)

        val center = Point2D(cx, cy)
        val undistorted = corrector.undistortPoint(center)

        assertEquals("Optical center should remain exactly identical", cx, undistorted.x, 1e-6)
        assertEquals("Optical center should remain exactly identical", cy, undistorted.y, 1e-6)
    }

    @Test
    fun testLensDistortionExpandsBarrelPeriphery() {
        val cx = 540.0
        val cy = 960.0
        val focalLength = 1000.0
        val corrector = LensDistortionCorrector(cx, cy, focalLength, k1 = -0.08, k2 = 0.01)

        // Point near corner (e.g. foul line gutter corner at 100, 1800)
        val corner = Point2D(100.0, 1800.0)
        val rOrig = sqrt((corner.x - cx) * (corner.x - cx) + (corner.y - cy) * (corner.y - cy))

        val undistorted = corrector.undistortPoint(corner)
        val rUndist = sqrt((undistorted.x - cx) * (undistorted.x - cx) + (undistorted.y - cy) * (undistorted.y - cy))

        // Negative k1 (barrel distortion) compresses the periphery; un-distorting expands it outward
        assertTrue("Barrel un-distortion must push peripheral points outward (rUndist > rOrig)", rUndist > rOrig)
    }

    @Test
    fun testLensDistortionCornersPreserveOrdering() {
        val corrector = LensDistortionCorrector(540.0, 960.0, 1080.0)

        val flL = Point2D(200.0, 1800.0)
        val flR = Point2D(880.0, 1800.0)
        val deckL = Point2D(400.0, 500.0)
        val deckR = Point2D(680.0, 500.0)

        val corners = corrector.undistortCalibrationCorners(flL, flR, deckL, deckR)
        assertEquals(4, corners.size)

        val uFlL = corners[0]
        val uFlR = corners[1]
        val uDeckL = corners[2]
        val uDeckR = corners[3]

        assertTrue("Left foul must be left of right foul", uFlL.x < uFlR.x)
        assertTrue("Deck must be above foul line", uDeckL.y < uFlL.y)
        assertTrue("Left deck must be left of right deck", uDeckL.x < uDeckR.x)
    }

    @Test
    fun testPocketAnalyzerOptimalStrikeProbability() {
        val analyzer = PocketAnalyzer()

        // Simulate ball entering pocket at board 17.5 with 6.0 deg entry angle over final 6 feet
        // Board 17.5 over 54 ft to 60 ft:
        // Angle 6.0 deg: deltaXInches = tan(6.0 deg) * 72 inches = 0.1051 * 72 = 7.567 inches
        // 7.567 inches / 1.0641 inches per board = 7.11 boards delta from 54 ft to 60 ft
        val p54 = Point2D(17.5 - 7.11, 54.0)
        val p57 = Point2D(17.5 - 3.55, 57.0)
        val p60 = Point2D(17.5, 60.0)

        val result = analyzer.evaluatePocketImpact(listOf(p54, p57, p60))
        assertNotNull("Pocket telemetry should be computed", result)
        assertEquals("Impact board should be 17.5", 17.5, result!!.impactBoard, 0.1)
        assertEquals("Entry angle should be ~6.0 deg", 6.0, result.entryAngleDegrees, 0.2)
        assertTrue("Peak strike probability should be ~95% (actual: ${result.strikeProbabilityPercent})",
            result.strikeProbabilityPercent in 92.0..96.0)
    }

    @Test
    fun testPocketAnalyzerOffTargetHitDecay() {
        val analyzer = PocketAnalyzer()

        // Ball missing pocket light (e.g. hit Board 14.5 with flat 2.0 deg angle)
        val p54 = Point2D(13.0, 54.0)
        val p57 = Point2D(13.8, 57.0)
        val p60 = Point2D(14.5, 60.0)

        val result = analyzer.evaluatePocketImpact(listOf(p54, p57, p60))
        assertNotNull(result)
        assertTrue("Strike probability should heavily decay for off-target/flat hit (actual: ${result!!.strikeProbabilityPercent})",
            result.strikeProbabilityPercent < 10.0)
    }

    @Test
    fun testPocketAnalyzerInsufficientPointsReturnsNull() {
        val analyzer = PocketAnalyzer()
        val result = analyzer.evaluatePocketImpact(listOf(Point2D(17.5, 59.0)))
        assertNull("Should return null if fewer than 3 trajectory points", result)
    }
}
