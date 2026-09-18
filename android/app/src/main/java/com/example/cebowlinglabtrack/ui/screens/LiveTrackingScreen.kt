package com.example.cebowlinglabtrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.cebowlinglabtrack.R
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cebowlinglabtrack.camera.CameraPreviewView
import com.example.cebowlinglabtrack.domain.calibration.ProjectedLaneGuides
import com.example.cebowlinglabtrack.domain.ml.PinDeckDetector
import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.model.VisualTargetLine
import com.example.cebowlinglabtrack.domain.tracking.TrackingState
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkCardBorderGold
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.GutterChevronCyan
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.TargetLineGold
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import com.example.cebowlinglabtrack.theme.UsbcGold
import com.example.cebowlinglabtrack.theme.UsbcGoldLight
import com.example.cebowlinglabtrack.theme.UsbcNavy
import com.example.cebowlinglabtrack.theme.UsbcNavyDark
import com.example.cebowlinglabtrack.theme.UsbcNavyLight
import com.example.cebowlinglabtrack.theme.UsbcRed
import com.example.cebowlinglabtrack.ui.TrackingUiState
import com.example.cebowlinglabtrack.ui.components.LaneTraxReviewView
import com.example.cebowlinglabtrack.ui.components.TelemetryHUDCard
import com.example.cebowlinglabtrack.ui.components.TripodAdvisorOverlay

/**
 * Main Live Camera Tracking Screen with 120 FPS optical tracking,
 * hardware zoom control, 10-pin deck AR detection, and Tripod Angle Advisor.
 */
