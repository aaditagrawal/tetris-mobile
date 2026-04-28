package com.maverick.tetris_mobile.game

object GameEngine {

    fun isValidPosition(board: List<List<PieceType?>>, piece: TetrisPiece): Boolean {
        return piece.cells().all { cell ->
            cell.x in 0 until GameState.BOARD_WIDTH &&
                cell.y in 0 until GameState.BOARD_HEIGHT &&
                board[cell.y][cell.x] == null
        }
    }

    fun placePiece(board: List<List<PieceType?>>, piece: TetrisPiece): List<List<PieceType?>> {
        val mutableBoard = board.map { it.toMutableList() }
        for (cell in piece.cells()) {
            if (cell.y in 0 until GameState.BOARD_HEIGHT && cell.x in 0 until GameState.BOARD_WIDTH) {
                mutableBoard[cell.y][cell.x] = piece.type
            }
        }
        return mutableBoard.map { it.toList() }
    }

    fun clearLines(board: List<List<PieceType?>>): Pair<List<List<PieceType?>>, Int> {
        val remaining = board.filter { row -> row.any { it == null } }
        val cleared = GameState.BOARD_HEIGHT - remaining.size
        if (cleared == 0) return Pair(board, 0)
        val emptyRows = List(cleared) { List<PieceType?>(GameState.BOARD_WIDTH) { null } }
        return Pair(emptyRows + remaining, cleared)
    }

    fun spawnPiece(type: PieceType): TetrisPiece {
        val x = when (type) {
            PieceType.I -> 3  // 4-wide piece centered in 10-wide board
            PieceType.O -> 4  // 2-wide piece centered
            else -> 3         // 3-wide pieces centered
        }
        return TetrisPiece(type = type, position = Position(x, 0))
    }

    fun randomPieceType(): PieceType {
        return PieceType.entries.random()
    }

    fun calculateScore(lines: Int, level: Int): Int {
        return when (lines) {
            1 -> 40 * (level + 1)
            2 -> 100 * (level + 1)
            3 -> 300 * (level + 1)
            4 -> 1200 * (level + 1)
            else -> 0
        }
    }

    fun getDropPosition(board: List<List<PieceType?>>, piece: TetrisPiece): TetrisPiece {
        var dropped = piece
        while (isValidPosition(board, dropped.moved(0, 1))) {
            dropped = dropped.moved(0, 1)
        }
        return dropped
    }
}
