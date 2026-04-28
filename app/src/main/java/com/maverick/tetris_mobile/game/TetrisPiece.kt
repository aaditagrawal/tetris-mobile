package com.maverick.tetris_mobile.game

enum class PieceType { I, O, T, S, Z, J, L }

data class Position(val x: Int, val y: Int)

data class TetrisPiece(
    val type: PieceType,
    val position: Position,
    val rotation: Int = 0
) {
    fun cells(): List<Position> {
        val shape = SHAPES[type]!![rotation % SHAPES[type]!!.size]
        return shape.map { Position(position.x + it.x, position.y + it.y) }
    }

    fun rotated(): TetrisPiece {
        val rotationCount = SHAPES[type]!!.size
        return copy(rotation = (rotation + 1) % rotationCount)
    }

    fun moved(dx: Int, dy: Int): TetrisPiece {
        return copy(position = Position(position.x + dx, position.y + dy))
    }

    companion object {
        // SRS rotation data
        // Positions are relative offsets (x = column, y = row) from the piece's position
        // y increases downward (row 0 is top)
        val SHAPES: Map<PieceType, List<List<Position>>> = mapOf(
            // I piece - 4x4 bounding box
            PieceType.I to listOf(
                listOf(Position(0, 1), Position(1, 1), Position(2, 1), Position(3, 1)),  // 0: horizontal
                listOf(Position(2, 0), Position(2, 1), Position(2, 2), Position(2, 3)),  // R: vertical
                listOf(Position(0, 2), Position(1, 2), Position(2, 2), Position(3, 2)),  // 2: horizontal shifted
                listOf(Position(1, 0), Position(1, 1), Position(1, 2), Position(1, 3))   // L: vertical shifted
            ),
            // O piece - 2x2 bounding box (no rotation change)
            PieceType.O to listOf(
                listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1)),
                listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1)),
                listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1)),
                listOf(Position(0, 0), Position(1, 0), Position(0, 1), Position(1, 1))
            ),
            // T piece - 3x3 bounding box
            PieceType.T to listOf(
                listOf(Position(1, 0), Position(0, 1), Position(1, 1), Position(2, 1)),  // 0
                listOf(Position(1, 0), Position(1, 1), Position(2, 1), Position(1, 2)),  // R
                listOf(Position(0, 1), Position(1, 1), Position(2, 1), Position(1, 2)),  // 2
                listOf(Position(1, 0), Position(0, 1), Position(1, 1), Position(1, 2))   // L
            ),
            // S piece - 3x3 bounding box
            PieceType.S to listOf(
                listOf(Position(1, 0), Position(2, 0), Position(0, 1), Position(1, 1)),  // 0
                listOf(Position(1, 0), Position(1, 1), Position(2, 1), Position(2, 2)),  // R
                listOf(Position(1, 1), Position(2, 1), Position(0, 2), Position(1, 2)),  // 2
                listOf(Position(0, 0), Position(0, 1), Position(1, 1), Position(1, 2))   // L
            ),
            // Z piece - 3x3 bounding box
            PieceType.Z to listOf(
                listOf(Position(0, 0), Position(1, 0), Position(1, 1), Position(2, 1)),  // 0
                listOf(Position(2, 0), Position(1, 1), Position(2, 1), Position(1, 2)),  // R
                listOf(Position(0, 1), Position(1, 1), Position(1, 2), Position(2, 2)),  // 2
                listOf(Position(1, 0), Position(0, 1), Position(1, 1), Position(0, 2))   // L
            ),
            // J piece - 3x3 bounding box
            PieceType.J to listOf(
                listOf(Position(0, 0), Position(0, 1), Position(1, 1), Position(2, 1)),  // 0
                listOf(Position(1, 0), Position(2, 0), Position(1, 1), Position(1, 2)),  // R
                listOf(Position(0, 1), Position(1, 1), Position(2, 1), Position(2, 2)),  // 2
                listOf(Position(1, 0), Position(1, 1), Position(0, 2), Position(1, 2))   // L
            ),
            // L piece - 3x3 bounding box
            PieceType.L to listOf(
                listOf(Position(2, 0), Position(0, 1), Position(1, 1), Position(2, 1)),  // 0
                listOf(Position(1, 0), Position(1, 1), Position(1, 2), Position(2, 2)),  // R
                listOf(Position(0, 1), Position(1, 1), Position(2, 1), Position(0, 2)),  // 2
                listOf(Position(0, 0), Position(1, 0), Position(1, 1), Position(1, 2))   // L
            )
        )
    }
}
