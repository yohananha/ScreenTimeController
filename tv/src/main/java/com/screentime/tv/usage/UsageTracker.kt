package com.screentime.tv.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * Returns foreground-millis-per-package for [date]. Uses event-pair
     * accounting (MOVE_TO_FOREGROUND → MOVE_TO_BACKGROUND) so it is robust
     * against the system pre-aggregating "today" partially.
     */
    fun millisPerPackage(date: LocalDate = LocalDate.now()): Map<String, Long> {
        val zone = ZoneId.systemDefault()
        val begin = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val events = manager.queryEvents(begin, end)
        val result = mutableMapOf<String, Long>()
        val openAt = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    openAt[pkg] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = openAt.remove(pkg) ?: continue
                    val delta = (event.timeStamp - start).coerceAtLeast(0)
                    result.merge(pkg, delta) { a, b -> a + b }
                }
            }
        }

        // Any package still open at "now" — count up to end-of-window.
        val now = System.currentTimeMillis().coerceAtMost(end)
        for ((pkg, start) in openAt) {
            val delta = (now - start).coerceAtLeast(0)
            result.merge(pkg, delta) { a, b -> a + b }
        }

        return result
    }
}
