package com.screentime.tv.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.screentime.shared.format.ClockFormat
import com.screentime.shared.format.DurationFormat

/**
 * Bundles the `:shared` duration/clock formatters for Compose call sites.
 * See [LocalFormats] and [ScreenTimeTvTheme][com.screentime.tv.ui.theme.ScreenTimeTvTheme].
 *
 * Note: the block overlay is hosted in a WindowManager-attached ComposeView
 * rather than an Activity (see BlockOverlayController), so its `LocalContext`
 * is whatever Context that ComposeView was built with — TvLocaleController
 * wraps that context for locale purposes, and the formatters built from it
 * pick up the same locale automatically since they read Context/Resources
 * at call time, not at construction time... except ClockFormat *does* close
 * over its Context at construction — see TvLocaleController for how the
 * overlay is rebuilt (not just recomposed) when the locale changes.
 */
data class Formats(val duration: DurationFormat, val clock: ClockFormat)

val LocalFormats = staticCompositionLocalOf<Formats> {
    error("LocalFormats not provided — wrap content in ScreenTimeTvTheme")
}
