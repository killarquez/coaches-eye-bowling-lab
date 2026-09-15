package com.example.cebowlinglabtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.model.BallMetrics
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary

/**
 * High-speed telemetry HUD card displaying key ball metrics.
 */
@Composable
fun TelemetryHUDCard(
    metrics: BallMetrics?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface.copy(alpha = 0.92f))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BALL TELEMETRY",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SPEED: ",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (metrics != null) "${metrics.launchSpeedMph} → ${metrics.deckSpeedMph} mph" else "--.- mph",
                    color = NeonStrikeGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TelemetryMetricPill(
                label = "LAYDOWN",
                value = metrics?.let { "B ${it.laydownBoard}" } ?: "--",
                subLabel = "0 ft",
                color = NeonCyan
            )
            TelemetryMetricPill(
                label = "ARROWS",
                value = metrics?.let { "B ${it.arrowBoard}" } ?: "--",
                subLabel = "15 ft",
                color = NeonCyan
            )
            TelemetryMetricPill(
                label = "BREAKPOINT",
                value = metrics?.let { "B ${it.breakpointBoard}" } ?: "--",
                subLabel = metrics?.let { "${it.breakpointDistanceFt} ft" } ?: "-- ft",
                color = ElectricAmber
            )
            TelemetryMetricPill(
                label = "ENTRY ANGLE",
                value = metrics?.let { "${it.entryAngleDeg}°" } ?: "--°",
                subLabel = "Pocket",
                color = if ((metrics?.entryAngleDeg ?: 0.0) in 4.0..6.0) NeonStrikeGreen else PowerCoral
            )
        }
    }
}

@Composable
private fun TelemetryMetricPill(
    label: String,
    value: String,
    subLabel: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(1.dp))
        Text(text = subLabel, color = TextSecondary, fontSize = 9.sp)
    }
}
