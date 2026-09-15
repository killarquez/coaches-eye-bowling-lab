package com.example.cebowlinglabtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.example.cebowlinglabtrack.domain.model.SpectoTelemetry
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.DarkSurfaceVariant
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.SoftPurple
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary

/**
 * Authentic Kegel Specto Scorecard displaying the full 22-parameter telemetry model.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpectoScorecard(
    telemetry: SpectoTelemetry,
    modifier: Modifier = Modifier,
    shotNumber: Int = 1
) {
    val sp = telemetry.spatial
    val an = telemetry.angles
    val sd = telemetry.speed
    val dy = telemetry.dynamics

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface.copy(alpha = 0.95f))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        // --- 1. HERO HEADER: SKILL TIER, ACCURACY SCORE & POWER SCORE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SPECTO SCORECARD",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricAmber.copy(alpha = 0.2f))
                            .border(1.dp, ElectricAmber, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dy.skillTier,
                            color = ElectricAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "SHOT #$shotNumber • 22-PARAMETER TELEMETRY",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Power Score and Accuracy Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreBadge(
                    label = "POWER",
                    value = String.format("%.2f", dy.powerScore),
                    accentColor = PowerCoral
                )
                ScoreBadge(
                    label = "ACCURACY",
                    value = String.format("%.1f", dy.accuracyScore),
                    accentColor = NeonStrikeGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. SPATIAL METRICS (Boards & Distances) ---
        CategoryHeader(title = "SPATIAL (BOARDS & DISTANCES)", accentColor = NeonCyan)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile("Laydown", "B ${sp.laydownBoard}", "0.0 ft", NeonCyan)
            MetricTile("Loft", "${sp.loftDistanceFt} ft", "Travel", NeonCyan)
            MetricTile("Arrows", "B ${sp.arrowBoard}", "15.0 ft", NeonCyan)
            MetricTile("Pattern Exit", "B ${sp.patternExitBoard}", "40.0 ft", NeonCyan)
            MetricTile("Breakpoint", "B ${sp.breakpointBoard}", "${sp.breakpointDistanceFt} ft", ElectricAmber)
            MetricTile("Pocket Entry", "B ${sp.entryBoard}", "60.0 ft", NeonStrikeGreen)
            MetricTile("Pin Deck Exit", "B ${sp.pinDeckExitBoard}", "62.8 ft", SoftPurple)
            MetricTile("Deflection", "${sp.pinDeckDeflection} B", "Through pins", SoftPurple)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. ANGLES & VECTORS ---
        CategoryHeader(title = "ANGULAR VECTORS", accentColor = ElectricAmber)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                "Launch Angle",
                "${an.launchAngleDeg}°",
                "Relative to center",
                ElectricAmber,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                "Breakpoint Angle",
                "${an.breakpointAngleDeg}°",
                "Hook inflection",
                ElectricAmber,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                "Impact Angle",
                "${an.impactAngleDeg}°",
                "Pocket entry",
                if (an.impactAngleDeg in 4.0..6.0) NeonStrikeGreen else PowerCoral,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. SPEED & DECAY ---
        CategoryHeader(title = "VELOCITY & DECAY", accentColor = NeonStrikeGreen)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile("Launch Speed", "${sd.launchSpeedMph} mph", "0-15 ft", NeonStrikeGreen, Modifier.weight(1f))
            MetricTile("Entry Speed", "${sd.entrySpeedMph} mph", "50-60 ft", NeonStrikeGreen, Modifier.weight(1f))
            MetricTile("Speed Loss", "${sd.speedLossMph} mph", "Friction decay", PowerCoral, Modifier.weight(1f))
            MetricTile("Avg Speed", "${sd.avgSpeedMph} mph", "Full lane", TextPrimary, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 5. BALL MOTION PHASES & DYNAMICS ---
        CategoryHeader(title = "MOTION PHASES & CORE DYNAMICS", accentColor = SoftPurple)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile("Rev Rate", "${dy.rpm} RPM", "Rotational rate", NeonCyan, Modifier.weight(1f))
            MetricTile("Skid Phase", "${dy.skidFt} ft", "Oil slide", NeonCyan, Modifier.weight(1f))
            MetricTile("Hook Phase", "${dy.hookFt} ft", "Friction turn", ElectricAmber, Modifier.weight(1f))
            MetricTile("Roll Phase", "${dy.rollFt} ft", "True forward roll", NeonStrikeGreen, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CategoryHeader(title: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(0.5.dp, DarkCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(text = label.uppercase(), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(1.dp))
        Text(text = subtext, color = TextSecondary, fontSize = 8.sp)
    }
}

@Composable
private fun ScoreBadge(label: String, value: String, accentColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}
