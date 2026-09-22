package com.screentime.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PeachPlumColors(
    val background: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val primary: Color,
    val primaryPressed: Color,
    val onPrimary: Color,
    val accent: Color,
    val accentContainer: Color,
    val positiveDisplay: Color,
    val positiveText: Color,
    val positiveContainer: Color,
    val warningDisplay: Color,
    val warningText: Color,
    val warningContainer: Color,
    val overDisplay: Color,
    val overText: Color,
    val overContainer: Color,
    val outline: Color,
    val outlineStrong: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val darkMutedText: Color,
)

/**
 * Values generated from design/i6c-peach-plum/tokens.json ("Peach & plum",
 * direction I6c) — same "keep names, swap values" approach used for the TV
 * app (see tv/.../ui/theme/PeachPlumTokens.kt), so every mobile screen
 * re-skins via `PeachPlum.colors.<name>` without per-call-site edits. The
 * Sprout->PeachPlum rename itself was deferred until the screens that still
 * referenced the old Sprout-named fields were migrated off Limits/Requests/
 * Codes/Settings, matching the TV redesign's order.
 * Mapping: primary/onPrimary -> peach accent + its ink text; accent -> rose;
 * positive* -> ok; warning* -> peach-tinted (no amber in the new palette);
 * over* -> the system's only red; dark* -> ink-card tones (the "TVs" card on
 * Family and the old HeroCard both draw a dark plum card on an otherwise
 * light screen — darkSurface/darkMutedText are for content sitting on it).
 */
val PeachPlumPalette = PeachPlumColors(
    background = Color(0xFFFFF6EE),
    surface = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFFFEDE0),
    ink = Color(0xFF2A1E2E),
    inkMuted = Color(0xFF6E5F66),
    inkFaint = Color(0xFFB9A9B5),
    primary = Color(0xFFFFB088),
    primaryPressed = Color(0xFFFFB088),
    onPrimary = Color(0xFF2A1E2E),
    accent = Color(0xFFF79AC0),
    accentContainer = Color(0x29FFB088),
    positiveDisplay = Color(0xFF12A87A),
    positiveText = Color(0xFF0F6B4E),
    positiveContainer = Color(0xFFDFF6EC),
    warningDisplay = Color(0xFFFFB088),
    warningText = Color(0xFF2A1E2E),
    warningContainer = Color(0x2EFFB088),
    overDisplay = Color(0xFFB3261E),
    overText = Color(0xFFB3261E),
    overContainer = Color(0xFFFBE2DF),
    outline = Color(0xFFF0E3D8),
    outlineStrong = Color(0xFFB9A9B5),
    darkBackground = Color(0xFF2A1E2E),
    darkSurface = Color(0x1FFFFFFF),
    darkMutedText = Color(0xFFB9A9B5),
)

val LocalPeachPlumColors = staticCompositionLocalOf { PeachPlumPalette }

object PeachPlumSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
}

object PeachPlumRadius {
    val icon = RoundedCornerShape(12.dp)
    val input = RoundedCornerShape(18.dp)
    val strip = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(22.dp)
    val large = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(999.dp)
}

/** Adaptive horizontal screen padding: 16dp compact / 32dp medium / 64dp expanded. */
@Composable
fun rememberScreenPadding(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w >= 840 -> 64.dp
        w >= 600 -> 32.dp
        else -> 16.dp
    }
}
