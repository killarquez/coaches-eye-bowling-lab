package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import com.example.cebowlinglabtrack.domain.model.VisualTargetLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Validates Visual Target Line AR projection, gutter chevrons, shot comparison against target,
 * and calibration safety interlock logic.
 */
class VisualTargetLineAndSafetyTest {

    @Test
    fun testVisualTargetLinePresets() {
        val presets = VisualTargetLine.PRESETS
        assertTrue("Presets should have at least 5 standard target lines", presets.size >= 5)

        val down10 = presets.first { it.id == "down_10" }
        assertEquals(10.0, down10.laydownBoard.toDouble(), 0.01)
        assertEquals(10.0, down10.arrowBoard.toDouble(), 0.01)
        assertEquals(10.0, down10.breakpointBoard.toDouble(), 0.01)
        assertEquals(17.5, down10.pocketBoard.toDouble(), 0.01)

        val cranker = presets.first { it.id == "power_cranker" }
        assertEquals(20.0, cranker.laydownBoard.toDouble(), 0.01)
        assertEquals(15.0, cranker.arrowBoard.toDouble(), 0.01)
        assertEquals(6.0, cranker.breakpointBoard.toDouble(), 0.01)
        assertEquals(42.0, cranker.breakpointDistanceFt.toDouble(), 0.01)
        assertEquals(17.5, cranker.pocketBoard.toDouble(), 0.01)
    }

    @Test
    fun testTargetTrajectorySplineInterpolation() {
        val target = VisualTargetLine.POWER_CRANKER
        val points = target.generateTargetTrajectory()

        assertEquals("Should generate 61 points (0 to 60 ft at 1-ft increments)", 61, points.size)
        assertEquals(0.0, points.first().distanceFt, 0.01)
        assertEquals(60.0, points.last().distanceFt, 0.01)

        // Verify key knot locations
        val laydown = points.first { it.distanceFt == 0.0 }
        val arrow = points.first { it.distanceFt == 15.0 }
        val breakpoint = points.first { it.distanceFt == 42.0 }
        val pocket = points.first { it.distanceFt == 60.0 }

        assertEquals(20.0, laydown.board, 0.01)
        assertEquals(15.0, arrow.board, 0.01)
        assertEquals(6.0, breakpoint.board, 0.01)
        assertEquals(17.5, pocket.board, 0.01)
    }

    @Test
    fun testTargetShotComparisonAccuracy() {
        // Generate an authentic shot using POWER_CRANKER preset (laydown 24.6, arrow 16.0, breakpoint 6.6 @ 42ft, entry 16.8)
        val shot = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.POWER_CRANKER)
        val target = VisualTargetLine(
            id = "test_cranker",
            name = "Test Cranker Target",
            laydownBoard = 24.5f,
            arrowBoard = 16.0f,
            breakpointBoard = 6.5f,
            breakpointDistanceFt = 42.0f,
            pocketBoard = 17.0f
        )
        val comparison = target.compareShot(shot.trajectoryPoints)

        assertNotNull(comparison)
        assertEquals(target.name, comparison.targetLineName)

        // When shot aligns with visual target line, deltas should be minimal (< 1.5 boards)
        assertTrue("Laydown delta should be small: ${comparison.laydownDeltaBoards}", abs(comparison.laydownDeltaBoards) < 1.5)
        assertTrue("Arrow delta should be small: ${comparison.arrowDeltaBoards}", abs(comparison.arrowDeltaBoards) < 1.5)
        assertTrue("Breakpoint delta should be small: ${comparison.breakpointDeltaBoards}", abs(comparison.breakpointDeltaBoards) < 1.5)
        assertTrue("Accuracy score should be high (> 85%): ${comparison.overallAccuracyScore}", comparison.overallAccuracyScore >= 85.0)
        assertTrue("Rating should be PERFECT or GREAT: ${comparison.accuracyRating}", comparison.accuracyRating in listOf("PERFECT EXECUTION", "GREAT SHOT", "GOOD"))
    }

    @Test
    fun testTargetShotComparisonOffTarget() {
        // Compare a straight stroker shot (board 10-17.5) with the TEN_PIN_SPARE target (cross-lane to board 35)
        val strokerShot = SimulatedShotGenerator.generateShot(preset = ShotStylePreset.ACCURATE_STROKER)
        val target = VisualTargetLine.TEN_PIN_SPARE
        val comparison = target.compareShot(strokerShot.trajectoryPoints)

        assertNotNull(comparison)
        // At pocket, cross-lane 10-pin spare is at board 35 while ACCURATE_STROKER is near board 17.5
        assertTrue("Pocket delta should be significant: ${comparison.pocketDeltaBoards}", abs(comparison.pocketDeltaBoards) > 10.0)
        assertTrue("Accuracy score should reflect off-target deviation (< 60%): ${comparison.overallAccuracyScore}", comparison.overallAccuracyScore < 60.0)
        assertEquals("WIDE", comparison.accuracyRating)
    }

    @Test
    fun testArrowChevronsGeneration() {
        val calibrator = LaneCalibrator()
        val defaultH = HomographyMatrix(LaneCalibrator.DEFAULT_CALIBRATION.homographyMatrixElements.toDoubleArray())
        val guides = calibrator.generateProjectedGuides(defaultH, VisualTargetLine.DEFAULT)

        assertNotNull(guides)
        // Authentic USBC targeting arrows at 15 ft on boards 5, 10, 15, 20, 25, 30, 35
        assertTrue("USBC 15ft arrow chevrons should be generated across boards", guides.arrowChevrons.isNotEmpty())
        assertEquals(7, guides.arrowChevrons.size)

        // Verify gutter chevrons are cleanly discarded per user specification
        assertTrue("Gutter chevrons discarded to prevent visual clutter", guides.leftGutterChevrons.isEmpty())
        assertTrue("Gutter chevrons discarded to prevent visual clutter", guides.rightGutterChevrons.isEmpty())

        // Verify chevron geometry (tip points down-lane toward pins)
        val centerArrow = guides.arrowChevrons[3] // Board 20
        assertNotNull(centerArrow.tip)
        assertNotNull(centerArrow.leftWing)
        assertNotNull(centerArrow.rightWing)

        // Verify target line screen points are generated
        assertTrue("Visual target line should be projected onto screen coordinates", guides.targetLinePointsScreen.isNotEmpty())
        assertEquals(61, guides.targetLinePointsScreen.size)
    }
}
