package com.example.cebowlinglabtrack.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoachingDarkColorScheme = darkColorScheme(
    primary = NeonStrikeGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00381B),
    onPrimaryContainer = NeonStrikeGreen,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00363D),
    onSecondaryContainer = NeonCyan,
    tertiary = ElectricAmber,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = PowerCoral
)

@Composable
fun CEBowlingLabTrackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CoachingDarkColorScheme,
        typography = Typography,
        content = content
    )
}
