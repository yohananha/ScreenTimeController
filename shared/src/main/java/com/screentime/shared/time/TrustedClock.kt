package com.screentime.shared.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The time enforcement decisions are made against.
 *
 * On the TV this must NOT be the device's wall clock: the child can change
 * the date/time in Settings, and every rule keyed on "now" or "today" (bonus
 * expiry, allowed hours, instant lock, allow-all-day, the daily quota) moved
 * with it. The TV binds an implementation anchored to server/network time;
 * the parent phone, which isn't the thing being restricted, binds
 * [SystemTrustedClock].
 */
interface TrustedClock {
    fun now(): Instant

    /** The family's time zone — what "today" and allowed hours are measured in. */
    fun zone(): ZoneId

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    fun localNow(): LocalDateTime = LocalDateTime.ofInstant(now(), zone())

    /**
     * Device wall clock minus trusted time, in millis. ~0 normally; large
     * after the device clock has been moved. UsageStatsManager stamps events
     * with the wall clock, so usage windows are shifted by this.
     */
    fun wallOffsetMillis(): Long = System.currentTimeMillis() - now().toEpochMilli()
}

/** Plain device clock — for the parent's phone, where nothing is being enforced. */
@Singleton
class SystemTrustedClock @Inject constructor() : TrustedClock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
