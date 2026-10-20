package com.maverick.tetris_mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val Background: Color @Composable @ReadOnlyComposable get() = LocalGamePalette.current.background
val Surface: Color @Composable @ReadOnlyComposable get() = LocalGamePalette.current.surface
val SurfaceBorder: Color @Composable @ReadOnlyComposable get() = LocalGamePalette.current.border
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalGamePalette.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalGamePalette.current.textSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalGamePalette.current.textTertiary
val PieceYellow: Color @Composable @ReadOnlyComposable get() = LocalGamePalette.current.yellow
