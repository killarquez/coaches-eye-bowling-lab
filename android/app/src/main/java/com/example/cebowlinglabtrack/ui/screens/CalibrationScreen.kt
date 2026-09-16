package com.example.cebowlinglabtrack.ui.screens

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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.PathEffect
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
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.theme.DarkBackground
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
import com.example.cebowlinglabtrack.theme.UsbcNavyDark
import com.example.cebowlinglabtrack.theme.UsbcNavyLight
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Interactive 4-Point Camera Perspective Calibration Screen with Live Camera Viewfinder,
 * Direct In-Viewport Zoom Synchronization, Gutter Anchor Geometry, Real-time 10-Pin Verification,
 * and Precision D-Pad Nudge Controls.
 */
@Composable
fun CalibrationScreen(
    currentCalibration: LaneCalibration?,
    zoomRatio: Float = 1.0f,
    autoCenterGuidance: String? = null,
    onZoomChange: (Float) -> Unit = {},
    onSaveCalibration: (Point2D, Point2D, Point2D, Point2D, CalibrationAnchorMode) -> Unit,
    onAutoDetectLaneDetailed: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int) -> AutoLaneDetector.AutoDetectionResult)? = null,
    onAutoDetectLane: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int) -> Boolean)? = null,
    onCalibrateDefault: (() -> Unit)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calibrator = remember { LaneCalibrator() }
    var currentGuidance by remember { mutableStateOf(autoCenterGuidance) }
    LaunchedEffect(autoCenterGuidance) { currentGuidance = autoCenterGuidance }

    var anchorMode by remember {
        mutableStateOf(
            currentCalibration?.anchorMode?.let { modeStr ->
                try { CalibrationAnchorMode.valueOf(modeStr) } catch (e: Exception) { null }
            } ?: CalibrationAnchorMode.GUTTERS_AT_ARROWS
        )
    }

    // 4 Draggable Anchor Pins in Screen Coordinates
    var flX by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.x?.toFloat() ?: 130f) }
    var flY by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.y?.toFloat() ?: 1680f) }

    var frX by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.x?.toFloat() ?: 950f) }
    var frY by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.y?.toFloat() ?: 1680f) }

    var alX by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.x?.toFloat() ?: 300f) }
    var alY by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.y?.toFloat() ?: 1000f) }

    var arX by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.x?.toFloat() ?: 780f) }
    var arY by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.y?.toFloat() ?: 1000f) }

    var selectedPinIndex by remember { mutableStateOf<Int?>(null) }
    var nudgeStepPx by remember { mutableStateOf(2f) } // 2px fine, 10px coarse
    var autoDetectionStatus by remember { mutableStateOf<String?>(null) }

    // Store latest live camera frame for 1-click Auto-Detect
    var latestFrameBytes by remember { mutableStateOf<ByteArray?>(null) }
    var frameWidth by remember { mutableStateOf(0) }
    var frameHeight by remember { mutableStateOf(0) }
    var frameStride by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LANE CALIBRATION",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "ALIGN 4 ANCHORS TO PHYSICAL GUTTERS & FOUL LINE",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // In-Screen Hardware Zoom Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom",
                    tint = UsbcGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CAMERA ZOOM:",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus button
                IconButton(
                    onClick = { onZoomChange((zoomRatio - 0.2f).coerceAtLeast(1.0f)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = if (zoomRatio > 1.0f) TextPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Zoom Ratio Chips
                listOf(1.0f, 1.8f, 2.2f, 2.5f, 3.0f).forEach { presetZoom ->
                    val isSelected = kotlin.math.abs(zoomRatio - presetZoom) < 0.15f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) UsbcGold else DarkSurfaceVariant)
                            .clickable { onZoomChange(presetZoom) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${presetZoom}x",
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }

                // Plus button
                IconButton(
                    onClick = { onZoomChange((zoomRatio + 0.2f).coerceAtMost(3.5f)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = if (zoomRatio < 3.5f) TextPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Anchor Mode Selection Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalibrationAnchorMode.values().forEach { mode ->
                val isSelected = anchorMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) UsbcNavyLight else DarkSurface)
                        .border(1.dp, if (isSelected) NeonCyan else DarkCardBorder, RoundedCornerShape(8.dp))
                        .clickable { anchorMode = mode }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Interactive Calibration Viewport with Live Camera Background
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()

            // Initialize default anchors to actual viewport constraints if uncalibrated or off-screen
            var sizeInitialized by remember { mutableStateOf(false) }
            LaunchedEffect(viewWidth, viewHeight) {
                if (!sizeInitialized && viewWidth > 50f && viewHeight > 50f) {
                    val isOffScreen = flY < 100f || flY > viewHeight * 1.2f || frX > viewWidth * 1.2f
                    val isUncalibrated = currentCalibration == null || isOffScreen
                    if (isUncalibrated) {
                        val (defaultCalib, _) = calibrator.createDefaultCalibration(viewWidth, viewHeight, zoomRatio, anchorMode)
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

            // Live Camera Feed running behind the calibration handles with active zoom
            CameraPreviewView(
                onFrameAvailable = { bytes, w, h, s, _ ->
                    latestFrameBytes = bytes
                    frameWidth = w
                    frameHeight = h
                    frameStride = s
                },
                zoomRatio = zoomRatio,
                modifier = Modifier.fillMaxSize()
            )

            // Touch interaction layer & AR Guide Rendering
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
                                selectedPinIndex = if (distances[minIdx] < 120f) minIdx else null
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
                    // Compute live real-time homography to project 10-pin deck and gutter chevrons
                    val tempResult = calibrator.calibrate(
                        foulLineLeft = Point2D(flX.toDouble(), flY.toDouble()),
                        foulLineRight = Point2D(frX.toDouble(), frY.toDouble()),
                        arrowsLeft = Point2D(alX.toDouble(), alY.toDouble()),
                        arrowsRight = Point2D(arX.toDouble(), arY.toDouble()),
                        anchorMode = anchorMode,
                        calibrationZoomRatio = zoomRatio
                    )
                    val liveH = tempResult?.second

                    // Live AR projected 10 pins for immediate verification against physical pins
                    liveH?.let { hMatrix ->
                        PinDeckDetector.STANDARD_PIN_COORDS.forEachIndexed { idx, pinCoord ->
                            val pinPos = hMatrix.forward(pinCoord)
                            if (pinPos.x in 0.0..size.width.toDouble() && pinPos.y in 0.0..size.height.toDouble()) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = 7f,
                                    center = Offset(pinPos.x.toFloat(), pinPos.y.toFloat())
                                )
                                drawCircle(
                                    color = NeonStrikeGreen,
                                    radius = 3.5f,
                                    center = Offset(pinPos.x.toFloat(), pinPos.y.toFloat())
                                )
                            }
                        }

                        // Project forward gutter chevrons
                        listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0).forEach { d ->
                            val leftTip = hMatrix.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.0, d + 1.2))
                            val leftW1 = hMatrix.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(0.4, d))
                            val leftW2 = hMatrix.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.6, d))
                            drawLine(GutterChevronCyan.copy(alpha = 0.7f), Offset(leftW1.x.toFloat(), leftW1.y.toFloat()), Offset(leftTip.x.toFloat(), leftTip.y.toFloat()), 2.5f)
                            drawLine(GutterChevronCyan.copy(alpha = 0.7f), Offset(leftW2.x.toFloat(), leftW2.y.toFloat()), Offset(leftTip.x.toFloat(), leftTip.y.toFloat()), 2.5f)

                            val rightTip = hMatrix.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.0, d + 1.2))
                            val rightW1 = hMatrix.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(38.4, d))
                            val rightW2 = hMatrix.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.6, d))
                            drawLine(GutterChevronCyan.copy(alpha = 0.7f), Offset(rightW1.x.toFloat(), rightW1.y.toFloat()), Offset(rightTip.x.toFloat(), rightTip.y.toFloat()), 2.5f)
                            drawLine(GutterChevronCyan.copy(alpha = 0.7f), Offset(rightW2.x.toFloat(), rightW2.y.toFloat()), Offset(rightTip.x.toFloat(), rightTip.y.toFloat()), 2.5f)
                        }
                    }

                    // Draw connecting quadrilateral (Lane boundaries)
                    // Foul Line (Bottom)
                    drawLine(NeonCyan, Offset(flX, flY), Offset(frX, frY), 4f)
                    // Left Gutter
                    drawLine(NeonCyan, Offset(flX, flY), Offset(alX, alY), 3f)
                    // Right Gutter
                    drawLine(NeonCyan, Offset(frX, frY), Offset(arX, arY), 3f)
                    // Top Boundary (Arrows or Pin Deck)
                    drawLine(ElectricAmber, Offset(alX, alY), Offset(arX, arY), 3f)

                    // Draw center guide line
                    val midFoul = Offset((flX + frX) / 2f, (flY + frY) / 2f)
                    val midArrows = Offset((alX + arX) / 2f, (alY + arY) / 2f)
                    drawLine(
                        Color.White.copy(alpha = 0.6f),
                        midFoul,
                        midArrows,
                        2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )

                    // Labels for top anchors based on mode
                    val topLabelL = when (anchorMode) {
                        CalibrationAnchorMode.GUTTERS_AT_ARROWS -> "3. LEFT GUTTER (15FT)"
                        CalibrationAnchorMode.ARROW_MARKERS -> "3. ARROW L (B5)"
                        CalibrationAnchorMode.PIN_DECK -> "3. LEFT DECK (60FT)"
                    }
                    val topLabelR = when (anchorMode) {
                        CalibrationAnchorMode.GUTTERS_AT_ARROWS -> "4. RIGHT GUTTER (15FT)"
                        CalibrationAnchorMode.ARROW_MARKERS -> "4. ARROW R (B35)"
                        CalibrationAnchorMode.PIN_DECK -> "4. RIGHT DECK (60FT)"
                    }

                    // Draw 4 Anchor Handles
                    drawPinHandle("1. FOUL L (B1)", Offset(flX, flY), NeonStrikeGreen, selectedPinIndex == 0)
                    drawPinHandle("2. FOUL R (B39)", Offset(frX, frY), NeonStrikeGreen, selectedPinIndex == 1)
                    drawPinHandle(topLabelL, Offset(alX, alY), ElectricAmber, selectedPinIndex == 2)
                    drawPinHandle(topLabelR, Offset(arX, arY), ElectricAmber, selectedPinIndex == 3)
                }
            }

            // Floating Precision Nudge Pad (Appears when any anchor is selected)
            selectedPinIndex?.let { pinIdx ->
                val anchorName = when (pinIdx) {
                    0 -> "1. FOUL LEFT (B1)"
                    1 -> "2. FOUL RIGHT (B39)"
                    2 -> if (anchorMode == CalibrationAnchorMode.GUTTERS_AT_ARROWS) "3. LEFT GUTTER (15FT)" else "3. TOP LEFT"
                    3 -> if (anchorMode == CalibrationAnchorMode.GUTTERS_AT_ARROWS) "4. RIGHT GUTTER (15FT)" else "4. TOP RIGHT"
                    else -> "ANCHOR"
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface.copy(alpha = 0.95f))
                        .border(1.dp, UsbcGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Text(
                                text = "NUDGE: $anchorName",
                                color = UsbcGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            // Step size toggle (2px vs 10px)
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
                                    fontSize = 9.sp,
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
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (nudgeStepPx == 10f) NeonCyan else Color.Transparent)
                                        .clickable { nudgeStepPx = 10f }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
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

                        // D-Pad Directional Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left
                            NudgeButton(icon = Icons.Default.ArrowBack) {
                                when (pinIdx) {
                                    0 -> flX -= nudgeStepPx
                                    1 -> frX -= nudgeStepPx
                                    2 -> alX -= nudgeStepPx
                                    3 -> arX -= nudgeStepPx
                                }
                            }
                            // Up
                            NudgeButton(icon = Icons.Default.ArrowUpward) {
                                when (pinIdx) {
                                    0 -> flY -= nudgeStepPx
                                    1 -> frY -= nudgeStepPx
                                    2 -> alY -= nudgeStepPx
                                    3 -> arY -= nudgeStepPx
                                }
                            }
                            // Down
                            NudgeButton(icon = Icons.Default.ArrowDownward) {
                                when (pinIdx) {
                                    0 -> flY += nudgeStepPx
                                    1 -> frY += nudgeStepPx
                                    2 -> alY += nudgeStepPx
                                    3 -> arY += nudgeStepPx
                                }
                            }
                            // Right
                            NudgeButton(icon = Icons.Default.ArrowForward) {
                                when (pinIdx) {
                                    0 -> flX += nudgeStepPx
                                    1 -> frX += nudgeStepPx
                                    2 -> alX += nudgeStepPx
                                    3 -> arX += nudgeStepPx
                                }
                            }
                        }
                    }
                }
            }

            // Auto-Center & Lane Alignment Guidance Chip (Top Center)
            val activeGuidance = currentGuidance ?: autoCenterGuidance
            activeGuidance?.let { guidance ->
                val isCentered = guidance.contains("CENTERED")
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isCentered) UsbcNavyDark.copy(alpha = 0.94f) else DarkSurface.copy(alpha = 0.94f))
                        .border(1.5.dp, if (isCentered) NeonStrikeGreen else ElectricAmber, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = guidance,
                        color = if (isCentered) NeonStrikeGreen else ElectricAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Auto-detect status banner if triggered (Bottom of Viewfinder)
            autoDetectionStatus?.let { status ->
                val isSuccess = status.startsWith("✓")
                val isError = status.contains("NO PIN RACK") || status.contains("UNCLEAR") || status.startsWith("❌")
                val bannerBorder = when {
                    isSuccess -> NeonStrikeGreen
                    isError -> PowerCoral
                    else -> NeonCyan
                }
                val bannerText = when {
                    isSuccess -> NeonStrikeGreen
                    isError -> PowerCoral
                    else -> NeonCyan
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 75.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface.copy(alpha = 0.95f))
                        .border(1.dp, bannerBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = status,
                        color = bannerText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CV Auto-Detect & USBC Standard Preset Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1-Click Auto-Detect CV Button
            Button(
                onClick = {
                    val frame = latestFrameBytes
                    if (frame != null) {
                        if (onAutoDetectLaneDetailed != null) {
                            val res = onAutoDetectLaneDetailed(frame, frameWidth, frameHeight, frameStride)
                            autoDetectionStatus = res.statusMessage
                            currentGuidance = res.autoCenterGuidance
                            if (res.isSuccess) {
                                flX = res.foulLineLeft.x.toFloat()
                                flY = res.foulLineLeft.y.toFloat()
                                frX = res.foulLineRight.x.toFloat()
                                frY = res.foulLineRight.y.toFloat()
                                alX = res.arrowsLeft.x.toFloat()
                                alY = res.arrowsLeft.y.toFloat()
                                arX = res.arrowsRight.x.toFloat()
                                arY = res.arrowsRight.y.toFloat()
                                if (abs(res.optimalZoomRatio - zoomRatio) > 0.15f) {
                                    onZoomChange(res.optimalZoomRatio)
                                }
                            }
                        } else if (onAutoDetectLane != null) {
                            val success = onAutoDetectLane(frame, frameWidth, frameHeight, frameStride)
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
                    .weight(1.3f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Auto Detect",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AUTO-DETECT CV",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // 1-Click USBC Standard Preset Button
            Button(
                onClick = {
                    val defaultPair = calibrator.createDefaultCalibration(viewWidth = 1080f, viewHeight = 1920f, zoomRatio = zoomRatio, anchorMode = anchorMode)
                    val defaultCal = defaultPair.first
                    flX = defaultCal.foulLineLeftScreen.x.toFloat()
                    flY = defaultCal.foulLineLeftScreen.y.toFloat()
                    frX = defaultCal.foulLineRightScreen.x.toFloat()
                    frY = defaultCal.foulLineRightScreen.y.toFloat()
                    alX = defaultCal.arrowsLeftScreen.x.toFloat()
                    alY = defaultCal.arrowsLeftScreen.y.toFloat()
                    arX = defaultCal.arrowsRightScreen.x.toFloat()
                    arY = defaultCal.arrowsRightScreen.y.toFloat()
                    onCalibrateDefault?.invoke()
                    autoDetectionStatus = "✓ USBC PRESET APPLIED (${String.format("%.1f", zoomRatio)}x)"
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UsbcGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "USBC Preset",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "USBC PRESET",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Action Buttons: Cancel and Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("CANCEL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

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
                    .weight(1.5f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "APPLY & ARM",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
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
            .size(34.dp)
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
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPinHandle(
    label: String,
    center: Offset,
    color: Color,
    isSelected: Boolean
) {
    val outerRadius = if (isSelected) 32f else 22f
    val innerRadius = if (isSelected) 12f else 8f

    drawCircle(
        color = color.copy(alpha = if (isSelected) 0.65f else 0.35f),
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
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}
