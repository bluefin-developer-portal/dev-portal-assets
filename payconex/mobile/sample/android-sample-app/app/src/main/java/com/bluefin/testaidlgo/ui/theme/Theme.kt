package com.bluefin.testaidlgo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BluefinColors.Blue,
    onPrimary = Color.White,
    primaryContainer = BluefinColors.PaleBlue,
    onPrimaryContainer = BluefinColors.Navy,
    secondary = BluefinColors.Sky,
    onSecondary = BluefinColors.Navy,
    tertiary = BluefinColors.Yellow,
    onTertiary = BluefinColors.Navy,
    background = BluefinColors.Background,
    onBackground = BluefinColors.Navy,
    surface = Color.White,
    onSurface = BluefinColors.Navy,
    surfaceVariant = BluefinColors.PaleBlue,
    onSurfaceVariant = BluefinColors.TextSecondary,
    outline = BluefinColors.Border,
    outlineVariant = Color(0xFFD5E1EC)
)

private val DarkColorScheme = darkColorScheme(
    primary = BluefinColors.DarkAction,
    onPrimary = BluefinColors.Navy,
    primaryContainer = Color(0xFF173454),
    onPrimaryContainer = BluefinColors.DarkText,
    secondary = BluefinColors.Sky,
    onSecondary = BluefinColors.Navy,
    tertiary = BluefinColors.Yellow,
    onTertiary = BluefinColors.Navy,
    background = BluefinColors.DarkBackground,
    onBackground = BluefinColors.DarkText,
    surface = BluefinColors.DarkSurface,
    onSurface = BluefinColors.DarkText,
    surfaceVariant = Color(0xFF173454),
    onSurfaceVariant = BluefinColors.DarkTextSecondary,
    outline = BluefinColors.DarkBorder,
    outlineVariant = Color(0xFF304864)
)

/**
 * Theme selection is explicit and defaults to dark. MainActivity persists the in-app choice
 * and passes it here; do not replace it with system appearance unless that is a product change.
 * Update both palettes (and the logo/system-bar treatment) when adapting the visual identity.
 */
@Composable
fun BluefinSampleTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    // Keep the Bluefin palette stable instead of inheriting Android wallpaper colors.
    MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography, content = content)
}
