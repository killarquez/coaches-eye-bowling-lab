package com.example.cebowlinglabtrack.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.cebowlinglabtrack.domain.calibration.AutonomousLaneRecognizer
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
enum class CalibrationMode {
    GUIDED_WIZARD,
    FREEFORM_MANUAL
}

enum class GuidedStep(val stepNumber: Int, val title: String) {
    STEP1_TRIPOD(1, "TRIPOD"),
    STEP2_PIN_DECK(2, "PIN DECK"),
    STEP3_ARROWS(3, "ARROWS"),
    STEP4_FOUL_LINE(4, "FOUL LINE")
}

@Composable
fun CalibrationScreen(
    currentCalibration: LaneCalibration?,
    zoomRatio: Float = 1.0f,
    autoCenterGuidance: String? = null,
    bowlerHandedness: Handedness = Handedness.RIGHT,
    tripodStatus: com.example.cebowlinglabtrack.camera.TripodAngleAdvisor.TripodStatus = com.example.cebowlinglabtrack.camera.TripodAngleAdvisor.TripodStatus(),
    onZoomChange: (Float) -> Unit = {},
    onSaveCalibration: (Point2D, Point2D, Point2D, Point2D, CalibrationAnchorMode) -> Unit,
    onAutoDetectLaneDetailed: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int, alignment: Handedness, anchorMode: CalibrationAnchorMode) -> AutoLaneDetector.AutoDetectionResult)? = null,
    onAutoDetectLane: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int, alignment: Handedness) -> Boolean)? = null,
    onCalibrateDefault: ((alignment: Handedness) -> Unit)? = null,
    onViewportSizeChanged: ((Float, Float) -> Unit)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calibrator = remember { LaneCalibrator() }
    var alignmentHandedness by remember { mutableStateOf(bowlerHandedness) }
    LaunchedEffect(bowlerHandedness) { alignmentHandedness = bowlerHandedness }
    var currentGuidance by remember { mutableStateOf(autoCenterGuidance) }
    LaunchedEffect(autoCenterGuidance) { currentGuidance = autoCenterGuidance }

    var anchorMode by remember { mutableStateOf(CalibrationAnchorMode.PIN_DECK) }

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

    val laneRecognizer = remember { AutonomousLaneRecognizer(calibrator) }
    var calibrationMode by remember { mutableStateOf(CalibrationMode.GUIDED_WIZARD) }
    var guidedStep by remember { mutableStateOf(GuidedStep.STEP1_TRIPOD) }
    var detectedPinRacks by remember { mutableStateOf<List<AutonomousLaneRecognizer.PinRackCandidate>>(emptyList()) }
    var selectedPinRack by remember { mutableStateOf<AutonomousLaneRecognizer.PinRackCandidate?>(null) }
    var pinDeckCertainty by remember { mutableStateOf(0.0) }
    var arrowsCertainty by remember { mutableStateOf(0.0) }

    // Camera frame buffers for 1-click Auto-Detect
    var latestFrameBytes by remember { mutableStateOf<ByteArray?>(null) }
    var frameWidth by remember { mutableStateOf(0) }
    var frameHeight by remember { mutableStateOf(0) }
    var frameStride by remember { mutableStateOf(0) }
    var lastViewportWidth by remember { mutableStateOf(1080f) }
    var lastViewportHeight by remember { mutableStateOf(2340f) }

    LaunchedEffect(latestFrameBytes, guidedStep, calibrationMode) {
        val bytes = latestFrameBytes ?: return@LaunchedEffect
        if (calibrationMode == CalibrationMode.GUIDED_WIZARD && guidedStep == GuidedStep.STEP2_PIN_DECK && frameWidth > 0 && frameHeight > 0) {
            val s = if (frameStride > 0) frameStride else frameWidth
            val racks = laneRecognizer.findAllPinRacks(bytes, frameWidth, frameHeight, s, zoomRatio)
            if (racks.isNotEmpty()) {
                detectedPinRacks = racks
            }
        }
    }

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
            LaunchedEffect(viewWidth, viewHeight) {
                onViewportSizeChanged?.invoke(viewWidth, viewHeight)
            }
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
                .pointerInput(calibrationMode, guidedStep, detectedPinRacks, latestFrameBytes) {
                    if (calibrationMode == CalibrationMode.GUIDED_WIZARD && guidedStep != GuidedStep.STEP4_FOUL_LINE) {
                        detectTapGestures { offset ->
                            val touchX = offset.x.toDouble()
                            val touchY = offset.y.toDouble()
                            val fw = if (frameWidth > 0) frameWidth else lastViewportWidth.toInt()
                            val fh = if (frameHeight > 0) frameHeight else lastViewportHeight.toInt()
                            val fs = if (frameStride > 0) frameStride else fw

                            when (guidedStep) {
                                GuidedStep.STEP1_TRIPOD -> {
                                    val rollCertainty = (1.0 - abs(tripodStatus.rollDeg) / 2.5).coerceIn(0.0, 1.0)
                                    val pitchCertainty = (1.0 - abs(tripodStatus.pitchDeg - (-8.0)) / 5.0).coerceIn(0.0, 1.0)
                                    val tripodCertainty = 0.50 * rollCertainty + 0.50 * pitchCertainty
                                    if (tripodCertainty < 0.80 && !tripodStatus.isLevel) {
                                        autoDetectionStatus = "⚠️ TRIPOD NOT LEVEL (${(tripodCertainty * 100).toInt()}% < 80%) - Level phone roll & pitch"
                                    } else {
                                        autoDetectionStatus = "✓ TRIPOD LEVEL VERIFIED (${(tripodCertainty * 100).toInt()}%)"
                                        guidedStep = GuidedStep.STEP2_PIN_DECK
                                    }
                                }
                                GuidedStep.STEP2_PIN_DECK -> {
                                    val tappedPoint = Point2D(touchX, touchY)
                                    // Search for nearest detected rack candidate
                                    val nearestCandidate = detectedPinRacks.minByOrNull {
                                        dist(touchX.toFloat(), touchY.toFloat(), it.centerX.toFloat(), it.bottomY.toFloat())
                                    }
                                    val (isVerified, cert) = laneRecognizer.verifyPinDeckAt(
                                        tappedPoint = tappedPoint,
                                        candidate = nearestCandidate,
                                        imageBytes = latestFrameBytes,
                                        width = fw,
                                        height = fh,
                                        stride = fs
                                    )

                                    if (!isVerified || nearestCandidate == null) {
                                        pinDeckCertainty = cert
                                        autoDetectionStatus = "❌ NO PIN CLUSTER VERIFIED (${(cert * 100).toInt()}% < 80%) - Aim camera down-lane at pins"
                                    } else {
                                        pinDeckCertainty = cert
                                        selectedPinRack = nearestCandidate
                                        val safeZoom = laneRecognizer.computeSafeZoomForRack(nearestCandidate, fw, fh)
                                        if (abs(safeZoom - zoomRatio) > 0.05f) {
                                            onZoomChange(safeZoom)
                                        }

                                        val halfW = nearestCandidate.widthPx / 2.0
                                        alX = (nearestCandidate.centerX - halfW).toFloat()
                                        arX = (nearestCandidate.centerX + halfW).toFloat()
                                        alY = nearestCandidate.bottomY.toFloat()
                                        arY = nearestCandidate.bottomY.toFloat()

                                        autoDetectionStatus = "✓ PIN DECK VERIFIED (${(cert * 100).toInt()}% Certainty)"
                                        guidedStep = GuidedStep.STEP3_ARROWS
                                    }
                                }
                                GuidedStep.STEP3_ARROWS -> {
                                    val rack = selectedPinRack
                                    if (rack == null) {
                                        autoDetectionStatus = "❌ PIN DECK NOT LOCKED - Return to Step 2"
                                        guidedStep = GuidedStep.STEP2_PIN_DECK
                                        return@detectTapGestures
                                    }

                                    val tappedPoint = Point2D(touchX, touchY)
                                    val (isVerified, cert) = laneRecognizer.verifyArrowsAt(
                                        tappedPoint = tappedPoint,
                                        pinRack = rack,
                                        imageBytes = latestFrameBytes,
                                        width = fw,
                                        height = fh,
                                        stride = fs
                                    )

                                    if (!isVerified) {
                                        arrowsCertainty = cert
                                        autoDetectionStatus = "❌ ARROWS NOT VERIFIED (${(cert * 100).toInt()}% < 80%) - Tap near the 15-ft triangular arrow chevrons"
                                    } else {
                                        arrowsCertainty = cert
                                        val snapped = laneRecognizer.snapArrowsToCenterline(tappedPoint, rack, fw, fh)
                                        val (foulL, foulR) = laneRecognizer.projectFoulCorners(rack, snapped, fw, fh, alignmentHandedness)

                                        flX = foulL.x.toFloat()
                                        flY = foulL.y.toFloat()
                                        frX = foulR.x.toFloat()
                                        frY = foulR.y.toFloat()

                                        autoDetectionStatus = "✓ ARROWS VERIFIED (${(cert * 100).toInt()}% Certainty)"
                                        guidedStep = GuidedStep.STEP4_FOUL_LINE
                                    }
                                }
                                GuidedStep.STEP4_FOUL_LINE -> {}
                            }
                        }
                    } else {
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

                    // 5. 10-Pin Deck Corner Brackets [  ] at 60 ft
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

                // 6. Guided Wizard Canvas Overlays
                if (calibrationMode == CalibrationMode.GUIDED_WIZARD) {
                    if (guidedStep == GuidedStep.STEP2_PIN_DECK) {
                        // Render glowing selectable target brackets over all candidate 7-pin clusters
                        for ((idx, rack) in detectedPinRacks.withIndex()) {
                            val rx = rack.centerX.toFloat()
                            val ry = rack.bottomY.toFloat()
                            val rw = rack.widthPx.toFloat()
                            val rh = (rack.bottomY - rack.topY).toFloat().coerceAtLeast(30f)
                            val isSelected = selectedPinRack?.let { kotlin.math.abs(it.centerX - rack.centerX) < 20.0 } ?: false
                            val chipColor = if (isSelected) NeonStrikeGreen else NeonCyan

                            val left = rx - rw / 2f
                            val right = rx + rw / 2f
                            val top = ry - rh
                            val bottom = ry
                            val bLen = 14f

                            drawLine(chipColor, Offset(left, top), Offset(left + bLen, top), 3f)
                            drawLine(chipColor, Offset(left, top), Offset(left, top + bLen), 3f)
                            drawLine(chipColor, Offset(right, top), Offset(right - bLen, top), 3f)
                            drawLine(chipColor, Offset(right, top), Offset(right, top + bLen), 3f)
                            drawLine(chipColor, Offset(left, bottom), Offset(left + bLen, bottom), 3f)
                            drawLine(chipColor, Offset(left, bottom), Offset(left, bottom - bLen), 3f)
                            drawLine(chipColor, Offset(right, bottom), Offset(right - bLen, bottom), 3f)
                            drawLine(chipColor, Offset(right, bottom), Offset(right, bottom - bLen), 3f)

                            drawContext.canvas.nativeCanvas.drawText(
                                "TAP LANE ${idx + 1}",
                                rx,
                                top - 12f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 28f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
                                }
                            )
                        }
                    } else if (guidedStep == GuidedStep.STEP3_ARROWS) {
                        val rackX = (alX + arX) / 2f
                        val rackY = alY
                        // Draw centerline vector towards arrows
                        drawLine(
                            color = ElectricAmber.copy(alpha = 0.8f),
                            start = Offset(rackX, rackY),
                            end = Offset(rackX, viewHeight * 0.70f),
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                        // Pulsing target circle at 15ft
                        drawCircle(
                            color = ElectricAmber.copy(alpha = 0.35f),
                            radius = 45f,
                            center = Offset(rackX, viewHeight * 0.52f)
                        )
                        drawCircle(
                            color = ElectricAmber,
                            radius = 6f,
                            center = Offset(rackX, viewHeight * 0.52f)
                        )
                    }
                }

                // 7. Draw Interactive Corner Handles with Labels
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

                if (calibrationMode == CalibrationMode.FREEFORM_MANUAL || guidedStep == GuidedStep.STEP4_FOUL_LINE) {
                    drawPinHandle("FL-L (B39)", Offset(flX, flY), if (alignmentHandedness == Handedness.LEFT) UsbcGold else NeonStrikeGreen, selectedPinIndex == 0)
                    drawPinHandle("FL-R (B1)", Offset(frX, frY), if (alignmentHandedness == Handedness.RIGHT) UsbcGold else NeonStrikeGreen, selectedPinIndex == 1)
                }
                if (calibrationMode == CalibrationMode.FREEFORM_MANUAL) {
                    drawPinHandle(topLabelL, Offset(alX, alY), if (alignmentHandedness == Handedness.LEFT) UsbcGold else ElectricAmber, selectedPinIndex == 2)
                    drawPinHandle(topLabelR, Offset(arX, arY), if (alignmentHandedness == Handedness.RIGHT) UsbcGold else ElectricAmber, selectedPinIndex == 3)
                }
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
                            text = if (calibrationMode == CalibrationMode.GUIDED_WIZARD) "GUIDED SETUP WIZARD" else "MANUAL CALIBRATION",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (calibrationMode == CalibrationMode.GUIDED_WIZARD) "STEP ${guidedStep.stepNumber} OF 4: ${guidedStep.title}" else "DRAG 4 CORNERS: FOUL LINE TO PIN DECK (0-60 FT)",
                            color = if (calibrationMode == CalibrationMode.GUIDED_WIZARD) NeonCyan else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            calibrationMode = if (calibrationMode == CalibrationMode.GUIDED_WIZARD) {
                                CalibrationMode.FREEFORM_MANUAL
                            } else {
                                guidedStep = GuidedStep.STEP1_TRIPOD
                                CalibrationMode.GUIDED_WIZARD
                            }
                        },
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (calibrationMode == CalibrationMode.GUIDED_WIZARD) NeonStrikeGreen.copy(alpha = 0.2f) else UsbcGold.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (calibrationMode == CalibrationMode.GUIDED_WIZARD) NeonStrikeGreen else UsbcGold),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (calibrationMode == CalibrationMode.GUIDED_WIZARD) "MANUAL NUDGE" else "GUIDED WIZARD",
                            color = if (calibrationMode == CalibrationMode.GUIDED_WIZARD) NeonStrikeGreen else UsbcGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(UsbcNavyLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", zoomRatio)}x",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (calibrationMode == CalibrationMode.GUIDED_WIZARD) {
                // Step Progress Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface.copy(alpha = 0.95f))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GuidedStep.values().forEach { step ->
                        val isCurrent = guidedStep == step
                        val isPassed = guidedStep.stepNumber > step.stepNumber
                        val stepBg = when {
                            isCurrent -> NeonCyan.copy(alpha = 0.25f)
                            isPassed -> NeonStrikeGreen.copy(alpha = 0.25f)
                            else -> Color.Transparent
                        }
                        val stepBorder = when {
                            isCurrent -> NeonCyan
                            isPassed -> NeonStrikeGreen
                            else -> Color.DarkGray
                        }
                        val stepTextCol = when {
                            isCurrent -> NeonCyan
                            isPassed -> NeonStrikeGreen
                            else -> TextMuted
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(stepBg)
                                .border(1.dp, stepBorder, RoundedCornerShape(6.dp))
                                .clickable { guidedStep = step }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${step.stepNumber}. ${step.title}",
                                color = stepTextCol,
                                fontSize = 9.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Instructions Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = when (guidedStep) {
                                GuidedStep.STEP1_TRIPOD -> "STEP 1: ALIGN TRIPOD BEHIND ${if (alignmentHandedness == Handedness.RIGHT) "RIGHT" else "LEFT"} GUTTER"
                                GuidedStep.STEP2_PIN_DECK -> "STEP 2: TAP YOUR LANE'S PIN DECK"
                                GuidedStep.STEP3_ARROWS -> "STEP 3: TAP 15-FT ARROWS ON YOUR LANE"
                                GuidedStep.STEP4_FOUL_LINE -> "STEP 4: CONFIRM FOUL LINE CORNERS"
                            },
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when (guidedStep) {
                                GuidedStep.STEP1_TRIPOD -> currentGuidance ?: "Place tripod 5-8 ft behind approach. Level roll and pitch phone -8° down toward pins."
                                GuidedStep.STEP2_PIN_DECK -> "Tap on your lane's pins to lock the deck. Camera will automatically apply safe zoom."
                                GuidedStep.STEP3_ARROWS -> "Tap near the center arrow (Board 20). Centerline vector will snap to wood markings."
                                GuidedStep.STEP4_FOUL_LINE -> "Verify that the handles touch the wood edges at the foul line, then tap Lock Lane."
                            },
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
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
            if (calibrationMode == CalibrationMode.GUIDED_WIZARD) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (guidedStep != GuidedStep.STEP1_TRIPOD) {
                        Button(
                            onClick = {
                                guidedStep = when (guidedStep) {
                                    GuidedStep.STEP2_PIN_DECK -> GuidedStep.STEP1_TRIPOD
                                    GuidedStep.STEP3_ARROWS -> GuidedStep.STEP2_PIN_DECK
                                    GuidedStep.STEP4_FOUL_LINE -> GuidedStep.STEP3_ARROWS
                                    else -> GuidedStep.STEP1_TRIPOD
                                }
                            },
                            modifier = Modifier
                                .weight(0.7f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "BACK",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    when (guidedStep) {
                        GuidedStep.STEP1_TRIPOD -> {
                            val rollCertainty = (1.0 - abs(tripodStatus.rollDeg) / 2.5).coerceIn(0.0, 1.0)
                            val pitchCertainty = (1.0 - abs(tripodStatus.pitchDeg - (-8.0)) / 5.0).coerceIn(0.0, 1.0)
                            val tripodCertainty = 0.50 * rollCertainty + 0.50 * pitchCertainty
                            val isTripodVerified = tripodCertainty >= 0.80 || tripodStatus.isLevel

                            Button(
                                onClick = {
                                    if (isTripodVerified) {
                                        autoDetectionStatus = "✓ TRIPOD LEVEL VERIFIED (${(tripodCertainty * 100).toInt()}%)"
                                        guidedStep = GuidedStep.STEP2_PIN_DECK
                                    } else {
                                        autoDetectionStatus = "⚠️ TRIPOD NOT LEVEL (${(tripodCertainty * 100).toInt()}% < 80%) - Level phone roll & pitch"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isTripodVerified) NeonCyan else DarkSurfaceVariant),
                                border = if (isTripodVerified) null else androidx.compose.foundation.BorderStroke(1.dp, PowerCoral),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (isTripodVerified) "NEXT: SELECT PIN DECK →" else "ALIGN TRIPOD (${(tripodCertainty * 100).toInt()}%)",
                                    color = if (isTripodVerified) Color.Black else TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        GuidedStep.STEP2_PIN_DECK -> {
                            val isDeckVerified = selectedPinRack != null && pinDeckCertainty >= 0.80
                            Button(
                                onClick = {
                                    if (selectedPinRack != null && pinDeckCertainty >= 0.80) {
                                        guidedStep = GuidedStep.STEP3_ARROWS
                                    } else if (detectedPinRacks.isNotEmpty()) {
                                        val rack = detectedPinRacks.maxByOrNull { it.confidence }
                                        if (rack != null && rack.confidence >= 0.80) {
                                            selectedPinRack = rack
                                            pinDeckCertainty = rack.confidence
                                            val fw = if (frameWidth > 0) frameWidth else lastViewportWidth.toInt()
                                            val fh = if (frameHeight > 0) frameHeight else lastViewportHeight.toInt()
                                            val safeZoom = laneRecognizer.computeSafeZoomForRack(rack, fw, fh)
                                            if (abs(safeZoom - zoomRatio) > 0.05f) {
                                                onZoomChange(safeZoom)
                                            }
                                            val halfW = rack.widthPx / 2.0
                                            alX = (rack.centerX - halfW).toFloat()
                                            arX = (rack.centerX + halfW).toFloat()
                                            alY = rack.bottomY.toFloat()
                                            arY = rack.bottomY.toFloat()
                                            autoDetectionStatus = "✓ PIN DECK VERIFIED (${(rack.confidence * 100).toInt()}%)"
                                            guidedStep = GuidedStep.STEP3_ARROWS
                                        } else {
                                            val cert = rack?.confidence ?: 0.20
                                            autoDetectionStatus = "❌ NO PIN CLUSTER VERIFIED (${(cert * 100).toInt()}% < 80%) - Aim camera at pins"
                                        }
                                    } else {
                                        autoDetectionStatus = "❌ NO PIN CLUSTER DETECTED (Certainty < 80%) - Aim camera at pins"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDeckVerified) NeonCyan else DarkSurfaceVariant),
                                border = if (isDeckVerified) null else androidx.compose.foundation.BorderStroke(1.dp, PowerCoral),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Select Pins",
                                    tint = if (isDeckVerified) Color.Black else TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isDeckVerified) "NEXT: TAP ARROWS →" else "AIM AT PINS (VERIFY ≥ 80%)",
                                    color = if (isDeckVerified) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        GuidedStep.STEP3_ARROWS -> {
                            val isArrowsVerified = arrowsCertainty >= 0.80
                            Button(
                                onClick = {
                                    if (isArrowsVerified) {
                                        guidedStep = GuidedStep.STEP4_FOUL_LINE
                                    } else {
                                        autoDetectionStatus = "❌ ARROWS NOT VERIFIED (${(arrowsCertainty * 100).toInt()}% < 80%) - Tap near 15-ft arrows"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isArrowsVerified) UsbcGold else DarkSurfaceVariant),
                                border = if (isArrowsVerified) null else androidx.compose.foundation.BorderStroke(1.dp, PowerCoral),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Next",
                                    tint = if (isArrowsVerified) Color.Black else TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArrowsVerified) "NEXT: FOUL LINE →" else "TAP 15-FT ARROWS (VERIFY ≥ 80%)",
                                    color = if (isArrowsVerified) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        GuidedStep.STEP4_FOUL_LINE -> {
                            val (isGeoValid, geoCertainty) = laneRecognizer.verifyLaneGeometry(
                                flL = Point2D(flX.toDouble(), flY.toDouble()),
                                flR = Point2D(frX.toDouble(), frY.toDouble()),
                                alL = Point2D(alX.toDouble(), alY.toDouble()),
                                alR = Point2D(arX.toDouble(), arY.toDouble()),
                                width = lastViewportWidth.toDouble(),
                                height = lastViewportHeight.toDouble(),
                                anchorMode = anchorMode
                            )

                            Button(
                                onClick = {
                                    if (isGeoValid) {
                                        onSaveCalibration(
                                            Point2D(flX.toDouble(), flY.toDouble()),
                                            Point2D(frX.toDouble(), frY.toDouble()),
                                            Point2D(alX.toDouble(), alY.toDouble()),
                                            Point2D(arX.toDouble(), arY.toDouble()),
                                            anchorMode
                                        )
                                    } else {
                                        autoDetectionStatus = "❌ CANNOT LOCK: Lane geometry certainty is ${(geoCertainty * 100).toInt()}% (< 80%) - Adjust corners"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isGeoValid) NeonStrikeGreen else DarkSurfaceVariant
                                ),
                                border = if (isGeoValid) null else androidx.compose.foundation.BorderStroke(1.dp, PowerCoral),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Lock and Start",
                                    tint = if (isGeoValid) Color.Black else PowerCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isGeoValid) "LOCK LANE (${(geoCertainty * 100).toInt()}%) ✓" else "ADJUST CORNERS (${(geoCertainty * 100).toInt()}% < 80%)",
                                    color = if (isGeoValid) Color.Black else PowerCoral,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            } else {
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
