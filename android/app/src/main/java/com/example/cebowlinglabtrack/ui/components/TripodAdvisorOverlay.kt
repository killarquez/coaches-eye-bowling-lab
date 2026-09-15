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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import com.example.cebowlinglabtrack.camera.TripodAngleAdvisor
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary

/**
 * Real-time Tripod Alignment Overlay showing bubble level and pitch/roll guidance.
 */
@Composable
fun TripodAdvisorOverlay(
    tripodStatus: TripodAngleAdvisor.TripodStatus,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface.copy(alpha = 0.88f))
            .border(
                1.dp,
                if (tripodStatus.isLevel) NeonStrikeGreen.copy(alpha = 0.6f) else ElectricAmber.copy(alpha = 0.8f),
                RoundedCornerShape(12.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (tripodStatus.isLevel) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Tripod Level",
                    tint = if (tripodStatus.isLevel) NeonStrikeGreen else ElectricAmber,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = if (tripodStatus.isLevel) "TRIPOD LEVEL" else "ALIGN TRIPOD",
                    color = if (tripodStatus.isLevel) NeonStrikeGreen else ElectricAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "P: ${String.format("%.1f", tripodStatus.pitchDeg)}° R: ${String.format("%.1f", tripodStatus.rollDeg)}°",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            AnimatedVisibility(
                visible = isExpanded || !tripodStatus.isLevel,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = tripodStatus.adviceText,
                        color = if (tripodStatus.isLevel) NeonCyan else ElectricAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: Pitch -6° to -10° | Roll 0.0°",
                        color = TextMuted,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
