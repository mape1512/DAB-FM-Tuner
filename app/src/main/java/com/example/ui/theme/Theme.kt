package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val CockpitDarkColorScheme = darkColorScheme(
    primary = ElectricAmber,
    onPrimary = Color(0xFF1A0B00),
    primaryContainer = ElectricAmberContainer,
    onPrimaryContainer = ElectricAmberBright,
    secondary = SignalCyan,
    onSecondary = Color(0xFF001F28),
    secondaryContainer = SignalCyanContainer,
    onSecondaryContainer = SignalCyanBright,
    tertiary = OnAirEmerald,
    onTertiary = Color(0xFF002112),
    background = CockpitObsidian,
    onBackground = TextPrimaryDark,
    surface = CockpitSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CockpitSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = CockpitSurfaceHighlight,
    outline = CockpitBorder,
    outlineVariant = Color(0xFF1E2736),
    error = ErrorCrimson,
    onError = Color.White
)

private val CockpitLightColorScheme = lightColorScheme(
    primary = DayAmberPrimary,
    onPrimary = Color.White,
    primaryContainer = DayAmberContainer,
    onPrimaryContainer = Color(0xFF3E1600),
    secondary = DayCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = DayCyanContainer,
    onSecondaryContainer = Color(0xFF002836),
    tertiary = Color(0xFF00874B),
    onTertiary = Color.White,
    background = DayBackground,
    onBackground = TextPrimaryLight,
    surface = DaySurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaySurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFDCE3EE),
    outline = DayBorder,
    outlineVariant = Color(0xFFDCE3EF),
    error = Color(0xFFBA1A2D),
    onError = Color.White
)

val AutoTunerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AutoTunerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CockpitDarkColorScheme else CockpitLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AutoTunerTypography,
        shapes = AutoTunerShapes,
        content = content
    )
}
