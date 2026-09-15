package com.example.cebowlinglabtrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.model.BallMetrics
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.SpectoTelemetry
import com.example.cebowlinglabtrack.domain.model.TrajectoryPoint
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.LaneGutter
import com.example.cebowlinglabtrack.theme.LaneMapleWood
import com.example.cebowlinglabtrack.theme.LanePineWood
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.OilPatternTint
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.SoftPurple
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import kotlin.math.abs

/**
 * 2D Interactive Top-Down Bowling Lane Canvas.
 *
 * Displays regulation 39 boards across width and 0 to 62.8 ft down-lane length.
 * Features full Kegel Specto annotations:
 * - Color-coded Skid (Cyan), Hook (Amber), and Roll (Green) motion phases
 * - 40 ft Pattern Exit line & board indicator
 * - Foul line, 15ft arrows, 40ft range finders, 60ft 10-pin triangle deck
 * - Pin deck deflection trajectory through the pins into the pit
 */
@Composable
fun TopDownLaneCanvas(
    trajectory: List<TrajectoryPoint>,
    metrics: BallMetrics? = null,
    spectoTelemetry: SpectoTelemetry? = null,
    modifier: Modifier = Modifier,
    highlightBreakpoint: Boolean = true,
    oilPatternLengthFt: Double = 40.0
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(DarkBackground)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalLaneFt = 64.0 // 0 to 60 ft + pin deck
            val gutterWidthPx = size.width * 0.07f
            val laneWidthPx = size.width - (gutterWidthPx * 2)
            val boardWidthPx = laneWidthPx / LaneConstants.TOTAL_BOARDS

            val laneTopY = size.height * 0.06f // 64 ft (pin deck into pit)
            val foulLineY = size.height * 0.94f // 0 ft (foul line)
            val laneLengthPx = foulLineY - laneTopY

            fun ftToScreenY(ft: Double): Float {
                val progress = (ft / totalLaneFt).toFloat().coerceIn(0f, 1f)
                return foulLineY - (progress * laneLengthPx)
            }

            fun boardToScreenX(board: Double): Float {
                // Board 1 is right gutter edge, Board 39 is left gutter edge (USBC perspective looking down lane)
                val boardIdxFromLeft = 39.0 - board
                return gutterWidthPx + (boardIdxFromLeft.toFloat() * boardWidthPx)
            }

            // 1. Draw Gutters
            drawRect(
                color = LaneGutter,
                topLeft = Offset(0f, laneTopY),
                size = Size(gutterWidthPx, laneLengthPx)
            )
            drawRect(
                color = LaneGutter,
                topLeft = Offset(size.width - gutterWidthPx, laneTopY),
                size = Size(gutterWidthPx, laneLengthPx)
            )

            // 2. Draw Lane Boards (39 individual wooden boards)
            for (b in 0 until LaneConstants.TOTAL_BOARDS) {
                val x = gutterWidthPx + b * boardWidthPx
                val woodColor = if (b % 2 == 0) LaneMapleWood else LanePineWood
                drawRect(
                    color = woodColor,
                    topLeft = Offset(x, laneTopY),
                    size = Size(boardWidthPx, laneLengthPx)
                )
            }

            // 3. Draw Oil Pattern Sheen (0 to oilPatternLengthFt)
            val oilPatternTopY = ftToScreenY(oilPatternLengthFt)
            drawRect(
                color = OilPatternTint,
                topLeft = Offset(gutterWidthPx, oilPatternTopY),
                size = Size(laneWidthPx, foulLineY - oilPatternTopY)
            )

            // 4. Draw Board Guide Ticks and Numbers at Foul Line
            val keyBoards = listOf(5, 10, 15, 20, 25, 30, 35)
            for (kb in keyBoards) {
                val bx = boardToScreenX(kb.toDouble())
                drawLine(
                    color = Color.Black.copy(alpha = 0.6f),
                    start = Offset(bx, foulLineY - 14f),
                    end = Offset(bx, foulLineY + 14f),
                    strokeWidth = 2f
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "$kb",
                    topLeft = Offset(bx - 8f, foulLineY + 16f),
                    style = TextStyle(color = TextMuted, fontSize = 9.sp)
                )
            }

            // 5. Draw Foul Line (0 ft)
            drawLine(
                color = Color.Black,
                start = Offset(gutterWidthPx, foulLineY),
                end = Offset(size.width - gutterWidthPx, foulLineY),
                strokeWidth = 5f
            )

            // 6. Draw 7 Targeting Arrows at 15 ft
            val arrowY = ftToScreenY(LaneConstants.ARROWS_DISTANCE_FT)
            for (kb in keyBoards) {
                val ax = boardToScreenX(kb.toDouble())
                drawArrowhead(ax, arrowY, 9f)
            }

            // 7. Draw Range Finders at 40 ft & Pattern Exit Marker
            val rangeY = ftToScreenY(LaneConstants.RANGE_FINDERS_DISTANCE_FT)
            val rangeBoards = listOf(10, 15, 25, 30)
            for (rb in rangeBoards) {
                val rx = boardToScreenX(rb.toDouble())
                drawCircle(
                    color = Color.Black.copy(alpha = 0.7f),
                    radius = 3.5f,
                    center = Offset(rx, rangeY)
                )
            }

            // Pattern Exit Dashed Line at 40 ft
            drawLine(
                color = NeonCyan.copy(alpha = 0.6f),
                start = Offset(gutterWidthPx, rangeY),
                end = Offset(size.width - gutterWidthPx, rangeY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "PATTERN EXIT (40 ft)",
                topLeft = Offset(gutterWidthPx + 6f, rangeY - 14f),
                style = TextStyle(color = NeonCyan.copy(alpha = 0.8f), fontSize = 8.sp)
            )

            // 8. Draw 10-Pin Triangle Deck at 60 ft
            drawPinDeck(::boardToScreenX, ::ftToScreenY)

            // 9. Draw Ball Trajectory Path with Motion Phase Color Coding
            if (trajectory.size >= 2) {
                val skidLimitFt = spectoTelemetry?.dynamics?.skidFt ?: 24.0
                val rollStartFt = (60.0 - (spectoTelemetry?.dynamics?.rollFt ?: 6.0)).coerceAtLeast(35.0)

                // Render trajectory segments color-coded by Specto Phase
                for (i in 0 until trajectory.size - 1) {
                    val p1 = trajectory[i]
                    val p2 = trajectory[i + 1]

                    val midY = (p1.yFt + p2.yFt) / 2.0
                    val phaseColor = when {
                        midY > 60.0 -> SoftPurple // Pin deck deflection
                        midY >= rollStartFt -> NeonStrikeGreen // Roll phase
                        midY >= skidLimitFt -> ElectricAmber // Hook phase
                        else -> NeonCyan // Skid phase
                    }

                    val sx1 = boardToScreenX(p1.xBoard)
                    val sy1 = ftToScreenY(p1.yFt)
                    val sx2 = boardToScreenX(p2.xBoard)
                    val sy2 = ftToScreenY(p2.yFt)

                    // Glow line
                    drawLine(
                        color = phaseColor.copy(alpha = 0.35f),
                        start = Offset(sx1, sy1),
                        end = Offset(sx2, sy2),
                        strokeWidth = 9f,
                        cap = StrokeCap.Round
                    )
                    // Core line
                    drawLine(
                        color = phaseColor,
                        start = Offset(sx1, sy1),
                        end = Offset(sx2, sy2),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 10. Draw Breakpoint Marker
            val bpBoard = spectoTelemetry?.spatial?.breakpointBoard ?: metrics?.breakpointBoard
            val bpDist = spectoTelemetry?.spatial?.breakpointDistanceFt ?: metrics?.breakpointDistanceFt

            if (highlightBreakpoint && bpBoard != null && bpDist != null) {
                val bpX = boardToScreenX(bpBoard)
                val bpY = ftToScreenY(bpDist)

                // Glow ring
                drawCircle(
                    color = ElectricAmber.copy(alpha = 0.4f),
                    radius = 16f,
                    center = Offset(bpX, bpY)
                )
                drawCircle(
                    color = ElectricAmber,
                    radius = 6f,
                    center = Offset(bpX, bpY)
                )

                // Callout Tag
                val bpText = "BP: B$bpBoard @ ${bpDist}ft"
                drawText(
                    textMeasurer = textMeasurer,
                    text = bpText,
                    topLeft = Offset(bpX + 10f, bpY - 12f),
                    style = TextStyle(color = ElectricAmber, fontSize = 10.sp)
                )
            }

            // 11. Entry Angle & Pin Deck Deflection Indicators
            val entryAngle = spectoTelemetry?.angles?.impactAngleDeg ?: metrics?.entryAngleDeg
            if (entryAngle != null) {
                val entryX = boardToScreenX(spectoTelemetry?.spatial?.entryBoard ?: 17.5)
                val entryY = ftToScreenY(59.5)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "ENTRY ${entryAngle}°",
                    topLeft = Offset(entryX + 10f, entryY - 14f),
                    style = TextStyle(color = NeonStrikeGreen, fontSize = 10.sp)
                )
            }

            spectoTelemetry?.spatial?.let { sp ->
                val exitX = boardToScreenX(sp.pinDeckExitBoard)
                val exitY = ftToScreenY(LaneConstants.TOTAL_LANE_LENGTH_FT)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "EXIT B${sp.pinDeckExitBoard} (Δ${sp.pinDeckDeflection}B)",
                    topLeft = Offset(exitX - 40f, exitY - 16f),
                    style = TextStyle(color = SoftPurple, fontSize = 9.sp)
                )
            }
        }
    }
}

