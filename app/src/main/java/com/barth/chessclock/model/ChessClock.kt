package com.barth.chessclock.model

/**
 * The chess clock engine. Pure Kotlin, no Android imports — unit-testable
 * on the JVM.
 *
 * Time is stored in milliseconds. The UI drives the clock by calling [tick]
 * with the real elapsed time between frames, so the clock neither drifts
 * nor loses time while paused (a paused [tick] is a no-op, and the next
 * tick simply continues from where the clock stopped).
 *
 * Bonus mechanic: the Fischer increment — the player who just completed a
 * move gains it (see [TimeControl.type]).
 */
class ChessClock(val control: TimeControl) {

    enum class Player(val displayName: String) {
        WHITE("White"),
        BLACK("Black");

        fun opposite(): Player = if (this == WHITE) BLACK else WHITE
    }

    var whiteTimeMs: Long = control.baseMillis
        private set
    var blackTimeMs: Long = control.baseMillis
        private set

    /** The side whose move is in progress. White moves first, as always. */
    var toMove: Player = Player.WHITE
        private set

    var isRunning: Boolean = false
        private set

    /** The winner once a side runs out of time; null while the game is alive. */
    var winner: Player? = null
        private set

    val isGameOver: Boolean get() = winner != null

    val winnerName: String? get() = winner?.displayName

    /** Starts the game. */
    fun start() {
        if (isRunning || isGameOver) return
        isRunning = true
    }

    fun pause() {
        isRunning = false
    }

    fun resume() {
        if (isGameOver) return
        isRunning = true
    }

    fun togglePause() {
        if (isRunning) pause() else resume()
    }

    /**
     * Advances the active clock by [elapsedMs] of real time.
     *
     * The clock is clamped at zero — reaching zero flags the side to move,
     * the opponent wins, and the game stops.
     * No-op while paused, finished, or for a non-positive [elapsedMs].
     */
    fun tick(elapsedMs: Long) {
        if (!isRunning || isGameOver || elapsedMs <= 0L) return
        if (toMove == Player.WHITE) {
            whiteTimeMs = (whiteTimeMs - elapsedMs).coerceAtLeast(0L)
            if (whiteTimeMs == 0L) flag(Player.WHITE)
        } else {
            blackTimeMs = (blackTimeMs - elapsedMs).coerceAtLeast(0L)
            if (blackTimeMs == 0L) flag(Player.BLACK)
        }
    }

    /**
     * The side to move has completed a move (in the app: the player pressed
     * their own clock). The mover gains the increment (if any) and the turn
     * passes to the opponent. No-op while paused or after game over.
     */
    fun changeTurn() {
        if (!isRunning || isGameOver) return
        if (control.incrementSeconds > 0) {
            if (toMove == Player.WHITE) whiteTimeMs += control.incrementMillis
            else blackTimeMs += control.incrementMillis
        }
        toMove = toMove.opposite()
    }

    private fun flag(loser: Player) {
        winner = loser.opposite()
        isRunning = false
    }
}
