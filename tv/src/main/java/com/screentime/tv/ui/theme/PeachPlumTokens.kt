package com.screentime.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
    val tvBackground: Color,
    val tvSurface: Color,
    val tvMutedText: Color,
    val tvCream: Color,
)

/**
 * Values generated from design/i6c-peach-plum/tokens.json ("Peach & plum",
 * direction I6c). Field *names* are inherited from the old Sprout system —
 * this file (and Theme.kt's accessor) was renamed Sprout→PeachPlum in README
 * §7's cleanup pass once the values below had fully replaced Sprout's, but
 * the individual color-role field names (background/ink/primary/…) were kept
 * as-is rather than renamed too, since every TV screen already reads through
 * them and there was no ambiguity left to resolve.
 * Mapping: primary/onPrimary → peach accent + its ink text; accent → rose
 * (Waiting circle); positive* → ok; warning* → peach-tinted (no amber in the
 * new palette, used only by the locked-out circle); over* → over/error tv
 * colors; outline* → translucent white tv.line/tv.surface tones; tv* → the
 * literal tv.ground/tv.surface/tv.muted/tv.ink tokens.
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
    overText = Color(0xFFFFD3CD),
    overContainer = Color(0x38B3261E),
    outline = Color(0x29FFFFFF),
    outlineStrong = Color(0x14FFFFFF),
    tvBackground = Color(0xFF1E1622),
    tvSurface = Color(0x14FFFFFF),
    tvMutedText = Color(0xFFB9A9B5),
    tvCream = Color(0xFFFFF6EE),
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
    val icon = RoundedCornerShape(10.dp)
    val input = RoundedCornerShape(18.dp)
    val card = RoundedCornerShape(24.dp)
    val large = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(999.dp)
}
