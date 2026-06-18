package com.screentime.shared.model

import java.time.DayOfWeek
import java.time.LocalDateTime

data class TimeFrameWindow(
    val startMinute: Int, // minutes from midnight, 0..1439
    val endMinute: Int,   // exclusive, 1..1440
)

data class TimeFrameSchedule(
    val enabled: Boolean = false,
    val windowsByDay: Map<DayOfWeek, List<TimeFrameWindow>> = emptyMap(),
) {
    fun isAllowedAt(local: LocalDateTime): Boolean {
        if (!enabled) return true
        val minuteOfDay = local.hour * 60 + local.minute
        val windows = windowsByDay[local.dayOfWeek] ?: return false
        return windows.any { it.startMinute <= minuteOfDay && minuteOfDay < it.endMinute }
    }

    fun nextAllowedMinute(from: LocalDateTime): LocalDateTime? {
        if (!enabled) return null
        for (dayOffset in 0..6) {
            val day = from.plusDays(dayOffset.toLong())
            val startOfDay = day.toLocalDate().atStartOfDay()
            val minuteOfDay = if (dayOffset == 0) day.hour * 60 + day.minute else 0
            val next = windowsByDay[day.dayOfWeek].orEmpty()
                .sortedBy { it.startMinute }
                .firstOrNull { it.startMinute > minuteOfDay }
            if (next != null) return startOfDay.plusMinutes(next.startMinute.toLong())
        }
        return null
    }

    companion object {
        val DEFAULT = TimeFrameSchedule()
    }
}
