package com.screentime.shared.model

data class Limits(
    val overallDailyMinutes: Int = DEFAULT_OVERALL_MINUTES,
    val resetTime: String = "00:00",
    val perApp: Map<String, AppLimit> = emptyMap(),
    val timeFrame: TimeFrameSchedule = TimeFrameSchedule.DEFAULT,
    /** yyyy-MM-dd date string. When it matches today, all limits and schedule are bypassed. */
    val allowAllDayDate: String? = null,
    /** When true, "Allow" stays active every day instead of resetting at midnight. */
    val allowAllDayIndefinite: Boolean = false,
    /**
     * yyyy-MM-dd date string. When it matches today, the TV is immediately
     * blocked regardless of schedule, limits, or bonus time — like
     * [allowAllDayDate], this resets itself at midnight instead of staying
     * locked indefinitely until a parent manually clears it.
     */
    val instantLockedDate: String? = null,
    /**
     * yyyy-MM-dd date string and extra minutes a parent granted. Counts only
     * while the date is today, so it expires by itself at midnight.
     */
    val bonusDate: String? = null,
    val bonusMinutes: Int = 0,
) {
    /** Extra minutes that apply on [today] (0 when the bonus is from another day). */
    fun bonusFor(today: String): Int = if (bonusDate == today) bonusMinutes.coerceAtLeast(0) else 0

    companion object {
        const val DEFAULT_OVERALL_MINUTES = 120
        const val UNLIMITED = -1
    }
}

data class AppLimit(
    val packageName: String,
    val dailyLimitMinutes: Int,
)
