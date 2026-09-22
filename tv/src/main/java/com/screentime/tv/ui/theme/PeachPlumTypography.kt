package com.screentime.tv.ui.theme

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

/**
 * Field names kept from the old Sprout system; values now come from
 * design/i6c-peach-plum/tokens.json#type.tv — one family (Rubik) for
 * everything, no separate body face. Reference sizes are for the 1920px
 * design width, scaled by [rememberPeachPlumTypeScale] the same way the old
 * ramp was. `displayHero` = Block's 140, `displayLarge` = the 120 default,
 * `titleLarge` = Keypad's 88; screens between those (Ask 132, Approved/
 * Denied/Unlocked 128, Locked 112, Pairing 104) apply a ratio against
 * `displayLarge` at the call site rather than adding a field per screen.
 */
@Immutable
data class PeachPlumTypography(
    val displayHero: TextStyle,
    val displayLarge: TextStyle,
    val titleLarge: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val button: TextStyle,
    val label: TextStyle,
    val keypadDigit: TextStyle,
    val codeTile: TextStyle,
)

val PeachPlumTypeScale = PeachPlumTypography(
    displayHero  = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 140.sp, lineHeight = 143.sp, letterSpacing = (-0.03f).em),
    displayLarge = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 120.sp, lineHeight = 122.sp, letterSpacing = (-0.03f).em),
    titleLarge   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 88.sp,  lineHeight = 90.sp,  letterSpacing = (-0.03f).em),
    bodyLarge    = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Normal,   fontSize = 36.sp,  lineHeight = 50.sp),
    bodyMedium   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = 26.sp,  lineHeight = 30.sp),
    button       = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 32.sp,  lineHeight = 36.sp),
    label        = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,  lineHeight = 32.sp),
    keypadDigit  = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 56.sp,  lineHeight = 56.sp),
    codeTile     = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = 84.sp,  lineHeight = 84.sp, letterSpacing = (-0.02f).em),
)

val LocalPeachPlumTypography = staticCompositionLocalOf { PeachPlumTypeScale }

// Scales the type ramp proportionally to screen width.
// Reference 1920px design width.
@Composable
internal fun rememberPeachPlumTypeScale(): PeachPlumTypography {
    val w = LocalConfiguration.current.screenWidthDp
    val s = w / 1920f
    return PeachPlumTypography(
        displayHero  = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (140 * s).sp, lineHeight = (143 * s).sp, letterSpacing = (-0.03f).em),
        displayLarge = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (120 * s).sp, lineHeight = (122 * s).sp, letterSpacing = (-0.03f).em),
        titleLarge   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (88 * s).sp,  lineHeight = (90 * s).sp,  letterSpacing = (-0.03f).em),
        bodyLarge    = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Normal,   fontSize = (36 * s).sp,  lineHeight = (50 * s).sp),
        bodyMedium   = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.Medium,   fontSize = (26 * s).sp,  lineHeight = (30 * s).sp),
        button       = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (32 * s).sp,  lineHeight = (36 * s).sp),
        label        = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (28 * s).sp,  lineHeight = (32 * s).sp),
        keypadDigit  = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (56 * s).sp,  lineHeight = (56 * s).sp),
        codeTile     = TextStyle(fontFamily = RubikFont, fontWeight = FontWeight.SemiBold, fontSize = (84 * s).sp,  lineHeight = (84 * s).sp, letterSpacing = (-0.02f).em),
    )
}

/** Ratio-scales an already-resolved style to a different reference size without needing the raw 1920px scale factor. */
fun TextStyle.atReferenceSize(target: Int, reference: Int = 120): TextStyle =
    copy(fontSize = fontSize * (target.toFloat() / reference), lineHeight = lineHeight * (target.toFloat() / reference))

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun materialTypeBridge(scale: PeachPlumTypography) = Typography(
    displayLarge = scale.displayHero,
    displayMedium = scale.displayLarge,
    headlineLarge = scale.titleLarge,
    bodyLarge = scale.bodyLarge,
    bodyMedium = scale.bodyMedium,
    labelLarge = scale.button,
    labelMedium = scale.label,
)
