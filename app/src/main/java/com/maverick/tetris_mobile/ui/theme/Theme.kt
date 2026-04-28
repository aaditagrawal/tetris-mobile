package com.maverick.tetris_mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = Background,
    secondary = TextSecondary,
    onSecondary = Background,
    tertiary = TextTertiary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceBorder,
    onSurfaceVariant = TextSecondary
)

@Composable
fun TetrismobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
