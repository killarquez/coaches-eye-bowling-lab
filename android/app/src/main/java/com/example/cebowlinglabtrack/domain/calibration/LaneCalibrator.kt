package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.LanePoint
import com.example.cebowlinglabtrack.domain.model.Point2D
import java.util.UUID

/**
 * Gutter forward AR chevron geometry projected onto screen pixels.
 */
data class GutterChevron(
    val tip: Point2D,
    val leftWing: Point2D,
    val rightWing: Point2D,
    val distanceFt: Double
)

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
    val pinDeckLine: Pair<Point2D, Point2D>,
    val leftGutterChevrons: List<GutterChevron> = emptyList(),
    val rightGutterChevrons: List<GutterChevron> = emptyList(),
    val targetLinePointsScreen: List<Point2D> = emptyList()
)

/**
 * Reference calibration anchor modes for lane homography.
 */
enum class CalibrationAnchorMode(val displayName: String) {
    GUTTERS_AT_ARROWS("GUTTERS (0 & 15 FT)"),
    ARROW_MARKERS("ARROWS (B5 & B35)"),
    PIN_DECK("PIN DECK (0 & 60 FT)")
}

/**
 * Manages lane calibration, homography computation, and AR guide projections.
 */
class LaneCalibrator {

    /**
     * Calibrates the perspective transform using 4 known anchor points.
     *
     * Standard calibration points:
     * - In GUTTERS_AT_ARROWS:
     *   1. Foul line left: Board 1.0, 0.0 ft (left gutter)
     *   2. Foul line right: Board 39.0, 0.0 ft (right gutter)
     *   3. 15ft Left: Board 1.0, 15.0 ft (left gutter at arrows)
     *   4. 15ft Right: Board 39.0, 15.0 ft (right gutter at arrows)
     * - In ARROW_MARKERS:
     *   1. Foul line left: Board 1.0, 0.0 ft
     *   2. Foul line right: Board 39.0, 0.0 ft
     *   3. Arrows left: Board 5.0, 15.0 ft (1st arrow)
     *   4. Arrows right: Board 35.0, 15.0 ft (7th arrow)
     */
    fun calibrate(
        foulLineLeft: Point2D,
        foulLineRight: Point2D,
        arrowsLeft: Point2D,
        arrowsRight: Point2D,
        laneName: String = "Lane 1",
        anchorMode: CalibrationAnchorMode = CalibrationAnchorMode.ARROW_MARKERS,
        calibrationZoomRatio: Float = 1.0f
    ): Pair<LaneCalibration, HomographyMatrix>? {
        val srcPoints = when (anchorMode) {
            CalibrationAnchorMode.GUTTERS_AT_ARROWS -> listOf(
                LanePoint(1.0, 0.0),                                       // Foul line left
                LanePoint(LaneConstants.TOTAL_BOARDS.toDouble(), 0.0),     // Foul line right
                LanePoint(1.0, LaneConstants.ARROWS_DISTANCE_FT),         // Left gutter at 15ft
                LanePoint(LaneConstants.TOTAL_BOARDS.toDouble(), LaneConstants.ARROWS_DISTANCE_FT) // Right gutter at 15ft
            )
            CalibrationAnchorMode.ARROW_MARKERS -> listOf(
                LanePoint(1.0, 0.0),                                       // Foul line left
                LanePoint(LaneConstants.TOTAL_BOARDS.toDouble(), 0.0),     // Foul line right
                LanePoint(5.0, LaneConstants.ARROWS_DISTANCE_FT),         // Arrows left (B5)
                LanePoint(35.0, LaneConstants.ARROWS_DISTANCE_FT)         // Arrows right (B35)
            )
            CalibrationAnchorMode.PIN_DECK -> listOf(
                LanePoint(1.0, 0.0),
                LanePoint(LaneConstants.TOTAL_BOARDS.toDouble(), 0.0),
                LanePoint(1.0, LaneConstants.FOUL_LINE_TO_HEADPIN_FT),
                LanePoint(LaneConstants.TOTAL_BOARDS.toDouble(), LaneConstants.FOUL_LINE_TO_HEADPIN_FT)
            )
        }

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
            reprojectionErrorRmse = rmse,
            calibrationZoomRatio = calibrationZoomRatio,
            anchorMode = anchorMode.name
        )

