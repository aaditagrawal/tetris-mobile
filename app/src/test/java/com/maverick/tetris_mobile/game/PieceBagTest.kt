package com.maverick.tetris_mobile.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceBagTest {

    @Test
    fun `first seven draws contain all seven piece types`() {
        val bag = PieceBag()
        val drawn = (1..7).map { bag.next() }.toSet()
        assertEquals(PieceType.entries.toSet(), drawn)
    }

    @Test
    fun `eighth draw starts a new bag with seven unique types`() {
        val bag = PieceBag()
        repeat(7) { bag.next() }
        val secondBagDraws = (1..7).map { bag.next() }.toSet()
        assertEquals(PieceType.entries.toSet(), secondBagDraws)
    }

    @Test
    fun `reset clears the bag so next draw refills`() {
        val bag = PieceBag()
        repeat(3) { bag.next() }
        bag.reset()
        val drawn = (1..7).map { bag.next() }.toSet()
        assertEquals(PieceType.entries.toSet(), drawn)
    }

    @Test
    fun `each draw returns a valid piece type`() {
        val bag = PieceBag()
        repeat(14) { draw ->
            assertTrue(PieceType.entries.contains(bag.next()))
        }
    }
}
