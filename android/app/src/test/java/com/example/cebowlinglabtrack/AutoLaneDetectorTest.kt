package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.AutoLaneDetector
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLaneDetectorTest {

    @Test
    fun testAutoLaneDetectionWithSyntheticGutters() {
        val detector = AutoLaneDetector()
        val width = 800
        val height = 1200
        val stride = width
        val imageBytes = ByteArray(width * height)

        // Generate synthetic bowling lane:
        // - Dark gutters on edges (luminance ~ 30)
        // - Bright polished lane in center (luminance ~ 180)
        // Perspective convergence: lane is wider at bottom, narrower at top
        for (y in 0 until height) {
            val progress = y.toDouble() / height // 0 at top, 1 at bottom
            val laneHalfWidth = 100 + (progress * 220) // 100px at top, 320px at bottom
            val centerX = width / 2

            val leftGutterEdge = (centerX - laneHalfWidth).toInt()
            val rightGutterEdge = (centerX + laneHalfWidth).toInt()

            for (x in 0 until width) {
                val isLane = x in leftGutterEdge..rightGutterEdge
                imageBytes[y * stride + x] = if (isLane) 180.toByte() else 30.toByte()
            }
        }

        val result = detector.detectLaneFromFrame(imageBytes, width, height, stride)

        assertNotNull("Detection result should not be null", result)
        assertTrue("Detection should succeed on clear synthetic lane", result.isSuccess)
        assertTrue("Confidence should be high (> 0.8)", result.confidence >= 0.8)

        // Check geometry: foul line (bottom) should be wider than arrows (mid)
        val foulWidth = result.foulLineRight.x - result.foulLineLeft.x
        val arrowWidth = result.arrowsRight.x - result.arrowsLeft.x
        assertTrue("Foul line must be wider than arrows due to perspective", foulWidth > arrowWidth)

        // Check that homography matrix elements are valid 3x3
        assertEquals(9, result.calibration.homographyMatrixElements.size)
    }

    @Test
    fun testScaleForZoomPreservesOpticalCenter() {
        // Base homography with known center
        val baseH = com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
        val cx = 540.0
        val cy = 960.0
        val zoom = 2.5

        val scaledH = baseH.scaleForZoom(zoom, cx, cy)

        // Center point in source lane space (u=cx, v=cy)
        val centerPt = com.example.cebowlinglabtrack.domain.model.LanePoint(cx, cy)
        val projCenter = scaledH.forward(centerPt)

        // Optical center should be completely invariant under zoom
        org.junit.Assert.assertEquals(cx, projCenter.x, 0.001)
        org.junit.Assert.assertEquals(cy, projCenter.y, 0.001)

        // A point 100px away from center should be scaled by exactly 2.5x -> 250px away
        val offsetPt = com.example.cebowlinglabtrack.domain.model.LanePoint(cx + 100.0, cy + 100.0)
        val projOffset = scaledH.forward(offsetPt)
        org.junit.Assert.assertEquals(cx + 250.0, projOffset.x, 0.001)
        org.junit.Assert.assertEquals(cy + 250.0, projOffset.y, 0.001)
    }

    @Test
    fun testGutterEdgeCalibration() {
        val calibrator = com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator()

        // 4 points on the gutters
        val flL = com.example.cebowlinglabtrack.domain.model.Point2D(100.0, 1600.0) // Left gutter at 0 ft
        val flR = com.example.cebowlinglabtrack.domain.model.Point2D(980.0, 1600.0) // Right gutter at 0 ft
        val alL = com.example.cebowlinglabtrack.domain.model.Point2D(250.0, 900.0)  // Left gutter at 15 ft
        val arR = com.example.cebowlinglabtrack.domain.model.Point2D(830.0, 900.0)  // Right gutter at 15 ft

        val result = calibrator.calibrateGutters(flL, flR, alL, arR)
        assertNotNull("Gutter calibration must succeed", result)
        val (calib, homography) = result!!

        assertTrue("Reprojection RMSE should be < 1.0 px", calib.reprojectionErrorRmse < 1.0)

        // Verify inverse mapping:
        // Left gutter at foul line should be Board 1.0, 0.0 ft
        val invFoulL = homography.inverse(flL)
        org.junit.Assert.assertEquals(1.0, invFoulL.board, 0.1)
        org.junit.Assert.assertEquals(0.0, invFoulL.distanceFt, 0.1)

        // Right gutter at foul line should be Board 39.0, 0.0 ft
        val invFoulR = homography.inverse(flR)
        org.junit.Assert.assertEquals(39.0, invFoulR.board, 0.1)
        org.junit.Assert.assertEquals(0.0, invFoulR.distanceFt, 0.1)

        // Left gutter at 15ft should be Board 1.0, 15.0 ft
        val invArrowsL = homography.inverse(alL)
        org.junit.Assert.assertEquals(1.0, invArrowsL.board, 0.1)
        org.junit.Assert.assertEquals(15.0, invArrowsL.distanceFt, 0.1)

        // Right gutter at 15ft should be Board 39.0, 15.0 ft
        val invArrowsR = homography.inverse(arR)
        org.junit.Assert.assertEquals(39.0, invArrowsR.board, 0.1)
        org.junit.Assert.assertEquals(15.0, invArrowsR.distanceFt, 0.1)
    }

    @Test
    fun testCreateDefaultCalibrationAtZoom() {
        val calibrator = com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator()

        val pair1x = calibrator.createDefaultCalibration(1080f, 1920f, zoomRatio = 1.0f)
        val pair25x = calibrator.createDefaultCalibration(1080f, 1920f, zoomRatio = 2.5f)

        val calib1x = pair1x.first
        val calib25x = pair25x.first

        // At 2.5x zoom, the foul line should be positioned lower down the screen than at 1.0x (to frame the lane)
        assertTrue("Foul line Y at 2.5x should be lower than at 1.0x",
            calib25x.foulLineLeftScreen.y > calib1x.foulLineLeftScreen.y)

        // Lane width at foul line should be wider at 2.5x (filling more of the screen)
        val width1x = calib1x.foulLineRightScreen.x - calib1x.foulLineLeftScreen.x
        val width25x = calib25x.foulLineRightScreen.x - calib25x.foulLineLeftScreen.x
        assertTrue("Lane width at foul line should be wider when zoomed in", width25x > width1x)
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
