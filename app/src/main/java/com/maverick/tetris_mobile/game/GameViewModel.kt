package com.maverick.tetris_mobile.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    val sessionId: Long get() = _gameState.value.sessionId

    private var gameLoopJob: Job? = null
    private var fixedLevel: Boolean = false
    private var startingLevel: Int = 1
    private var manualLevelOffset: Int = 0
    private val pieceBag = PieceBag()
    private var latestSessionId = 0L

    fun reserveSessionIdsAfter(id: Long) { latestSessionId = maxOf(latestSessionId, id) }

    fun snapshot(): GameSnapshot? = gameState.value.takeIf { it.sessionId > 0 }?.let {
        GameSnapshot(it, startingLevel, fixedLevel, manualLevelOffset, pieceBag.snapshot())
    }

    fun restore(snapshot: GameSnapshot) {
        require(snapshot.isValid())
        gameLoopJob?.cancel()
        gameLoopJob = null
        startingLevel = snapshot.startingLevel
        fixedLevel = snapshot.fixedLevel
        manualLevelOffset = snapshot.manualLevelOffset
        pieceBag.restore(snapshot.remainingBag)
        reserveSessionIdsAfter(snapshot.state.sessionId)
        val state = snapshot.state.copy(isPaused = !snapshot.state.isGameOver)
        _gameState.value = state.copy(ghostPiece = ghostFor(state.board, state.currentPiece))
    }

    private fun gravityDelay(): Long {
        val level = _gameState.value.level
        return maxOf(100L, 1000L - (level - 1) * 85L)
    }

    private fun ghostFor(board: List<List<PieceType?>>, piece: TetrisPiece?): TetrisPiece? =
        piece?.let { GameEngine.withGhost(board, it) }

    private fun emitPiece(state: GameState, piece: TetrisPiece?, scoreDelta: Int = 0): GameState =
        state.copy(
            currentPiece = piece,
            ghostPiece = ghostFor(state.board, piece),
            score = state.score + scoreDelta
        )

    fun startGame(level: Int = 1, fixedLevelMode: Boolean = false) {
        require(level in 1..15)
        latestSessionId = maxOf(System.currentTimeMillis(), latestSessionId + 1, sessionId + 1)
        fixedLevel = fixedLevelMode
        startingLevel = level
        manualLevelOffset = 0
        pieceBag.reset()
        val firstType = pieceBag.next()
        val secondType = pieceBag.next()
        val current = GameEngine.spawnPiece(firstType)
        val emptyBoard = List(GameState.BOARD_HEIGHT) { List<PieceType?>(GameState.BOARD_WIDTH) { null } }
        _gameState.value = GameState(
            sessionId = latestSessionId,
            board = emptyBoard,
            currentPiece = current,
            nextPiece = GameEngine.spawnPiece(secondType),
            ghostPiece = ghostFor(emptyBoard, current),
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
        val state = _gameState.value
        if (state.sessionId == 0L || state.isPaused || state.isGameOver) return
        gameLoopJob?.cancel()
        gameLoopJob = null
        _gameState.value = state.copy(isPaused = true)
    }

    fun resumeGame() {
        val state = _gameState.value
        if (!state.isPaused || state.isGameOver) return
        _gameState.value = state.copy(isPaused = false)
        startGameLoop()
    }

    fun endGame() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        val state = _gameState.value
        val piece = state.currentPiece
        val board = if (piece != null) {
            GameEngine.placePiece(state.board, piece)
        } else {
            state.board
        }
        _gameState.value = state.copy(
            board = board,
            currentPiece = null,
            ghostPiece = null,
            isGameOver = true,
            isPaused = false
        )
    }

    fun restartGame() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        startGame(startingLevel, fixedLevel)
    }

    fun moveLeft(): Boolean {
        val state = _gameState.value
        val piece = state.currentPiece ?: return false
        if (state.isGameOver || state.isPaused) return false
        val moved = piece.moved(-1, 0)
        if (!GameEngine.isValidPosition(state.board, moved)) return false
        _gameState.value = emitPiece(state, moved)
        return true
    }

    fun moveRight(): Boolean {
        val state = _gameState.value
        val piece = state.currentPiece ?: return false
        if (state.isGameOver || state.isPaused) return false
        val moved = piece.moved(1, 0)
        if (!GameEngine.isValidPosition(state.board, moved)) return false
        _gameState.value = emitPiece(state, moved)
        return true
    }

    fun rotate(): Boolean {
        val state = _gameState.value
        val piece = state.currentPiece ?: return false
        if (state.isGameOver || state.isPaused) return false

        val rotated = piece.rotated()
        for ((dx, dy) in WALL_KICK_OFFSETS) {
            val candidate = if (dx == 0 && dy == 0) rotated else rotated.moved(dx, dy)
            if (GameEngine.isValidPosition(state.board, candidate)) {
                _gameState.value = emitPiece(state, candidate)
                return true
            }
        }
        return false
    }

    fun hold() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused || !state.canHold) return

        val newHold = GameEngine.spawnPiece(piece.type)
        val newCurrent = if (state.holdPiece != null) {
            GameEngine.spawnPiece(state.holdPiece.type)
        } else {
            state.nextPiece ?: return
        }

        if (!GameEngine.isValidPosition(state.board, newCurrent)) {
            endGame()
            return
        }

        val newNext = if (state.holdPiece == null) {
            GameEngine.spawnPiece(pieceBag.next())
        } else {
            state.nextPiece
        }

        _gameState.value = state.copy(
            currentPiece = newCurrent,
            ghostPiece = ghostFor(state.board, newCurrent),
            holdPiece = newHold,
            nextPiece = newNext,
            canHold = false,
            lastClearCount = 0
        )
    }

    fun softDrop() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused) return
        val moved = piece.moved(0, 1)
        if (GameEngine.isValidPosition(state.board, moved)) {
            // Moving down on the same board leaves the landing position unchanged.
            _gameState.value = state.copy(currentPiece = moved, score = state.score + 1)
        } else {
            lockAndAdvance()
        }
    }

    fun hardDrop() {
        val state = _gameState.value
        val piece = state.currentPiece ?: return
        if (state.isGameOver || state.isPaused) return

        val dropped = state.ghostPiece ?: GameEngine.getDropPosition(state.board, piece)
        val distance = dropped.position.y - piece.position.y

        lockAndAdvance(state, dropped, scoreDelta = 2 * distance)
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

    private fun lockAndAdvance(
        state: GameState = _gameState.value,
        lockingPiece: TetrisPiece? = state.currentPiece,
        scoreDelta: Int = 0
    ) {
        val piece = lockingPiece ?: return

        val boardAfterPlace = GameEngine.placePiece(state.board, piece)
        val (boardAfterClear, lines) = GameEngine.clearLines(boardAfterPlace)

        val newLinesCleared = state.linesCleared + lines
        val newLevel = if (fixedLevel) {
            startingLevel
        } else {
            ((newLinesCleared / 10) + startingLevel + manualLevelOffset).coerceIn(1, 15)
        }
        val lineScore = GameEngine.calculateScore(lines, state.level)

        val newCurrentPiece = state.nextPiece ?: GameEngine.spawnPiece(pieceBag.next())
        val newNextPiece = GameEngine.spawnPiece(pieceBag.next())

        val gameOver = !GameEngine.isValidPosition(boardAfterClear, newCurrentPiece)

        _gameState.value = state.copy(
            board = boardAfterClear,
            currentPiece = if (gameOver) null else newCurrentPiece,
            nextPiece = if (gameOver) null else newNextPiece,
            ghostPiece = if (gameOver) null else ghostFor(boardAfterClear, newCurrentPiece),
            score = state.score + scoreDelta + lineScore,
            level = newLevel,
            linesCleared = newLinesCleared,
            lastClearCount = lines,
            canHold = true,
            isGameOver = gameOver
        )

        if (gameOver) {
            gameLoopJob?.cancel()
            gameLoopJob = null
        }
    }

    private companion object {
        val WALL_KICK_OFFSETS = arrayOf(
            0 to 0,
            -1 to 0,
            1 to 0,
            -2 to 0,
            2 to 0,
            0 to -1,
            -1 to -1,
            1 to -1,
        )
    }
}
