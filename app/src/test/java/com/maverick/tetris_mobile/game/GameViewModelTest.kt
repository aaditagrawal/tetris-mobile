package com.maverick.tetris_mobile.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = GameViewModel()
    }

    @After
    fun tearDown() {
        viewModel.pauseGame()
        Dispatchers.resetMain()
    }

    @Test
    fun `soft drop preserves landing position and awards one point`() = runTest {
        viewModel.startGame()
        val before = viewModel.gameState.value

        viewModel.softDrop()

        val after = viewModel.gameState.value
        assertEquals(before.currentPiece!!.moved(0, 1), after.currentPiece)
        assertSame(before.ghostPiece, after.ghostPiece)
        assertEquals(GameEngine.getDropPosition(after.board, after.currentPiece!!), after.ghostPiece)
        assertEquals(before.score + 1, after.score)
        assertSame(before.board, after.board)
    }

    @Test
    fun `gravity preserves ghost and does not award drop points`() = runTest {
        viewModel.startGame()
        val before = viewModel.gameState.value
        runCurrent()

        advanceTimeBy(1000)
        runCurrent()

        val after = viewModel.gameState.value
        assertEquals(before.currentPiece!!.moved(0, 1), after.currentPiece)
        assertSame(before.ghostPiece, after.ghostPiece)
        assertEquals(before.score, after.score)
    }

    @Test
    fun `hard drop publishes only final locked state with correct distance score`() = runTest {
        viewModel.startGame()
        val before = viewModel.gameState.value
        val emissions = mutableListOf<GameState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.gameState.collect { emissions.add(it) }
        }
        val landing = before.ghostPiece!!

        viewModel.hardDrop()

        val after = viewModel.gameState.value
        assertEquals(2, emissions.size)
        assertEquals(GameEngine.placePiece(before.board, landing), after.board)
        assertEquals(2 * (landing.position.y - before.currentPiece!!.position.y), after.score)
        assertEquals(before.nextPiece, after.currentPiece)
        assertEquals(GameEngine.getDropPosition(after.board, after.currentPiece!!), after.ghostPiece)
    }

    @Test
    fun `cached hard drops match engine placement and scoring on stacked boards`() = runTest {
        viewModel.startGame()
        repeat(50) {
            val before = viewModel.gameState.value
            if (before.isGameOver) return@repeat
            viewModel.moveLeft()
            viewModel.rotate()
            val moved = viewModel.gameState.value
            val piece = moved.currentPiece!!
            val landing = GameEngine.getDropPosition(moved.board, piece)
            assertEquals(landing, moved.ghostPiece)
            val (expectedBoard, lines) = GameEngine.clearLines(GameEngine.placePiece(moved.board, landing))

            viewModel.hardDrop()

            val after = viewModel.gameState.value
            assertEquals(expectedBoard, after.board)
            assertEquals(moved.score + 2 * (landing.position.y - piece.position.y) +
                GameEngine.calculateScore(lines, moved.level), after.score)
            assertEquals(moved.linesCleared + lines, after.linesCleared)
            after.currentPiece?.let { current ->
                assertEquals(GameEngine.getDropPosition(after.board, current), after.ghostPiece)
            }
        }
    }

    @Test
    fun `pause cancels gravity and resume starts one timer`() = runTest {
        viewModel.startGame(level = 15)
        runCurrent()
        advanceTimeBy(100)
        runCurrent()
        viewModel.pauseGame()
        val paused = viewModel.gameState.value

        advanceTimeBy(2000)
        runCurrent()
        assertSame(paused, viewModel.gameState.value)

        viewModel.resumeGame()
        viewModel.resumeGame()
        runCurrent()
        advanceTimeBy(100)
        runCurrent()
        assertFalse(viewModel.gameState.value.isPaused)
        assertEquals(paused.currentPiece!!.moved(0, 1), viewModel.gameState.value.currentPiece)
    }

    @Test
    fun `restart changes session and replaces existing gravity timer`() = runTest {
        viewModel.startGame(level = 15)
        runCurrent()
        val firstSession = viewModel.sessionId

        viewModel.restartGame()
        val restarted = viewModel.gameState.value
        assertTrue(restarted.sessionId > firstSession)
        assertEquals(restarted.sessionId, viewModel.sessionId)
        runCurrent()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(restarted.currentPiece!!.moved(0, 1), viewModel.gameState.value.currentPiece)
    }

    @Test
    fun `lifecycle pause and resume leave a finished game unchanged`() = runTest {
        viewModel.startGame()
        viewModel.endGame()
        val finished = viewModel.gameState.value

        viewModel.pauseGame()
        viewModel.resumeGame()
        advanceTimeBy(2000)
        runCurrent()

        assertSame(finished, viewModel.gameState.value)
        assertFalse(viewModel.gameState.value.isPaused)
    }
}
