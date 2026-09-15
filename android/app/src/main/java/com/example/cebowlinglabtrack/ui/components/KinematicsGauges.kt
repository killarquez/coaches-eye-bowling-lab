package com.example.cebowlinglabtrack.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
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
 * Biomechanical circular arc gauge for an individual joint or posture angle.
 */
@Composable
fun KinematicArcGauge(
    title: String,
    value: Double,
    unit: String = "°",
    minVal: Double = 0.0,
    maxVal: Double = 90.0,
    idealRange: ClosedFloatingPointRange<Double> = 30.0..45.0,
    accentColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    val progress = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
    val isIdeal = value in idealRange

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                // Background track (240 deg sweep, starting at 150 deg)
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Filled arc
                val activeSweep = 240f * progress
                drawArc(
                    color = if (isIdeal) accentColor else ElectricAmber,
                    startAngle = 150f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${value}$unit",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isIdeal) "OPTIMAL" else "REVIEW",
            color = if (isIdeal) NeonStrikeGreen else ElectricAmber,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Grid of all core biomechanical metrics for the bowler.
 */
@Composable
fun BowlerKinematicsDashboard(
    kinematics: BowlerKinematics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BOWLER KINEMATICS",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "RELEASE LEVERAGE",
                color = NeonStrikeGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4 Biomechanical Angle Gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KinematicArcGauge(
                title = "Spine Tilt",
                value = kinematics.spineTiltReleaseDeg,
                idealRange = 30.0..45.0,
                accentColor = NeonStrikeGreen,
                modifier = Modifier.weight(1f)
            )
            KinematicArcGauge(
                title = "Forward Flexion",
                value = kinematics.forwardTiltReleaseDeg,
                idealRange = 22.0..36.0,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            KinematicArcGauge(
                title = "Knee Flexion",
                value = kinematics.slideKneeFlexionDeg,
                minVal = 90.0,
                maxVal = 170.0,
                idealRange = 118.0..138.0,
                accentColor = SoftPurple,
                modifier = Modifier.weight(1f)
            )
            KinematicArcGauge(
                title = "Hip-Shoulder Sep",
                value = kinematics.shoulderHipSeparationDeg,
                idealRange = 24.0..40.0,
                accentColor = ElectricAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Foot Alignment & Drift Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SLIDE FOOT BOARD", color = TextMuted, fontSize = 10.sp)
                Text("Board ${kinematics.slideFootBoard}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("STANCE BOARD", color = TextMuted, fontSize = 10.sp)
                Text("Board ${kinematics.stanceFootBoard}", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("NET LATERAL DRIFT", color = TextMuted, fontSize = 10.sp)
                val driftSign = if (kinematics.footDriftBoards > 0) "+" else ""
                Text(
                    "$driftSign${kinematics.footDriftBoards} boards",
                    color = if (kinematics.footDriftBoards in -5.0..0.0) NeonStrikeGreen else PowerCoral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Kinematic Chain Timing Timeline Bar
        KinematicChainTimingTimeline(kinematics)
    }
}

/**
 * Visual sequence timeline showing Apex -> Slide Plant -> Release deltas.
 */
@Composable
fun KinematicChainTimingTimeline(
    kinematics: BowlerKinematics,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "KINEMATIC CHAIN TIMING",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apex node
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan)
                )
                Text("APEX", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Interval 1: Apex -> Plant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${kinematics.apexToPlantMs}ms", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NeonCyan.copy(alpha = 0.6f))
                )
                Text("Power Step", color = TextMuted, fontSize = 8.sp)
            }

            // Plant node
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ElectricAmber)
                )
                Text("PLANT", color = ElectricAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Interval 2: Plant -> Release
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${kinematics.plantToReleaseMs}ms", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(ElectricAmber.copy(alpha = 0.6f))
                )
                Text("Leverage", color = TextMuted, fontSize = 8.sp)
            }

            // Release node
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonStrikeGreen)
                )
                Text("RELEASE", color = NeonStrikeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
