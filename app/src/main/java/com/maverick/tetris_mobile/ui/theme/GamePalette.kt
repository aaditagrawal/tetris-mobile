package com.maverick.tetris_mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.maverick.tetris_mobile.game.PieceType

@Immutable
enum class GamePalette(
    val id: String,
    val title: String,
    val description: String,
    val background: Color,
    val surface: Color,
    val border: Color,
    private val cyan: Color,
    val yellow: Color,
    private val purple: Color,
    private val green: Color,
    private val red: Color,
    private val blue: Color,
    private val orange: Color
) {
    MUTED("muted", "Muted", "Soft, understated colors", Color(0xFF0A0A0A), Color(0xFF161616), Color(0xFF2A2A2A),
        Color(0xFF5B9EA6), Color(0xFFB8A455), Color(0xFF8B6FAE), Color(0xFF6B9E6B), Color(0xFFA66B6B), Color(0xFF5B72A6), Color(0xFFA6835B)),
    ARCADE("arcade", "Arcade", "Bright arcade classics", Color(0xFF090B13), Color(0xFF151925), Color(0xFF2B3345),
        Color(0xFF39D9E8), Color(0xFFF2D64B), Color(0xFFB782F5), Color(0xFF73D66A), Color(0xFFF77983), Color(0xFF689DF5), Color(0xFFF5AD59)),
    AURORA("aurora", "Aurora", "Cool pastels on violet", Color(0xFF100D18), Color(0xFF1D1829), Color(0xFF363044),
        Color(0xFF93D9DE), Color(0xFFE9DDA2), Color(0xFFC6A3E8), Color(0xFFA9D4BA), Color(0xFFE9A4BD), Color(0xFF9EBAE9), Color(0xFFE5BD9B)),
    EMBER("ember", "Ember", "Warm colors on charcoal", Color(0xFF140E0C), Color(0xFF231B17), Color(0xFF3D3029),
        Color(0xFF9CC5BC), Color(0xFFE6C36B), Color(0xFFC69ABB), Color(0xFFB1C586), Color(0xFFE48D80), Color(0xFF91AEC8), Color(0xFFE7AF75)),
    OCEAN("ocean", "Ocean", "Sea glass on deep blue", Color(0xFF081218), Color(0xFF11232D), Color(0xFF2B404D),
        Color(0xFF6CD5DA), Color(0xFFE0D58F), Color(0xFFAFACE0), Color(0xFF87D4B2), Color(0xFFEFA7AF), Color(0xFF82B6E4), Color(0xFFE6BA92));

    val textPrimary: Color get() = Color(0xFFF0F0F0)
    val textSecondary: Color get() = Color(0xFFB0B0B0)
    val textTertiary: Color get() = Color(0xFF979797)

    fun colorFor(type: PieceType): Color = when (type) {
        PieceType.I -> cyan
        PieceType.O -> yellow
        PieceType.T -> purple
        PieceType.S -> green
        PieceType.Z -> red
        PieceType.J -> blue
        PieceType.L -> orange
    }

    companion object {
        fun fromId(id: String?): GamePalette = entries.firstOrNull { it.id == id } ?: MUTED
    }
}

val LocalGamePalette = staticCompositionLocalOf { GamePalette.MUTED }
