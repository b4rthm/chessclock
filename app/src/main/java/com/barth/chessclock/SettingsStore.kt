package com.barth.chessclock

import android.content.Context
import com.barth.chessclock.model.TimeControlCategory

/** What the user last set up. Cosmetic only — nothing sensitive. */
data class ClockSettings(
    val baseMinutes: Double,
    val incrementSeconds: Int,
    val category: TimeControlCategory,
    val layoutRotation: Int,
    val design: Int, // 0 = default dark boxes, 1 = contrast (white box light, black box dark)
)

/**
 * Persists the (cosmetic) setup in private SharedPreferences.
 * Saves are narrow: each write copies only the fields it owns over the
 * loaded object, so a write from one screen can never clobber fields owned
 * by another.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ClockSettings = ClockSettings(
        baseMinutes = prefs.getFloat(KEY_BASE_MINUTES, DEFAULT.baseMinutes.toFloat()).toDouble(),
        incrementSeconds = prefs.getInt(KEY_INCREMENT_SECONDS, DEFAULT.incrementSeconds),
        category = TimeControlCategory.entries
            .firstOrNull { it.name == prefs.getString(KEY_CATEGORY, null) }
            ?: DEFAULT.category,
        layoutRotation = prefs.getInt(KEY_LAYOUT_ROTATION, DEFAULT.layoutRotation),
        design = prefs.getInt(KEY_DESIGN, DEFAULT.design),
    )

    /** Narrow write: the two setup inputs + the selected category. */
    fun save(
        loaded: ClockSettings,
        baseMinutes: Double,
        incrementSeconds: Int,
        category: TimeControlCategory,
    ): ClockSettings {
        val updated = loaded.copy(
            baseMinutes = baseMinutes,
            incrementSeconds = incrementSeconds,
            category = category,
        )
        prefs.edit()
            .putFloat(KEY_BASE_MINUTES, updated.baseMinutes.toFloat())
            .putInt(KEY_INCREMENT_SECONDS, updated.incrementSeconds)
            .putString(KEY_CATEGORY, updated.category.name)
            .apply()
        return updated
    }

    /** Narrow write: the layout mode (0 = white on top, 1 = flipped). */
    fun saveLayoutRotation(loaded: ClockSettings, layoutRotation: Int): ClockSettings {
        val updated = loaded.copy(layoutRotation = layoutRotation)
        prefs.edit().putInt(KEY_LAYOUT_ROTATION, updated.layoutRotation).apply()
        return updated
    }

    /** Narrow write: the design (0 = default, 1 = contrast). */
    fun saveDesign(loaded: ClockSettings, design: Int): ClockSettings {
        val updated = loaded.copy(design = design)
        prefs.edit().putInt(KEY_DESIGN, updated.design).apply()
        return updated
    }

    companion object {
        const val PREFS_NAME = "chess_clock_settings"

        val DEFAULT = ClockSettings(
            baseMinutes = 10.0,
            incrementSeconds = 5,
            category = TimeControlCategory.RAPID,
            layoutRotation = 0,
            design = 0,
        )

        private const val KEY_BASE_MINUTES = "base_minutes"
        private const val KEY_INCREMENT_SECONDS = "increment_seconds"
        private const val KEY_CATEGORY = "category"
        private const val KEY_LAYOUT_ROTATION = "layout_rotation"
        private const val KEY_DESIGN = "design"
    }
}
