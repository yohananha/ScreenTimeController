package com.screentime.tv.usage

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class UsageDayTest {

    private val today = LocalDate.of(2026, 10, 7)
    private val dayStart = 1_000_000_000_000L
    private val dayEnd = dayStart + 86_400_000L
    private val day = 86_400_000L
    private val yt = "com.google.android.youtube"

    @Test fun `no clock change - the whole trusted day is queried and counts pass through`() {
        val u = UsageDay()
        val w = u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart + 3_600_000)
        assertThat(w).isEqualTo(UsageDay.Window(dayStart, dayEnd))
        assertThat(u.fold(mapOf(yt to 60_000L))).isEqualTo(mapOf(yt to 60_000L))
    }

    @Test fun `setting the clock forward a day does not reset the quota`() {
        val u = UsageDay()
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart + 7_200_000)
        u.fold(mapOf(yt to 2 * 3_600_000L)) // 2h watched

        // Child moves the clock +1 day. Same trusted day; new wall frame.
        val jumpWall = dayStart + 7_200_000 + day
        val w = u.window(today, dayStart, dayEnd, offset = day, wallNow = jumpWall)
        assertThat(w.beginWallMillis).isEqualTo(jumpWall) // count only from the jump

        // In the new frame, 10 more minutes are watched.
        assertThat(u.fold(mapOf(yt to 600_000L))).isEqualTo(mapOf(yt to 2 * 3_600_000L + 600_000L))
    }

    @Test fun `the day total never decreases (e g a query that now sees less)`() {
        val u = UsageDay()
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart)
        u.fold(mapOf(yt to 600_000L))
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart + 60_000)
        assertThat(u.fold(mapOf(yt to 300_000L))).isEqualTo(mapOf(yt to 600_000L))
    }

    @Test fun `small offset drift (NTP correction) is not treated as a jump`() {
        val u = UsageDay()
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart)
        val w = u.window(today, dayStart, dayEnd, offset = 30_000, wallNow = dayStart + 60_000)
        assertThat(w.beginWallMillis).isEqualTo(dayStart)
    }

    @Test fun `a new trusted day starts from zero`() {
        val u = UsageDay()
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart)
        u.fold(mapOf(yt to 600_000L))
        u.window(today.plusDays(1), dayEnd, dayEnd + day, offset = 0, wallNow = dayEnd + 1)
        assertThat(u.fold(emptyMap())).isEmpty()
    }

    @Test fun `observed foreground time floors an event count that fell short`() {
        val u = UsageDay()
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart + 3_600_000)
        repeat(30) { u.accrueLive(today, yt, 60_000L) }
        // The event log only saw 3 of the 30 minutes.
        assertThat(u.fold(mapOf(yt to 3 * 60_000L))).isEqualTo(mapOf(yt to 30 * 60_000L))
    }

    @Test fun `observed time never adds to the event count, it only floors it`() {
        val u = UsageDay()
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart + 3_600_000)
        repeat(10) { u.accrueLive(today, yt, 60_000L) }
        assertThat(u.fold(mapOf(yt to 40 * 60_000L))).isEqualTo(mapOf(yt to 40 * 60_000L))
    }

    @Test fun `observed time from another day is ignored`() {
        val u = UsageDay()
        u.accrueLive(today.minusDays(1), yt, 600_000L)
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart + 60_000)
        assertThat(u.fold(emptyMap())).isEmpty()
    }

    @Test fun `observed time survives a clock jump`() {
        val u = UsageDay()
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart + 600_000)
        repeat(10) { u.accrueLive(today, yt, 60_000L) }
        u.fold(emptyMap())
        u.window(today, dayStart, dayEnd, offset = day, wallNow = dayStart + 600_000 + day)
        repeat(5) { u.accrueLive(today, yt, 60_000L) }
        assertThat(u.fold(emptyMap())).isEqualTo(mapOf(yt to 15 * 60_000L))
    }

    @Test fun `seeded values from Room floor the total after a restart`() {
        val u = UsageDay()
        u.seed(today, mapOf(yt to 3_600_000L))
        u.window(today, dayStart, dayEnd, offset = -day, wallNow = dayStart - day + 10)
        assertThat(u.fold(mapOf(yt to 60_000L))).isEqualTo(mapOf(yt to 3_600_000L))
    }

    @Test fun `seed for another day is ignored`() {
        val u = UsageDay()
        u.seed(today.minusDays(1), mapOf(yt to 3_600_000L))
        u.window(today, dayStart, dayEnd, offset = 0, wallNow = dayStart)
        assertThat(u.fold(emptyMap())).isEmpty()
    }
}
