package com.maverick.tetris_mobile.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameSnapshotTest {
    private val models = mutableListOf<GameViewModel>()
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { models.forEach { it.pauseGame() }; Dispatchers.resetMain() }
    private fun model() = GameViewModel().also { models.add(it) }

    @Test fun `checkpoint restores board hold rotation score and bag without running gravity`() = runTest {
        val original = model()
        original.startGame(7, true)
        original.hardDrop(); original.hold(); original.rotate(); original.moveLeft(); original.softDrop()
        original.pauseGame()
        val before = original.gameState.value
        val decoded = requireNotNull(GameSnapshotCodec.decode(GameSnapshotCodec.encode(requireNotNull(original.snapshot()))))
        val restored = model(); restored.restore(decoded)
        assertEquals(before, restored.gameState.value)
        advanceTimeBy(2000); runCurrent()
        assertEquals(before, restored.gameState.value)
        original.resumeGame(); restored.resumeGame()
        original.hardDrop(); restored.hardDrop()
        assertEquals(original.gameState.value, restored.gameState.value)
        restored.restartGame()
        assertEquals(7, restored.gameState.value.level)
        assertTrue(restored.sessionId > before.sessionId)
    }

    @Test fun `active checkpoints are restored paused and reject gameplay input`() = runTest {
        val original = model(); original.startGame()
        val restored = model()
        restored.restore(requireNotNull(GameSnapshotCodec.decode(GameSnapshotCodec.encode(requireNotNull(original.snapshot())))))
        val paused = restored.gameState.value
        assertTrue(paused.isPaused)
        assertFalse(restored.moveLeft()); assertFalse(restored.moveRight()); assertFalse(restored.rotate())
        restored.hardDrop(); restored.softDrop(); restored.hold()
        assertEquals(paused, restored.gameState.value)
    }

    @Test fun `damaged truncated oversized and future checkpoints are ignored`() = runTest {
        val original = model(); original.startGame()
        val bytes = GameSnapshotCodec.encode(requireNotNull(original.snapshot()))
        assertNull(GameSnapshotCodec.decode(bytes.copyOf(bytes.size - 1)))
        assertNull(GameSnapshotCodec.decode(bytes.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() }))
        assertNull(GameSnapshotCodec.decode(bytes.copyOf().also { it[3] = 2 }))
        assertNull(GameSnapshotCodec.decode(ByteArray(1025)))
    }

    @Test fun `manual level mode survives restoring and restarting`() = runTest {
        val original = model(); original.startGame(3); original.adjustLevel(4); original.pauseGame()
        val restored = model(); restored.restore(requireNotNull(original.snapshot()))
        assertEquals(7, restored.gameState.value.level)
        restored.restartGame()
        assertEquals(3, restored.gameState.value.level)
    }
}
