package com.example.cebowlinglabtrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.calibration.ProjectedLaneGuides
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
import com.example.cebowlinglabtrack.ui.components.SkeletonOverlay
import com.example.cebowlinglabtrack.ui.components.TelemetryHUDCard

/**
 * Main Live Camera Tracking Screen with AR Lane & Skeletal Overlays.
 */
@Composable
fun LiveTrackingScreen(
    state: TrackingUiState,
    onSimulateShot: (ShotStylePreset) -> Unit,
    onNavigateCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf(ShotStylePreset.POWER_CRANKER) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Simulated Camera Viewfinder & AR Projected Lane Guides
        Box(modifier = Modifier.fillMaxSize()) {
            // Viewfinder background (simulated dark concourse/lane view)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Subtle perspective background
                drawRect(Color(0xFF0F141F))

                // Render AR projected lane guides
                state.projectedGuides?.let { guides ->
                    drawProjectedGuides(guides)
                }

                // Render in-flight ball circle if tracking
                if (state.liveTrajectory.isNotEmpty()) {
                    val lastPt = state.liveTrajectory.last()
                    // Project lane coord to screen if guides are available
                    val hElements = state.calibration?.homographyMatrixElements
                    if (hElements != null && hElements.size == 9) {
                        val m = com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix(
                            hElements.toDoubleArray()
                        )
                        val screenPt = m.forward(
                            com.example.cebowlinglabtrack.domain.model.LanePoint(lastPt.xBoard, lastPt.yFt)
                        )
                        // Glowing ball centroid
                        drawCircle(
                            color = NeonStrikeGreen.copy(alpha = 0.4f),
                            radius = 18f,
                            center = Offset(screenPt.x.toFloat(), screenPt.y.toFloat())
                        )
                        drawCircle(
                            color = NeonStrikeGreen,
                            radius = 9f,
                            center = Offset(screenPt.x.toFloat(), screenPt.y.toFloat())
                        )
                    }
                }
            }

            // 2. Real-time Skeletal Pose Overlay
            SkeletonOverlay(
                pose = state.livePose,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Top System Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding & State Pill
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when (state.trackingState) {
                                    TrackingState.IDLE -> TextMuted
                                    TrackingState.SHOT_COMPLETED -> NeonStrikeGreen
                                    else -> PowerCoral
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CE BOWLING LAB",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = " • TRACK",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = "STATE: ${state.trackingState.name.replace("_", " ")}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Hardware Performance Badge (Snapdragon 8 Elite NPU telemetry)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface.copy(alpha = 0.85f))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NPU: ${state.inferenceLatencyMs}ms",
                        color = NeonStrikeGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.fps} FPS LOCKED",
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

        // 4. Bottom Controls and Real-time Telemetry HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Live Telemetry HUD Card
            TelemetryHUDCard(metrics = state.liveMetrics)

            Spacer(modifier = Modifier.height(12.dp))

            // Shot Simulation / Trigger Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preset selector buttons
                OutlinedButton(
                    onClick = {
                        val values = ShotStylePreset.values()
                        selectedPreset = values[(selectedPreset.ordinal + 1) % values.size]
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = selectedPreset.name.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Simulate Shot Action Button
                Button(
                    onClick = { onSimulateShot(selectedPreset) },
                    enabled = !state.isSimulating,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (state.isSimulating) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Trigger Shot",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isSimulating) "TRACKING..." else "SIMULATE SHOT",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
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
