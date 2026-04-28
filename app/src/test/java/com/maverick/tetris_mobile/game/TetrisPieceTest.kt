package com.maverick.tetris_mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TetrisPieceTest {

    @Test
    fun `all seven piece types have shape definitions`() {
        for (type in PieceType.entries) {
            assertNotNull("Missing shape for $type", TetrisPiece.SHAPES[type])
            assertEquals("Each piece must have 4 rotation states", 4, TetrisPiece.SHAPES[type]!!.size)
        }
    }

    @Test
    fun `every rotation has exactly four cells`() {
        for (type in PieceType.entries) {
            TetrisPiece.SHAPES[type]!!.forEachIndexed { i, rot ->
                assertEquals("$type rotation $i should have 4 cells", 4, rot.size)
            }
        }
    }

    @Test
    fun `cells are translated by piece position`() {
        val piece = TetrisPiece(PieceType.I, Position(3, 0))
        val cells = piece.cells()
        // I piece rotation 0: (0,1)(1,1)(2,1)(3,1) -> at x=3 becomes (3,1)(4,1)(5,1)(6,1)
        assertEquals(setOf(Position(3, 1), Position(4, 1), Position(5, 1), Position(6, 1)), cells.toSet())
    }

    @Test
    fun `rotated returns to original after four rotations`() {
        for (type in PieceType.entries) {
            val original = TetrisPiece(type, Position(4, 4))
            val fullCycle = original.rotated().rotated().rotated().rotated()
            assertEquals("$type should cycle in 4 rotations", original.cells().toSet(), fullCycle.cells().toSet())
        }
    }

    @Test
    fun `O piece rotation does not change cells`() {
        val o = TetrisPiece(PieceType.O, Position(4, 0))
        assertEquals(o.cells().toSet(), o.rotated().cells().toSet())
    }

    @Test
    fun `I piece horizontal and vertical orientations differ`() {
        val horizontal = TetrisPiece(PieceType.I, Position(0, 0))
        val vertical = horizontal.rotated()
        assertTrue(horizontal.cells().toSet() != vertical.cells().toSet())
    }

    @Test
    fun `moved shifts position by delta`() {
        val piece = TetrisPiece(PieceType.T, Position(4, 0))
        val moved = piece.moved(2, 3)
        assertEquals(Position(6, 3), moved.position)
        assertEquals(piece.rotation, moved.rotation)
    }

    @Test
    fun `moved by zero returns equivalent cells`() {
        val piece = TetrisPiece(PieceType.S, Position(4, 5), rotation = 2)
        assertEquals(piece.cells().toSet(), piece.moved(0, 0).cells().toSet())
    }
}
