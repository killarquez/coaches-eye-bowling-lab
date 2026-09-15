package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.LanePoint
import com.example.cebowlinglabtrack.domain.model.Point2D
import java.util.UUID

/**
 * Lane guide line geometry projected onto screen pixel coordinates.
 */
data class ProjectedLaneGuides(
    val foulLine: Pair<Point2D, Point2D>,
    val leftGutterLine: List<Point2D>,
    val rightGutterLine: List<Point2D>,
    val centerline: List<Point2D>,
    val arrowsLine: Pair<Point2D, Point2D>,
    val arrowPoints: List<Point2D>,
    val headpinPoint: Point2D,
    val pinDeckLine: Pair<Point2D, Point2D>
)

/**
 * Manages lane calibration, homography computation, and AR guide projections.
 */
class LaneCalibrator {

    /**
     * Calibrates the perspective transform using 4 known anchor points.
     *
     * Standard calibration points:
     * 1. Foul line left (gutter edge): Board 1.0, 0.0 ft
     * 2. Foul line right (gutter edge): Board 39.0, 0.0 ft
     * 3. Arrows left (Board 5 arrow or gutter at 15 ft): Board 5.0, 15.0 ft
     * 4. Arrows right (Board 35 arrow or gutter at 15 ft): Board 35.0, 15.0 ft
     */
    fun calibrate(
        foulLineLeft: Point2D,
        foulLineRight: Point2D,
        arrowsLeft: Point2D,
        arrowsRight: Point2D,
        laneName: String = "Lane 1"
    ): Pair<LaneCalibration, HomographyMatrix>? {
        val srcPoints = listOf(
            LanePoint(1.0, 0.0),                     // Foul line left
            LanePoint(LaneConstants.TOTAL_BOARDS.toDouble(), 0.0), // Foul line right
            LanePoint(5.0, LaneConstants.ARROWS_DISTANCE_FT),     // Arrows left
            LanePoint(35.0, LaneConstants.ARROWS_DISTANCE_FT)     // Arrows right
        )

        val dstPoints = listOf(
            foulLineLeft,
            foulLineRight,
            arrowsLeft,
            arrowsRight
        )

        val homography = HomographyMatrix.computeDLT(srcPoints, dstPoints) ?: return null
        val rmse = HomographyMatrix.calculateRmse(homography, srcPoints, dstPoints)

        val calibration = LaneCalibration(
            id = UUID.randomUUID().toString(),
            laneName = laneName,
            foulLineLeftScreen = foulLineLeft,
            foulLineRightScreen = foulLineRight,
            arrowsLeftScreen = arrowsLeft,
            arrowsRightScreen = arrowsRight,
            homographyMatrixElements = homography.elements,
            reprojectionErrorRmse = rmse
        )

        return Pair(calibration, homography)
    }

    /**
     * Generates standard lane AR guide lines projected into camera screen coordinates.
     */
    fun generateProjectedGuides(homography: HomographyMatrix): ProjectedLaneGuides {
        // Foul Line
        val foulLeft = homography.forward(LanePoint(1.0, 0.0))
        val foulRight = homography.forward(LanePoint(39.0, 0.0))

        // Left Gutter (Board 1 from 0 to 60 ft)
        val leftGutter = listOf(
            homography.forward(LanePoint(1.0, 0.0)),
            homography.forward(LanePoint(1.0, 15.0)),
            homography.forward(LanePoint(1.0, 30.0)),
            homography.forward(LanePoint(1.0, 45.0)),
            homography.forward(LanePoint(1.0, 60.0))
        )

        // Right Gutter (Board 39 from 0 to 60 ft)
        val rightGutter = listOf(
            homography.forward(LanePoint(39.0, 0.0)),
            homography.forward(LanePoint(39.0, 15.0)),
            homography.forward(LanePoint(39.0, 30.0)),
            homography.forward(LanePoint(39.0, 45.0)),
            homography.forward(LanePoint(39.0, 60.0))
        )

        // Centerline (Board 20 from 0 to 60 ft)
        val center = listOf(
            homography.forward(LanePoint(20.0, 0.0)),
            homography.forward(LanePoint(20.0, 15.0)),
            homography.forward(LanePoint(20.0, 30.0)),
            homography.forward(LanePoint(20.0, 45.0)),
            homography.forward(LanePoint(20.0, 60.0))
        )

        // 7 Targeting Arrows at 15 ft (Boards 5, 10, 15, 20, 25, 30, 35)
        val arrowBoards = listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0)
        val arrowPoints = arrowBoards.map { b ->
            homography.forward(LanePoint(b, LaneConstants.ARROWS_DISTANCE_FT))
        }

        val arrowsLine = Pair(
            homography.forward(LanePoint(1.0, LaneConstants.ARROWS_DISTANCE_FT)),
            homography.forward(LanePoint(39.0, LaneConstants.ARROWS_DISTANCE_FT))
        )

        // Headpin (Board 20, 60 ft)
        val headpin = homography.forward(LanePoint(20.0, LaneConstants.FOUL_LINE_TO_HEADPIN_FT))

        // Pin Deck baseline
        val pinDeckLine = Pair(
            homography.forward(LanePoint(1.0, 60.0)),
            homography.forward(LanePoint(39.0, 60.0))
        )

        return ProjectedLaneGuides(
            foulLine = Pair(foulLeft, foulRight),
            leftGutterLine = leftGutter,
            rightGutterLine = rightGutter,
            centerline = center,
            arrowsLine = arrowsLine,
            arrowPoints = arrowPoints,
            headpinPoint = headpin,
            pinDeckLine = pinDeckLine
        )
    }

    /**
     * Provides default screen anchor coordinates for a standard 1080x1920 or 1080x2400 viewfinder
     * with tripod mounted behind foul line.
     */
    fun createDefaultCalibration(viewWidth: Float, viewHeight: Float): Pair<LaneCalibration, HomographyMatrix> {
        val foulLeft = Point2D(viewWidth * 0.12, viewHeight * 0.88)
        val foulRight = Point2D(viewWidth * 0.88, viewHeight * 0.88)
        val arrowsLeft = Point2D(viewWidth * 0.28, viewHeight * 0.52)
        val arrowsRight = Point2D(viewWidth * 0.72, viewHeight * 0.52)

        return calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)
            ?: run {
                val id = HomographyMatrix.identity()
                Pair(
                    LaneCalibration(
                        id = UUID.randomUUID().toString(),
                        foulLineLeftScreen = foulLeft,
                        foulLineRightScreen = foulRight,
                        arrowsLeftScreen = arrowsLeft,
                        arrowsRightScreen = arrowsRight,
                        homographyMatrixElements = id.elements
                    ),
                    id
                )
            }
    }
}
