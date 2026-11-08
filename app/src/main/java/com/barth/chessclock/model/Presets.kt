package com.barth.chessclock.model

/** The official time control categories, same lineup as on chess.com. */
enum class TimeControlCategory(val title: String) {
    BULLET("Bullet"),
    BLITZ("Blitz"),
    RAPID("Rapid"),
    CLASSICAL("Classical"),
}

/** A ready-made time control, e.g. "3 + 2" blitz. */
data class TimeControlPreset(
    val id: String,
    val category: TimeControlCategory,
    val control: TimeControl,
)

/**
 * The preset catalog. Values follow the official conventions: bullet/blitz
 * /rapid use small Fischer increments, classical uses (larger) increments.
 * Any base time and increment can also be entered manually.
 */
object TimeControlPresets {

    val ALL: List<TimeControlPreset> = listOf(
        // Bullet: the classic 1 + 0.
        preset("bullet_1", TimeControlCategory.BULLET, 60),
        // Blitz.
        preset("blitz_3_2", TimeControlCategory.BLITZ, 180, 2),
        preset("blitz_5_0", TimeControlCategory.BLITZ, 300),
        preset("blitz_5_1", TimeControlCategory.BLITZ, 300, 1),
        // Rapid.
        preset("rapid_10_5", TimeControlCategory.RAPID, 600, 5),
        preset("rapid_15_5", TimeControlCategory.RAPID, 900, 5),
        preset("rapid_15_10", TimeControlCategory.RAPID, 900, 10),
        // Classical.
        preset("classical_30_0", TimeControlCategory.CLASSICAL, 1800),
        preset("classical_30_20", TimeControlCategory.CLASSICAL, 1800, 20),
        preset("classical_60_30", TimeControlCategory.CLASSICAL, 3600, 30),
    )

    fun forCategory(category: TimeControlCategory): List<TimeControlPreset> =
        ALL.filter { it.category == category }

    private fun preset(
        id: String,
        category: TimeControlCategory,
        baseSeconds: Int,
        incrementSeconds: Int = 0,
    ) = TimeControlPreset(id, category, TimeControl(baseSeconds, incrementSeconds))
}
