package com.maverick.tetris_mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun TetrismobileTheme(
    palette: GamePalette = GamePalette.MUTED,
    content: @Composable () -> Unit
) {
    val colors = remember(palette) {
        darkColorScheme(
            primary = palette.textPrimary,
            onPrimary = palette.background,
            secondary = palette.textSecondary,
            onSecondary = palette.background,
            tertiary = palette.textTertiary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.border,
            onSurfaceVariant = palette.textSecondary
        )
    }
    val typography = remember(palette) { gameTypography(palette) }
    CompositionLocalProvider(LocalGamePalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
