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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.camera.CameraPreviewView
import com.example.cebowlinglabtrack.domain.calibration.ProjectedLaneGuides
import com.example.cebowlinglabtrack.domain.ml.PinDeckDetector
import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.tracking.TrackingState
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
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
    onZoomChange: ((Float) -> Unit)? = null,
    onSimulateShot: ((ShotStylePreset) -> Unit)? = null,
    onSaveShot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReviewMode by remember { mutableStateOf(false) }
    var showDebugSimulate by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(ShotStylePreset.POWER_CRANKER) }

    val activeShot = state.activeShot
    val inReview = (showReviewMode || state.trackingState == TrackingState.SHOT_COMPLETED) && activeShot != null

    if (inReview) {
        // 1:1 LaneTrax Split-Screen Shot Review with Video Replay and 2x2 Metric Cards
        LaneTraxReviewView(
            shot = activeShot,
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

                    // Actions: Replay toggle, 120 FPS status badge, Calibration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
}
