package com.barth.chessclock.model

/**
 * A chess time control: a base time plus an optional Fischer increment
 * (the bonus seconds the player who just moved gains).
 *
 * Pure Kotlin, no Android imports — unit-testable on the JVM.
 */
data class TimeControl(
    val baseSeconds: Int,
    val incrementSeconds: Int = 0,
) {
    init {
        require(baseSeconds > 0) { "base time must be positive, was $baseSeconds s" }
        require(incrementSeconds >= 0) { "increment must not be negative, was $incrementSeconds s" }
    }

    val baseMillis: Long get() = baseSeconds * 1000L
    val incrementMillis: Long get() = incrementSeconds * 1000L

    val type: Type
        get() = if (incrementSeconds > 0) Type.INCREMENT else Type.NONE

    /** Official-style name, e.g. "10 min + 5 s" or "1 min". */
    val label: String
        get() {
            val base = durationLabel(baseSeconds)
            return if (incrementSeconds > 0) "$base + $incrementSeconds s" else base
        }

    private fun durationLabel(seconds: Int): String =
        if (seconds % 60 == 0) "${seconds / 60} min" else "${seconds} s"

    enum class Type { NONE, INCREMENT }
}

/**
 * Formats a clock value in milliseconds as `m:ss.t` (tenths of a second),
 * the standard digital chess clock display. Negative values clamp to zero.
 */
fun formatClockTime(millis: Long): String {
    val clamped = millis.coerceAtLeast(0L)
    val minutes = clamped / 60_000
    val seconds = (clamped / 1_000) % 60
    val tenths = (clamped / 100) % 10
    return "${minutes}:${seconds.toString().padStart(2, '0')}.${tenths}"
}
