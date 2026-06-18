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
data class SproutColors(
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
)

val SproutPalette = SproutColors(
    background = Color(0xFFFCF6F0),
    surface = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFFAF5EF),
    ink = Color(0xFF3A2A4D),
    inkMuted = Color(0xFF8A7C96),
    inkFaint = Color(0xFFB6ABBF),
    primary = Color(0xFFFF6B5E),
    primaryPressed = Color(0xFFF0584B),
    onPrimary = Color(0xFF3A2A4D),
    accent = Color(0xFFB9A8F0),
    accentContainer = Color(0xFFECE5FB),
    positiveDisplay = Color(0xFF4FCFA1),
    positiveText = Color(0xFF15795A),
    positiveContainer = Color(0xFFDFF6EC),
    warningDisplay = Color(0xFFF2A93B),
    warningText = Color(0xFF9A6313),
    warningContainer = Color(0xFFFCEFD7),
    overDisplay = Color(0xFFE5483A),
    overText = Color(0xFFB5281C),
    overContainer = Color(0xFFFBE2DF),
    outline = Color(0xFFEADFD4),
    outlineStrong = Color(0xFFD9CABD),
    tvBackground = Color(0xFF2E2140),
    tvSurface = Color(0xFF52436A),
    tvMutedText = Color(0xFFC9BBD6),
)

val LocalSproutColors = staticCompositionLocalOf { SproutPalette }

object SproutSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
}

object SproutRadius {
    val icon = RoundedCornerShape(10.dp)
    val input = RoundedCornerShape(18.dp)
    val card = RoundedCornerShape(24.dp)
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
