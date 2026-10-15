package com.screentime.tv.usage

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UsageAccountingTest {

    private val netflix = "com.netflix.ninja"
    private val youtube = "com.google.android.youtube.tv"
    private val launcher = "com.google.android.tvlauncher"
    private val self = "com.screentime.tv"

    // Times are offsets from an arbitrary midnight so the assertions read as
    // minutes rather than epoch millis.
    private val midnight = 1_700_000_000_000L
    private fun min(n: Long) = midnight + n * 60_000L
    private val endOfDay = midnight + 24 * 60 * 60_000L

    private fun run(
        events: List<UsageEventRecord>,
        countable: Set<String> = setOf(netflix, youtube),
        now: Long = min(600),
        screenInteractiveNow: Boolean = true,
    ) = UsageAccounting.foregroundMillis(
        events, countable, now, endOfDay, screenInteractiveNow,
    )

    private fun resume(pkg: String, at: Long) =
        UsageEventRecord(pkg, UsageAccounting.ACTIVITY_RESUMED, at)

    private fun pause(pkg: String, at: Long) =
        UsageEventRecord(pkg, UsageAccounting.ACTIVITY_PAUSED, at)

    @Test
    fun `counts a launcher-visible app for its foreground stretch`() {
        val result = run(listOf(resume(netflix, min(10)), pause(netflix, min(20))))
        assertThat(result).containsExactly(netflix, 10 * 60_000L)
    }

    @Test
    fun `does not count the home launcher`() {
        val result = run(listOf(resume(launcher, min(0)), pause(launcher, min(30))))
        assertThat(result).isEmpty()
    }

    @Test
    fun `does not count this app itself`() {
        val result = run(listOf(resume(self, min(0)), pause(self, min(30))))
        assertThat(result).isEmpty()
    }

    @Test
    fun `counts an app with no launcher entry when it is explicitly limited`() {
        val sideloaded = "com.example.hidden"
        val events = listOf(resume(sideloaded, min(5)), pause(sideloaded, min(25)))

        assertThat(run(events)).isEmpty()
        assertThat(run(events, countable = setOf(netflix, sideloaded)))
            .containsExactly(sideloaded, 20 * 60_000L)
    }

    @Test
    fun `a repeated resume keeps the earliest start`() {
        val result = run(
            listOf(
                resume(netflix, min(10)),
                resume(netflix, min(15)),
                pause(netflix, min(20)),
            ),
        )
        assertThat(result).containsExactly(netflix, 10 * 60_000L)
    }

    @Test
    fun `a stop on its own never closes a session`() {
        // E.g. the copy of the app left in an older task stopping minutes
        // after the app was reopened from a home-screen recommendation.
        val result = run(
            listOf(
                resume(netflix, min(10)),
                UsageEventRecord(netflix, UsageAccounting.ACTIVITY_STOPPED, min(13)),
            ),
            now = min(40),
        )
        assertThat(result).containsExactly(netflix, 30 * 60_000L)
    }

    @Test
    fun `a pause followed by a stop counts only up to the pause`() {
        val result = run(
            listOf(
                resume(netflix, min(10)),
                pause(netflix, min(18)),
                UsageEventRecord(netflix, UsageAccounting.ACTIVITY_STOPPED, min(19)),
            ),
        )
        assertThat(result).containsExactly(netflix, 8 * 60_000L)
    }

    @Test
    fun `a session already open at the window start counts from the window start`() {
        val result = UsageAccounting.foregroundMillis(
            events = listOf(resume(netflix, midnight - 30 * 60_000L), pause(netflix, min(20))),
            countable = setOf(netflix),
            nowMillis = min(600),
            windowEndMillis = endOfDay,
            windowStartMillis = midnight,
        )
        assertThat(result).containsExactly(netflix, 20 * 60_000L)
    }

    @Test
    fun `a session entirely before the window start counts nothing`() {
        val result = UsageAccounting.foregroundMillis(
            events = listOf(resume(netflix, midnight - 30 * 60_000L), pause(netflix, midnight - 60_000L)),
            countable = setOf(netflix),
            nowMillis = min(600),
            windowEndMillis = endOfDay,
            windowStartMillis = midnight,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun `screen off closes an open session instead of billing until now`() {
        val result = run(
            listOf(
                resume(netflix, min(10)),
                UsageEventRecord("", UsageAccounting.SCREEN_NON_INTERACTIVE, min(15)),
            ),
            now = min(600),
        )
        assertThat(result).containsExactly(netflix, 5 * 60_000L)
    }

    @Test
    fun `device shutdown closes the open session rather than billing the rest of the day`() {
        // Netflix is already closed by YouTube resuming; the shutdown is what
        // ends YouTube, which would otherwise still be open at the tail.
        val result = run(
            listOf(
                resume(netflix, min(10)),
                resume(youtube, min(12)),
                UsageEventRecord("android", UsageAccounting.DEVICE_SHUTDOWN, min(20)),
            ),
            now = min(600),
        )
        assertThat(result).containsExactly(
            netflix, 2 * 60_000L,
            youtube, 8 * 60_000L,
        )
    }

    @Test
    fun `keyguard shown closes an open session`() {
        val result = run(
            listOf(
                resume(netflix, min(10)),
                UsageEventRecord("", UsageAccounting.KEYGUARD_SHOWN, min(40)),
            ),
        )
        assertThat(result).containsExactly(netflix, 30 * 60_000L)
    }

    @Test
    fun `a session that never closes is capped`() {
        val result = run(listOf(resume(netflix, min(10))), now = min(1400))
        assertThat(result).containsExactly(netflix, UsageAccounting.MAX_OPEN_SESSION_MILLIS)
    }

    @Test
    fun `a still-open session under the cap is charged up to now`() {
        val result = run(listOf(resume(netflix, min(10))), now = min(40))
        assertThat(result).containsExactly(netflix, 30 * 60_000L)
    }

    @Test
    fun `a still-open session is never charged past the end of the day`() {
        val result = UsageAccounting.foregroundMillis(
            events = listOf(resume(netflix, endOfDay - 60_000L)),
            countable = setOf(netflix),
            nowMillis = endOfDay + 10 * 60_000L,
            windowEndMillis = endOfDay,
        )
        assertThat(result).containsExactly(netflix, 60_000L)
    }

    @Test
    fun `separate stretches of the same app add up`() {
        val result = run(
            listOf(
                resume(netflix, min(10)),
                pause(netflix, min(20)),
                resume(netflix, min(50)),
                pause(netflix, min(55)),
            ),
        )
        assertThat(result).containsExactly(netflix, 15 * 60_000L)
    }

    @Test
    fun `unknown event types are ignored`() {
        val result = run(
            listOf(
                resume(netflix, min(10)),
                UsageEventRecord(netflix, 12345, min(15)),
                pause(netflix, min(20)),
            ),
        )
        assertThat(result).containsExactly(netflix, 10 * 60_000L)
    }

    @Test
    fun `another app resuming closes the previous session`() {
        // Netflix never emits PAUSED — YouTube coming forward is the only
        // signal that Netflix stopped.
        val result = run(
            listOf(
                resume(netflix, min(10)),
                resume(youtube, min(25)),
                pause(youtube, min(30)),
            ),
        )
        assertThat(result).containsExactly(
            netflix, 15 * 60_000L,
            youtube, 5 * 60_000L,
        )
    }

    @Test
    fun `the launcher coming forward closes an app session even though it is uncountable`() {
        val result = run(
            listOf(
                resume(netflix, min(10)),
                resume(launcher, min(22)),
            ),
            now = min(600),
        )
        assertThat(result).containsExactly(netflix, 12 * 60_000L)
    }

    @Test
    fun `an open session is credited only to the last event when the screen is off now`() {
        val result = run(
            listOf(resume(netflix, min(10)), resume(netflix, min(30))),
            now = min(600),
            screenInteractiveNow = false,
        )
        assertThat(result).containsExactly(netflix, 20 * 60_000L)
    }

    @Test
    fun `a long closed session is never capped`() {
        val result = run(listOf(resume(netflix, min(0)), pause(netflix, min(8 * 60))))
        assertThat(result).containsExactly(netflix, 8 * 60 * 60_000L)
    }

    @Test
    fun `a pause with no matching resume is ignored`() {
        assertThat(run(listOf(pause(netflix, min(20))))).isEmpty()
    }

    private fun event(pkg: String, type: Int, activity: String, at: Long) =
        UsageEventRecord(pkg, type, at, className = activity)

    @Test
    fun `switching activities inside one app keeps counting`() {
        // Real lifecycle order: the old activity pauses, the new one resumes,
        // and only then does the old one report STOPPED. That late STOPPED
        // must not end the session the new activity just opened.
        val result = run(
            listOf(
                event(netflix, UsageAccounting.ACTIVITY_RESUMED, "Browse", min(10)),
                event(netflix, UsageAccounting.ACTIVITY_PAUSED, "Browse", min(12)),
                event(netflix, UsageAccounting.ACTIVITY_RESUMED, "Player", min(12)),
                event(netflix, UsageAccounting.ACTIVITY_STOPPED, "Browse", min(12)),
                event(netflix, UsageAccounting.ACTIVITY_PAUSED, "Player", min(70)),
            ),
        )
        assertThat(result).containsExactly(netflix, 60 * 60_000L)
    }

    @Test
    fun `a late stop of the previous activity leaves a still-open session running`() {
        val result = run(
            listOf(
                event(netflix, UsageAccounting.ACTIVITY_RESUMED, "Splash", min(10)),
                event(netflix, UsageAccounting.ACTIVITY_PAUSED, "Splash", min(10)),
                event(netflix, UsageAccounting.ACTIVITY_RESUMED, "Main", min(10)),
                event(netflix, UsageAccounting.ACTIVITY_STOPPED, "Splash", min(11)),
            ),
            now = min(50),
        )
        assertThat(result).containsExactly(netflix, 40 * 60_000L)
    }

    @Test
    fun `a close for the activity in front still ends the session`() {
        val result = run(
            listOf(
                event(youtube, UsageAccounting.ACTIVITY_RESUMED, "Main", min(10)),
                event(youtube, UsageAccounting.ACTIVITY_PAUSED, "Main", min(25)),
            ),
        )
        assertThat(result).containsExactly(youtube, 15 * 60_000L)
    }

    @Test
    fun `no events produces an empty map`() {
        assertThat(run(emptyList())).isEmpty()
    }
}
