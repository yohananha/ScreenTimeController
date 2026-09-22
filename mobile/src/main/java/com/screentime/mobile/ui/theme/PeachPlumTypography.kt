package com.screentime.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.screentime.mobile.R

// Variable font covers all weights (100–900) in a single file.
val RubikFont = FontFamily(
    Font(R.font.rubik_variablefont_wght, FontWeight.Normal),
    Font(R.font.rubik_variablefont_wght, FontWeight.Medium),
    Font(R.font.rubik_variablefont_wght, FontWeight.SemiBold),
    Font(R.font.rubik_variablefont_wght, FontWeight.Bold),
)

/**
 * Field names kept from the old Sprout system; values now come from
 * design/i6c-peach-plum/tokens.json#type.phone — one family (Rubik) for
 * everything, no separate body face (VarelaFont is gone). Mapping: title ->
 * phone.title(30), title(field) -> phone.sheetTitle(24, exact), headline ->
 * phone.heading(18), bodyL -> phone.bodyStrong(16, exact), body ->
 * phone.body(15, exact), label -> phone.nav(13, exact), caption ->
 * phone.caption(13, exact). `gauge`/`gaugeWord`/`codeTile` are new fields for
 * TimeGauge and the unlock code tiles, which didn't exist before.
 */
@Immutable
data class PeachPlumTypography(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val bodyL: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val gauge: TextStyle,
    val gaugeWord: TextStyle,
    val codeTile: TextStyle,
)

val PeachPlumTypeScale = PeachPlumTypography(
    display    = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.03f).em),
    title      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 27.sp, letterSpacing = (-0.02f).em),
    headline   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 23.sp, letterSpacing = (-0.01f).em),
    bodyL      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    body       = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = 15.sp, lineHeight = 21.sp),
    bodyStrong = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    label      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    caption    = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = 13.sp, lineHeight = 17.sp),
    gauge      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = (-0.03f).em),
    gaugeWord  = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 18.sp),
    codeTile   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 40.sp),
)

val LocalPeachPlumTypography = staticCompositionLocalOf { PeachPlumTypeScale }

// Scales proportionally to screen width.
// Reference 360dp = standard phone. Tablets scale up gently, clamped to 1.5×.
@Composable
internal fun rememberPeachPlumTypeScale(): PeachPlumTypography {
    val w = LocalConfiguration.current.screenWidthDp
    val s = (w / 360f).coerceIn(0.85f, 1.5f)
    return PeachPlumTypography(
        display    = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (30 * s).sp, lineHeight = (32 * s).sp, letterSpacing = (-0.03f).em),
        title      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (24 * s).sp, lineHeight = (27 * s).sp, letterSpacing = (-0.02f).em),
        headline   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (18 * s).sp, lineHeight = (23 * s).sp, letterSpacing = (-0.01f).em),
        bodyL      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (16 * s).sp, lineHeight = (22 * s).sp),
        body       = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = (15 * s).sp, lineHeight = (21 * s).sp),
        bodyStrong = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (15 * s).sp, lineHeight = (21 * s).sp),
        label      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (13 * s).sp, lineHeight = (16 * s).sp),
        caption    = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = (13 * s).sp, lineHeight = (17 * s).sp),
        gauge      = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (56 * s).sp, lineHeight = (56 * s).sp, letterSpacing = (-0.03f).em),
        gaugeWord  = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = (16 * s).sp, lineHeight = (18 * s).sp),
        codeTile   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (40 * s).sp, lineHeight = (40 * s).sp),
    )
}

@Composable
internal fun materialTypeBridge(scale: PeachPlumTypography) = Typography(
    displayLarge   = scale.display,
    headlineMedium = scale.title,
    titleLarge     = scale.title,
    titleMedium    = scale.headline,
    bodyLarge      = scale.bodyL,
    bodyMedium     = scale.body,
    labelLarge     = scale.label,
    labelSmall     = scale.caption,
)
