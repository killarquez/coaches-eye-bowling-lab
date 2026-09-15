package com.example.cebowlinglabtrack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LineAxis
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.ui.TrackingViewModel
import com.example.cebowlinglabtrack.ui.screens.CalibrationScreen
import com.example.cebowlinglabtrack.ui.screens.KinematicsDetailScreen
import com.example.cebowlinglabtrack.ui.screens.LiveTrackingScreen
import com.example.cebowlinglabtrack.ui.screens.SessionHistoryScreen
import com.example.cebowlinglabtrack.ui.screens.TopDownLaneScreen

enum class BowlingScreenTab(val title: String, val icon: ImageVector) {
    TRACK("Live Track", Icons.Default.Videocam),
    LANE_2D("2D Lane", Icons.Default.LineAxis),
    KINEMATICS("Kinematics", Icons.Default.Person),
    CALIBRATE("Calibrate", Icons.Default.Tune),
    HISTORY("History", Icons.Default.History)
}

@Composable
fun MainNavigation(
    viewModel: TrackingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedShots by viewModel.savedShots.collectAsState()

    var currentTab by remember { mutableStateOf(BowlingScreenTab.TRACK) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = TextPrimary
            ) {
                BowlingScreenTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (selected) NeonStrikeGreen else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (selected) NeonStrikeGreen else TextMuted,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF00381B)
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (currentTab) {
                BowlingScreenTab.TRACK -> {
                    LiveTrackingScreen(
                        state = uiState,
                        onSimulateShot = { preset -> viewModel.simulateShot(preset) },
                        onNavigateCalibration = { currentTab = BowlingScreenTab.CALIBRATE }
                    )
                }
                BowlingScreenTab.LANE_2D -> {
                    TopDownLaneScreen(shot = uiState.activeShot)
                }
                BowlingScreenTab.KINEMATICS -> {
                    KinematicsDetailScreen(kinematics = uiState.liveKinematics ?: uiState.activeShot?.kinematics)
                }
                BowlingScreenTab.CALIBRATE -> {
                    CalibrationScreen(
                        currentCalibration = uiState.calibration,
                        onSaveCalibration = { flL, flR, arL, arR ->
                            viewModel.updateCalibration(flL, flR, arL, arR)
                            currentTab = BowlingScreenTab.TRACK
                        },
                        onCancel = { currentTab = BowlingScreenTab.TRACK }
                    )
                }
                BowlingScreenTab.HISTORY -> {
                    SessionHistoryScreen(
                        shots = savedShots,
                        activeShotId = uiState.activeShot?.shotId,
                        onSelectShot = { shot ->
                            viewModel.selectShot(shot)
                            currentTab = BowlingScreenTab.LANE_2D
                        }
                    )
                }
            }
        }
    }
}
