package com.screentime.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import com.screentime.tv.R

// Variable font covers all weights (100–900) in a single file.
val RubikFont = FontFamily(
    Font(R.font.rubik_variablefont_wght, FontWeight.Normal),
    Font(R.font.rubik_variablefont_wght, FontWeight.Medium),
    Font(R.font.rubik_variablefont_wght, FontWeight.SemiBold),
    Font(R.font.rubik_variablefont_wght, FontWeight.Bold),
)

// Single weight — Compose synthesizes heavier weights via FontSynthesis.
val VarelaFont = FontFamily(
    Font(R.font.varela_round_regular, FontWeight.Normal),
    Font(R.font.varela_round_regular, FontWeight.SemiBold),
    Font(R.font.varela_round_regular, FontWeight.Bold),
    Font(R.font.varela_round_regular, FontWeight.ExtraBold),
    Font(R.font.varela_round_regular, FontWeight.Black),
)

@Immutable
data class SproutTypography(
    val displayHero: TextStyle,
    val displayLarge: TextStyle,
    val titleLarge: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val button: TextStyle,
    val label: TextStyle,
    val keypadDigit: TextStyle,
)

val SproutTypeScale = SproutTypography(
    displayHero  = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = 45.sp, lineHeight = 48.sp),
    displayLarge = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = 42.sp, lineHeight = 45.sp),
    titleLarge   = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = 36.sp, lineHeight = 40.sp),
    bodyLarge    = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium   = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold,  fontSize = 13.sp, lineHeight = 18.sp),
    button       = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 18.sp),
    label        = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 15.sp),
    keypadDigit  = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = 42.sp, lineHeight = 42.sp),
)

val LocalSproutTypography = staticCompositionLocalOf { SproutTypeScale }

// Scales the type ramp proportionally to screen width.
// Reference 1920px design width.
@Composable
internal fun rememberSproutTypeScale(): SproutTypography {
    val w = LocalConfiguration.current.screenWidthDp
    val s = w / 1920f
    return SproutTypography(
        displayHero  = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = (90 * s).sp, lineHeight = (96 * s).sp),
        displayLarge = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = (84 * s).sp, lineHeight = (90 * s).sp),
        titleLarge   = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = (72 * s).sp, lineHeight = (78 * s).sp),
        bodyLarge    = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold,  fontSize = (32 * s).sp, lineHeight = (46 * s).sp),
        bodyMedium   = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.SemiBold,  fontSize = (26 * s).sp, lineHeight = (36 * s).sp),
        button       = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.ExtraBold, fontSize = (30 * s).sp, lineHeight = (36 * s).sp),
        label        = TextStyle(fontFamily = VarelaFont, fontWeight = FontWeight.ExtraBold, fontSize = (24 * s).sp, lineHeight = (30 * s).sp),
        keypadDigit  = TextStyle(fontFamily = RubikFont,  fontWeight = FontWeight.SemiBold,  fontSize = (84 * s).sp, lineHeight = (84 * s).sp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun materialTypeBridge(scale: SproutTypography) = Typography(
    displayLarge = scale.displayHero,
    displayMedium = scale.displayLarge,
    headlineLarge = scale.titleLarge,
    bodyLarge = scale.bodyLarge,
    bodyMedium = scale.bodyMedium,
    labelLarge = scale.button,
    labelMedium = scale.label,
)
