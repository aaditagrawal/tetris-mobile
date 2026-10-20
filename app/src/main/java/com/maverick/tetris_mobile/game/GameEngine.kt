package com.maverick.tetris_mobile.game

object GameEngine {

    fun isValidPosition(board: List<List<PieceType?>>, piece: TetrisPiece): Boolean {
        piece.forEachCell { x, y ->
            if (x !in 0 until GameState.BOARD_WIDTH ||
                y !in 0 until GameState.BOARD_HEIGHT ||
                board[y][x] != null
            ) {
                return false
            }
        }
        return true
    }

    fun placePiece(board: List<List<PieceType?>>, piece: TetrisPiece): List<List<PieceType?>> {
        val touchedRows = hashSetOf<Int>()
        piece.forEachCell { _, y ->
            if (y in 0 until GameState.BOARD_HEIGHT) {
                touchedRows.add(y)
            }
        }
        if (touchedRows.isEmpty()) return board

        val mutableRows = touchedRows.associateWith { row -> board[row].toMutableList() }
        piece.forEachCell { x, y ->
            if (y in 0 until GameState.BOARD_HEIGHT && x in 0 until GameState.BOARD_WIDTH) {
                mutableRows.getValue(y)[x] = piece.type
            }
        }
        return board.mapIndexed { index, row ->
            mutableRows[index]?.toList() ?: row
        }
    }

    fun clearLines(board: List<List<PieceType?>>): Pair<List<List<PieceType?>>, Int> {
        val cleared = board.count { row -> row.all { it != null } }
        if (cleared == 0) return Pair(board, 0)
        val remaining = board.filter { row -> row.any { it == null } }
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

    /** Pure random piece selection; prefer [PieceBag] for gameplay. */
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
        var distance = 0
        while (canMoveDown(board, piece, distance + 1)) {
            distance++
        }
        return if (distance == 0) piece else piece.moved(0, distance)
    }

    fun withGhost(board: List<List<PieceType?>>, piece: TetrisPiece): TetrisPiece =
        getDropPosition(board, piece)

    private fun canMoveDown(
        board: List<List<PieceType?>>,
        piece: TetrisPiece,
        distance: Int
    ): Boolean {
        piece.forEachCell { x, y ->
            val movedY = y + distance
            if (x !in 0 until GameState.BOARD_WIDTH ||
                movedY !in 0 until GameState.BOARD_HEIGHT ||
                board[movedY][x] != null
            ) {
                return false
            }
        }
        return true
    }
}

class PieceBag {
    private val bag = mutableListOf<PieceType>()

    fun next(): PieceType {
        if (bag.isEmpty()) {
            bag.addAll(PieceType.entries.shuffled())
        }
        return bag.removeAt(0)
    }

    fun snapshot(): List<PieceType> = bag.toList()

    fun restore(remaining: List<PieceType>) {
        require(remaining.size <= PieceType.entries.size && remaining.distinct().size == remaining.size)
        bag.clear()
        bag.addAll(remaining)
    }

    fun reset() {
        bag.clear()
    }
}