/**
 * Draws standard chevron arrow on lane board.
 */
private fun DrawScope.drawArrowhead(cx: Float, cy: Float, sizePx: Float) {
    val path = Path().apply {
        moveTo(cx, cy - sizePx)
        lineTo(cx + sizePx * 0.7f, cy + sizePx)
        lineTo(cx, cy + sizePx * 0.4f)
        lineTo(cx - sizePx * 0.7f, cy + sizePx)
        close()
    }
    drawPath(path, color = Color.Black.copy(alpha = 0.75f))
}

/**
 * Draws standard USBC 10-pin triangle layout at 60 ft.
 */
private fun DrawScope.drawPinDeck(
    boardToX: (Double) -> Float,
    ftToY: (Double) -> Float
) {
    val pins = listOf(
        Pair(20.0, 60.0), // Pin 1
        Pair(25.5, 60.85), Pair(14.5, 60.85), // Pins 2, 3
        Pair(31.0, 61.70), Pair(20.0, 61.70), Pair(9.0, 61.70), // Pins 4, 5, 6
        Pair(36.5, 62.55), Pair(25.5, 62.55), Pair(14.5, 62.55), Pair(3.5, 62.55) // Pins 7, 8, 9, 10
    )

    for (p in pins) {
        val px = boardToX(p.first)
        val py = ftToY(p.second)
        // Pin body
        drawCircle(
            color = Color.White,
            radius = 4.5f,
            center = Offset(px, py)
        )
        // Pin red neck stripe
        drawCircle(
            color = Color.Red,
            radius = 2f,
            center = Offset(px, py)
        )
    }
}
