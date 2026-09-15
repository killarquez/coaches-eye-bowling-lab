package com.example.cebowlinglabtrack.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.data.export.ShotJsonExporter
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.tracking.TelemetryExtractor
import com.example.cebowlinglabtrack.theme.DarkBackground
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
 * Session History Screen: Multi-Shot Consistency Analytics, Specto Skill Tier, and JSON Exporter.
 */
@Composable
fun SessionHistoryScreen(
    shots: List<ShotData>,
    activeShotId: String?,
    onSelectShot: (ShotData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExportDialogForShot by remember { mutableStateOf<ShotData?>(null) }

    val sessionStats = remember(shots) {
        TelemetryExtractor.calculateSessionStats(shots.map { it.spectoTelemetry })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
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
                    text = "SESSION HISTORY",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${shots.size} SHOTS ANALYZED • KEGEL SPECTO ENGINE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "COACH'S EYE LAB",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Session Consistency Summary Banner
        if (shots.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SESSION TIER: ",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricAmber.copy(alpha = 0.2f))
                                .border(1.dp, ElectricAmber, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = sessionStats.skillTier.tierName,
                                color = ElectricAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "AVG POWER: ${sessionStats.averagePowerScore}",
                            color = PowerCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ACCURACY: ${sessionStats.accuracyScore}",
                            color = NeonStrikeGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryPill("AVG SPEED", "${sessionStats.avgLaunchSpeedMph} mph", "Δ ${sessionStats.launchSpeedRangeMph}")
                    SummaryPill("AVG RPM", "${sessionStats.avgRpm}", "Δ ${sessionStats.rpmRange}")
                    SummaryPill("AVG ANGLE", "${sessionStats.avgLaunchAngleDeg}°", "Δ ${sessionStats.launchAngleRangeDeg}°")
                    SummaryPill("BP RANGE", "Δ ${sessionStats.breakpointBoardRange} B", "Tolerance")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (shots.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No shots recorded yet. Trigger a shot in Live Tracking.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shots) { shot ->
                    val isSelected = shot.shotId == activeShotId
                    ShotHistoryCard(
                        shot = shot,
                        isSelected = isSelected,
                        onClick = { onSelectShot(shot) },
                        onExportJson = { showExportDialogForShot = shot }
                    )
                }
            }
        }
    }

    // JSON Export Dialog
    showExportDialogForShot?.let { shotToExport ->
        val jsonText = remember(shotToExport) { ShotJsonExporter.exportToJson(shotToExport) }

        AlertDialog(
            onDismissRequest = { showExportDialogForShot = null },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Bowling Shot JSON", jsonText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Section 4 JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showExportDialogForShot = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY SECTION 4 JSON", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialogForShot = null }) {
                    Text("CLOSE", color = TextPrimary)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SHOT TELEMETRY JSON", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("22 SPECTO PARAMS", color = ElectricAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = jsonText,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun SummaryPill(label: String, value: String, subtext: String) {
    Column {
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtext, color = TextSecondary, fontSize = 8.sp)
    }
}

@Composable
private fun ShotHistoryCard(
    shot: ShotData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onExportJson: () -> Unit
) {
    val sp = shot.spectoTelemetry.spatial
    val an = shot.spectoTelemetry.angles
    val sd = shot.spectoTelemetry.speed
    val dy = shot.spectoTelemetry.dynamics
    val kn = shot.kinematics

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) DarkSurfaceVariant else DarkSurface)
            .border(
                1.dp,
                if (isSelected) NeonStrikeGreen else DarkCardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SHOT #${shot.shotNumber}",
                        color = if (isSelected) NeonStrikeGreen else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElectricAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(dy.skillTier, color = ElectricAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = shot.timestamp.take(19).replace("T", " "),
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${sd.launchSpeedMph} → ${sd.entrySpeedMph} mph",
                        color = NeonStrikeGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Power: ${String.format("%.2f", dy.powerScore)} • ${dy.rpm} RPM",
                        color = PowerCoral,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onExportJson) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export JSON",
                        tint = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Key 22-Specto parameters row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("LAYDOWN", color = TextMuted, fontSize = 8.sp)
                Text("B ${sp.laydownBoard}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("ARROWS", color = TextMuted, fontSize = 8.sp)
                Text("B ${sp.arrowBoard}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("BREAKPOINT", color = TextMuted, fontSize = 8.sp)
                Text("B ${sp.breakpointBoard} @ ${sp.breakpointDistanceFt}ft", color = ElectricAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("ENTRY", color = TextMuted, fontSize = 8.sp)
                Text("B ${sp.entryBoard} (${an.impactAngleDeg}°)", color = NeonStrikeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("DEFLECT", color = TextMuted, fontSize = 8.sp)
                Text("${sp.pinDeckDeflection} B", color = SoftPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
