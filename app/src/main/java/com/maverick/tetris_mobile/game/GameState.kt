package com.maverick.tetris_mobile.game

import androidx.compose.runtime.Immutable

@Immutable
data class GameState(
    val sessionId: Long = 0,
    val board: List<List<PieceType?>> = List(BOARD_HEIGHT) { List(BOARD_WIDTH) { null } },
    val currentPiece: TetrisPiece? = null,
    val nextPiece: TetrisPiece? = null,
    val holdPiece: TetrisPiece? = null,
    val canHold: Boolean = true,
    val ghostPiece: TetrisPiece? = null,
    val score: Int = 0,
    val level: Int = 1,
    val linesCleared: Int = 0,
    val lastClearCount: Int = 0,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false
) {
    companion object {
        const val BOARD_WIDTH = 10
        const val BOARD_HEIGHT = 20
    }
}
