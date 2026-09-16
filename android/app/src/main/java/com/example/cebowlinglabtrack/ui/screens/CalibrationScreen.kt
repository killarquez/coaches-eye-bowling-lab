package com.example.cebowlinglabtrack.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.camera.CameraPreviewView
import com.example.cebowlinglabtrack.domain.calibration.AutoLaneDetector
import com.example.cebowlinglabtrack.domain.calibration.CalibrationAnchorMode
import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.ml.PinDeckDetector
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.DarkSurfaceVariant
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.GutterChevronCyan
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import com.example.cebowlinglabtrack.theme.UsbcGold
import com.example.cebowlinglabtrack.theme.UsbcNavyLight
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 100% Full-Screen 4-Corner Camera Perspective Calibration Screen.
 *
 * Provides a 1:1 match with LiveTrackingScreen's full-screen viewport,
 * eliminating all square letterboxing and coordinate distortion.
 *
 * Calibration geometry maps the entire active lane (0 to 60 ft, Boards 1 to 39):
 * - Handle 0: Foul Line Left (0 ft, Board 1)
 * - Handle 1: Foul Line Right (0 ft, Board 39)
 * - Handle 2: Pin Deck Left (60 ft, Board 1)
 * - Handle 3: Pin Deck Right (60 ft, Board 39)
 *
 * Real physical pins sit naturally inside the 60 ft pin deck boundary and bracket.
 */
