package com.maverick.tetris_mobile.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    private var gameLoopJob: Job? = null
    private var fixedLevel: Boolean = false
    private var startingLevel: Int = 1
    private var manualLevelOffset: Int = 0

    private fun gravityDelay(): Long {
        val level = _gameState.value.level
        return maxOf(100L, 1000L - (level - 1) * 85L)
    }

    fun startGame(level: Int = 1, fixedLevelMode: Boolean = false) {
        fixedLevel = fixedLevelMode
        startingLevel = level
        manualLevelOffset = 0
        val firstType = GameEngine.randomPieceType()
        val secondType = GameEngine.randomPieceType()
        _gameState.value = GameState(
            currentPiece = GameEngine.spawnPiece(firstType),
            nextPiece = GameEngine.spawnPiece(secondType),
            level = level
        )
        startGameLoop()
    }

    fun adjustLevel(delta: Int) {
        val state = _gameState.value
        if (state.isGameOver) return
        val newLevel = (state.level + delta).coerceIn(1, 15)
        if (newLevel != state.level) {
            if (fixedLevel) {
                startingLevel = newLevel
            } else {
                manualLevelOffset += (newLevel - state.level)
            }
            _gameState.value = state.copy(level = newLevel)
        }
    }

    fun pauseGame() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        _gameState.value = _gameState.value.copy(isPaused = true)
    }

    fun resumeGame() {
        if (!_gameState.value.isPaused) return
        _gameState.value = _gameState.value.copy(isPaused = false)
        startGameLoop()
    }

    fun endGame() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        _gameState.value = _gameState.value.copy(
            isGameOver = true,
            isPaused = false
        )
    }

    fun restartGame() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        startGame(startingLevel, fixedLevel)
    }

    fun moveLeft() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused) return
        val moved = piece.moved(-1, 0)
        if (GameEngine.isValidPosition(state.board, moved)) {
            _gameState.value = state.copy(currentPiece = moved)
        }
    }

    fun moveRight() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused) return
        val moved = piece.moved(1, 0)
        if (GameEngine.isValidPosition(state.board, moved)) {
            _gameState.value = state.copy(currentPiece = moved)
        }
    }

    fun rotate() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused) return

        val rotated = piece.rotated()

        // Try basic rotation, then wall kicks
        val candidates = listOf(
            rotated,
            rotated.moved(-1, 0),
            rotated.moved(1, 0),
            rotated.moved(-2, 0),
            rotated.moved(2, 0),
            rotated.moved(0, -1),
            rotated.moved(-1, -1),
            rotated.moved(1, -1)
        )

        for (candidate in candidates) {
            if (GameEngine.isValidPosition(state.board, candidate)) {
                _gameState.value = state.copy(currentPiece = candidate)
                return
            }
        }
    }

    fun softDrop() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused) return
        val moved = piece.moved(0, 1)
        if (GameEngine.isValidPosition(state.board, moved)) {
            _gameState.value = state.copy(
                currentPiece = moved,
                score = state.score + 1
            )
        } else {
            lockAndAdvance()
        }
    }

    fun hardDrop() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused) return

        val dropped = GameEngine.getDropPosition(state.board, piece)
        val distance = dropped.position.y - piece.position.y

        _gameState.value = state.copy(
            currentPiece = dropped,
            score = state.score + 2 * distance
        )
        lockAndAdvance()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(gravityDelay())
                val state = _gameState.value
                if (state.isGameOver || state.isPaused) break
                tick()
            }
        }
    }

    private fun tick() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return

        val moved = piece.moved(0, 1)
        if (GameEngine.isValidPosition(state.board, moved)) {
            _gameState.value = state.copy(currentPiece = moved)
        } else {
            lockAndAdvance()
        }
    }

    private fun lockAndAdvance() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return

        val boardAfterPlace = GameEngine.placePiece(state.board, piece)
        val (boardAfterClear, lines) = GameEngine.clearLines(boardAfterPlace)

        val newLinesCleared = state.linesCleared + lines
        val newLevel = if (fixedLevel) startingLevel else ((newLinesCleared / 10) + startingLevel + manualLevelOffset).coerceIn(1, 15)
        val lineScore = GameEngine.calculateScore(lines, state.level)

        val nextType = GameEngine.randomPieceType()
        val newCurrentPiece = state.nextPiece ?: GameEngine.spawnPiece(GameEngine.randomPieceType())
        val newNextPiece = GameEngine.spawnPiece(nextType)

        val gameOver = !GameEngine.isValidPosition(boardAfterClear, newCurrentPiece)

        _gameState.value = state.copy(
            board = boardAfterClear,
            currentPiece = if (gameOver) null else newCurrentPiece,
            nextPiece = if (gameOver) null else newNextPiece,
            score = state.score + lineScore,
            level = newLevel,
            linesCleared = newLinesCleared,
            isGameOver = gameOver
        )

        if (gameOver) {
            gameLoopJob?.cancel()
            gameLoopJob = null
        }
    }
}
