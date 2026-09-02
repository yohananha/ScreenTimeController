package com.screentime.tv.usage

/**
 * One raw transition reported by `UsageStatsManager`, decoupled from
 * `UsageEvents.Event` so [UsageAccounting] can be exercised on the JVM —
 * `UsageStatsManager` cannot be constructed in a plain unit test.
 */
data class UsageEventRecord(
    val packageName: String,
    val eventType: Int,
    val timestampMillis: Long,
)

/**
 * Turns a day's worth of usage events into foreground-millis-per-package.
 *
 * Only packages in [countable] accrue time: the TV launcher, SystemUI, the
 * screensaver and this app itself are all resumed activities as far as
 * `UsageStatsManager` is concerned, and counting them means a child burns
 * their quota by leaving the TV idle on the home row. See [CountablePackages]
 * for how that set is built.
 */
object UsageAccounting {

    /** Opens a session. Same value as `UsageEvents.Event.MOVE_TO_FOREGROUND`. */
    const val ACTIVITY_RESUMED = 1

    /** Closes a session. Same value as `UsageEvents.Event.MOVE_TO_BACKGROUND`. */
    const val ACTIVITY_PAUSED = 2

    /** Closes a session. `UsageEvents.Event.ACTIVITY_STOPPED`, public since API 29. */
    const val ACTIVITY_STOPPED = 23

    // Device-wide closing events. These carry no meaningful package name, so
    // each closes *every* open session. They have no public constant on
    // UsageEvents.Event (SCREEN_NON_INTERACTIVE and KEYGUARD_SHOWN are hidden
    // API), hence the literals — the values are stable platform contract and
    // an unknown event type here is simply ignored by the loop below.
    const val SCREEN_NON_INTERACTIVE = 16
    const val KEYGUARD_SHOWN = 17
    const val DEVICE_SHUTDOWN = 26

    /**
     * Ceiling on a session that is *still open* at the end of the window — the
     * last-resort guard for a device that emits no closing event at all, where
     * the fallback would otherwise bill the entire rest of the day to whatever
     * happened to be foreground when the TV was switched off.
     *
     * Deliberately generous: a session that has genuinely been closed is never
     * capped, because an all-afternoon binge is real screen time. Six hours is
     * past any plausible single unbroken sitting but well short of a day.
     */
    const val MAX_OPEN_SESSION_MILLIS = 6 * 60 * 60 * 1000L

    /**
     * @param events all events in the day's window, in ascending timestamp order.
     * @param countable packages allowed to accrue time; everything else is dropped.
     * @param nowMillis wall clock, used to close sessions still open at the end.
     * @param windowEndMillis exclusive end of the day being accounted for.
     * @param screenInteractiveNow whether the panel is awake right now. When it
     *   is not, a session left open never ran past the last event we saw, so it
     *   is credited only that far rather than up to [nowMillis].
     */
    fun foregroundMillis(
        events: List<UsageEventRecord>,
        countable: Set<String>,
        nowMillis: Long,
        windowEndMillis: Long,
        screenInteractiveNow: Boolean = true,
    ): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val openAt = mutableMapOf<String, Long>()
        var lastEventAt = Long.MIN_VALUE

        fun close(pkg: String, at: Long) {
            val start = openAt.remove(pkg) ?: return
            result.merge(pkg, (at - start).coerceAtLeast(0)) { a, b -> a + b }
        }

        fun closeAllExcept(pkg: String?, at: Long) {
            for (other in openAt.keys.toList()) {
                if (other != pkg) close(other, at)
            }
        }

        for (event in events) {
            val at = event.timestampMillis
            lastEventAt = maxOf(lastEventAt, at)
            val pkg = event.packageName

            when (event.eventType) {
                SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN, DEVICE_SHUTDOWN ->
                    closeAllExcept(null, at)

                ACTIVITY_RESUMED -> if (pkg.isNotEmpty()) {
                    // Exactly one app is foreground on a TV. Closing every
                    // other open session here is what stops a missing PAUSED —
                    // routine when an app is killed or the panel sleeps — from
                    // leaving a stale session to soak up the tail credit below.
                    // Note this runs for uncountable packages too: it is the
                    // launcher coming forward that ends the app the child just
                    // backed out of.
                    closeAllExcept(pkg, at)
                    if (pkg in countable) openAt.putIfAbsent(pkg, at)
                }

                // A second RESUMED without an intervening close means we missed
                // the close event; putIfAbsent above keeps the earliest start,
                // which is the honest reading — overwriting silently discards
                // that stretch.
                ACTIVITY_PAUSED, ACTIVITY_STOPPED -> if (pkg.isNotEmpty()) close(pkg, at)
            }
        }

        val cutoff = if (screenInteractiveNow) {
            minOf(nowMillis, windowEndMillis)
        } else {
            minOf(lastEventAt, nowMillis, windowEndMillis)
        }
        for ((pkg, start) in openAt) {
            val delta = (cutoff - start).coerceAtLeast(0).coerceAtMost(MAX_OPEN_SESSION_MILLIS)
            result.merge(pkg, delta) { a, b -> a + b }
        }
        return result.filterValues { it > 0 }
    }
}