@Composable
fun CalibrationScreen(
    currentCalibration: LaneCalibration?,
    zoomRatio: Float = 1.0f,
    autoCenterGuidance: String? = null,
    bowlerHandedness: Handedness = Handedness.RIGHT,
    onZoomChange: (Float) -> Unit = {},
    onSaveCalibration: (Point2D, Point2D, Point2D, Point2D, CalibrationAnchorMode) -> Unit,
    onAutoDetectLaneDetailed: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int, alignment: Handedness, anchorMode: CalibrationAnchorMode) -> AutoLaneDetector.AutoDetectionResult)? = null,
    onAutoDetectLane: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int, alignment: Handedness) -> Boolean)? = null,
    onCalibrateDefault: ((alignment: Handedness) -> Unit)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calibrator = remember { LaneCalibrator() }
    var alignmentHandedness by remember { mutableStateOf(bowlerHandedness) }
    LaunchedEffect(bowlerHandedness) { alignmentHandedness = bowlerHandedness }
    var currentGuidance by remember { mutableStateOf(autoCenterGuidance) }
    LaunchedEffect(autoCenterGuidance) { currentGuidance = autoCenterGuidance }

    var anchorMode by remember {
        mutableStateOf(
            currentCalibration?.anchorMode?.let { modeStr ->
                try { CalibrationAnchorMode.valueOf(modeStr) } catch (e: Exception) { null }
            } ?: CalibrationAnchorMode.PIN_DECK
        )
    }

    // 4 Draggable Anchor Handles in Full-Screen Pixel Coordinates
    var flX by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.x?.toFloat() ?: 180f) }
    var flY by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.y?.toFloat() ?: 1750f) }

    var frX by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.x?.toFloat() ?: 900f) }
    var frY by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.y?.toFloat() ?: 1750f) }

    var alX by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.x?.toFloat() ?: 360f) }
    var alY by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.y?.toFloat() ?: 550f) }

    var arX by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.x?.toFloat() ?: 720f) }
    var arY by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.y?.toFloat() ?: 550f) }

    var selectedPinIndex by remember { mutableStateOf<Int?>(null) }
    var nudgeStepPx by remember { mutableStateOf(2f) }
    var autoDetectionStatus by remember { mutableStateOf<String?>(null) }

    // Camera frame buffers for 1-click Auto-Detect
    var latestFrameBytes by remember { mutableStateOf<ByteArray?>(null) }
    var frameWidth by remember { mutableStateOf(0) }
    var frameHeight by remember { mutableStateOf(0) }
    var frameStride by remember { mutableStateOf(0) }
    var lastViewportWidth by remember { mutableStateOf(1080f) }
    var lastViewportHeight by remember { mutableStateOf(2340f) }

    // Full-Screen Root Container
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        if (viewWidth > 50f && viewHeight > 50f) {
            lastViewportWidth = viewWidth
            lastViewportHeight = viewHeight
        }

        // Initialize default 0 to 60 ft full lane anchors on full-screen layout
        var sizeInitialized by remember { mutableStateOf(false) }
        LaunchedEffect(viewWidth, viewHeight) {
            if (!sizeInitialized && viewWidth > 50f && viewHeight > 50f) {
                val isOffScreen = flY < 100f || flY > viewHeight * 1.15f || frX > viewWidth * 1.15f
                val isOldSquareCalibration = currentCalibration != null && (currentCalibration.foulLineLeftScreen.y < viewHeight * 0.45f)
                val isUncalibrated = currentCalibration == null || isOffScreen || isOldSquareCalibration
                if (isUncalibrated) {
                    val (defaultCalib, _) = calibrator.createDefaultCalibration(
                        viewWidth = viewWidth,
                        viewHeight = viewHeight,
                        zoomRatio = zoomRatio,
                        anchorMode = anchorMode,
                        alignment = alignmentHandedness
                    )
                    flX = defaultCalib.foulLineLeftScreen.x.toFloat()
                    flY = defaultCalib.foulLineLeftScreen.y.toFloat()
                    frX = defaultCalib.foulLineRightScreen.x.toFloat()
                    frY = defaultCalib.foulLineRightScreen.y.toFloat()
                    alX = defaultCalib.arrowsLeftScreen.x.toFloat()
                    alY = defaultCalib.arrowsLeftScreen.y.toFloat()
                    arX = defaultCalib.arrowsRightScreen.x.toFloat()
                    arY = defaultCalib.arrowsRightScreen.y.toFloat()
                }
                sizeInitialized = true
            }
        }

        // 1. Full-Screen CameraX Preview Stream (120 FPS target with active hardware zoom)
        CameraPreviewView(
            onFrameAvailable = { bytes, w, h, s, _ ->
                latestFrameBytes = bytes
                frameWidth = w
                frameHeight = h
                frameStride = s
            },
            zoomRatio = zoomRatio,
            targetFps = 120,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Full-Screen Interactive Touch & AR Guide Overlay Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val touchX = offset.x
                            val touchY = offset.y
                            val distances = listOf(
                                dist(touchX, touchY, flX, flY),
                                dist(touchX, touchY, frX, frY),
                                dist(touchX, touchY, alX, alY),
                                dist(touchX, touchY, arX, arY)
                            )
                            val minIdx = distances.indices.minByOrNull { distances[it] } ?: 0
                            selectedPinIndex = if (distances[minIdx] < 140f) minIdx else null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            when (selectedPinIndex) {
                                0 -> { flX += dragAmount.x; flY += dragAmount.y }
                                1 -> { frX += dragAmount.x; frY += dragAmount.y }
                                2 -> { alX += dragAmount.x; alY += dragAmount.y }
                                3 -> { arX += dragAmount.x; arY += dragAmount.y }
                            }
                        },
                        onDragEnd = {},
                        onDragCancel = {}
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Compute real-time homography from the 4 anchor corners
                val tempResult = calibrator.calibrate(
                    foulLineLeft = Point2D(flX.toDouble(), flY.toDouble()),
                    foulLineRight = Point2D(frX.toDouble(), frY.toDouble()),
                    arrowsLeft = Point2D(alX.toDouble(), alY.toDouble()),
                    arrowsRight = Point2D(arX.toDouble(), arY.toDouble()),
                    anchorMode = anchorMode,
                    calibrationZoomRatio = zoomRatio
                )
                val liveH = tempResult?.second

                // Full-lane trapezoid path (0 ft to 60 ft)
                val laneTrapezoid = Path().apply {
                    moveTo(flX, flY)
                    lineTo(frX, frY)
                    lineTo(arX, arY)
                    lineTo(alX, alY)
                    close()
                }

                // Translucent cyan wash across the lane bed
                drawPath(path = laneTrapezoid, color = Color(0x1800E5FF))
                // Perimeter outline
                drawPath(path = laneTrapezoid, color = NeonCyan.copy(alpha = 0.45f), style = Stroke(width = 2f))

                // Lane Boundary Lines
                // Foul Line (Bottom - 0 ft)
                drawLine(Color.White, Offset(flX, flY), Offset(frX, frY), strokeWidth = 3.5f)
                // Left Gutter (highlighted in UsbcGold if Left Gutter aligned)
                drawLine(
                    color = if (alignmentHandedness == Handedness.LEFT) UsbcGold else NeonCyan,
                    start = Offset(flX, flY),
                    end = Offset(alX, alY),
                    strokeWidth = if (alignmentHandedness == Handedness.LEFT) 4.5f else 2.5f
                )
                // Right Gutter (highlighted in UsbcGold if Right Gutter aligned)
                drawLine(
                    color = if (alignmentHandedness == Handedness.RIGHT) UsbcGold else NeonCyan,
                    start = Offset(frX, frY),
                    end = Offset(arX, arY),
                    strokeWidth = if (alignmentHandedness == Handedness.RIGHT) 4.5f else 2.5f
                )
                // Top Pin Deck Boundary Line (60 ft)
                drawLine(ElectricAmber, Offset(alX, alY), Offset(arX, arY), strokeWidth = 2.5f)

                // Dashed Centerline Guide (Board 20)
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset((flX + frX) / 2f, (flY + frY) / 2f),
                    end = Offset((alX + arX) / 2f, (alY + arY) / 2f),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )

                // Projected AR Lane Markings & Guides
                if (liveH != null) {
                    val guides = calibrator.generateProjectedGuides(liveH)

                    // 1. Indicator Dots at 7.5 ft (USBC spec: boards 3, 5, 8, 11, 14, 26, 29, 32, 35, 37)
                    for (pt in guides.indicatorDots) {
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.85f),
                            radius = 3.5f,
                            center = Offset(pt.x.toFloat(), pt.y.toFloat())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.95f),
                            radius = 1.8f,
                            center = Offset(pt.x.toFloat(), pt.y.toFloat())
                        )
                    }

                    // 2. Arrows Guideline at 15 ft
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.35f),
                        start = Offset(guides.arrowsLine.first.x.toFloat(), guides.arrowsLine.first.y.toFloat()),
                        end = Offset(guides.arrowsLine.second.x.toFloat(), guides.arrowsLine.second.y.toFloat()),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // 3. 7 Targeting Arrow Chevrons at 12-15 ft (pointing toward pins)
                    for (ch in guides.arrowChevrons) {
                        drawLine(
                            color = ElectricAmber.copy(alpha = 0.95f),
                            start = Offset(ch.leftWing.x.toFloat(), ch.leftWing.y.toFloat()),
                            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = ElectricAmber.copy(alpha = 0.95f),
                            start = Offset(ch.rightWing.x.toFloat(), ch.rightWing.y.toFloat()),
                            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                    for (pt in guides.arrowPoints) {
                        drawCircle(
                            color = ElectricAmber,
                            radius = 3f,
                            center = Offset(pt.x.toFloat(), pt.y.toFloat())
                        )
                    }

                    // 4. Range Finders from 37 ft to 43 ft (Boards 10, 15, 25, 30)
                    for (rf in guides.rangeFinders) {
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.85f),
                            start = Offset(rf.start.x.toFloat(), rf.start.y.toFloat()),
                            end = Offset(rf.end.x.toFloat(), rf.end.y.toFloat()),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 5. Forward AR Gutter Chevrons pointing down-lane
                    for (ch in guides.leftGutterChevrons) {
                        drawLine(
                            color = GutterChevronCyan.copy(alpha = 0.8f),
                            start = Offset(ch.leftWing.x.toFloat(), ch.leftWing.y.toFloat()),
                            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = GutterChevronCyan.copy(alpha = 0.8f),
                            start = Offset(ch.rightWing.x.toFloat(), ch.rightWing.y.toFloat()),
                            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }
                    for (ch in guides.rightGutterChevrons) {
                        drawLine(
                            color = GutterChevronCyan.copy(alpha = 0.8f),
                            start = Offset(ch.leftWing.x.toFloat(), ch.leftWing.y.toFloat()),
                            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = GutterChevronCyan.copy(alpha = 0.8f),
                            start = Offset(ch.rightWing.x.toFloat(), ch.rightWing.y.toFloat()),
                            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 6. 10-Pin Deck Corner Brackets [  ] at 60 ft
                    // (Real physical pins in the alley sit cleanly inside this bracket with no artificial circles)
                    val pinScreenPts = PinDeckDetector.STANDARD_PIN_COORDS.map { liveH.forward(it) }
                    if (pinScreenPts.isNotEmpty()) {
                        var minX = Float.MAX_VALUE
                        var maxX = Float.MIN_VALUE
                        var minY = Float.MAX_VALUE
                        var maxY = Float.MIN_VALUE
                        for (pt in pinScreenPts) {
                            val x = pt.x.toFloat()
                            val y = pt.y.toFloat()
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                        }
                        val padX = 14f
                        val padY = 10f
                        val left = minX - padX
                        val right = maxX + padX
                        val top = minY - padY
                        val bottom = maxY + padY
                        val bracketLen = 14f
                        val bracketColor = Color.White.copy(alpha = 0.9f)

                        drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), 2.5f)
                        drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), 2.5f)
                        drawLine(bracketColor, Offset(right, top), Offset(right - bracketLen, top), 2.5f)
                        drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLen), 2.5f)
                        drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLen, bottom), 2.5f)
                        drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - bracketLen), 2.5f)
                        drawLine(bracketColor, Offset(right, bottom), Offset(right - bracketLen, bottom), 2.5f)
                        drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - bracketLen), 2.5f)
                    }
                }

                // 7. Draw 4 Interactive Corner Handles with Labels
                val topLabelL = when (anchorMode) {
                    CalibrationAnchorMode.PIN_DECK -> "DECK-L (60FT)"
                    CalibrationAnchorMode.GUTTERS_AT_ARROWS -> "GUTTER-L (15FT)"
                    CalibrationAnchorMode.ARROW_MARKERS -> "ARROW-L (B5)"
                }
                val topLabelR = when (anchorMode) {
                    CalibrationAnchorMode.PIN_DECK -> "DECK-R (60FT)"
                    CalibrationAnchorMode.GUTTERS_AT_ARROWS -> "GUTTER-R (15FT)"
                    CalibrationAnchorMode.ARROW_MARKERS -> "ARROW-R (B35)"
                }

                drawPinHandle("FL-L (B1)", Offset(flX, flY), if (alignmentHandedness == Handedness.LEFT) UsbcGold else NeonStrikeGreen, selectedPinIndex == 0)
                drawPinHandle("FL-R (B39)", Offset(frX, frY), if (alignmentHandedness == Handedness.RIGHT) UsbcGold else NeonStrikeGreen, selectedPinIndex == 1)
                drawPinHandle(topLabelL, Offset(alX, alY), if (alignmentHandedness == Handedness.LEFT) UsbcGold else ElectricAmber, selectedPinIndex == 2)
                drawPinHandle(topLabelR, Offset(arX, arY), if (alignmentHandedness == Handedness.RIGHT) UsbcGold else ElectricAmber, selectedPinIndex == 3)
            }
        }

        // 3. Floating Top HUD Panel (Header, Zoom Bar & Gutter Alignment)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface.copy(alpha = 0.88f))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "FULL LANE CALIBRATION",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "DRAG 4 CORNERS: FOUL LINE TO PIN DECK (0-60 FT)",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(UsbcNavyLight)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "DLT 3x3 • ${String.format("%.1f", zoomRatio)}x",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Zoom Bar & Gutter Alignment Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface.copy(alpha = 0.88f))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom",
                        tint = UsbcGold,
                        modifier = Modifier.size(16.dp)
                    )
                    IconButton(
                        onClick = { onZoomChange((zoomRatio - 0.2f).coerceAtLeast(1.0f)) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = if (zoomRatio > 1.0f) TextPrimary else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    listOf(1.0f, 1.8f, 2.2f, 2.5f, 3.0f).forEach { presetZoom ->
                        val isSelected = abs(zoomRatio - presetZoom) < 0.15f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isSelected) UsbcGold else DarkSurfaceVariant)
                                .clickable { onZoomChange(presetZoom) }
                                .padding(horizontal = 5.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = "${presetZoom}x",
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                    IconButton(
                        onClick = { onZoomChange((zoomRatio + 0.2f).coerceAtMost(3.5f)) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = if (zoomRatio < 3.5f) TextPrimary else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Gutter Alignment Toggle
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Handedness.RIGHT to "R GUTTER",
                        Handedness.LEFT to "L GUTTER"
                    ).forEach { (hand, label) ->
                        val isSelected = alignmentHandedness == hand
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) UsbcNavyLight else DarkSurfaceVariant)
                                .border(1.dp, if (isSelected) UsbcGold else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable {
                                    alignmentHandedness = hand
                                    val (defaultCalib, _) = calibrator.createDefaultCalibration(
                                        viewWidth = lastViewportWidth,
                                        viewHeight = lastViewportHeight,
                                        zoomRatio = zoomRatio,
                                        anchorMode = anchorMode,
                                        alignment = hand
                                    )
                                    flX = defaultCalib.foulLineLeftScreen.x.toFloat()
                                    flY = defaultCalib.foulLineLeftScreen.y.toFloat()
                                    frX = defaultCalib.foulLineRightScreen.x.toFloat()
                                    frY = defaultCalib.foulLineRightScreen.y.toFloat()
                                    alX = defaultCalib.arrowsLeftScreen.x.toFloat()
                                    alY = defaultCalib.arrowsLeftScreen.y.toFloat()
                                    arX = defaultCalib.arrowsRightScreen.x.toFloat()
                                    arY = defaultCalib.arrowsRightScreen.y.toFloat()
                                    autoDetectionStatus = "VIEW ALIGNED: ${if (hand == Handedness.RIGHT) "RIGHT" else "LEFT"} GUTTER"
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) UsbcGold else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 4. Floating Bottom HUD Panel (Nudge Controls, Status Banner & Action Buttons)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Floating Precision Nudge Controls (Visible when any anchor is selected)
            selectedPinIndex?.let { pinIdx ->
                val anchorName = when (pinIdx) {
                    0 -> "1. FOUL L (B1)"
                    1 -> "2. FOUL R (B39)"
                    2 -> if (anchorMode == CalibrationAnchorMode.PIN_DECK) "3. DECK L (60FT)" else "3. TOP LEFT"
                    3 -> if (anchorMode == CalibrationAnchorMode.PIN_DECK) "4. DECK R (60FT)" else "4. TOP RIGHT"
                    else -> "CORNER"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface.copy(alpha = 0.94f))
                        .border(1.dp, UsbcGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NUDGE: $anchorName",
                            color = UsbcGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        // Step size (2px vs 10px)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "2px",
                                color = if (nudgeStepPx == 2f) Color.Black else TextSecondary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (nudgeStepPx == 2f) NeonCyan else Color.Transparent)
                                    .clickable { nudgeStepPx = 2f }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "10px",
                                color = if (nudgeStepPx == 10f) Color.Black else TextSecondary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (nudgeStepPx == 10f) NeonCyan else Color.Transparent)
                                    .clickable { nudgeStepPx = 10f }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        // Directional Nudge Buttons
                        NudgeButton(icon = Icons.Default.ArrowBack) {
                            when (pinIdx) {
                                0 -> flX -= nudgeStepPx
                                1 -> frX -= nudgeStepPx
                                2 -> alX -= nudgeStepPx
                                3 -> arX -= nudgeStepPx
                            }
                        }
                        NudgeButton(icon = Icons.Default.ArrowUpward) {
                            when (pinIdx) {
                                0 -> flY -= nudgeStepPx
                                1 -> frY -= nudgeStepPx
                                2 -> alY -= nudgeStepPx
                                3 -> arY -= nudgeStepPx
                            }
                        }
                        NudgeButton(icon = Icons.Default.ArrowDownward) {
                            when (pinIdx) {
                                0 -> flY += nudgeStepPx
                                1 -> frY += nudgeStepPx
                                2 -> alY += nudgeStepPx
                                3 -> arY += nudgeStepPx
                            }
                        }
                        NudgeButton(icon = Icons.Default.ArrowForward) {
                            when (pinIdx) {
                                0 -> flX += nudgeStepPx
                                1 -> frX += nudgeStepPx
                                2 -> alX += nudgeStepPx
                                3 -> arX += nudgeStepPx
                            }
                        }

                        // Dismiss button
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Deselect",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { selectedPinIndex = null }
                        )
                    }
                }
            }

            // Guidance & Detection Status Banner
            val activeGuidance = currentGuidance ?: autoCenterGuidance
            val displayStatus = autoDetectionStatus ?: activeGuidance
            displayStatus?.let { msg ->
                val isSuccess = msg.contains("✓") || msg.contains("ALIGNED") || msg.contains("CENTERED")
                val isError = msg.contains("NO PIN RACK") || msg.contains("UNCLEAR") || msg.contains("❌")
                val borderCol = when {
                    isSuccess -> NeonStrikeGreen
                    isError -> PowerCoral
                    else -> NeonCyan
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface.copy(alpha = 0.92f))
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = msg,
                        color = borderCol,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Primary Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-Detect CV
                Button(
                    onClick = {
                        val frame = latestFrameBytes
                        if (frame != null) {
                            if (onAutoDetectLaneDetailed != null) {
                                val res = onAutoDetectLaneDetailed(frame, frameWidth, frameHeight, frameStride, alignmentHandedness, anchorMode)
                                autoDetectionStatus = res.statusMessage
                                currentGuidance = res.autoCenterGuidance
                                if (res.isSuccess) {
                                    flX = res.foulLineLeft.x.toFloat()
                                    flY = res.foulLineLeft.y.toFloat()
                                    frX = res.foulLineRight.x.toFloat()
                                    frY = res.foulLineRight.y.toFloat()
                                    val topL = if (anchorMode == CalibrationAnchorMode.PIN_DECK) (res.pinDeckLeft ?: res.arrowsLeft) else res.arrowsLeft
                                    val topR = if (anchorMode == CalibrationAnchorMode.PIN_DECK) (res.pinDeckRight ?: res.arrowsRight) else res.arrowsRight
                                    alX = topL.x.toFloat()
                                    alY = topL.y.toFloat()
                                    arX = topR.x.toFloat()
                                    arY = topR.y.toFloat()
                                    if (abs(res.optimalZoomRatio - zoomRatio) > 0.15f) {
                                        onZoomChange(res.optimalZoomRatio)
                                    }
                                }
                            } else if (onAutoDetectLane != null) {
                                val success = onAutoDetectLane(frame, frameWidth, frameHeight, frameStride, alignmentHandedness)
                                if (success) {
                                    currentCalibration?.let {
                                        flX = it.foulLineLeftScreen.x.toFloat()
                                        flY = it.foulLineLeftScreen.y.toFloat()
                                        frX = it.foulLineRightScreen.x.toFloat()
                                        frY = it.foulLineRightScreen.y.toFloat()
                                        alX = it.arrowsLeftScreen.x.toFloat()
                                        alY = it.arrowsLeftScreen.y.toFloat()
                                        arX = it.arrowsRightScreen.x.toFloat()
                                        arY = it.arrowsRightScreen.y.toFloat()
                                    }
                                    autoDetectionStatus = "✓ LANE AUTO-DETECTED & SNAPPED"
                                } else {
                                    autoDetectionStatus = "❌ NO PIN RACK DETECTED - AIM AT PINS"
                                }
                            }
                        } else {
                            autoDetectionStatus = "POINT CAMERA AT LANE TO AUTO-DETECT"
                        }
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Auto Detect",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AUTO-DETECT",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // USBC Reset Preset
                Button(
                    onClick = {
                        val defaultPair = calibrator.createDefaultCalibration(
                            viewWidth = lastViewportWidth,
                            viewHeight = lastViewportHeight,
                            zoomRatio = zoomRatio,
                            anchorMode = anchorMode,
                            alignment = alignmentHandedness
                        )
                        val defaultCal = defaultPair.first
                        flX = defaultCal.foulLineLeftScreen.x.toFloat()
                        flY = defaultCal.foulLineLeftScreen.y.toFloat()
                        frX = defaultCal.foulLineRightScreen.x.toFloat()
                        frY = defaultCal.foulLineRightScreen.y.toFloat()
                        alX = defaultCal.arrowsLeftScreen.x.toFloat()
                        alY = defaultCal.arrowsLeftScreen.y.toFloat()
                        arX = defaultCal.arrowsRightScreen.x.toFloat()
                        arY = defaultCal.arrowsRightScreen.y.toFloat()
                        onCalibrateDefault?.invoke(alignmentHandedness)
                        autoDetectionStatus = "✓ FULL LANE PRESET (${String.format("%.1f", zoomRatio)}x)"
                    },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UsbcGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Preset",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "RESET",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Save and Arm
                Button(
                    onClick = {
                        onSaveCalibration(
                            Point2D(flX.toDouble(), flY.toDouble()),
                            Point2D(frX.toDouble(), frY.toDouble()),
                            Point2D(alX.toDouble(), alY.toDouble()),
                            Point2D(arX.toDouble(), arY.toDouble()),
                            anchorMode
                        )
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "APPLY & ARM",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(UsbcNavyLight)
            .border(1.dp, NeonCyan.copy(alpha = 0.7f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Nudge",
            tint = NeonCyan,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPinHandle(
    label: String,
    center: Offset,
    color: Color,
    isSelected: Boolean
) {
    val outerRadius = if (isSelected) 30f else 20f
    val innerRadius = if (isSelected) 12f else 8f

    drawCircle(
        color = color.copy(alpha = if (isSelected) 0.55f else 0.25f),
        radius = outerRadius,
        center = center
    )
    drawCircle(
        color = color,
        radius = innerRadius,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = center
    )

    val textPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = 28f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText(label, center.x + 24f, center.y + 8f, textPaint)
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}
