package com.screentime.tv.usage

import java.time.LocalDate
import kotlin.math.abs

/**
 * Keeps one trusted day's usage total honest when the device clock moves.
 *
 * UsageStatsManager stamps events with the device's wall clock. Normally
 * that equals trusted time and the day's window is simply
 * [trusted day start, trusted day end] shifted into wall-clock terms. When
 * the clock is changed mid-day, events before and after the change live in
 * different frames — set the clock forward a day and "today" in the new
 * frame starts empty, which used to hand the child a fresh quota.
 *
 * So on a detected jump (wall-minus-trusted offset moving by more than
 * [JUMP_TOLERANCE_MILLIS]) the total so far is banked and counting resumes
 * from the jump in the new frame: total = bank + this segment. And the day's
 * total is a high-water mark — it never goes down within a trusted day.
 *
 * Over-counting is possible after a jump *back* (the new frame can overlap
 * real earlier usage); that errs on the side of less screen time, never more.
 *
 * It also holds a second, independent count: foreground time the
 * accessibility service observed directly, minute by minute ([accrueLive]).
 * Each package's total is the larger of the two, so a gap in the platform's
 * event log — a missing RESUMED, a stray close event — can no longer make
 * time on screen vanish from the count.
 *
 * Pure (no Android) so it can be unit-tested; [UsageTracker] drives it.
 */
class UsageDay {

    data class Window(val beginWallMillis: Long, val endWallMillis: Long)

    private var day: LocalDate? = null
    private var bank: Map<String, Long> = emptyMap()
    private var segmentStartWall = 0L
    private var segmentOffset = 0L
    private var highWater: Map<String, Long> = emptyMap()
    private var seed: Pair<LocalDate, Map<String, Long>>? = null
    private var liveDay: LocalDate? = null
    private var live: Map<String, Long> = emptyMap()

    /**
     * The wall-clock window to query for [today]. [dayStartTrusted] /
     * [dayEndTrusted] are the trusted day's bounds in epoch millis; [offset]
     * is wall minus trusted right now.
     */
    @Synchronized
    fun window(today: LocalDate, dayStartTrusted: Long, dayEndTrusted: Long, offset: Long, wallNow: Long): Window {
        if (today != day) {
            day = today
            bank = emptyMap()
            highWater = seed?.takeIf { it.first == today }?.second.orEmpty()
            segmentStartWall = dayStartTrusted + offset
            segmentOffset = offset
        } else if (abs(offset - segmentOffset) > JUMP_TOLERANCE_MILLIS) {
            bank = highWater
            segmentStartWall = wallNow
            segmentOffset = offset
        }
        return Window(segmentStartWall, dayEndTrusted + offset)
    }

    /** Folds the current segment's counts in; returns the day's (never-decreasing) total. */
    @Synchronized
    fun fold(segment: Map<String, Long>): Map<String, Long> {
        val observed = if (liveDay == day) live else emptyMap()
        val total = sum(bank, segment).let { counted ->
            (counted.keys + observed.keys).associateWith { pkg ->
                maxOf(counted[pkg] ?: 0L, observed[pkg] ?: 0L)
            }
        }
        highWater = (highWater.keys + total.keys).associateWith { pkg ->
            maxOf(highWater[pkg] ?: 0L, total[pkg] ?: 0L)
        }
        return highWater
    }

    /**
     * Credits [millis] of foreground time for [pkg] on [date], as observed
     * directly rather than reconstructed from usage events. Measured with
     * the monotonic clock by the caller, so a clock change can't inflate it.
     */
    @Synchronized
    fun accrueLive(date: LocalDate, pkg: String, millis: Long) {
        if (millis <= 0) return
        if (date != liveDay) {
            liveDay = date
            live = emptyMap()
        }
        live = live + (pkg to (live[pkg] ?: 0L) + millis)
    }

    /**
     * Floors [date]'s total with values already recorded (e.g. from Room
     * after a process restart, when the in-memory state is gone).
     */
    @Synchronized
    fun seed(date: LocalDate, recorded: Map<String, Long>) {
        seed = date to recorded
        if (date == day) {
            highWater = (highWater.keys + recorded.keys).associateWith { pkg ->
                maxOf(highWater[pkg] ?: 0L, recorded[pkg] ?: 0L)
            }
        }
    }

    private fun sum(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> =
        (a.keys + b.keys).associateWith { (a[it] ?: 0L) + (b[it] ?: 0L) }

    companion object {
        /** NTP corrections are far smaller; anything above this is a manual clock change. */
        const val JUMP_TOLERANCE_MILLIS = 2 * 60 * 1000L
    }
}
