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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.cebowlinglabtrack.ui.components.SkeletonOverlay
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
    onSaveShot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReviewMode by remember { mutableStateOf(false) }
    var showDebugSimulate by remember { mutableStateOf(false) }
    var showTargetSelectorModal by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(ShotStylePreset.POWER_CRANKER) }

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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. Live CameraX Preview Stream (120 FPS Target with Hardware Zoom)
            CameraPreviewView(
                onFrameAvailable = onFrameAvailable,
                zoomRatio = state.zoomRatio,
                targetFps = 120,
                modifier = Modifier.fillMaxSize()
            )

            // 2. AR Overlays: Projected Lane Guides, 10-Pin Deck & In-Flight Ball Marker
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Render AR projected lane guides over physical lane
                state.projectedGuides?.let { guides ->
                    drawProjectedGuides(guides)
                }

                // Render 10 Pins on Pin Deck (USBC coordinates)
                val hElements = state.calibration?.homographyMatrixElements
                if (hElements != null && hElements.size == 9) {
                    val m = com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix(
                        hElements.toDoubleArray()
                    )
                    PinDeckDetector.STANDARD_PIN_COORDS.forEachIndexed { idx, pinCoord ->
                        val pinNum = idx + 1
                        val pinPos = m.forward(pinCoord)
                        val isStanding = state.standingPins.contains(pinNum)

                        if (isStanding) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.95f),
                                radius = 7f,
                                center = Offset(pinPos.x.toFloat(), pinPos.y.toFloat())
                            )
                            drawCircle(
                                color = NeonStrikeGreen,
                                radius = 3.5f,
                                center = Offset(pinPos.x.toFloat(), pinPos.y.toFloat())
                            )
                        } else {
                            drawCircle(
                                color = Color.Gray.copy(alpha = 0.4f),
                                radius = 6f,
                                center = Offset(pinPos.x.toFloat(), pinPos.y.toFloat()),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                }

                // Render in-flight ball marker
                if (state.liveTrajectory.isNotEmpty()) {
                    val lastPt = state.liveTrajectory.last()
                    if (hElements != null && hElements.size == 9) {
                        val m = com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix(
                            hElements.toDoubleArray()
                        )
                        val screenPt = m.forward(
                            com.example.cebowlinglabtrack.domain.model.LanePoint(lastPt.xBoard, lastPt.yFt)
                        )
                        drawCircle(
                            color = NeonStrikeGreen.copy(alpha = 0.4f),
                            radius = 24f,
                            center = Offset(screenPt.x.toFloat(), screenPt.y.toFloat())
                        )
                        drawCircle(
                            color = NeonStrikeGreen,
                            radius = 12f,
                            center = Offset(screenPt.x.toFloat(), screenPt.y.toFloat())
                        )
                    }
                }
            }

            // 3. Real-time Bowler Skeletal Pose Overlay
            SkeletonOverlay(
                pose = state.livePose,
                modifier = Modifier.fillMaxSize()
            )

            // 4. Top Status Header & Tripod Alignment Advisor
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
                    }
                }

                // Tripod Angle & Alignment Advisor Bar
                TripodAdvisorOverlay(
                    tripodStatus = state.tripodStatus,
                    modifier = Modifier.fillMaxWidth()
                )
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

                    // Zoom Controls
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onZoomChange?.invoke((state.zoomRatio - 0.2f).coerceAtLeast(1.0f)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${String.format("%.1f", state.zoomRatio)}x",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onZoomChange?.invoke((state.zoomRatio + 0.2f).coerceAtMost(3.5f)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TextPrimary, modifier = Modifier.size(18.dp))
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

    // 3. Arrows line
    drawLine(
        color = NeonCyan.copy(alpha = 0.4f),
        start = Offset(guides.arrowsLine.first.x.toFloat(), guides.arrowsLine.first.y.toFloat()),
        end = Offset(guides.arrowsLine.second.x.toFloat(), guides.arrowsLine.second.y.toFloat()),
        strokeWidth = 1.5f
    )

    // 4. Arrow Dots
    for (pt in guides.arrowPoints) {
        drawCircle(
            color = NeonCyan,
            radius = 3.5f,
            center = Offset(pt.x.toFloat(), pt.y.toFloat())
        )
    }

    // 5. Headpin Dot
    drawCircle(
        color = PowerCoral,
        radius = 5f,
        center = Offset(guides.headpinPoint.x.toFloat(), guides.headpinPoint.y.toFloat())
    )

    // 6. Forward AR Gutter Chevrons (Delimiting gutter channels pointing down-lane, Strike.app style)
    for (ch in guides.leftGutterChevrons) {
        drawLine(
            color = GutterChevronCyan.copy(alpha = 0.85f),
            start = Offset(ch.leftWing.x.toFloat(), ch.leftWing.y.toFloat()),
            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = GutterChevronCyan.copy(alpha = 0.85f),
            start = Offset(ch.rightWing.x.toFloat(), ch.rightWing.y.toFloat()),
            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = GutterChevronCyan,
            radius = 3f,
            center = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat())
        )
    }

    for (ch in guides.rightGutterChevrons) {
        drawLine(
            color = GutterChevronCyan.copy(alpha = 0.85f),
            start = Offset(ch.leftWing.x.toFloat(), ch.leftWing.y.toFloat()),
            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = GutterChevronCyan.copy(alpha = 0.85f),
            start = Offset(ch.rightWing.x.toFloat(), ch.rightWing.y.toFloat()),
            end = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat()),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = GutterChevronCyan,
            radius = 3f,
            center = Offset(ch.tip.x.toFloat(), ch.tip.y.toFloat())
        )
    }

    // 7. Visual Target Line (Strike.app style in glowing gold)
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
