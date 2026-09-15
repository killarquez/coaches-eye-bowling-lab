package com.example.cebowlinglabtrack.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoachingDarkColorScheme = darkColorScheme(
    primary = UsbcGold,
    onPrimary = Color.Black,
    primaryContainer = UsbcNavyDark,
    onPrimaryContainer = UsbcGoldLight,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF07274E),
    onSecondaryContainer = NeonCyan,
    tertiary = UsbcRed,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = UsbcRed
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
