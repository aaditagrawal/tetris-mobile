package com.maverick.tetris_mobile

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maverick.tetris_mobile.game.GameState
import com.maverick.tetris_mobile.game.GameViewModel
import com.maverick.tetris_mobile.game.HighScoreManager
import com.maverick.tetris_mobile.game.ScoreSaveViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameReliabilityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun recreationKeepsTheSessionAndPausesIt() {
        compose.onNodeWithText("play").performClick()
        lateinit var before: GameState
        compose.runOnIdle {
            val model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
            model.hardDrop()
            model.hold()
            model.pauseGame()
            before = model.gameState.value
        }

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()

        compose.runOnIdle {
            val model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
            assertEquals(before, model.gameState.value)
        }
        compose.onNodeWithText("paused").assertExists()
    }

    @Test
    fun backgroundingStopsGravityUntilExplicitResume() {
        compose.onNodeWithText("play").performClick()
        lateinit var model: GameViewModel
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[GameViewModel::class.java]
            model.adjustLevel(14)
        }

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        val stopped = model.gameState.value
        assertTrue(stopped.isPaused)
        SystemClock.sleep(350)
        assertEquals(stopped, model.gameState.value)

        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNodeWithText("paused").assertExists()
        compose.onNodeWithText("resume").performClick()
        compose.runOnIdle { assertTrue(!model.gameState.value.isPaused) }
    }

    @Test
    fun concurrentSubmissionsSaveOneRecordAndNewSessionsSaveAgain() = runBlocking {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val isolatedContext = object : ContextWrapper(targetContext) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences("${name}_reliability_test", mode)
        }
        val prefs = isolatedContext.getSharedPreferences("tetris_high_scores", Context.MODE_PRIVATE)
        prefs.edit(commit = true) { clear() }
        val manager = HighScoreManager(isolatedContext)
        val store = ViewModelStore()
        try {
            withContext(Dispatchers.Main) {
                val saver = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())
                    .get(ScoreSaveViewModel::class.java)
                val state = GameState(sessionId = 1L, score = 120, level = 3, linesCleared = 2)
                val canceledWaiter = async(start = CoroutineStart.UNDISPATCHED) {
                    saver.saveScore(manager, state)
                }
                canceledWaiter.cancel()
                canceledWaiter.join()
                val ranks = List(20) { async { saver.saveScore(manager, state) } }.awaitAll()
                assertTrue(ranks.all { it == 1 })
                assertEquals(1, manager.getScoreHistory().size)
                assertEquals(1, manager.getHighScores().size)

                saver.saveScore(manager, state.copy(sessionId = 2L))
                assertEquals(2, manager.getScoreHistory().size)
                assertEquals(2, manager.getHighScores().size)
                saver.saveScore(manager, state)
                assertEquals(2, manager.getScoreHistory().size)
            }
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            prefs.edit(commit = true) { clear() }
        }
    }
}
