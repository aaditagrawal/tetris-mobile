package com.maverick.tetris_mobile

import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.click
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.maverick.tetris_mobile.game.GameViewModel
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResponsiveLayoutTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun boardAndControlsFitTheAvailableWindow() {
        compose.onNodeWithText("play").performScrollTo().performClick()
        assertBoardFits()
    }

    @Test
    fun touchAndButtonsDriveTheRealGame() {
        compose.onNodeWithText("play").performScrollTo().performClick()
        val model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
        val startX = checkNotNull(model.gameState.value.currentPiece).position.x
        compose.onNodeWithTag("game-board").performTouchInput { swipeLeft(durationMillis = 300) }
        compose.runOnIdle { assertTrue(checkNotNull(model.gameState.value.currentPiece).position.x < startX) }
        compose.onNodeWithTag("game-board").performTouchInput { swipeRight(durationMillis = 300) }
        compose.onNodeWithTag("game-board").performTouchInput { click() }
        compose.onNodeWithText("\u21C4", useUnmergedTree = true).performClick()
        compose.runOnIdle {
            assertTrue(model.gameState.value.holdPiece != null)
            assertTrue(!model.gameState.value.canHold)
        }
        compose.onNodeWithText("\u21E9", useUnmergedTree = true).performClick()
        compose.runOnIdle {
            assertTrue(model.gameState.value.board.any { row -> row.any { it != null } })
            assertTrue(model.gameState.value.score > 0)
            assertTrue(model.gameState.value.canHold)
        }
        compose.onNodeWithText("\u2759\u2759", useUnmergedTree = true).performClick()
        compose.onNodeWithText("paused").assertIsDisplayed()
        compose.runOnIdle { assertTrue(model.gameState.value.isPaused) }
        compose.onNodeWithText("resume").performClick()
        assertBoardFits()
    }

    @Test
    fun rotatingPreservesTheSessionAndFitsBothOrientations() {
        val oldAuto = shell("settings get system accelerometer_rotation")
        val oldRotation = shell("settings get system user_rotation")
        try {
            compose.onNodeWithText("play").performScrollTo().performClick()
            val model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
            compose.runOnIdle {
                model.hardDrop()
                model.pauseGame()
            }
            val before = model.gameState.value
            shell("settings put system accelerometer_rotation 0")
            shell("settings put system user_rotation 1")
            compose.waitUntil(10_000) {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            compose.runOnIdle { assertEquals(before, model.gameState.value) }
            compose.onNodeWithText("resume").performClick()
            assertBoardFits()
            compose.runOnIdle { model.pauseGame() }
            val landscape = model.gameState.value
            shell("settings put system user_rotation 0")
            compose.waitUntil(10_000) {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            }
            compose.runOnIdle { assertEquals(landscape, model.gameState.value) }
            compose.onNodeWithText("resume").performClick()
            assertBoardFits()
        } finally {
            shell("settings put system user_rotation $oldRotation")
            shell("settings put system accelerometer_rotation $oldAuto")
        }
    }

    private fun assertBoardFits() {
        compose.waitForIdle()
        val board = compose.onNodeWithTag("game-board").getUnclippedBoundsInRoot()
        val window = compose.onRoot().getUnclippedBoundsInRoot()
        val score = compose.onNodeWithText("SCORE").getUnclippedBoundsInRoot()
        assertTrue("Board must retain its 1:2 aspect ratio", abs((board.bottom - board.top).value - (board.right - board.left).value * 2) < 2)
        assertTrue("Board must fit horizontally", board.left >= window.left && board.right <= window.right)
        assertTrue("Board must fit vertically", board.top >= window.top && board.bottom <= window.bottom)
        assertTrue("Board must leave room for scores", board.top >= score.bottom)
        compose.onNodeWithText("ROTATE").assertIsDisplayed()
        compose.onNodeWithText("SLAM").assertIsDisplayed()
        compose.onNodeWithText("PAUSE").assertIsDisplayed()
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
    }
}
