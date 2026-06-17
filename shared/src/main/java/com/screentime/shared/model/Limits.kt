package com.screentime.shared.model

data class Limits(
    val overallDailyMinutes: Int = DEFAULT_OVERALL_MINUTES,
    val resetTime: String = "00:00",
    val perApp: Map<String, AppLimit> = emptyMap(),
) {
    companion object {
        const val DEFAULT_OVERALL_MINUTES = 120

        /** Sentinel for [AppLimit.dailyLimitMinutes] / [overallDailyMinutes]: never blocks. */
        const val UNLIMITED = -1
    }
}

data class AppLimit(
    val packageName: String,
    val dailyLimitMinutes: Int,
)