@Composable
fun LiveTrackingScreen(
    state: TrackingUiState,
    onFrameAvailable: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int, timestampMs: Long) -> Unit)? = null,
    onArmNextShot: () -> Unit = {},
    onNavigateCalibration: () -> Unit = {},
    onSelectTargetLine: ((VisualTargetLine) -> Unit)? = null,
    onAutoCalibrate: (() -> Unit)? = null,
    onCalibrateDefault: (() -> Unit)? = null,
    onZoomChange: ((Float) -> Unit)? = null,
    onSimulateShot: ((ShotStylePreset) -> Unit)? = null,
    onPlayVideoFeed: ((Uri) -> Unit)? = null,
    onStopVideoFeed: (() -> Unit)? = null,
    onSaveShot: (() -> Unit)? = null,
    onViewportSizeChanged: ((Float, Float) -> Unit)? = null,
    onRebaselineStability: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPlayVideoFeed?.invoke(it) }
    }

    var showReviewMode by remember { mutableStateOf(false) }
    var showDebugSimulate by remember { mutableStateOf(false) }
    var showTargetSelectorModal by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(ShotStylePreset.POWER_CRANKER) }
    var isHudVisible by remember { mutableStateOf(true) }

    val activeShot = state.activeShot
    val inReview = (showReviewMode || state.trackingState == TrackingState.SHOT_COMPLETED) && activeShot != null

    if (inReview) {
        // 1:1 LaneTrax Split-Screen Shot Review with Video Replay, Target Comparison and 2x2 Metric Cards
        LaneTraxReviewView(
            shot = activeShot,
            targetLine = state.activeTargetLine,
            targetComparison = state.targetComparison,
            onSaveShot = {
                onSaveShot?.invoke()
                showReviewMode = false
                onArmNextShot()
            },
            onDeleteShot = {
                showReviewMode = false
                onArmNextShot()
            },
            onCorrectCalibration = onNavigateCalibration,
            onNewShot = {
                showReviewMode = false
                onArmNextShot()
            },
            modifier = modifier
        )
    } else {
        // Live Camera Viewfinder & AR Overlays
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()
            if (screenW > 50f && screenH > 50f) {
                LaunchedEffect(screenW, screenH) {
                    onViewportSizeChanged?.invoke(screenW, screenH)
                }
            }
            // 1. Live CameraX Preview Stream or Virtual Video Feed
            if (state.virtualVideoBitmap != null) {
                Image(
                    bitmap = state.virtualVideoBitmap.asImageBitmap(),
                    contentDescription = "Virtual Camera Feed",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CameraPreviewView(
                    onFrameAvailable = onFrameAvailable,
                    zoomRatio = state.zoomRatio,
                    targetFps = 120,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. AR Overlays: Cyan Lane Trapezoid, 10-Pin Rack Lock Box & In-Flight Ball Tracking Reticle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hElements = state.calibration?.homographyMatrixElements
                val homography = if (hElements != null && hElements.size == 9) {
                    com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix(hElements.toDoubleArray())
                } else null

                // 2A. Cyan Lane Surface Trapezoid Fill (Foul line to Pin Deck)
                if (homography != null) {
                    val pBL = homography.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.0, 0.0))
                    val pBR = homography.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.0, 0.0))
                    val pTR = homography.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(39.0, 60.0))
                    val pTL = homography.forward(com.example.cebowlinglabtrack.domain.model.LanePoint(1.0, 60.0))

                    val laneTrapezoid = Path().apply {
                        moveTo(pBL.x.toFloat(), pBL.y.toFloat())
                        lineTo(pBR.x.toFloat(), pBR.y.toFloat())
                        lineTo(pTR.x.toFloat(), pTR.y.toFloat())
                        lineTo(pTL.x.toFloat(), pTL.y.toFloat())
                        close()
                    }
                    // Subtle translucent cyan wash across physical lane
                    drawPath(path = laneTrapezoid, color = Color(0x1200E5FF))
                    // Subtle perimeter outline
                    drawPath(
                        path = laneTrapezoid,
                        color = NeonCyan.copy(alpha = 0.35f),
                        style = Stroke(width = 1.5f)
                    )
                }

                // 2B. Projected Lane Guides (Foul line, arrows, targets, chevrons)
                state.projectedGuides?.let { guides ->
                    drawProjectedGuides(guides)
                }

                // 2C. 10-Pin Rack AR Target Box [||||||||||] (Strike.app style bracket lock at 60 ft)
                if (homography != null) {
                    val pinScreenPts = PinDeckDetector.STANDARD_PIN_COORDS.map { homography.forward(it) }
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

                        val bracketColor = if (state.standingPins.size == 10) Color.White else NeonCyan

                        // High-tech corner brackets [  ] around 10-pin triangle
                        // Top-Left
                        drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), strokeWidth = 2.5f)
                        drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), strokeWidth = 2.5f)
                        // Top-Right
                        drawLine(bracketColor, Offset(right, top), Offset(right - bracketLen, top), strokeWidth = 2.5f)
                        drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLen), strokeWidth = 2.5f)
                        // Bottom-Left
                        drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLen, bottom), strokeWidth = 2.5f)
                        drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - bracketLen), strokeWidth = 2.5f)
                        // Bottom-Right
                        drawLine(bracketColor, Offset(right, bottom), Offset(right - bracketLen, bottom), strokeWidth = 2.5f)
                        drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - bracketLen), strokeWidth = 2.5f)
                        // Physical pins sit cleanly inside the 60 ft pin rack corner brackets [  ]
                    }
                }

                // 2D. Pro In-Flight Ball Tracking Reticle [ ● ] and Trajectory Trail
                if (state.liveTrajectory.isNotEmpty() && homography != null) {
                    // Glowing motion trajectory path
                    if (state.liveTrajectory.size >= 2) {
                        for (i in 0 until state.liveTrajectory.size - 1) {
                            val p1 = homography.forward(
                                com.example.cebowlinglabtrack.domain.model.LanePoint(
                                    state.liveTrajectory[i].xBoard,
                                    state.liveTrajectory[i].yFt
                                )
                            )
                            val p2 = homography.forward(
                                com.example.cebowlinglabtrack.domain.model.LanePoint(
                                    state.liveTrajectory[i + 1].xBoard,
                                    state.liveTrajectory[i + 1].yFt
                                )
                            )
                            // Outer glow
                            drawLine(
                                color = NeonStrikeGreen.copy(alpha = 0.25f),
                                start = Offset(p1.x.toFloat(), p1.y.toFloat()),
                                end = Offset(p2.x.toFloat(), p2.y.toFloat()),
                                strokeWidth = 7f,
                                cap = StrokeCap.Round
                            )
                            // Core trajectory line
                            drawLine(
                                color = NeonStrikeGreen.copy(alpha = 0.90f),
                                start = Offset(p1.x.toFloat(), p1.y.toFloat()),
                                end = Offset(p2.x.toFloat(), p2.y.toFloat()),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Dynamic Ball-Diameter Reticle & Contact Patch Deciding Factor
                    val lastPt = state.liveTrajectory.last()
                    // lastPt is the physical CONTACT PATCH on the lane surface (Z = 0)
                    val contactScreenPt = homography.forward(
                        com.example.cebowlinglabtrack.domain.model.LanePoint(lastPt.xBoard, lastPt.yFt)
                    )
                    val bx = contactScreenPt.x.toFloat()
                    val contactY = contactScreenPt.y.toFloat()

                    // Ball diameter = 8.59 inches ≈ 8.07 boards (radius = 4.035 boards)
                    val pLeft = homography.forward(
                        com.example.cebowlinglabtrack.domain.model.LanePoint(lastPt.xBoard - 4.035, lastPt.yFt)
                    )
                    val pRight = homography.forward(
                        com.example.cebowlinglabtrack.domain.model.LanePoint(lastPt.xBoard + 4.035, lastPt.yFt)
                    )
                    val rRadius = (kotlin.math.abs(pRight.x - pLeft.x) / 2.0).toFloat().coerceIn(12f, 95f)
                    val bArm = (rRadius * 0.35f).coerceIn(6f, 18f)
                    val by = contactY - rRadius // Ball 3D centroid elevated in screen perspective

                    val rLeft = bx - rRadius
                    val rRight = bx + rRadius
                    val rTop = by - rRadius
                    val rBottom = by + rRadius

                    // 1. Full physical ball diameter ring matching perspective at current depth
                    drawCircle(
                        color = NeonStrikeGreen,
                        radius = rRadius,
                        center = Offset(bx, by),
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = NeonStrikeGreen.copy(alpha = 0.12f),
                        radius = rRadius,
                        center = Offset(bx, by)
                    )

                    // 2. Vertical plumb line from ball center down to lane contact patch
                    drawLine(
                        color = UsbcGold.copy(alpha = 0.85f),
                        start = Offset(bx, by),
                        end = Offset(bx, contactY),
                        strokeWidth = 1.5f
                    )

                    // 3. Contact Patch Indicator (tangent point of circle parallel to lane surface)
                    val patchRx = (rRadius * 0.45f).coerceAtLeast(6f)
                    val patchRy = (rRadius * 0.18f).coerceAtLeast(3f)
                    drawOval(
                        color = UsbcGold,
                        topLeft = Offset(bx - patchRx, contactY - patchRy),
                        size = androidx.compose.ui.geometry.Size(patchRx * 2f, patchRy * 2f),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(color = PowerCoral, radius = 3.5f, center = Offset(bx, contactY))
                    drawCircle(color = Color.White, radius = 1.8f, center = Offset(bx, contactY))

                    // 4. Ball centroid marker
                    drawCircle(color = NeonStrikeGreen, radius = 3.5f, center = Offset(bx, by))

                    // 5. Corner brackets [  ] framing ball diameter
                    drawLine(NeonStrikeGreen, Offset(rLeft, rTop), Offset(rLeft + bArm, rTop), strokeWidth = 2.5f)
                    drawLine(NeonStrikeGreen, Offset(rLeft, rTop), Offset(rLeft, rTop + bArm), strokeWidth = 2.5f)
                    drawLine(NeonStrikeGreen, Offset(rRight, rTop), Offset(rRight - bArm, rTop), strokeWidth = 2.5f)
                    drawLine(NeonStrikeGreen, Offset(rRight, rTop), Offset(rRight, rTop + bArm), strokeWidth = 2.5f)
                    drawLine(NeonStrikeGreen, Offset(rLeft, rBottom), Offset(rLeft + bArm, rBottom), strokeWidth = 2.5f)
                    drawLine(NeonStrikeGreen, Offset(rLeft, rBottom), Offset(rLeft, rBottom - bArm), strokeWidth = 2.5f)
                    drawLine(NeonStrikeGreen, Offset(rRight, rBottom), Offset(rRight - bArm, rBottom), strokeWidth = 2.5f)
                    drawLine(NeonStrikeGreen, Offset(rRight, rBottom), Offset(rRight, rBottom - bArm), strokeWidth = 2.5f)
                }
            }

            // 4. Top Status Header & Tripod Alignment Advisor
            if (isHudVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Official Coach's Eye Bowling Lab Emblem Badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0B1E3B))
                                .border(1.5.dp, Color(0xFFC39D5E), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_emblem),
                                contentDescription = "Coach's Eye Bowling Lab Emblem",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (state.trackingState) {
                                                TrackingState.IDLE -> NeonStrikeGreen
                                                TrackingState.APPROACH_DETECTED, TrackingState.BALL_RELEASED -> ElectricAmber
                                                TrackingState.BALL_IN_FLIGHT -> NeonCyan
                                                TrackingState.PIN_DECK_ENTRY -> PowerCoral
                                                TrackingState.SHOT_COMPLETED -> NeonStrikeGreen
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CE BOWLING LAB",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = " • TRACK",
                                    color = NeonCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                        // Status & 1st/2nd Ball Indicator Dots (Matching LaneTrax)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.ballNumber >= 1) NeonCyan else Color.DarkGray)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.ballNumber >= 2) NeonCyan else Color.DarkGray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BALL ${state.ballNumber} • ${state.trackingState.name.replace("_", " ")}",
                                color = when (state.trackingState) {
                                    TrackingState.IDLE -> NeonStrikeGreen
                                    TrackingState.BALL_IN_FLIGHT -> NeonCyan
                                    else -> ElectricAmber
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                    // Actions: Target line selector, Safety interlock badge, Replay, 120 FPS badge, Calibrate
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Visual Target Line Button (Strike.app style)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(UsbcNavyDark.copy(alpha = 0.9f))
                                .border(1.dp, TargetLineGold, RoundedCornerShape(8.dp))
                                .clickable { showTargetSelectorModal = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = state.activeTargetLine.name.take(13),
                                    color = TargetLineGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Safety Interlock Status Chip (Locks tracking if uncalibrated)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.isLaneCalibrated) UsbcNavyDark.copy(alpha = 0.85f)
                                    else PowerCoral.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (state.isLaneCalibrated) DarkCardBorder else PowerCoral,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { if (!state.isLaneCalibrated) onNavigateCalibration() }
                                .padding(horizontal = 7.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (state.isLaneCalibrated) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = "Calibration Status",
                                    tint = if (state.isLaneCalibrated) NeonStrikeGreen else PowerCoral,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (state.isLaneCalibrated) "ARMED" else "LOCKED",
                                    color = if (state.isLaneCalibrated) NeonStrikeGreen else PowerCoral,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Virtual Video Feed Chip (Test any MP4 file as live camera input)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.isPlayingVideoFeed) NeonStrikeGreen.copy(alpha = 0.25f)
                                    else UsbcNavyDark.copy(alpha = 0.85f)
                                )
                                .border(
                                    1.dp,
                                    if (state.isPlayingVideoFeed) NeonStrikeGreen else DarkCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (state.isPlayingVideoFeed) {
                                        onStopVideoFeed?.invoke()
                                    } else {
                                        videoPickerLauncher.launch("video/*")
                                    }
                                }
                                .padding(horizontal = 7.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (state.isPlayingVideoFeed) Icons.Default.Stop else Icons.Default.VideoFile,
                                    contentDescription = "Virtual Video Feed",
                                    tint = if (state.isPlayingVideoFeed) NeonStrikeGreen else UsbcGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (state.isPlayingVideoFeed) "STOP FEED" else "TEST VIDEO",
                                    color = if (state.isPlayingVideoFeed) NeonStrikeGreen else UsbcGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // 10-Pin Deck Lock Status Chip with Mini 10-Pin Graphic (Strike.app style)
                        if (state.isLaneCalibrated) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(UsbcNavyDark.copy(alpha = 0.90f))
                                    .border(1.dp, if (state.standingPins.size == 10) NeonStrikeGreen.copy(alpha = 0.8f) else ElectricAmber, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Mini 10-Pin Triangle Canvas (Pins 7,8,9,10 / 4,5,6 / 2,3 / 1)
                                    Canvas(modifier = Modifier.size(width = 18.dp, height = 16.dp)) {
                                        val pinR = 1.4f
                                        val pinPositions = mapOf(
                                            7 to Offset(1.5f, 2f), 8 to Offset(6f, 2f), 9 to Offset(10.5f, 2f), 10 to Offset(15f, 2f),
                                            4 to Offset(3.5f, 6.5f), 5 to Offset(8.25f, 6.5f), 6 to Offset(13f, 6.5f),
                                            2 to Offset(5.5f, 11f), 3 to Offset(11f, 11f),
                                            1 to Offset(8.25f, 15f)
                                        )
                                        for ((pinNum, offset) in pinPositions) {
                                            val isStanding = state.standingPins.contains(pinNum)
                                            drawCircle(
                                                color = if (isStanding) Color.White else Color(0x40888888),
                                                radius = pinR,
                                                center = offset
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = when (state.trackingState) {
                                                TrackingState.PIN_DECK_ENTRY -> "SCATTERING..."
                                                TrackingState.BALL_IN_FLIGHT -> "BALL IN FLIGHT"
                                                TrackingState.BALL_RELEASED -> "BALL RELEASED"
                                                else -> if (state.standingPins.size == 10) "10 PINS READY" else "${state.standingPins.size} PINS"
                                            },
                                            color = when (state.trackingState) {
                                                TrackingState.PIN_DECK_ENTRY -> ElectricAmber
                                                TrackingState.BALL_IN_FLIGHT, TrackingState.BALL_RELEASED -> NeonStrikeGreen
                                                else -> Color.White
                                            },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (state.standingPins.size in 1..9 && state.trackingState == TrackingState.IDLE) {
                                            Text(
                                                text = "LEAVING: ${state.standingPins.joinToString(",")}",
                                                color = ElectricAmber,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Camera Stability Badge
                        if (state.isLaneCalibrated) {
                            val badgeBg = if (state.isCameraDrifted) PowerCoral.copy(alpha = 0.85f) else NeonStrikeGreen.copy(alpha = 0.15f)
                            val textCol = if (state.isCameraDrifted) Color.White else NeonStrikeGreen
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .border(1.dp, if (state.isCameraDrifted) PowerCoral else NeonStrikeGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (state.isCameraDrifted) "⚠️ SHIFT ${state.cameraStabilityScore}%" else "● LOCKED ${state.cameraStabilityScore}%",
                                    color = textCol,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (activeShot != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricAmber.copy(alpha = 0.2f))
                                    .border(1.dp, ElectricAmber, RoundedCornerShape(8.dp))
                                    .clickable { showReviewMode = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("REPLAY", color = ElectricAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface.copy(alpha = 0.85f))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "120 FPS ACTIVE",
                                color = NeonStrikeGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "LATENCY: ${String.format("%.1f", state.inferenceLatencyMs)}ms",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }

                        IconButton(
                            onClick = onNavigateCalibration,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface.copy(alpha = 0.85f))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Calibration",
                                tint = NeonCyan
                            )
                        }

                        IconButton(
                            onClick = { isHudVisible = !isHudVisible },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface.copy(alpha = 0.85f))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = if (isHudVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle HUD",
                                tint = NeonCyan
                            )
                        }
                    }
                }

                // Camera Drift Alert Banner (if tripod shifted < 80% match)
                if (state.isCameraDrifted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2A0D0D).copy(alpha = 0.95f))
                            .border(1.dp, PowerCoral, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CAMERA SHIFT DETECTED (${state.cameraStabilityScore}% MATCH)",
                                color = PowerCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Tripod moved or vibrated during shot",
                                color = Color.LightGray,
                                fontSize = 9.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onNavigateCalibration,
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PowerCoral),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("RE-CALIBRATE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onRebaselineStability?.invoke() },
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("KEEP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Tripod Angle & Alignment Advisor Bar
                TripodAdvisorOverlay(
                    tripodStatus = state.tripodStatus,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Minimal floating Eye button at top right to unhide HUD
            IconButton(
                onClick = { isHudVisible = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.75f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Show HUD",
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

            // 4b. Safety Interlock Notification Card (Locks shot recording until calibrated)
            if (!state.isLaneCalibrated) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface.copy(alpha = 0.95f))
                            .border(2.dp, UsbcGold, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PowerCoral.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Safety Lock",
                                tint = PowerCoral,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "LANE CALIBRATION REQUIRED",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Shot recording is locked. Calibrate lane geometry to enable 100% hands-free automatic optical tracking.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAutoCalibrate?.invoke()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = UsbcGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Auto",
                                    tint = UsbcNavyDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AUTO-DETECT",
                                    color = UsbcNavyDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateCalibration,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonCyan),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "4-POINT MANUAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "USE USBC STANDARD PRESET",
                            color = UsbcGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onCalibrateDefault?.invoke() }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // 5. Bottom Live Telemetry, Zoom Controls & Action Bar
            if (isHudVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                TelemetryHUDCard(metrics = state.liveMetrics)

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Arm, Zoom, Calibrate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Arm / Reset Button
                    Button(
                        onClick = onArmNextShot,
                        modifier = Modifier.weight(1.1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.trackingState == TrackingState.IDLE) DarkSurface else ElectricAmber
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Arm Tracker",
                            tint = if (state.trackingState == TrackingState.IDLE) NeonStrikeGreen else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.trackingState == TrackingState.IDLE) "ARMED" else "RESET",
                            color = if (state.trackingState == TrackingState.IDLE) NeonStrikeGreen else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Zoom Controls & 1x / 1.5x / 2x / 3x Quick Chips
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 1x Quick Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (kotlin.math.abs(state.zoomRatio - 1.0f) < 0.15f) UsbcGold else Color.Transparent)
                                .clickable { onZoomChange?.invoke(1.0f) }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "1x",
                                color = if (kotlin.math.abs(state.zoomRatio - 1.0f) < 0.15f) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 1.5x Quick Chip (Recommended for foul-line-to-deck full view)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (kotlin.math.abs(state.zoomRatio - 1.5f) < 0.15f) UsbcGold else Color.Transparent)
                                .clickable { onZoomChange?.invoke(1.5f) }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "1.5x",
                                color = if (kotlin.math.abs(state.zoomRatio - 1.5f) < 0.15f) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2x Quick Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (kotlin.math.abs(state.zoomRatio - 2.0f) < 0.15f) UsbcGold else Color.Transparent)
                                .clickable { onZoomChange?.invoke(2.0f) }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "2x",
                                color = if (kotlin.math.abs(state.zoomRatio - 2.0f) < 0.15f) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3x Quick Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (kotlin.math.abs(state.zoomRatio - 3.0f) < 0.15f) UsbcGold else Color.Transparent)
                                .clickable { onZoomChange?.invoke(3.0f) }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "3x",
                                color = if (kotlin.math.abs(state.zoomRatio - 3.0f) < 0.15f) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { onZoomChange?.invoke((state.zoomRatio - 0.2f).coerceAtLeast(1.0f)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${String.format("%.1f", state.zoomRatio)}x",
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onZoomChange?.invoke((state.zoomRatio + 0.2f).coerceAtMost(3.5f)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Calibrate Quick Button
                    OutlinedButton(
                        onClick = onNavigateCalibration,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "CALIBRATE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Offline simulation toggle
                    if (onSimulateShot != null) {
                        IconButton(
                            onClick = { showDebugSimulate = !showDebugSimulate },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface.copy(alpha = 0.85f))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Simulate",
                                tint = if (showDebugSimulate) ElectricAmber else TextMuted
                            )
                        }
                    }
                }

                // Expandable Offline Simulation Strip (Only if toggled)
                if (showDebugSimulate && onSimulateShot != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface.copy(alpha = 0.95f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val values = ShotStylePreset.values()
                                selectedPreset = values[(selectedPreset.ordinal + 1) % values.size]
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedPreset.name.replace("_", " "), fontSize = 10.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = { onSimulateShot(selectedPreset) },
                            enabled = !state.isSimulating,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RUN TEST", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }
    }

    // 6. Visual Target Line Selector Modal (Strike.app style)
    if (showTargetSelectorModal || state.showTargetLineSelector) {
        Dialog(
            onDismissRequest = { showTargetSelectorModal = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.5.dp, DarkCardBorderGold, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VISUAL TARGET LINE",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = { showTargetSelectorModal = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }
                Text(
                    text = "Projected AR trajectory & gutter chevrons down the lane. Compares actual ball roll against target boards.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VisualTargetLine.PRESETS.forEach { preset ->
                        val isSelected = preset.id == state.activeTargetLine.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) UsbcNavyLight.copy(alpha = 0.4f) else DarkBackground.copy(alpha = 0.6f))
                                .border(
                                    1.5.dp,
                                    if (isSelected) TargetLineGold else DarkCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectTargetLine?.invoke(preset)
                                    showTargetSelectorModal = false
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = preset.name,
                                            color = if (isSelected) TargetLineGold else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ACTIVE",
                                                color = UsbcNavyDark,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TargetLineGold)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = preset.description,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Lay: B${preset.laydownBoard.toInt()}",
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Arrow: B${preset.arrowBoard.toInt()}",
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Break: B${preset.breakpointBoard.toInt()} @ ${preset.breakpointDistFt.toInt()}ft",
                                            color = ElectricAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = TargetLineGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProjectedGuides(
    guides: ProjectedLaneGuides
) {
    // 1. Foul Line
    drawLine(
        color = Color.White,
        start = Offset(guides.foulLine.first.x.toFloat(), guides.foulLine.first.y.toFloat()),
        end = Offset(guides.foulLine.second.x.toFloat(), guides.foulLine.second.y.toFloat()),
        strokeWidth = 3f
    )

    // 2. Left and Right Gutter Rails
    fun drawPolyLine(pts: List<com.example.cebowlinglabtrack.domain.model.Point2D>, color: Color) {
        for (i in 0 until pts.size - 1) {
            drawLine(
                color = color,
                start = Offset(pts[i].x.toFloat(), pts[i].y.toFloat()),
                end = Offset(pts[i + 1].x.toFloat(), pts[i + 1].y.toFloat()),
                strokeWidth = 2f
            )
        }
    }

    drawPolyLine(guides.leftGutterLine, NeonCyan.copy(alpha = 0.7f))
    drawPolyLine(guides.rightGutterLine, NeonCyan.copy(alpha = 0.7f))
    drawPolyLine(guides.centerline, Color.White.copy(alpha = 0.25f))
    // 3. Indicator Dots at 7.5 ft (USBC spec: boards 3, 5, 8, 11, 14, 26, 29, 32, 35, 37)
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

    // 4. Targeting Chevron Arrows at 12.5 - 15.5 ft (forward pointing ^ on boards 5, 10, 15, 20, 25, 30, 35)
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
    // Arrow marker dots
    for (pt in guides.arrowPoints) {
        drawCircle(
            color = ElectricAmber,
            radius = 3f,
            center = Offset(pt.x.toFloat(), pt.y.toFloat())
        )
    }

    // 6. Range Finders from 37 ft to 43 ft (Boards 10, 15, 25, 30)
    for (rf in guides.rangeFinders) {
        drawLine(
            color = NeonCyan.copy(alpha = 0.85f),
            start = Offset(rf.start.x.toFloat(), rf.start.y.toFloat()),
            end = Offset(rf.end.x.toFloat(), rf.end.y.toFloat()),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    // 7. Headpin Dot (60 ft, Board 20)
    drawCircle(
        color = PowerCoral,
        radius = 5f,
        center = Offset(guides.headpinPoint.x.toFloat(), guides.headpinPoint.y.toFloat())
    )

    // 6. Visual Target Line (Strike.app style in glowing gold)
    if (guides.targetLinePointsScreen.size >= 2) {
        for (i in 0 until guides.targetLinePointsScreen.size - 1) {
            val p1 = guides.targetLinePointsScreen[i]
            val p2 = guides.targetLinePointsScreen[i + 1]
            drawLine(
                color = TargetLineGold.copy(alpha = 0.85f),
                start = Offset(p1.x.toFloat(), p1.y.toFloat()),
                end = Offset(p2.x.toFloat(), p2.y.toFloat()),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        }

        // Draw target rings at laydown, arrows, breakpoint, and pocket
        val targetIndices = listOf(0, 15, 42, guides.targetLinePointsScreen.size - 1)
        for (idx in targetIndices) {
            if (idx < guides.targetLinePointsScreen.size) {
                val pt = guides.targetLinePointsScreen[idx]
                drawCircle(
                    color = TargetLineGold,
                    radius = 7f,
                    center = Offset(pt.x.toFloat(), pt.y.toFloat()),
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = TargetLineGold.copy(alpha = 0.4f),
                    radius = 14f,
                    center = Offset(pt.x.toFloat(), pt.y.toFloat())
                )
            }
        }
    }
}
