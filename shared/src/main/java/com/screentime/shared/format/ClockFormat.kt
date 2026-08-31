package com.screentime.shared.format

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import com.screentime.shared.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locale-aware clock/date formatting, replacing the hand-rolled AM/PM and
 * `DateTimeFormatter.ofPattern("h:mm a")` / `ofPattern("EEEE, MMM d")` copies
 * that used to live in LimitsScreen.kt, TimeFrameScreen.kt, RequestsScreen.kt
 * and EnforcementAccessibilityService.kt. Those were all English-locked (a
 * literal `"AM"`/`"PM"`) and ignored the user's 24-hour clock preference.
 *
 * `android.text.format.DateFormat.getBestDateTimePattern` builds the right
 * field order and separators for the target locale (e.g. Hebrew's 24-hour
 * convention), and `is24HourFormat` respects the device's own setting rather
 * than assuming 12-hour. `HistoryScreen.kt`'s existing
 * `getDisplayName(style, Locale.getDefault())` calls were already correct —
 * [dayName] below is that same pattern, centralized.
 */
@Singleton
class ClockFormat @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Renders a minute-of-day (0..1439) as a locale- and 24h-setting-aware clock time. */
    fun timeOfDay(minuteOfDay: Int, locale: Locale = Locale.getDefault()): String =
        timeOfDay(LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60), locale)

    fun timeOfDay(time: LocalTime, locale: Locale = Locale.getDefault()): String {
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return DateTimeFormatter.ofPattern(pattern, locale).format(time).bidiWrap()
    }

    /** Joins two already-formatted clock times into a "%1$s–%2$s" range, isolated so the dash can't flip. */
    fun range(res: Resources, start: String, end: String): String =
        res.getString(R.string.clock_range, start, end).bidiWrap()

    /** Replaces `ofPattern("EEEE, MMM d")` / `ofPattern("MMM d")`. */
    fun dayAndDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMd")
        return DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }

    fun dayName(day: DayOfWeek, style: TextStyle, locale: Locale = Locale.getDefault()): String =
        day.getDisplayName(style, locale)
}
