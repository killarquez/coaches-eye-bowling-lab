package com.example.cebowlinglabtrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.model.RevTrackingMethod
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.TargetComparisonResult
import com.example.cebowlinglabtrack.domain.model.VisualTargetLine
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkCardBorderGold
import com.example.cebowlinglabtrack.theme.DarkSurfaceVariant
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import com.example.cebowlinglabtrack.theme.UsbcGold
import com.example.cebowlinglabtrack.theme.UsbcNavy
import com.example.cebowlinglabtrack.theme.UsbcRed
import kotlin.math.abs

/**
 * 1:1 LaneTrax-style Split-Screen Shot Review & Replay Interface.
 *
 * Left side: Continuous 2D Top-Down Lane with neon green ball trajectory and Target Line.
 * Right side:
 *   - Video Replay Player (looping shot playback with fullscreen toggle)
 *   - Target vs Actual Comparison Card (Strike.app style)
 *   - Action Buttons ("Correct", "Save", "Delete")
 *   - Scrollable 2-Column Grid of Metric Cards (Foul Line, Arrows, Entry Board, Rev Rate, Breakpoint, Angles, Speeds)
 */
@Composable
fun LaneTraxReviewView(
    shot: ShotData,
    targetLine: VisualTargetLine? = null,
    targetComparison: TargetComparisonResult? = null,
    onSaveShot: () -> Unit,
    onDeleteShot: () -> Unit,
    onCorrectCalibration: () -> Unit,
    onNewShot: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVideoFullscreen by remember { mutableStateOf(false) }

    val sp = shot.spectoTelemetry.spatial
    val an = shot.spectoTelemetry.angles
    val sd = shot.spectoTelemetry.speed
    val dy = shot.spectoTelemetry.dynamics

    // Official CE Bowling Lab theme colors
    val cardBackground = DarkSurfaceVariant // Brand navy surface
    val cardBorder = DarkCardBorderGold     // Brand gold border
    val buttonBlue = UsbcNavy
    val buttonGold = UsbcGold
    val buttonRed = UsbcRed

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (isVideoFullscreen) {
            // Fullscreen video replay modal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoReplayViewport(
                    shot = shot,
                    isFullscreen = true,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { isVideoFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        } else {
            // Standard LaneTrax Split-Screen Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ==================== LEFT: 2D TOP-DOWN LANE ====================
                Box(
                    modifier = Modifier
                        .weight(0.33f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                ) {
                    TopDownLaneCanvas(
                        trajectory = shot.trajectoryPoints,
                        spectoTelemetry = shot.spectoTelemetry,
                        targetLine = targetLine,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ==================== RIGHT: REPLAY & STATS COLUMN ====================
                Column(
                    modifier = Modifier
                        .weight(0.67f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. VIDEO REPLAY PLAYER PANE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        VideoReplayViewport(
                            shot = shot,
                            isFullscreen = false,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Fullscreen Toggle Button in bottom-right corner of video
                        IconButton(
                            onClick = { isVideoFullscreen = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(buttonBlue.copy(alpha = 0.85f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen Replay",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 1.5 TARGET ACCURACY COMPARISON CARD (Strike.app style)
                    targetComparison?.let { comp ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBackground)
                                .border(1.5.dp, cardBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎯 TARGET: ", color = buttonGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(targetLine?.name ?: "CUSTOM", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (comp.overallAccuracyScore >= 80) buttonGold.copy(alpha = 0.2f) else buttonRed.copy(alpha = 0.2f))
                                            .border(1.dp, if (comp.overallAccuracyScore >= 80) buttonGold else buttonRed, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "${comp.overallAccuracyScore}% • ${comp.accuracyRating}",
                                            color = if (comp.overallAccuracyScore >= 80) buttonGold else buttonRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("LAYDOWN", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("${if (comp.laydownDelta >= 0) "+" else ""}${"%.1f".format(comp.laydownDelta)}B", color = if (abs(comp.laydownDelta) <= 1.0f) NeonStrikeGreen else ElectricAmber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ARROWS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("${if (comp.arrowDelta >= 0) "+" else ""}${"%.1f".format(comp.arrowDelta)}B", color = if (abs(comp.arrowDelta) <= 1.0f) NeonStrikeGreen else ElectricAmber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("BREAKPOINT", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("${if (comp.breakpointDelta >= 0) "+" else ""}${"%.1f".format(comp.breakpointDelta)}B", color = if (abs(comp.breakpointDelta) <= 1.0f) NeonStrikeGreen else ElectricAmber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("POCKET", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(if (abs(comp.pocketDelta) <= 0.8f) "HIT!" else "${if (comp.pocketDelta >= 0) "+" else ""}${"%.1f".format(comp.pocketDelta)}B", color = if (abs(comp.pocketDelta) <= 0.8f) NeonStrikeGreen else PowerCoral, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. ACTION BUTTONS: CORRECT, SAVE, DELETE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Correct Button (Blue)
                        Button(
                            onClick = onCorrectCalibration,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonBlue)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Edit, contentDescription = "Correct", tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("Correct", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Save Button (Gold)
                        Button(
                            onClick = onSaveShot,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonGold)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SaveAlt, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(18.dp))
                                Text("Save", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Delete Button (Red)
                    Button(
                        onClick = onDeleteShot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonRed)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. METRIC CARDS 2x2 GRID (LaneTrax Style in Brand Navy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Foul Line", "${sp.laydownBoard}", "", cardBackground, cardBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Arrows", "${sp.arrowBoard}", "", cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Entry Board", "${sp.entryBoard}", "", cardBackground, cardBorder, Modifier.weight(1f))
                        val revBadge = when (dy.revTrackingMethod) {
                            RevTrackingMethod.OPTICAL_TAPE -> "rpm • Tape"
                            RevTrackingMethod.NATURAL_FEATURE -> "rpm • Feature"
                            RevTrackingMethod.TRAJECTORY_ESTIMATE -> "rpm • Est"
                        }
                        LaneTraxMetricCard("Rev Rate", "${dy.rpm}", revBadge, cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Breakpoint\nBoard", "${sp.breakpointBoard}", "", cardBackground, cardBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Breakpoint\nDistance", "${sp.breakpointDistanceFt}", "ft", cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Launch\nAngle", "${an.launchAngleDeg}°", "", cardBackground, cardBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Impact\nAngle", "${an.impactAngleDeg}°", "", cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Launch\nSpeed", "${sd.launchSpeedMph}", "mph", cardBackground, cardBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Impact\nSpeed", "${sd.entrySpeedMph}", "mph", cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Power\nScore", String.format("%.2f", dy.powerScore), "", cardBackground, cardBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Deflection", "${sp.pinDeckDeflection}", "b", cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Axis Tilt", "${dy.axisTiltDeg}°", if (dy.isOpticalRevCounted) "Optical" else "Est", cardBackground, cardBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Axis Rotation", "${dy.axisRotationDeg}°", if (dy.isOpticalRevCounted) "Optical" else "Est", cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Total Revs", "${dy.totalRotations}", "revs", cardBackground, cardBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Accuracy", "${dy.accuracyScore.toInt()}", "Pro <50", cardBackground, cardBorder, Modifier.weight(1f))
                    }

                    // Next Shot Trigger button
                    Button(
                        onClick = onNewShot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen)
                    ) {
                        Text("READY FOR NEXT SHOT", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity metric card tile styled exactly like LaneTrax.
 */
@Composable
private fun LaneTraxMetricCard(
    label: String,
    value: String,
    unit: String,
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

/**
 * Replay Viewport that renders the looping delivery clip.
 */
@Composable
private fun VideoReplayViewport(
    shot: ShotData,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // In on-device production, this hosts the looping MediaPlayer / VideoView / ExoPlayer.
        // For preview / demo, we render the approach pose overlay and looping indicator.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Looping Replay",
                tint = NeonStrikeGreen,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SHOT REPLAY (LOOPING)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "1080x1920 • 60 FPS • ${shot.shotId.take(8)}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp
            )
        }
    }
}
