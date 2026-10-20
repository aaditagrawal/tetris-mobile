package com.maverick.tetris_mobile

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.ui.theme.GamePalette
import com.maverick.tetris_mobile.ui.theme.PaletteViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PaletteTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun restoreDefault() {
        compose.runOnIdle {
            ViewModelProvider(compose.activity)[PaletteViewModel::class.java].select(GamePalette.MUTED)
        }
        compose.waitUntil(5_000) {
            compose.activity.getSharedPreferences("appearance", Context.MODE_PRIVATE)
                .getString("palette", null) == GamePalette.MUTED.id
        }
    }

    @Test
    fun choosePalettesPersistAndRenderSelectedColors() {
        compose.onNodeWithText("palette ·", substring = true).performScrollTo().performClick()
        GamePalette.entries.forEach { palette ->
            compose.onNodeWithText(palette.title).performScrollTo().performClick().assertIsSelected()
            compose.runOnIdle {
                assertEquals(palette, ViewModelProvider(compose.activity)[PaletteViewModel::class.java].palette.value)
            }
        }
        compose.onNodeWithText("done").assertIsDisplayed().performClick()
        compose.onNodeWithText("palette · Ocean").performScrollTo().assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.activity.getSharedPreferences("appearance", Context.MODE_PRIVATE)
                .getString("palette", null) == GamePalette.OCEAN.id
        }

        // A fresh model reads the persisted selection, independently of the retained activity model.
        val store = ViewModelStore()
        lateinit var restored: PaletteViewModel
        try {
            compose.runOnIdle {
                restored = ViewModelProvider(store,
                    ViewModelProvider.AndroidViewModelFactory.getInstance(compose.activity.application))[PaletteViewModel::class.java]
            }
            compose.waitUntil(5_000) { restored.palette.value == GamePalette.OCEAN }
        } finally {
            compose.runOnIdle { store.clear() }
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("palette · Ocean").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("play").performScrollTo().performClick()
        compose.onNodeWithText("SLAM").assertIsDisplayed()
        compose.onNodeWithText("ROTATE").assertIsDisplayed()
        compose.onNodeWithText("PAUSE").assertIsDisplayed()
        lateinit var model: GameViewModel
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
            model.hold()
            model.hardDrop()
        }
        compose.waitForIdle()
        val expectedColors = compose.runOnIdle {
            val game = model.gameState.value
            setOfNotNull(game.currentPiece?.type, game.holdPiece?.type, game.nextPiece?.type)
                .map { GamePalette.OCEAN.colorFor(it).toArgb() }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Screenshot polling waits for the Compose frame to reach the display.
        compose.waitUntil(5_000) {
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                expectedColors.all { color -> pixels.count { it == color } > 20 }
            } finally {
                bitmap.recycle()
            }
        }
        compose.runOnIdle { model.pauseGame() }
        assertTrue(model.gameState.value.isPaused)
    }
}
