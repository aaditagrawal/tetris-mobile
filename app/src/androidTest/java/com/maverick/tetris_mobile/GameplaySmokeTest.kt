package com.maverick.tetris_mobile

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.maverick.tetris_mobile.game.GameViewModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GameplaySmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun playHoldDropPauseAndRestart() {
        compose.onNodeWithText("play").assertIsDisplayed()
        capture("start")
        compose.onNodeWithText("play").performClick()
        compose.onNodeWithText("SLAM").assertIsDisplayed()

        val model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
        compose.runOnIdle {
            model.hold()
            val held = model.gameState.value.holdPiece
            assertNotNull(held)
            assertFalse(model.gameState.value.canHold)
            model.hold()
            assertEquals(held, model.gameState.value.holdPiece)

            // Place real pieces across the board using the normal game actions.
            for (column in listOf(0, 7, 3, 5, 0, 7, 3, 5)) {
                repeat(10) { model.moveLeft() }
                repeat(column) { model.moveRight() }
                model.hardDrop()
                assertFalse(model.gameState.value.isGameOver)
            }
            assertTrue(model.gameState.value.score > 0)
            assertTrue(model.gameState.value.board.any { row -> row.any { it != null } })
            assertTrue(model.gameState.value.canHold)
        }
        capture("gameplay")

        compose.runOnIdle { model.pauseGame() }
        compose.onNodeWithText("paused").assertIsDisplayed()
        capture("paused")
        compose.onNodeWithText("resume").performClick()
        compose.runOnIdle { assertFalse(model.gameState.value.isPaused) }
        compose.runOnIdle { model.restartGame() }
        compose.runOnIdle {
            assertEquals(0, model.gameState.value.score)
            assertEquals(0, model.gameState.value.linesCleared)
            assertTrue(model.gameState.value.board.all { row -> row.all { it == null } })
            assertFalse(model.gameState.value.isGameOver)
            model.pauseGame()
        }
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("screenshots") != "true") return
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Allow the committed Compose frame to reach the emulator's display surface.
        SystemClock.sleep(250)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots")
        check(directory.mkdirs() || directory.isDirectory)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
