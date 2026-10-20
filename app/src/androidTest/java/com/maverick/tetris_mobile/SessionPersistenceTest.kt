package com.maverick.tetris_mobile

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.app.Application
import android.os.SystemClock
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.maverick.tetris_mobile.game.GameSessionViewModel
import com.maverick.tetris_mobile.game.GameSnapshotCodec
import com.maverick.tetris_mobile.game.GameState
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.game.HighScoreManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SessionPersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun losingFocusPausesAndRequiresExplicitResume() {
        compose.onNodeWithText("play").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("SLAM").fetchSemanticsNodes().isNotEmpty() }
        val model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        val paused = model.gameState.value
        assertTrue(paused.isPaused)
        SystemClock.sleep(1100)
        assertEquals(paused, model.gameState.value)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNodeWithText("paused").assertExists()
        assertEquals(paused, model.gameState.value)
        compose.onNodeWithText("resume").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(!model.gameState.value.isPaused) }
    }

    @Test fun menuKeepsPlayableCheckpointWithoutSubmittingAnUnfinishedScore() = runBlocking {
        compose.onNodeWithText("play").performScrollTo().performClick()
        lateinit var expected: GameState
        val model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
        compose.runOnIdle {
            model.hardDrop(); model.hold(); model.rotate(); model.pauseGame()
            expected = model.gameState.value
        }
        val scores = HighScoreManager(compose.activity)
        val before = withContext(Dispatchers.IO) { scores.getScoreHistory() }
        compose.onNodeWithText("menu").performScrollTo().performClick()
        compose.onNodeWithText("continue").performScrollTo().performClick()
        compose.onNodeWithText("paused").assertExists()
        compose.runOnIdle { assertEquals(expected, model.gameState.value) }
        assertEquals(before, withContext(Dispatchers.IO) { scores.getScoreHistory() })
        val snapshot = withContext(Dispatchers.IO) {
            GameSnapshotCodec.decode(File(compose.activity.filesDir, "game-checkpoint.bin").readBytes())
        }
        assertEquals(expected.copy(ghostPiece = null), requireNotNull(snapshot).state)
        // A fresh model has no retained Activity state to rely on.
        compose.runOnIdle {
            val fresh = GameViewModel(); fresh.restore(snapshot)
            assertEquals(expected, fresh.gameState.value)
        }
        withContext(Dispatchers.Main) {
            ViewModelProvider(compose.activity)[GameSessionViewModel::class.java].saveNow(model)
        }
    }

    @Test fun completedScoreIsDeduplicatedAcrossFreshManagers() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val isolated = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences("${name}_durable_test", mode)
        }
        val prefs = isolated.getSharedPreferences("tetris_high_scores", Context.MODE_PRIVATE)
        withContext(Dispatchers.IO) {
            prefs.edit(commit = true) { clear() }
            try {
                assertEquals(1, HighScoreManager(isolated).addScore(200, 3, 2, 100))
                assertEquals(1, HighScoreManager(isolated).addScore(200, 3, 2, 100))
                assertEquals(1, HighScoreManager(isolated).getScoreHistory().size)
                HighScoreManager(isolated).addScore(300, 4, 3, 101)
                HighScoreManager(isolated).addScore(200, 3, 2, 100)
                assertEquals(2, HighScoreManager(isolated).getScoreHistory().size)
            } finally { prefs.edit(commit = true) { clear() } }
        }
    }

    @Test fun failedCheckpointWriteCanBeRetriedWithoutLosingTheGame() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val blocked = File(context.cacheDir, "checkpoint-write-failure-test")
        blocked.deleteRecursively()
        blocked.writeText("This file blocks the checkpoint directory")
        val application = object : Application() {
            init { attachBaseContext(context) }
            override fun getFilesDir(): File = blocked
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                context.getSharedPreferences("${name}_checkpoint_failure_test", mode)
        }
        val store = ViewModelStore()
        lateinit var game: GameViewModel
        lateinit var persistence: GameSessionViewModel
        try {
            withContext(Dispatchers.Main) {
                game = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[GameViewModel::class.java]
                persistence = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory(application))[GameSessionViewModel::class.java]
                persistence.bind(game)
            }
            compose.waitUntil(5_000) { persistence.ready.value }
            withContext(Dispatchers.Main) {
                game.startGame(); game.hardDrop(); game.hold(); game.pauseGame()
                var failed = false
                try { persistence.saveNow(game) } catch (_: java.io.IOException) { failed = true }
                assertTrue("The failed write must not claim success", failed)
            }
            val before = game.gameState.value
            assertFalse(File(blocked, "game-checkpoint.bin").exists())
            blocked.delete(); assertTrue(blocked.mkdirs())
            withContext(Dispatchers.Main) { persistence.retrySave(game) }
            val saved = withContext(Dispatchers.IO) {
                requireNotNull(GameSnapshotCodec.decode(File(blocked, "game-checkpoint.bin").readBytes()))
            }
            assertEquals(before.copy(ghostPiece = null), saved.state)
            assertEquals(null, persistence.saveError.value)
        } finally {
            withContext(Dispatchers.Main) { game.pauseGame(); store.clear() }
            blocked.deleteRecursively()
        }
    }
}
