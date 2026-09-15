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
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral

/**
 * 1:1 LaneTrax-style Split-Screen Shot Review & Replay Interface.
 *
 * Left side: Continuous 2D Top-Down Lane with neon green ball trajectory.
 * Right side:
 *   - Video Replay Player (looping shot playback with fullscreen toggle)
 *   - Action Buttons ("Correct", "Save", "Delete")
 *   - Scrollable 2-Column Grid of Metric Cards (Foul Line, Arrows, Entry Board, Rev Rate, Breakpoint, Angles, Speeds)
 */
@Composable
fun LaneTraxReviewView(
    shot: ShotData,
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

    val cardTealBackground = Color(0xFF134E4A) // Dark teal card matching LaneTrax
    val cardTealBorder = Color(0xFF14B8A6).copy(alpha = 0.35f)
    val buttonBlue = Color(0xFF0284C7)
    val buttonPurple = Color(0xFF7C3AED)
    val buttonRed = Color(0xFFB91C1C)

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
                                .background(Color(0xFF6D28D9).copy(alpha = 0.85f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen Replay",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
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
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonBlue)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Edit, contentDescription = "Correct", tint = Color.White, modifier = Modifier.size(20.dp))
                                Text("Correct", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Save Button (Purple)
                        Button(
                            onClick = onSaveShot,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonPurple)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SaveAlt, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(20.dp))
                                Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Delete Button (Red)
                    Button(
                        onClick = onDeleteShot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonRed)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. METRIC CARDS 2x2 GRID (LaneTrax Style)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Foul Line", "${sp.laydownBoard}", "", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Arrows", "${sp.arrowBoard}", "", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Entry Board", "${sp.entryBoard}", "", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Rev Rate", "${dy.rpm}", "rpm", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Breakpoint\nBoard", "${sp.breakpointBoard}", "", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Breakpoint\nDistance", "${sp.breakpointDistanceFt}", "ft", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Launch\nAngle", "${an.launchAngleDeg}°", "", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Impact\nAngle", "${an.impactAngleDeg}°", "", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Launch\nSpeed", "${sd.launchSpeedMph}", "mph", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Impact\nSpeed", "${sd.entrySpeedMph}", "mph", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LaneTraxMetricCard("Power\nScore", String.format("%.2f", dy.powerScore), "", cardTealBackground, cardTealBorder, Modifier.weight(1f))
                        LaneTraxMetricCard("Deflection", "${sp.pinDeckDeflection}", "b", cardTealBackground, cardTealBorder, Modifier.weight(1f))
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
