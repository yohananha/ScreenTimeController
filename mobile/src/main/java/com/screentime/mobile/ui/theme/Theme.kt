package com.screentime.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.screentime.shared.format.ClockFormat
import com.screentime.shared.format.DurationFormat

private val SproutMaterialColors = lightColorScheme(
    primary = SproutPalette.primary,
    onPrimary = SproutPalette.onPrimary,
    primaryContainer = SproutPalette.accentContainer,
    onPrimaryContainer = SproutPalette.ink,
    secondary = SproutPalette.accent,
    onSecondary = SproutPalette.ink,
    secondaryContainer = SproutPalette.accentContainer,
    onSecondaryContainer = SproutPalette.ink,
    tertiary = SproutPalette.positiveDisplay,
    onTertiary = SproutPalette.positiveText,
    tertiaryContainer = SproutPalette.positiveContainer,
    onTertiaryContainer = SproutPalette.positiveText,
    background = SproutPalette.background,
    onBackground = SproutPalette.ink,
    surface = SproutPalette.surface,
    onSurface = SproutPalette.ink,
    surfaceVariant = SproutPalette.surfaceSunken,
    onSurfaceVariant = SproutPalette.inkMuted,
    outline = SproutPalette.outline,
    outlineVariant = SproutPalette.outlineStrong,
    error = SproutPalette.overDisplay,
    onError = SproutPalette.surface,
    errorContainer = SproutPalette.overContainer,
    onErrorContainer = SproutPalette.overText,
)

private val SproutShapes = Shapes(
    extraSmall = SproutRadius.icon,
    small = SproutRadius.input,
    medium = SproutRadius.card,
    large = SproutRadius.large,
    extraLarge = SproutRadius.large,
)

@Composable
fun ScreenTimeTheme(content: @Composable () -> Unit) {
    val typeScale = rememberSproutTypeScale()
    // Plain instances, not routed through Hilt: both formatters are stateless
    // and safe to construct directly, and :shared has no Compose dependency
    // to host a CompositionLocal of its own. Non-Compose consumers (e.g. the
    // TV's EnforcementAccessibilityService) still get Hilt-managed
    // @Singleton instances of the same classes — harmless duplication since
    // there's no shared mutable state.
    val appContext = LocalContext.current.applicationContext
    val formats = remember(appContext) { Formats(DurationFormat(), ClockFormat(appContext)) }
    CompositionLocalProvider(
        LocalSproutColors provides SproutPalette,
        LocalSproutTypography provides typeScale,
        LocalFormats provides formats,
    ) {
        MaterialTheme(
            colorScheme = SproutMaterialColors,
            typography = materialTypeBridge(typeScale),
            shapes = SproutShapes,
            content = content,
        )
    }
}

object Sprout {
    val colors: SproutColors
        @Composable get() = LocalSproutColors.current
    val typography: SproutTypography
        @Composable get() = LocalSproutTypography.current
    val spacing: SproutSpacing = SproutSpacing
    val radius: SproutRadius = SproutRadius
}
