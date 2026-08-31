package com.screentime.shared.format

import android.content.res.Resources
import com.screentime.shared.R
import com.screentime.shared.model.Limits
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a minute count as a short duration label ("45m", "1h", "1h 30m"),
 * replacing the ~7 near-identical copies of this logic that used to live in
 * LimitsScreen.kt, TimeFrameScreen.kt, BlockOverlayContent.kt and
 * CodesScreen.kt. Centralizing it here means the locale fix — and the bidi
 * isolation these values need once they're embedded in a Hebrew sentence —
 * only has to happen once.
 *
 * Methods take [Resources] rather than a [android.content.Context] because
 * the TV overlay must sometimes format against a *different* Resources than
 * the process default (see TvLocaleController.wrap); Compose call sites
 * simply pass `LocalContext.current.resources`.
 */
@Singleton
class DurationFormat @Inject constructor() {

    /** "45m", "1h", "1h 30m", "No limit" (for [Limits.UNLIMITED]), "0m". */
    fun minutes(res: Resources, totalMinutes: Int): String = when {
        totalMinutes == Limits.UNLIMITED -> res.getString(R.string.duration_none)
        totalMinutes <= 0 -> res.getString(R.string.duration_zero)
        totalMinutes % 60 == 0 -> res.getString(R.string.duration_h, totalMinutes / 60)
        totalMinutes < 60 -> res.getString(R.string.duration_m, totalMinutes)
        else -> res.getString(R.string.duration_h_m, totalMinutes / 60, totalMinutes % 60)
    }.bidiWrap()

    /** "4:05" style countdown, for a lockout timer or a code-expiry banner. */
    fun countdown(res: Resources, totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        return res.getString(R.string.countdown_m_s, s / 60, s % 60).bidiWrap()
    }

    /** "1 minute" / "2 minutes" — real CLDR plural rules, not an if/else ternary. */
    fun quantityMinutes(res: Resources, count: Int): String =
        res.getQuantityString(R.plurals.minutes, count, count).bidiWrap()

    /** "1 day" / "2 days". */
    fun quantityDays(res: Resources, count: Int): String =
        res.getQuantityString(R.plurals.days, count, count).bidiWrap()

    /** "1 app" / "2 apps". */
    fun quantityApps(res: Resources, count: Int): String =
        res.getQuantityString(R.plurals.apps, count, count)
}
