package com.example.cebowlinglabtrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.tracking.TelemetryExtractor
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.DarkSurfaceVariant
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import com.example.cebowlinglabtrack.ui.components.SpectoScorecard
import com.example.cebowlinglabtrack.ui.components.TelemetryHUDCard
import com.example.cebowlinglabtrack.ui.components.TopDownLaneCanvas

/**
 * 2D Top-Down Lane Trajectory Analysis Screen with full 22-parameter Kegel Specto inspection.
 */
@Composable
fun TopDownLaneScreen(
    shot: ShotData?,
    modifier: Modifier = Modifier
) {
    var scrubDistanceFt by remember { mutableFloatStateOf(62.8f) }
    var showFullScorecard by remember { mutableStateOf(true) }

    val trajectory = shot?.trajectoryPoints ?: emptyList()
    val visibleTrajectory = trajectory.filter { it.yFt <= scrubDistanceFt }
    val metrics = shot?.ballMetrics
    val specto = shot?.spectoTelemetry

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LANE TRAJECTORY",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "USBC 60-FT + SPECTO 22-PARAMETER VIEW",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // View toggle pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showFullScorecard) ElectricAmber else DarkSurfaceVariant)
                        .clickable { showFullScorecard = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "SPECTO",
                        color = if (showFullScorecard) DarkBackground else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!showFullScorecard) NeonCyan else DarkSurfaceVariant)
                        .clickable { showFullScorecard = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "HUD",
                        color = if (!showFullScorecard) DarkBackground else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Full 2D Lane Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
        ) {
            TopDownLaneCanvas(
                trajectory = visibleTrajectory,
                metrics = metrics,
                spectoTelemetry = specto,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Distance Scrubber Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("DISTANCE SCRUBBER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                val currentScrubBoard = if (trajectory.isNotEmpty()) {
                    TelemetryExtractor.interpolateBoardAtDistance(trajectory, scrubDistanceFt.toDouble())
                } else 20.0
                Text(
                    text = "${scrubDistanceFt.toInt()} ft (Board ${(currentScrubBoard * 10).toInt() / 10.0})",
                    color = NeonStrikeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = scrubDistanceFt,
                onValueChange = { scrubDistanceFt = it },
                valueRange = 0f..62.8f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonStrikeGreen,
                    activeTrackColor = NeonStrikeGreen,
                    inactiveTrackColor = DarkCardBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full Specto Scorecard or HUD Card
        if (showFullScorecard && specto != null) {
            SpectoScorecard(
                telemetry = specto,
                shotNumber = shot.shotNumber,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TelemetryHUDCard(metrics = metrics, modifier = Modifier.fillMaxWidth())
        }
    }
}
