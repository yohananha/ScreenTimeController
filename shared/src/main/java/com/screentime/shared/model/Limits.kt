package com.screentime.shared.model

data class Limits(
    val overallDailyMinutes: Int = DEFAULT_OVERALL_MINUTES,
    val resetTime: String = "00:00",
    val perApp: Map<String, AppLimit> = emptyMap(),
    val timeFrame: TimeFrameSchedule = TimeFrameSchedule.DEFAULT,
    /** yyyy-MM-dd date string. When it matches today, all limits and schedule are bypassed. */
    val allowAllDayDate: String? = null,
    /** When true the TV is immediately blocked regardless of schedule, limits, or bonus time. */
    val instantLocked: Boolean = false,
) {
    companion object {
        const val DEFAULT_OVERALL_MINUTES = 120
        const val UNLIMITED = -1
    }
}

data class AppLimit(
    val packageName: String,
    val dailyLimitMinutes: Int,
)
