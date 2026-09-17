package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.model.LanePoint
import com.example.cebowlinglabtrack.domain.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HomographyTest {

    @Test
    fun testDLTSolverAndRoundtripProjection() {
        val calibrator = LaneCalibrator()

        // Realistic camera screen coordinates for 1080x1920 portrait view
        val foulLeft = Point2D(120.0, 1680.0)    // Board 1, Y = 0 ft
        val foulRight = Point2D(960.0, 1680.0)   // Board 39, Y = 0 ft
        val arrowsLeft = Point2D(310.0, 980.0)   // Board 5, Y = 15 ft
        val arrowsRight = Point2D(770.0, 980.0)  // Board 35, Y = 15 ft

        val result = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)
        assertNotNull("Homography calibration should succeed", result)

        val (calibration, homography) = result!!

        // Verify RMSE is sub-pixel (< 1.0 px)
        assertTrue("RMSE should be < 1.0 px, was: ${calibration.reprojectionErrorRmse}",
            calibration.reprojectionErrorRmse < 1.0)

        // Test forward projection of anchor points (USBC: Board 39 Left, Board 1 Right)
        val projFoulLeft = homography.forward(LanePoint(39.0, 0.0))
        assertEquals(foulLeft.x, projFoulLeft.x, 0.5)
        assertEquals(foulLeft.y, projFoulLeft.y, 0.5)

        val projFoulRight = homography.forward(LanePoint(1.0, 0.0))
        assertEquals(foulRight.x, projFoulRight.x, 0.5)
        assertEquals(foulRight.y, projFoulRight.y, 0.5)

        // Test inverse projection (screen pixel -> lane coordinates)
        val invFoulLeft = homography.inverse(foulLeft)
        assertEquals(39.0, invFoulLeft.board, 0.1)
        assertEquals(0.0, invFoulLeft.distanceFt, 0.1)

        val invFoulRight = homography.inverse(foulRight)
        assertEquals(1.0, invFoulRight.board, 0.1)
        assertEquals(0.0, invFoulRight.distanceFt, 0.1)

        val invArrowsLeft = homography.inverse(arrowsLeft)
        assertEquals(35.0, invArrowsLeft.board, 0.1)
        assertEquals(15.0, invArrowsLeft.distanceFt, 0.1)

        val invArrowsRight = homography.inverse(arrowsRight)
        assertEquals(5.0, invArrowsRight.board, 0.1)
        assertEquals(15.0, invArrowsRight.distanceFt, 0.1)
    }

    @Test
    fun testArbitraryPointRoundtrip() {
        val calibrator = LaneCalibrator()
        val (_, homography) = calibrator.createDefaultCalibration(1080f, 1920f)

        // Test several internal points on the lane
        val testPoints = listOf(
            LanePoint(15.0, 10.0),
            LanePoint(20.0, 30.0),
            LanePoint(8.0, 42.0),
            LanePoint(17.5, 60.0)
        )

        for (pt in testPoints) {
            val screenPt = homography.forward(pt)
            val recovered = homography.inverse(screenPt)

            assertEquals("Board roundtrip mismatch for $pt", pt.board, recovered.board, 0.05)
            assertEquals("Distance roundtrip mismatch for $pt", pt.distanceFt, recovered.distanceFt, 0.05)
        }
    }
}
