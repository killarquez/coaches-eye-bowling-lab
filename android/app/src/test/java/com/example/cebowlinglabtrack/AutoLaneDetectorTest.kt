package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.AutoLaneDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLaneDetectorTest {

    @Test
    fun testRejectNonBowlingRoomEnvironment() {
        val detector = AutoLaneDetector()
        val width = 800
        val height = 1200
        val stride = width
        val roomImageBytes = ByteArray(width * height)

        // Simulate non-bowling bedroom/office frame:
        // Moderately lit room wall/floor with diffuse illumination (lum ~ 130)
        // and minor texture noise without high-contrast pin deck or gutters
        for (i in roomImageBytes.indices) {
            roomImageBytes[i] = (120 + (i % 25)).toByte()
        }

        val result = detector.detectLaneFromFrame(roomImageBytes, width, height, stride)

        assertNotNull("Detection result should not be null", result)
        assertFalse("Auto-detection MUST FAIL in a bedroom/office without pins", result.isSuccess)
        assertFalse("Pin rack must NOT be detected in non-bowling environment", result.pinRackDetected)
        assertTrue("Status message should warn about no pin rack",
            result.statusMessage.contains("NO PIN RACK"))
    }

    @Test
    fun testAutoLaneDetectionWithFullPinRackAndGutters() {
        val detector = AutoLaneDetector()
        val width = 800
        val height = 1200
        val stride = width
        val imageBytes = createSyntheticLaneFrame(width, height, pinRackCenterX = 400.0, includePins = true)

        val result = detector.detectLaneFromFrame(imageBytes, width, height, stride)

        assertNotNull("Detection result should not be null", result)
        assertTrue("Detection should succeed on lane with full pin rack", result.isSuccess)
        assertTrue("Pin rack must be detected", result.pinRackDetected)
        assertTrue("Gutters must be detected", result.guttersDetected)
        assertTrue("Confidence should be high (> 0.8)", result.confidence >= 0.8)

        // Check geometry: foul line (bottom) should be wider than arrows (mid)
        val foulWidth = result.foulLineRight.x - result.foulLineLeft.x
        val arrowWidth = result.arrowsRight.x - result.arrowsLeft.x
        assertTrue("Foul line must be wider than arrows due to perspective", foulWidth > arrowWidth)

        // Check auto-center guidance
        assertEquals("🟢 LANE CENTERED", result.autoCenterGuidance)

        // Check that homography matrix elements are valid 3x3
        assertEquals(9, result.calibration.homographyMatrixElements.size)

        // Verify that forward-projected 15-ft arrows map back to physical 15.0 ft
        val homography = com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix(
            result.calibration.homographyMatrixElements.toDoubleArray()
        )
        val invArrowL = homography.inverse(result.arrowsLeft)
        val invArrowR = homography.inverse(result.arrowsRight)
        org.junit.Assert.assertEquals(15.0, invArrowL.distanceFt, 0.2)
        org.junit.Assert.assertEquals(15.0, invArrowR.distanceFt, 0.2)

        // Verify projectLaneToPixel round-trip for center arrow (Board 20, 15 ft)
        val centerArrowPixel = homography.projectLaneToPixel(20.0, 15.0)
        val invCenter = homography.inverse(centerArrowPixel)
        org.junit.Assert.assertEquals(20.0, invCenter.board, 0.05)
        org.junit.Assert.assertEquals(15.0, invCenter.distanceFt, 0.05)
    }

    @Test
    fun testAutoCenterGuidancePanRightAndPanLeft() {
        val detector = AutoLaneDetector()
        val width = 800
        val height = 1200
        val stride = width

        // Frame with pin rack shifted right (pins at X = 550, center is 400 -> coach should pan right)
        val imageShiftedRight = createSyntheticLaneFrame(width, height, pinRackCenterX = 550.0, includePins = true)
        val resultRight = detector.detectLaneFromFrame(imageShiftedRight, width, height, stride)
        assertTrue("Pin rack must be detected when shifted right", resultRight.pinRackDetected)
        assertTrue("Guidance should prompt panning right", resultRight.autoCenterGuidance?.contains("PAN RIGHT") == true)

        // Frame with pin rack shifted left (pins at X = 250, center is 400 -> coach should pan left)
        val imageShiftedLeft = createSyntheticLaneFrame(width, height, pinRackCenterX = 250.0, includePins = true)
        val resultLeft = detector.detectLaneFromFrame(imageShiftedLeft, width, height, stride)
        assertTrue("Pin rack must be detected when shifted left", resultLeft.pinRackDetected)
        assertTrue("Guidance should prompt panning left", resultLeft.autoCenterGuidance?.contains("PAN LEFT") == true)
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

    private fun createSyntheticLaneFrame(
        width: Int,
        height: Int,
        pinRackCenterX: Double = width / 2.0,
        includePins: Boolean = true
    ): ByteArray {
        val stride = width
        val imageBytes = ByteArray(width * height)

        val pinDeckY = (height * 0.32).toInt()
        val foulY = (height * 0.65).toInt()

        for (y in 0 until height) {
            val rowOffset = y * stride
            when {
                y < pinDeckY - 20 -> {
                    // Wall / curtain above pins
                    for (x in 0 until width) imageBytes[rowOffset + x] = 35.toByte()
                }
                y in (pinDeckY - 20)..pinDeckY -> {
                    // Pin deck pit region: dark cushion with white pin reflections
                    val halfW = 35.0
                    val leftEdge = (pinRackCenterX - halfW).toInt()
                    val rightEdge = (pinRackCenterX + halfW).toInt()
                    for (x in 0 until width) {
                        if (includePins && y in (pinDeckY - 14)..(pinDeckY - 2) && x in leftEdge..rightEdge) {
                            // 4 pin reflection peaks across width on illuminated pin deck
                            val relX = x - leftEdge
                            val isPinPeak = (relX in 8..13 || relX in 24..29 || relX in 40..45 || relX in 56..61)
                            imageBytes[rowOffset + x] = if (isPinPeak) 240.toByte() else 140.toByte()
                        } else {
                            imageBytes[rowOffset + x] = 35.toByte() // Dark pit cushion
                        }
                    }
                }
                y in (pinDeckY + 1)..foulY -> {
                    // Lane surface between gutters
                    val progress = (y - pinDeckY).toDouble() / (foulY - pinDeckY)
                    val laneHalfWidth = 35.0 + progress * 165.0 // Diverges from 35 to 200px
                    val leftGutter = (pinRackCenterX - laneHalfWidth).toInt()
                    val rightGutter = (pinRackCenterX + laneHalfWidth).toInt()

                    for (x in 0 until width) {
                        imageBytes[rowOffset + x] = if (x in leftGutter..rightGutter) 180.toByte() else 35.toByte()
                    }
                }
                else -> {
                    // Approach behind foul line
                    for (x in 0 until width) imageBytes[rowOffset + x] = 110.toByte()
                }
            }
        }
        return imageBytes
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
