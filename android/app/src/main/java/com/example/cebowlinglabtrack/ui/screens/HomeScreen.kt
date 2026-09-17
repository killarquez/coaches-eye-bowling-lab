package com.example.cebowlinglabtrack.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.data.export.SessionPdfReportGenerator
import com.example.cebowlinglabtrack.domain.model.BowlerProfile
import com.example.cebowlinglabtrack.domain.model.BowlingStyle
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.TapeColor
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkCardBorderGold
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.DarkSurfaceVariant
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import com.example.cebowlinglabtrack.theme.UsbcGold
import com.example.cebowlinglabtrack.theme.UsbcGoldDark
import com.example.cebowlinglabtrack.theme.UsbcNavy
import com.example.cebowlinglabtrack.theme.UsbcNavyDark
import com.example.cebowlinglabtrack.theme.UsbcNavyLight
import com.example.cebowlinglabtrack.theme.UsbcRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun HomeScreen(
    activeBowler: BowlerProfile?,
    allBowlers: List<BowlerProfile>,
    savedShots: List<ShotData>,
    isOpticalRevActive: Boolean,
    onSelectBowler: (BowlerProfile) -> Unit,
    onSaveBowler: (BowlerProfile) -> Unit,
    onToggleOpticalRev: (Boolean) -> Unit,
    onStartTrainingSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showAddBowlerDialog by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    val filteredBowlers = remember(searchQuery, allBowlers) {
        if (searchQuery.isBlank()) {
            allBowlers
        } else {
            allBowlers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val athleteShots = remember(activeBowler, savedShots) {
        if (activeBowler == null) emptyList()
        else savedShots.filter { it.bowlerId == activeBowler.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. BRAND HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(UsbcNavy, UsbcNavyLight)
                                )
                            )
                            .border(1.5.dp, UsbcGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CE",
                            color = UsbcGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COACH'S EYE BOWLING LAB",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Professional Biomechanical Coaching & Specto Analytics",
                    color = UsbcGold,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showAddBowlerDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorderGold, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Student",
                    tint = UsbcGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. SEARCH & ROSTER SELECTOR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search student by name or ID (e.g. Marcus, CEB-101)...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = UsbcGold,
                unfocusedBorderColor = DarkCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Athlete Chips Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredBowlers.forEach { bowler ->
                val isSelected = activeBowler?.id == bowler.id
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) UsbcNavyLight else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) UsbcGold else DarkCardBorder
                    ),
                    modifier = Modifier.clickable { onSelectBowler(bowler) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) UsbcGold else DarkCardBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bowler.name.take(1).uppercase(),
                                color = if (isSelected) UsbcNavyDark else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${bowler.name} (${bowler.id})",
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = UsbcGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. ACTIVE BOWLER PROFILE CARD
        if (activeBowler != null) {
            AthleteProfileCard(
                bowler = activeBowler,
                shotsCount = athleteShots.size,
                onEditClick = { showAddBowlerDialog = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. BIOMECHANICAL BENCHMARK BASELINES (2x2 Grid)
            BenchmarkBaselinesSection(bowler = activeBowler)

            Spacer(modifier = Modifier.height(14.dp))

            // 5. DIRECT OPTICAL REV COUNTER (COG TO PAP) TOGGLE
            OpticalRevToggleCard(
                isActive = isOpticalRevActive,
                onToggle = onToggleOpticalRev
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. ACTION BUTTONS: START TRAINING SESSION & EXPORT PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartTrainingSession,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UsbcGold,
                        contentColor = UsbcNavyDark
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "START SESSION",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (isExportingPdf) return@OutlinedButton
                        isExportingPdf = true
                        coroutineScope.launch {
                            try {
                                val pdfFile = withContext(Dispatchers.IO) {
                                    SessionPdfReportGenerator.generateSessionPdf(
                                        context = context,
                                        bowler = activeBowler,
                                        shots = athleteShots,
                                        sessionTitle = "Coach's Eye Training - ${activeBowler.name}"
                                    )
                                }
                                val shareIntent = SessionPdfReportGenerator.createSharePdfIntent(context, pdfFile)
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Specto PDF Report"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isExportingPdf = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, UsbcGold),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = UsbcGold
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExportingPdf) "GENERATING..." else "EXPORT PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. RECENT SESSIONS / SHOTS AUDIT FOR THIS ATHLETE
            RecentShotsSection(
                bowler = activeBowler,
                shots = athleteShots
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No athlete selected from roster",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showAddBowlerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = UsbcNavyLight)
                    ) {
                        Text("+ Add First Student", color = UsbcGold)
                    }
                }
            }
        }
    }

    // Modal to Add / Edit Athlete Profile
    if (showAddBowlerDialog) {
        AddBowlerDialog(
            existingBowler = activeBowler,
            onDismiss = { showAddBowlerDialog = false },
            onSave = { bowler ->
                onSaveBowler(bowler)
                showAddBowlerDialog = false
            }
        )
    }
}

// =============================================================================
// SUB-COMPONENTS: ATHLETE CARD, BENCHMARKS, OPTICAL REV TOGGLE, RECENT SHOTS
// =============================================================================

@Composable
private fun AthleteProfileCard(
    bowler: BowlerProfile,
    shotsCount: Int,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderGold),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Name, ID, Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(UsbcNavy, UsbcNavyLight)
                            )
                        )
                        .border(1.5.dp, UsbcGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bowler.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2),
                        color = UsbcGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bowler.name,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = UsbcNavyDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = bowler.id,
                                color = UsbcGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${bowler.style.displayName()}  •  ${bowler.handedness.name} Handed  •  ${bowler.heightInches.toInt()} in tall",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = DarkCardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Stats row: Book Avg, High Game, High Series, PAP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(title = "BOOK AVG", value = "${bowler.bookAverage}")
                StatPill(title = "HIGH GAME", value = "${bowler.careerHighGame}")
                StatPill(title = "HIGH SERIES", value = "${bowler.careerHighSeries}")
                StatPill(title = "PAP COORDINATE", value = bowler.papCoordinates)
            }

            if (bowler.primaryGoal.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grade,
                            contentDescription = null,
                            tint = UsbcGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Goal: ${bowler.primaryGoal}",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REV TRACKING SETUP",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = UsbcNavyLight,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, UsbcGold)
                ) {
                    Text(
                        text = when (bowler.tapeColor) {
                            TapeColor.WHITE -> "⚪ White Tape"
                            TapeColor.NEON_GREEN -> "🟢 Neon Green Tape"
                            TapeColor.HOT_PINK -> "🟣 Hot Pink Tape"
                            TapeColor.NO_TAPE -> "🚫 Untaped (Auto-Feature / Physics)"
                        },
                        color = UsbcGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(title: String, value: String) {
    Column {
        Text(text = title, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BenchmarkBaselinesSection(bowler: BowlerProfile) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BIOMECHANICAL BENCHMARKS (PRELOADED)",
                color = UsbcGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Sessions: ${bowler.totalSessionsCoached}",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BenchmarkCard(
                title = "LAUNCH SPEED",
                value = String.format(Locale.US, "%.1f mph", bowler.benchmarkSpeedMph),
                sub = "Target ±0.6",
                color = UsbcGold,
                modifier = Modifier.weight(1f)
            )
            BenchmarkCard(
                title = "REV RATE (RPM)",
                value = "${bowler.benchmarkRpm} RPM",
                sub = "Optical COG-PAP",
                color = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            BenchmarkCard(
                title = "AXIS TILT",
                value = String.format(Locale.US, "%.0f°", bowler.benchmarkAxisTiltDeg),
                sub = "Norm 10°-18°",
                color = NeonStrikeGreen,
                modifier = Modifier.weight(1f)
            )
            BenchmarkCard(
                title = "AXIS ROTATION",
                value = String.format(Locale.US, "%.0f°", bowler.benchmarkAxisRotationDeg),
                sub = "Norm 45°-65°",
                color = ElectricAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BenchmarkCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = sub, color = TextMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun OpticalRevToggleCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) UsbcGold else DarkCardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isActive) NeonStrikeGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Direct Optical Rev Counter (Tape COG to PAP)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tracks tape marker at 120 FPS to measure exact revolutions, Axis Tilt (0°-30°), and Axis Rotation (0°-90°).",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = UsbcNavyDark,
                    checkedTrackColor = UsbcGold,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkCardBorder
                )
            )
        }
    }
}

