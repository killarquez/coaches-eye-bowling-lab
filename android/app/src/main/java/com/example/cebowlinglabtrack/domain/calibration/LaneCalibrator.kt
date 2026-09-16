package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.Handedness
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
 * Range finder visual hash mark geometry projected onto screen pixels (37 ft to 43 ft).
 */
data class RangeFinder(
    val start: Point2D,
    val end: Point2D,
    val board: Double
)

/**
 * Lane guide line geometry projected onto screen pixel coordinates.
 */
data class ProjectedLaneGuides(
    val foulLine: Pair<Point2D, Point2D>,
    val leftGutterLine: List<Point2D>,
    val rightGutterLine: List<Point2D>,
    val centerline: List<Point2D>,
    val indicatorDots: List<Point2D> = emptyList(),
    val arrowsLine: Pair<Point2D, Point2D>,
    val arrowPoints: List<Point2D>,
    val arrowChevrons: List<GutterChevron> = emptyList(),
    val rangeFinders: List<RangeFinder> = emptyList(),
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
    PIN_DECK("FULL LANE (0 & 60 FT)"),
    GUTTERS_AT_ARROWS("GUTTERS (0 & 15 FT)"),
    ARROW_MARKERS("ARROWS (B5 & B35)")
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

        // 1. Indicator Dots at 7.5 ft (USBC spec: boards 3, 5, 8, 11, 14, 26, 29, 32, 35, 37)
        val dotBoards = listOf(3.0, 5.0, 8.0, 11.0, 14.0, 26.0, 29.0, 32.0, 35.0, 37.0)
        val indicatorDots = dotBoards.map { b ->
            homography.forward(LanePoint(b, 7.5))
        }

        // 2. 7 Targeting Arrows at 12 to 15 ft (forward chevron pattern)
        val arrowPositions = listOf(
            Pair(5.0, 12.0),
            Pair(10.0, 13.0),
            Pair(15.0, 14.0),
            Pair(20.0, 15.0),
            Pair(25.0, 14.0),
            Pair(30.0, 13.0),
            Pair(35.0, 12.0)
        )
        val arrowPoints = arrowPositions.map { (b, d) ->
            homography.forward(LanePoint(b, d))
        }
        val arrowChevrons = arrowPositions.map { (b, d) ->
            GutterChevron(
                tip = homography.forward(LanePoint(b, d + 0.8)),
                leftWing = homography.forward(LanePoint(b - 0.6, d - 0.2)),
                rightWing = homography.forward(LanePoint(b + 0.6, d - 0.2)),
                distanceFt = d
            )
        }

        val arrowsLine = Pair(
            homography.forward(LanePoint(1.0, LaneConstants.ARROWS_DISTANCE_FT)),
            homography.forward(LanePoint(39.0, LaneConstants.ARROWS_DISTANCE_FT))
        )

        // 3. Range Finders from 37 ft to 43 ft (Boards 10, 15, 25, 30)
        val rangeFinderBoards = listOf(10.0, 15.0, 25.0, 30.0)
        val rangeFinders = rangeFinderBoards.map { b ->
            RangeFinder(
                start = homography.forward(LanePoint(b, 37.0)),
                end = homography.forward(LanePoint(b, 43.0)),
                board = b
            )
        }

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
            indicatorDots = indicatorDots,
            arrowsLine = arrowsLine,
            arrowPoints = arrowPoints,
            arrowChevrons = arrowChevrons,
            rangeFinders = rangeFinders,
            headpinPoint = headpin,
            pinDeckLine = pinDeckLine,
            leftGutterChevrons = leftChevrons,
            rightGutterChevrons = rightChevrons,
            targetLinePointsScreen = targetPointsScreen
        )
    }

    /**
     * Provides default screen anchor coordinates for a standard phone viewfinder
     * with tripod mounted behind foul line, scaled to the current zoom ratio and
     * aligned with either the Right Gutter (Righty) or Left Gutter (Lefty).
     */
    fun createDefaultCalibration(
        viewWidth: Float,
        viewHeight: Float,
        zoomRatio: Float = 1.0f,
        anchorMode: CalibrationAnchorMode = CalibrationAnchorMode.PIN_DECK,
        alignment: Handedness = Handedness.RIGHT
    ): Pair<LaneCalibration, HomographyMatrix> {
        val zoomFactor = (zoomRatio - 1.0f).coerceIn(0f, 2.5f)

        // Perspective mapping for standard bowling alley view from approach
        val foulY = if (zoomRatio >= 2.0f) {
            viewHeight * (0.80f + 0.03f * (zoomRatio - 2.0f).coerceAtMost(2.0f))
        } else {
            viewHeight * (0.64f + 0.16f * (zoomRatio - 1.0f))
        }

        val topAnchorY = if (anchorMode == CalibrationAnchorMode.PIN_DECK) {
            // Pin deck at 60 ft near the top of the frame
            if (zoomRatio >= 2.0f) viewHeight * 0.22f else viewHeight * 0.28f
        } else {
            // Arrows at 15 ft near the middle
            if (zoomRatio >= 2.0f) viewHeight * 0.48f else viewHeight * 0.42f
        }

        val (foulLeft, foulRight, topAnchorLeft, topAnchorRight) = when (alignment) {
            Handedness.RIGHT -> {
                // Camera aligned with RIGHT gutter (board 39):
                // Right gutter runs almost vertical near the right third
                // Left gutter diverges outwards to the left toward the foul line
                val rFoulX = viewWidth * (0.82f + 0.06f * zoomFactor).coerceAtMost(0.96f)
                val lFoulX = viewWidth * (0.16f - 0.06f * zoomFactor).coerceAtLeast(0.04f)

                val (lTopX, rTopX) = if (anchorMode == CalibrationAnchorMode.PIN_DECK) {
                    Pair(
                        viewWidth * (0.34f - 0.04f * zoomFactor).coerceAtLeast(0.12f),
                        viewWidth * (0.70f + 0.02f * zoomFactor).coerceAtMost(0.88f)
                    )
                } else {
                    Pair(
                        viewWidth * (0.38f - 0.04f * zoomFactor).coerceAtLeast(0.15f),
                        viewWidth * (0.73f + 0.04f * zoomFactor).coerceAtMost(0.85f)
                    )
                }

                listOf(
                    Point2D(lFoulX.toDouble(), foulY.toDouble()),
                    Point2D(rFoulX.toDouble(), foulY.toDouble()),
                    Point2D(lTopX.toDouble(), topAnchorY.toDouble()),
                    Point2D(rTopX.toDouble(), topAnchorY.toDouble())
                )
            }
            Handedness.LEFT -> {
                // Camera aligned with LEFT gutter (board 1):
                // Left gutter runs almost vertical near the left third
                // Right gutter diverges outwards to the right toward the foul line
                val lFoulX = viewWidth * (0.18f - 0.06f * zoomFactor).coerceAtLeast(0.04f)
                val rFoulX = viewWidth * (0.84f + 0.06f * zoomFactor).coerceAtMost(0.96f)

                val (lTopX, rTopX) = if (anchorMode == CalibrationAnchorMode.PIN_DECK) {
                    Pair(
                        viewWidth * (0.30f - 0.02f * zoomFactor).coerceAtLeast(0.12f),
                        viewWidth * (0.66f + 0.04f * zoomFactor).coerceAtMost(0.88f)
                    )
                } else {
                    Pair(
                        viewWidth * (0.27f - 0.04f * zoomFactor).coerceAtLeast(0.15f),
                        viewWidth * (0.62f + 0.04f * zoomFactor).coerceAtMost(0.85f)
                    )
                }

                listOf(
                    Point2D(lFoulX.toDouble(), foulY.toDouble()),
                    Point2D(rFoulX.toDouble(), foulY.toDouble()),
                    Point2D(lTopX.toDouble(), topAnchorY.toDouble()),
                    Point2D(rTopX.toDouble(), topAnchorY.toDouble())
                )
            }
        }

        return calibrate(
            foulLineLeft = foulLeft,
            foulLineRight = foulRight,
            arrowsLeft = topAnchorLeft,
            arrowsRight = topAnchorRight,
            anchorMode = anchorMode,
            calibrationZoomRatio = zoomRatio
        ) ?: run {
            val id = HomographyMatrix.identity()
            Pair(
                LaneCalibration(
                    id = UUID.randomUUID().toString(),
                    foulLineLeftScreen = foulLeft,
                    foulLineRightScreen = foulRight,
                    arrowsLeftScreen = topAnchorLeft,
                    arrowsRightScreen = topAnchorRight,
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
