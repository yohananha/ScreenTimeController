package com.screentime.tv.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import com.screentime.shared.room.AppDatabase
import com.screentime.shared.time.TrustedClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val countablePackages: CountablePackages,
    private val clock: TrustedClock,
    private val database: AppDatabase,
) {
    private val manager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val power: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val usageDay = UsageDay()

    /**
     * Returns foreground-millis-per-package for the trusted "today" (see
     * [TrustedClock]) — not the device's own date, which the child can
     * change. Uses event-pair accounting (MOVE_TO_FOREGROUND →
     * MOVE_TO_BACKGROUND) so it is robust against the system pre-aggregating
     * "today" partially, and [UsageDay] so a mid-day clock change can't reset
     * the count.
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
    fun millisPerPackage(alwaysCount: Set<String> = emptySet()): Map<String, Long> {
        val zone = clock.zone()
        val today = clock.today()
        val wallNow = System.currentTimeMillis()
        val window = usageDay.window(
            today = today,
            dayStartTrusted = today.atStartOfDay(zone).toInstant().toEpochMilli(),
            dayEndTrusted = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            offset = clock.wallOffsetMillis(),
            wallNow = wallNow,
        )

        // Look back before the window so a session already open at its start
        // (watching across midnight, or across a clock jump) is seen and
        // credited from the window start, instead of not at all.
        val events = manager.queryEvents(
            window.beginWallMillis - UsageAccounting.MAX_OPEN_SESSION_MILLIS,
            window.endWallMillis,
        )
        val records = mutableListOf<UsageEventRecord>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            records += UsageEventRecord(
                packageName = event.packageName.orEmpty(),
                eventType = event.eventType,
                timestampMillis = event.timeStamp,
                className = event.className,
            )
        }

        val segment = UsageAccounting.foregroundMillis(
            events = records,
            countable = (countablePackages.packages() + alwaysCount) - context.packageName,
            nowMillis = wallNow,
            windowEndMillis = window.endWallMillis,
            screenInteractiveNow = power.isInteractive,
            windowStartMillis = window.beginWallMillis,
        )
        return usageDay.fold(segment)
    }

    /**
     * Credits [millis] to [pkg] as directly observed foreground time — the
     * accessibility service's per-minute view of which app window is active.
     * Counted on the same terms as the event log: only countable packages,
     * never this app, and only while the screen is on. See [UsageDay.accrueLive].
     */
    fun creditObservedForeground(pkg: String, millis: Long, alwaysCount: Set<String> = emptySet()) {
        if (pkg == context.packageName || !power.isInteractive) return
        if (pkg !in alwaysCount && pkg !in countablePackages.packages()) return
        usageDay.accrueLive(clock.today(), pkg, millis)
    }

    /**
     * Floors today's total with what was already recorded to Room, so a
     * process restart (whose in-memory [UsageDay] starts empty) after a
     * clock change can't lower the count. Call once at startup.
     */
    suspend fun restoreToday() {
        val today = clock.today()
        val recorded = database.usageDao().loadForDate(today.toString())
            .associate { it.packageName to it.millis }
        usageDay.seed(today, recorded)
    }
}