@Composable
private fun RecentShotsSection(
    bowler: BowlerProfile,
    shots: List<ShotData>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT SESSION LOGS (${shots.size} SHOTS)",
                color = UsbcGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            if (shots.isNotEmpty()) {
                val avgAcc = shots.map { it.spectoTelemetry.dynamics.accuracyScore }.average()
                Text(
                    text = String.format(Locale.US, "Avg Accuracy: %.1f", avgAcc),
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (shots.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No shots recorded yet for ${bowler.name}. Tap 'START SESSION' to begin tracking live at 120 FPS.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(14.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            shots.take(5).forEach { shot ->
                ShotHistoryItemRow(shot = shot)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ShotHistoryItemRow(shot: ShotData) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(UsbcNavyLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${shot.shotNumber}",
                        color = UsbcGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    val sp = shot.spectoTelemetry.spatial
                    val sd = shot.spectoTelemetry.speed
                    Text(
                        text = "Board ${sp.arrowBoard} @ 15ft  →  Pocket ${sp.entryBoard}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${sd.launchSpeedMph} mph  •  ${shot.spectoTelemetry.dynamics.rpm} RPM  •  Acc ${shot.spectoTelemetry.dynamics.accuracyScore.toInt()}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = DarkSurfaceVariant
            ) {
                Text(
                    text = "${shot.spectoTelemetry.dynamics.powerScore} Pwr",
                    color = UsbcGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// =============================================================================
// MODAL DIALOG: ADD / EDIT ATHLETE PROFILE
// =============================================================================

@Composable
private fun AddBowlerDialog(
    existingBowler: BowlerProfile?,
    onDismiss: () -> Unit,
    onSave: (BowlerProfile) -> Unit
) {
    var id by remember { mutableStateOf(existingBowler?.id ?: "CEB-${(100..999).random()}") }
    var name by remember { mutableStateOf(existingBowler?.name ?: "") }
    var style by remember { mutableStateOf(existingBowler?.style ?: BowlingStyle.TWO_HANDED) }
    var handedness by remember { mutableStateOf(existingBowler?.handedness ?: Handedness.RIGHT) }
    var heightInches by remember { mutableStateOf(existingBowler?.heightInches?.toString() ?: "70") }
    var bookAverage by remember { mutableStateOf(existingBowler?.bookAverage?.toString() ?: "190") }
    var careerHighGame by remember { mutableStateOf(existingBowler?.careerHighGame?.toString() ?: "279") }
    var careerHighSeries by remember { mutableStateOf(existingBowler?.careerHighSeries?.toString() ?: "680") }
    var papCoordinates by remember { mutableStateOf(existingBowler?.papCoordinates ?: "4 3/4\" over by 1/2\" up") }
    var speed by remember { mutableStateOf(existingBowler?.benchmarkSpeedMph?.toString() ?: "16.0") }
    var rpm by remember { mutableStateOf(existingBowler?.benchmarkRpm?.toString() ?: "420") }
    var primaryGoal by remember { mutableStateOf(existingBowler?.primaryGoal ?: "Rev Rate & Ball Speed Synchronization") }
    var tapeColor by remember { mutableStateOf(existingBowler?.tapeColor ?: TapeColor.WHITE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingBowler == null) "Add Student Athlete" else "Edit Student Profile",
                color = UsbcGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("Student ID") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightInches,
                        onValueChange = { heightInches = it },
                        label = { Text("Height (in)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Delivery Style Selector
                Text("Delivery Style:", color = TextSecondary, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BowlingStyle.values().filter { it != BowlingStyle.ONE_HANDED }.forEach { s ->
                        val isSel = style == s
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) UsbcNavyLight else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) UsbcGold else DarkCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { style = s }
                        ) {
                            Text(
                                text = s.displayName(),
                                color = if (isSel) UsbcGold else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Handedness Selector
                Text("Handedness:", color = TextSecondary, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Handedness.values().forEach { h ->
                        val isSel = handedness == h
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) UsbcNavyLight else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) UsbcGold else DarkCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { handedness = h }
                        ) {
                            Text(
                                text = if (h == Handedness.RIGHT) "Right Handed" else "Left Handed",
                                color = if (isSel) UsbcGold else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bookAverage,
                        onValueChange = { bookAverage = it },
                        label = { Text("Book Avg") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = careerHighGame,
                        onValueChange = { careerHighGame = it },
                        label = { Text("High Game") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = careerHighSeries,
                        onValueChange = { careerHighSeries = it },
                        label = { Text("High Series") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = papCoordinates,
                    onValueChange = { papCoordinates = it },
                    label = { Text("PAP Coordinates") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = speed,
                        onValueChange = { speed = it },
                        label = { Text("Bench Speed (mph)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rpm,
                        onValueChange = { rpm = it },
                        label = { Text("Bench RPM") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = primaryGoal,
                    onValueChange = { primaryGoal = it },
                    label = { Text("Primary Training Goal") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Rev Tracking Setup Selector
                Text("Optical Rev Tracking Setup:", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TapeColor.values().forEach { tc ->
                        val isSel = tapeColor == tc
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) UsbcNavyLight else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) UsbcGold else DarkCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tapeColor = tc }
                        ) {
                            Text(
                                text = when (tc) {
                                    TapeColor.WHITE -> "⚪ White"
                                    TapeColor.NEON_GREEN -> "🟢 Green"
                                    TapeColor.HOT_PINK -> "🟣 Pink"
                                    TapeColor.NO_TAPE -> "🚫 None"
                                },
                                color = if (isSel) UsbcGold else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val updated = BowlerProfile(
                        id = id.trim(),
                        name = name.trim(),
                        heightInches = heightInches.toDoubleOrNull() ?: 70.0,
                        handedness = handedness,
                        style = style,
                        bookAverage = bookAverage.toIntOrNull() ?: 190,
                        careerHighGame = careerHighGame.toIntOrNull() ?: 279,
                        careerHighSeries = careerHighSeries.toIntOrNull() ?: 650,
                        papCoordinates = papCoordinates.trim(),
                        benchmarkSpeedMph = speed.toDoubleOrNull() ?: 16.0,
                        benchmarkRpm = rpm.toIntOrNull() ?: 420,
                        primaryGoal = primaryGoal.trim(),
                        tapeColor = tapeColor
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = UsbcGold, contentColor = UsbcNavyDark)
            ) {
                Text("Save Athlete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkSurface
    )
}