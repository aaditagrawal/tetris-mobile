package com.maverick.tetris_mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun emptyBoard(): List<List<PieceType?>> =
        List(GameState.BOARD_HEIGHT) { List(GameState.BOARD_WIDTH) { null } }

    private fun boardWithRow(rowIndex: Int, fill: PieceType?): List<List<PieceType?>> =
        List(GameState.BOARD_HEIGHT) { y ->
            List(GameState.BOARD_WIDTH) { if (y == rowIndex) fill else null }
        }

    // ---------- isValidPosition ----------

    @Test
    fun `piece in bounds on empty board is valid`() {
        val piece = TetrisPiece(PieceType.T, Position(4, 0))
        assertTrue(GameEngine.isValidPosition(emptyBoard(), piece))
    }

    @Test
    fun `piece off the left edge is invalid`() {
        val piece = TetrisPiece(PieceType.I, Position(-1, 0))
        assertFalse(GameEngine.isValidPosition(emptyBoard(), piece))
    }

    @Test
    fun `piece off the right edge is invalid`() {
        val piece = TetrisPiece(PieceType.I, Position(GameState.BOARD_WIDTH - 2, 0))
        assertFalse(GameEngine.isValidPosition(emptyBoard(), piece))
    }

    @Test
    fun `piece below the floor is invalid`() {
        val piece = TetrisPiece(PieceType.O, Position(4, GameState.BOARD_HEIGHT - 1))
        assertFalse(GameEngine.isValidPosition(emptyBoard(), piece))
    }

    @Test
    fun `piece overlapping existing block is invalid`() {
        val board = boardWithRow(GameState.BOARD_HEIGHT - 1, PieceType.J).map { it.toMutableList() }
        val piece = TetrisPiece(PieceType.O, Position(4, GameState.BOARD_HEIGHT - 2))
        assertFalse(GameEngine.isValidPosition(board, piece))
    }

    // ---------- placePiece ----------

    @Test
    fun `placePiece marks the four cells with piece type`() {
        val piece = TetrisPiece(PieceType.T, Position(4, 0))
        val placed = GameEngine.placePiece(emptyBoard(), piece)
        val markedCells = placed.flatMapIndexed { y, row ->
            row.mapIndexedNotNull { x, cell -> if (cell != null) Position(x, y) to cell else null }
        }
        assertEquals(4, markedCells.size)
        assertTrue(markedCells.all { it.second == PieceType.T })
        assertEquals(piece.cells().toSet(), markedCells.map { it.first }.toSet())
    }

    @Test
    fun `placePiece does not mutate original board`() {
        val board = emptyBoard()
        val piece = TetrisPiece(PieceType.O, Position(4, 0))
        GameEngine.placePiece(board, piece)
        assertTrue(board.all { row -> row.all { it == null } })
    }

    // ---------- clearLines ----------

    @Test
    fun `clearLines returns zero on empty board`() {
        val (board, cleared) = GameEngine.clearLines(emptyBoard())
        assertEquals(0, cleared)
        assertEquals(emptyBoard(), board)
    }

    @Test
    fun `clearLines clears a single full row`() {
        val full = List(GameState.BOARD_WIDTH) { PieceType.L }
        val board = List(GameState.BOARD_HEIGHT) { y ->
            if (y == GameState.BOARD_HEIGHT - 1) full else List(GameState.BOARD_WIDTH) { null }
        }
        val (after, cleared) = GameEngine.clearLines(board)
        assertEquals(1, cleared)
        assertTrue(after.all { row -> row.all { it == null } })
        assertEquals(GameState.BOARD_HEIGHT, after.size)
    }

    @Test
    fun `clearLines does not clear a partial row`() {
        val partial = List(GameState.BOARD_WIDTH) { i -> if (i == 0) null else PieceType.S }
        val board = List(GameState.BOARD_HEIGHT) { y ->
            if (y == GameState.BOARD_HEIGHT - 1) partial else List(GameState.BOARD_WIDTH) { null }
        }
        val (after, cleared) = GameEngine.clearLines(board)
        assertEquals(0, cleared)
        assertEquals(partial, after.last())
    }

    @Test
    fun `clearLines clears a tetris of four full rows`() {
        val full = List(GameState.BOARD_WIDTH) { PieceType.I }
        val board = List(GameState.BOARD_HEIGHT) { y ->
            if (y >= GameState.BOARD_HEIGHT - 4) full else List(GameState.BOARD_WIDTH) { null }
        }
        val (after, cleared) = GameEngine.clearLines(board)
        assertEquals(4, cleared)
        assertTrue(after.all { row -> row.all { it == null } })
    }

    @Test
    fun `clearLines preserves non-full rows above and shifts them down`() {
        val full = List(GameState.BOARD_WIDTH) { PieceType.J }
        val partial = List(GameState.BOARD_WIDTH) { i -> if (i == 5) PieceType.T else null }
        val board = List(GameState.BOARD_HEIGHT) { y ->
            when (y) {
                GameState.BOARD_HEIGHT - 2 -> partial
                GameState.BOARD_HEIGHT - 1 -> full
                else -> List(GameState.BOARD_WIDTH) { null }
            }
        }
        val (after, cleared) = GameEngine.clearLines(board)
        assertEquals(1, cleared)
        // The partial row should now be at the bottom (gravity pulled it down by one)
        assertEquals(partial, after[GameState.BOARD_HEIGHT - 1])
        // Top rows should be empty
        assertTrue(after[0].all { it == null })
    }

    // ---------- calculateScore ----------

    @Test
    fun `score formula matches Tetris standard scoring`() {
        // Level 0
        assertEquals(40, GameEngine.calculateScore(1, 0))
        assertEquals(100, GameEngine.calculateScore(2, 0))
        assertEquals(300, GameEngine.calculateScore(3, 0))
        assertEquals(1200, GameEngine.calculateScore(4, 0))
        // Level 1 = (level + 1) = 2x multiplier
        assertEquals(80, GameEngine.calculateScore(1, 1))
        assertEquals(2400, GameEngine.calculateScore(4, 1))
        // Level 9
        assertEquals(12000, GameEngine.calculateScore(4, 9))
    }

    @Test
    fun `score is zero for zero or invalid line counts`() {
        assertEquals(0, GameEngine.calculateScore(0, 5))
        assertEquals(0, GameEngine.calculateScore(5, 5))
        assertEquals(0, GameEngine.calculateScore(-1, 5))
    }

    // ---------- spawnPiece ----------

    @Test
    fun `I piece spawns at column 3`() {
        assertEquals(Position(3, 0), GameEngine.spawnPiece(PieceType.I).position)
    }

    @Test
    fun `O piece spawns at column 4`() {
        assertEquals(Position(4, 0), GameEngine.spawnPiece(PieceType.O).position)
    }

    @Test
    fun `three-wide pieces spawn at column 3`() {
        for (type in listOf(PieceType.T, PieceType.S, PieceType.Z, PieceType.J, PieceType.L)) {
            assertEquals("$type spawn", Position(3, 0), GameEngine.spawnPiece(type).position)
        }
    }

    @Test
    fun `spawnPiece is valid on empty board for every piece type`() {
        for (type in PieceType.entries) {
            assertTrue("$type should spawn validly", GameEngine.isValidPosition(emptyBoard(), GameEngine.spawnPiece(type)))
        }
    }

    // ---------- getDropPosition ----------

    @Test
    fun `dropping on an empty board lands at the floor`() {
        val piece = TetrisPiece(PieceType.O, Position(4, 0))
        val dropped = GameEngine.getDropPosition(emptyBoard(), piece)
        // O piece occupies y=0,1 relative; bottom-most cell must be at BOARD_HEIGHT - 1
        val maxY = dropped.cells().maxOf { it.y }
        assertEquals(GameState.BOARD_HEIGHT - 1, maxY)
    }

    @Test
    fun `drop position is valid and one step further is not`() {
        val piece = TetrisPiece(PieceType.T, Position(4, 0))
        val dropped = GameEngine.getDropPosition(emptyBoard(), piece)
        assertTrue(GameEngine.isValidPosition(emptyBoard(), dropped))
        assertFalse(GameEngine.isValidPosition(emptyBoard(), dropped.moved(0, 1)))
    }

    @Test
    fun `drop stops on top of existing stack`() {
        val board = boardWithRow(GameState.BOARD_HEIGHT - 1, PieceType.L)
        val piece = TetrisPiece(PieceType.O, Position(4, 0))
        val dropped = GameEngine.getDropPosition(board, piece)
        // O piece bottom cell should rest at row BOARD_HEIGHT - 2 (just above filled row)
        val maxY = dropped.cells().maxOf { it.y }
        assertEquals(GameState.BOARD_HEIGHT - 2, maxY)
    }

    @Test
    fun `drop does not change horizontal position`() {
        val piece = TetrisPiece(PieceType.J, Position(2, 0))
        val dropped = GameEngine.getDropPosition(emptyBoard(), piece)
        assertEquals(2, dropped.position.x)
        assertNotEquals(0, dropped.position.y)
    }
}
