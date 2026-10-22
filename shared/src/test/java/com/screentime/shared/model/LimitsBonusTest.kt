package com.screentime.shared.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LimitsBonusTest {
    @Test
    fun bonusCountsOnlyOnItsOwnDate() {
        val limits = Limits(bonusDate = "2026-10-22", bonusMinutes = 45)
        assertEquals(45, limits.bonusFor("2026-10-22"))
        assertEquals(0, limits.bonusFor("2026-10-23"))
    }

    @Test
    fun noBonusIsZero() {
        assertEquals(0, Limits().bonusFor("2026-10-22"))
    }

    @Test
    fun negativeBonusIsIgnored() {
        assertEquals(0, Limits(bonusDate = "2026-10-22", bonusMinutes = -5).bonusFor("2026-10-22"))
    }
}
