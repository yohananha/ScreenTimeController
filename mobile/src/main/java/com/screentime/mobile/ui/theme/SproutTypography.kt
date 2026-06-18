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
import androidx.compose.ui.unit.sp
import com.screentime.mobile.R

// Variable font covers all weights (100–900) in a single file.
val RubikFont = FontFamily(
    Font(R.font.rubik_variablefont_wght, FontWeight.Normal),
    Font(R.font.rubik_variablefont_wght, FontWeight.Medium),
    Font(R.font.rubik_variablefont_wght, FontWeight.SemiBold),
    Font(R.font.rubik_variablefont_wght, FontWeight.Bold),
)

// Single weight only — registering multiple weights for the same file triggers synthesis artifacts.
val VarelaFont = FontFamily(
    Font(R.font.varela_round_regular, FontWeight.Normal),
)

@Immutable
data class SproutTypography(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val bodyL: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

val SproutTypeScale = SproutTypography(
    display = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 36.sp),
    title = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
    headline = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    bodyL = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    body = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyStrong = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    label = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 14.sp),
    caption = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
)

val LocalSproutTypography = staticCompositionLocalOf { SproutTypeScale }

// Scales proportionally to screen width.
// Reference 360dp = standard phone. Tablets scale up gently, clamped to 1.5×.
@Composable
internal fun rememberSproutTypeScale(): SproutTypography {
    val w = LocalConfiguration.current.screenWidthDp
    val s = (w / 360f).coerceIn(0.85f, 1.5f)
    return SproutTypography(
        display    = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = (34 * s).sp, lineHeight = (36 * s).sp),
        title      = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = (24 * s).sp, lineHeight = (28 * s).sp),
        headline   = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.Medium,    fontSize = (20 * s).sp, lineHeight = (26 * s).sp),
        bodyL      = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold,  fontSize = (16 * s).sp, lineHeight = (24 * s).sp),
        body       = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.Normal,    fontSize = (15 * s).sp, lineHeight = (22 * s).sp),
        bodyStrong = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold,  fontSize = (15 * s).sp, lineHeight = (22 * s).sp),
        label      = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.Bold,      fontSize = (13 * s).sp, lineHeight = (14 * s).sp),
        caption    = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold,  fontSize = (12 * s).sp, lineHeight = (16 * s).sp),
    )
}

@Composable
internal fun materialTypeBridge(scale: SproutTypography) = Typography(
    displayLarge   = scale.display,
    headlineMedium = scale.title,
    titleLarge     = scale.title,
    titleMedium    = scale.headline,
    bodyLarge      = scale.bodyL,
    bodyMedium     = scale.body,
    labelLarge     = scale.label,
    labelSmall     = scale.caption,
)