        return Pair(calibration, homography)
    }

    /**
     * Calibrates directly to physical gutter edges at foul line (0 ft) and arrows (15 ft).
     */
    fun calibrateGutters(
        foulLineLeft: Point2D,
        foulLineRight: Point2D,
        gutterLeft15ft: Point2D,
        gutterRight15ft: Point2D,
        laneName: String = "Lane 1",
        calibrationZoomRatio: Float = 1.0f
    ): Pair<LaneCalibration, HomographyMatrix>? {
        return calibrate(
            foulLineLeft = foulLineLeft,
            foulLineRight = foulLineRight,
            arrowsLeft = gutterLeft15ft,
            arrowsRight = gutterRight15ft,
            laneName = laneName,
            anchorMode = CalibrationAnchorMode.GUTTERS_AT_ARROWS,
            calibrationZoomRatio = calibrationZoomRatio
        )
    }

    /**
     * Generates standard lane AR guide lines projected into camera screen coordinates,
     * including forward gutter chevrons (Strike.app style) and visual target line.
     */
    fun generateProjectedGuides(
        homography: HomographyMatrix,
        targetLine: com.example.cebowlinglabtrack.domain.model.VisualTargetLine? = null
    ): ProjectedLaneGuides {
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

        // Forward AR Gutter Chevrons every 5 ft down the lane (Strike.app style)
        val chevronDistances = listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0)
        val leftChevrons = chevronDistances.map { d ->
            GutterChevron(
                tip = homography.forward(LanePoint(1.0, d + 1.2)),
                leftWing = homography.forward(LanePoint(0.4, d)),
                rightWing = homography.forward(LanePoint(1.6, d)),
                distanceFt = d
            )
        }
        val rightChevrons = chevronDistances.map { d ->
            GutterChevron(
                tip = homography.forward(LanePoint(39.0, d + 1.2)),
                leftWing = homography.forward(LanePoint(38.4, d)),
                rightWing = homography.forward(LanePoint(39.6, d)),
                distanceFt = d
            )
        }

        // Project visual target line into camera viewport
        val targetPointsScreen = targetLine?.generateTrajectory(60)?.map { pt ->
            homography.forward(pt)
        } ?: emptyList()

        return ProjectedLaneGuides(
            foulLine = Pair(foulLeft, foulRight),
            leftGutterLine = leftGutter,
            rightGutterLine = rightGutter,
            centerline = center,
            arrowsLine = arrowsLine,
            arrowPoints = arrowPoints,
            headpinPoint = headpin,
            pinDeckLine = pinDeckLine,
            leftGutterChevrons = leftChevrons,
            rightGutterChevrons = rightChevrons,
            targetLinePointsScreen = targetPointsScreen
        )
    }

    /**
     * Provides default screen anchor coordinates for a standard phone viewfinder
     * with tripod mounted behind foul line, scaled to the current zoom ratio.
     */
    fun createDefaultCalibration(
        viewWidth: Float,
        viewHeight: Float,
        zoomRatio: Float = 1.0f,
        anchorMode: CalibrationAnchorMode = CalibrationAnchorMode.GUTTERS_AT_ARROWS
    ): Pair<LaneCalibration, HomographyMatrix> {
        // Perspective mapping for standard bowling alley view from approach
        val foulY = if (zoomRatio >= 2.0f) {
            viewHeight * (0.76f + 0.04f * (zoomRatio - 2.0f).coerceAtMost(2.0f))
        } else {
            viewHeight * (0.58f + 0.18f * (zoomRatio - 1.0f))
        }
        val arrowsY = if (zoomRatio >= 2.0f) {
            viewHeight * (0.46f + 0.02f * (zoomRatio - 2.0f).coerceAtMost(2.0f))
        } else {
            viewHeight * (0.42f + 0.04f * (zoomRatio - 1.0f))
        }

        val foulHalfW = (viewWidth * (0.20f + 0.20f * (zoomRatio - 1.0f).coerceIn(0f, 1.5f))).coerceAtMost(viewWidth * 0.45f)
        val arrowsHalfW = (viewWidth * (0.12f + 0.14f * (zoomRatio - 1.0f).coerceIn(0f, 1.5f))).coerceAtMost(viewWidth * 0.35f)
        val midX = viewWidth * 0.5f

        val foulLeft = Point2D((midX - foulHalfW).toDouble(), foulY.toDouble())
        val foulRight = Point2D((midX + foulHalfW).toDouble(), foulY.toDouble())
        val arrowsLeft = Point2D((midX - arrowsHalfW).toDouble(), arrowsY.toDouble())
        val arrowsRight = Point2D((midX + arrowsHalfW).toDouble(), arrowsY.toDouble())

        return calibrate(
            foulLineLeft = foulLeft,
            foulLineRight = foulRight,
            arrowsLeft = arrowsLeft,
            arrowsRight = arrowsRight,
            anchorMode = anchorMode,
            calibrationZoomRatio = zoomRatio
        ) ?: run {
            val id = HomographyMatrix.identity()
            Pair(
                LaneCalibration(
                    id = UUID.randomUUID().toString(),
                    foulLineLeftScreen = foulLeft,
                    foulLineRightScreen = foulRight,
                    arrowsLeftScreen = arrowsLeft,
                    arrowsRightScreen = arrowsRight,
                    homographyMatrixElements = id.elements,
                    calibrationZoomRatio = zoomRatio,
                    anchorMode = anchorMode.name
                ),
                id
            )
        }
    }

    companion object {
        val DEFAULT_CALIBRATION: LaneCalibration by lazy {
            LaneCalibrator().createDefaultCalibration(1080f, 1920f).first
        }
    }
}
