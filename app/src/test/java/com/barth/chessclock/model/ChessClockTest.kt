package com.barth.chessclock.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the *domain rules* of a chess clock, not the implementation:
 * what real tournament time controls require.
 */
class ChessClockTest {

    private fun clock(base: Int, increment: Int = 0) = ChessClock(TimeControl(base, increment))

    // ------------------------------- fresh game -------------------------------

    @Test
    fun `a fresh game is not running, both sides full, white to move`() {
        val c = clock(60)
        assertEquals(60_000L, c.whiteTimeMs)
        assertEquals(60_000L, c.blackTimeMs)
        assertFalse(c.isRunning)
        assertNull(c.winner)
        assertEquals(ChessClock.Player.WHITE, c.toMove)
    }

    @Test
    fun `start makes the clock run`() {
        val c = clock(60).also { it.start() }
        assertTrue(c.isRunning)
        assertNull(c.winner)
    }

    // ------------------------------- ticking -------------------------------

    @Test
    fun `tick reduces the side to move, never the opponent`() {
        val c = clock(60).also { it.start() }
        c.tick(1_500)
        assertEquals(58_500L, c.whiteTimeMs)
        assertEquals(60_000L, c.blackTimeMs)
    }

    @Test
    fun `tick is a no-op while paused`() {
        val c = clock(60).also { it.start() }
        c.pause()
        c.tick(5_000)
        assertEquals(60_000L, c.whiteTimeMs)
        c.resume()
        c.tick(1_000)
        assertEquals(59_000L, c.whiteTimeMs)
    }

    @Test
    fun `tick is a no-op for non-positive elapsed time`() {
        val c = clock(60).also { it.start() }
        c.tick(0)
        c.tick(-100)
        assertEquals(60_000L, c.whiteTimeMs)
    }

    // ------------------------------- changing turns -------------------------------

    @Test
    fun `change turn hands the move to the opponent`() {
        val c = clock(60).also { it.start() }
        c.changeTurn()
        assertEquals(ChessClock.Player.BLACK, c.toMove)
        c.changeTurn()
        assertEquals(ChessClock.Player.WHITE, c.toMove)
    }

    @Test
    fun `change turn is ignored while paused`() {
        val c = clock(60).also { it.start() }
        c.pause()
        c.changeTurn()
        assertEquals(ChessClock.Player.WHITE, c.toMove)
    }

    @Test
    fun `a completed move grants the fischer increment to the mover`() {
        val c = clock(180, increment = 2).also { it.start() }
        c.tick(1_000) // white now at 179 s
        c.changeTurn()
        assertEquals(181_000L, c.whiteTimeMs) // 179 s + 2 s increment
        assertEquals(180_000L, c.blackTimeMs) // untouched, now to move
    }

    @Test
    fun `the increment can accumulate over many moves`() {
        val c = clock(180, increment = 2).also { it.start() }
        repeat(10) { c.changeTurn() } // back to white after an even number of moves
        // white moved 5 times, gaining 5 x 2 s, without ever losing base time
        assertEquals(190_000L, c.whiteTimeMs)
    }

    @Test
    fun `no increment is granted without one`() {
        val c = clock(180).also { it.start() }
        c.tick(1_000)
        c.changeTurn()
        assertEquals(179_000L, c.whiteTimeMs)
    }

    // ------------------------------- flagging -------------------------------

    @Test
    fun `the clock is clamped at zero and the opponent wins`() {
        val c = clock(60).also { it.start() }
        c.tick(61_000)
        assertEquals(0L, c.whiteTimeMs)
        assertEquals(ChessClock.Player.BLACK, c.winner)
        assertFalse(c.isRunning)
        assertTrue(c.isGameOver)
        assertEquals("Black", c.winnerName)
    }

    @Test
    fun `after a flag the clock is frozen and turns are ignored`() {
        val c = clock(60).also { it.start() }
        c.tick(60_000)
        val blackBefore = c.blackTimeMs
        c.tick(5_000)
        c.changeTurn()
        assertEquals(ChessClock.Player.BLACK, c.winner)
        assertEquals(blackBefore, c.blackTimeMs)
        assertEquals(ChessClock.Player.WHITE, c.toMove) // unchanged
    }

    @Test
    fun `resume is refused after game over`() {
        val c = clock(60).also { it.start() }
        c.tick(60_000)
        c.resume()
        assertFalse(c.isRunning)
    }
}
