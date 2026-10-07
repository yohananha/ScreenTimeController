package com.screentime.tv.time

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.screentime.shared.time.TrustedClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The TV's notion of "now", independent of the device clock the child can
 * change in Settings. Sources, best first:
 *
 *  1. A server anchor: Firestore's own timestamp from [TvHeartbeat], advanced
 *     by the monotonic `elapsedRealtime` (which Settings can't touch).
 *  2. Android's network time (NTP/NITZ, API 33+), when the platform has it.
 *  3. A lower bound: the last trusted time we saw, plus time since then.
 *     Before any trusted source answers this boot (e.g. offline right after
 *     a reboot), "now" is `max(device clock, lower bound)` — a clock set
 *     *back* is caught; a clock set forward can't be told apart offline.
 *
 * The zone comes from the family setting (see [TvHeartbeat]) so changing the
 * TV's time zone doesn't move "today" either.
 *
 * Pure time logic, no I/O — all inputs are injectable for tests.
 */
@Singleton
class TvTrustedClock internal constructor(
    private val prefs: SharedPreferences,
    private val elapsedRealtime: () -> Long,
    private val wallClock: () -> Long,
    private val networkTime: () -> Long?,
    private val bootCount: () -> Int,
) : TrustedClock {

    @Inject constructor(@ApplicationContext context: Context) : this(
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        elapsedRealtime = SystemClock::elapsedRealtime,
        wallClock = System::currentTimeMillis,
        networkTime = ::platformNetworkTimeMillis,
        bootCount = { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, UNKNOWN_BOOT) },
    )

    private data class Anchor(val trustedMillis: Long, val elapsedAt: Long)

    @Volatile private var anchor: Anchor? = null
    @Volatile private var familyZone: ZoneId? = null

    override fun now(): Instant = Instant.ofEpochMilli(nowMillis())

    override fun zone(): ZoneId = familyZone ?: ZoneId.systemDefault()

    override fun wallOffsetMillis(): Long = wallClock() - nowMillis()

    fun nowMillis(): Long {
        val elapsed = elapsedRealtime()
        anchor?.let { return it.trustedMillis + (elapsed - it.elapsedAt) }
        networkTime()?.let { return it }
        return maxOf(wallClock(), lowerBound(elapsed))
    }

    /** Anchors to a trusted (server) time observed just now, and persists it as the new lower bound. */
    fun setAnchor(trusted: Instant) {
        val elapsed = elapsedRealtime()
        anchor = Anchor(trusted.toEpochMilli(), elapsed)
        prefs.edit()
            .putLong(KEY_TRUSTED, trusted.toEpochMilli())
            .putLong(KEY_ELAPSED, elapsed)
            .putInt(KEY_BOOT, bootCount())
            .apply()
    }

    /** IANA id from the family settings, or null to fall back to the device zone. */
    fun setZone(zoneId: String?) {
        familyZone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() }
    }

    /**
     * The earliest "now" can be, from the last persisted trusted time: in the
     * same boot, exactly that time plus elapsed since; in a later boot, at
     * least that time plus the uptime of this boot. If we can't tell which
     * boot it was, just that time — never an overestimate.
     */
    private fun lowerBound(elapsedNow: Long): Long {
        if (!prefs.contains(KEY_TRUSTED)) return Long.MIN_VALUE
        val trusted = prefs.getLong(KEY_TRUSTED, 0L)
        val savedBoot = prefs.getInt(KEY_BOOT, UNKNOWN_BOOT)
        val boot = bootCount()
        return when {
            savedBoot == UNKNOWN_BOOT || boot == UNKNOWN_BOOT -> trusted
            savedBoot == boot -> trusted + (elapsedNow - prefs.getLong(KEY_ELAPSED, elapsedNow)).coerceAtLeast(0)
            else -> trusted + elapsedNow
        }
    }

    private companion object {
        const val PREFS_NAME = "trusted_clock"
        const val KEY_TRUSTED = "trusted_millis"
        const val KEY_ELAPSED = "elapsed_at_trusted"
        const val KEY_BOOT = "boot_count_at_trusted"
        const val UNKNOWN_BOOT = -1
    }
}

/** Platform network time (not the user-settable clock), or null if unavailable. */
private fun platformNetworkTimeMillis(): Long? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return try {
        SystemClock.currentNetworkTimeClock().millis()
    } catch (_: DateTimeException) {
        null
    }
}
