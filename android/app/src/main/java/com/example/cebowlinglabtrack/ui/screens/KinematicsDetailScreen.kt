package com.example.cebowlinglabtrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.DarkSurfaceVariant
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import com.example.cebowlinglabtrack.ui.components.BowlerKinematicsDashboard

/**
 * Detailed Bowler Kinematics and Biomechanics Breakdown Screen.
 */
@Composable
fun KinematicsDetailScreen(
    kinematics: BowlerKinematics?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BIOMECHANICS LAB",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "ON-DEVICE SKELETAL POSE ESTIMATION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "SNAPDRAGON NPU",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (kinematics != null) {
            // Main Kinematics Dashboard
            BowlerKinematicsDashboard(kinematics = kinematics)

            Spacer(modifier = Modifier.height(14.dp))

            // Coaching Biomechanical Diagnosis Card
            CoachingDiagnosisCard(kinematics)

            Spacer(modifier = Modifier.height(14.dp))

            // Reference Standards Reference
            BiomechanicalStandardsCard()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Kinematic Shot Data Available",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Trigger a shot on the Live View to capture skeletal kinematics.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CoachingDiagnosisCard(k: BowlerKinematics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "COACH'S EYE DIAGNOSIS",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        val spineStatus = if (k.spineTiltReleaseDeg in 30.0..45.0) {
            "Optimal lateral spine tilt (${k.spineTiltReleaseDeg}°). Excellent ball clearance and head stability over the swing line."
        } else if (k.spineTiltReleaseDeg < 30.0) {
            "Upright spine angle (${k.spineTiltReleaseDeg}°). Consider tilting more towards ball-side to lower center of gravity."
        } else {
            "Excessive lateral tilt (${k.spineTiltReleaseDeg}°). Risk of losing leverage at release."
        }
        DiagnosisItem(label = "SPINE TILT", text = spineStatus, isGood = k.spineTiltReleaseDeg in 30.0..45.0)

        Spacer(modifier = Modifier.height(8.dp))

        val kneeStatus = if (k.slideKneeFlexionDeg in 118.0..138.0) {
            "Deep leverage bend (${k.slideKneeFlexionDeg}°). Allows smooth transfer of momentum into the deck."
        } else {
            "High knee position (${k.slideKneeFlexionDeg}°). Deepen knee bend on slide plant for better drive."
        }
        DiagnosisItem(label = "KNEE LEVERAGE", text = kneeStatus, isGood = k.slideKneeFlexionDeg in 118.0..138.0)

        Spacer(modifier = Modifier.height(8.dp))

        val separationStatus = if (k.shoulderHipSeparationDeg in 24.0..40.0) {
            "Strong core rotational separation (${k.shoulderHipSeparationDeg}°). Creating high elastic energy stored in torso."
        } else {
            "Moderate hip-shoulder rotation (${k.shoulderHipSeparationDeg}°). Open hips earlier in backswing."
        }
        DiagnosisItem(label = "HIP-SHOULDER SEPARATION", text = separationStatus, isGood = k.shoulderHipSeparationDeg in 24.0..40.0)
    }
}

@Composable
private fun DiagnosisItem(label: String, text: String, isGood: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "• ",
            color = if (isGood) NeonStrikeGreen else ElectricAmber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Column {
            Text(
                text = label,
                color = if (isGood) NeonStrikeGreen else ElectricAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun BiomechanicalStandardsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "PRO TOUR BENCHMARKS",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        BenchmarkRow(metric = "Lateral Spine Tilt", proRange = "32° - 44°", modernTwoHander = "38° - 48°")
        BenchmarkRow(metric = "Forward Trunk Flexion", proRange = "22° - 35°", modernTwoHander = "28° - 42°")
        BenchmarkRow(metric = "Slide Knee Flexion", proRange = "120° - 135°", modernTwoHander = "115° - 130°")
        BenchmarkRow(metric = "Hip-Shoulder Separation", proRange = "25° - 35°", modernTwoHander = "35° - 45°")
        BenchmarkRow(metric = "Apex to Plant Timing", proRange = "250ms - 320ms", modernTwoHander = "220ms - 290ms")
    }
}

@Composable
private fun BenchmarkRow(metric: String, proRange: String, modernTwoHander: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(metric, color = TextMuted, fontSize = 11.sp)
        Text(proRange, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
