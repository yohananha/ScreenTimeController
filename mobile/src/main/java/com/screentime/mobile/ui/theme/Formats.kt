package com.screentime.mobile.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.screentime.shared.format.ClockFormat
import com.screentime.shared.format.DurationFormat

/**
 * Bundles the `:shared` duration/clock formatters for Compose call sites, so
 * screens don't have to construct or inject them individually. See
 * [LocalFormats] and [ScreenTimeTheme][com.screentime.mobile.ui.theme.ScreenTimeTheme].
 */
data class Formats(val duration: DurationFormat, val clock: ClockFormat)

val LocalFormats = staticCompositionLocalOf<Formats> {
    error("LocalFormats not provided — wrap content in ScreenTimeTheme")
}
