package com.barth.chessclock.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeControlTest {

    @Test
    fun `type is derived from the increment`() {
        assertEquals(TimeControl.Type.NONE, TimeControl(60).type)
        assertEquals(TimeControl.Type.INCREMENT, TimeControl(60, 2).type)
    }

    @Test
    fun `base time must be positive`() {
        val e = org.junit.Assert.assertThrows(Exception::class.java) { TimeControl(0) }
        assertEquals("base time must be positive, was 0 s", e.message)
    }

    @Test
    fun `increment must not be negative`() {
        org.junit.Assert.assertThrows(Exception::class.java) { TimeControl(60, -1) }
    }

    @Test
    fun `label follows the official notation`() {
        assertEquals("1 min", TimeControl(60).label)
        assertEquals("10 min + 5 s", TimeControl(600, 5).label)
        assertEquals("65 s", TimeControl(65).label) // non-round base
    }

    @Test
    fun `presets cover all four categories`() {
        for (category in TimeControlCategory.entries) {
            val presets = TimeControlPresets.forCategory(category)
            assertTrue("category $category has presets", presets.isNotEmpty())
            for (preset in presets) {
                assertTrue(preset.control.baseSeconds > 0)
                assertEquals(category, preset.category)
            }
        }
    }

    @Test
    fun `preset catalog matches the official controls`() {
        fun of(id: String) = TimeControlPresets.ALL.first { it.id == id }
        assertEquals(TimeControl(60), of("bullet_1").control)
        assertEquals(TimeControl(180, 2), of("blitz_3_2").control)
        assertEquals(TimeControl(300, 1), of("blitz_5_1").control)
        assertEquals(TimeControl(900, 10), of("rapid_15_10").control)
        assertEquals(TimeControl(3600, 30), of("classical_60_30").control)
    }

    @Test
    fun `formatClockTime renders minutes, seconds and tenths, and clamps at zero`() {
        assertEquals("0:00.0", formatClockTime(0))
        assertEquals("0:00.1", formatClockTime(100))
        assertEquals("1:01.5", formatClockTime(61_500))
        assertEquals("10:00.0", formatClockTime(600_000))
        assertEquals("61:01.5", formatClockTime(3_661_500))
        assertEquals("0:00.0", formatClockTime(-5))
    }
}
