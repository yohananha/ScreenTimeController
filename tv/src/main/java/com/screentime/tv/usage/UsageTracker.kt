package com.screentime.tv.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val countablePackages: CountablePackages,
) {
    private val manager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val power: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /**
     * Returns foreground-millis-per-package for [date]. Uses event-pair
     * accounting (MOVE_TO_FOREGROUND → MOVE_TO_BACKGROUND) so it is robust
     * against the system pre-aggregating "today" partially.
     *
     * Only launcher-visible apps accrue time — idle time on the home screen,
     * in Settings, in the screensaver or in this app is not screen use. See
     * [CountablePackages] and [UsageAccounting].
     *
     * @param alwaysCount packages that count even without a launcher entry,
     *   so an app the parent explicitly set a limit on can never escape the
     *   quota. Callers that hold a `Limits` pass its `perApp` keys. This app
     *   is never counted, even if a limit names it — the block overlay is our
     *   own foreground time, and letting it feed the quota would make a block
     *   self-sustaining.
     */
    fun millisPerPackage(
        date: LocalDate = LocalDate.now(),
        alwaysCount: Set<String> = emptySet(),
    ): Map<String, Long> {
        val zone = ZoneId.systemDefault()
        val begin = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val events = manager.queryEvents(begin, end)
        val records = mutableListOf<UsageEventRecord>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            records += UsageEventRecord(
                packageName = event.packageName.orEmpty(),
                eventType = event.eventType,
                timestampMillis = event.timeStamp,
            )
        }

        return UsageAccounting.foregroundMillis(
            events = records,
            countable = (countablePackages.packages() + alwaysCount) - context.packageName,
            nowMillis = System.currentTimeMillis(),
            windowEndMillis = end,
            screenInteractiveNow = power.isInteractive,
        )
    }
}
