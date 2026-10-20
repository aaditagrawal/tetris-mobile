package com.maverick.tetris_mobile.ui.theme

import androidx.compose.ui.graphics.luminance
import com.maverick.tetris_mobile.game.PieceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePaletteTest {
    @Test
    fun savedIdentifiersAreUniqueAndUnknownValuesUseDefault() {
        assertEquals(GamePalette.entries.size, GamePalette.entries.map { it.id }.toSet().size)
        GamePalette.entries.forEach { assertEquals(it, GamePalette.fromId(it.id)) }
        assertEquals(GamePalette.MUTED, GamePalette.fromId(null))
        assertEquals(GamePalette.MUTED, GamePalette.fromId("removed-palette"))
    }

    @Test
    fun everyPaletteHasSevenDistinctVisiblePieceColors() {
        GamePalette.entries.forEach { palette ->
            val colors = PieceType.entries.map(palette::colorFor)
            assertEquals(palette.title, 7, colors.toSet().size)
            colors.forEach { color ->
                val contrast = (color.luminance() + 0.05f) / (palette.surface.luminance() + 0.05f)
                assertTrue("${palette.title} piece contrast: $contrast", contrast >= 3f)
            }
        }
    }

    @Test
    fun textIsReadableOnAllPaletteSurfaces() {
        GamePalette.entries.forEach { palette ->
            listOf(palette.background, palette.surface).forEach { background ->
                listOf(palette.textPrimary, palette.textSecondary, palette.textTertiary).forEach { text ->
                    val contrast = (text.luminance() + 0.05f) / (background.luminance() + 0.05f)
                    assertTrue("${palette.title} text contrast: $contrast", contrast >= 4.5f)
                }
            }
        }
    }
}
