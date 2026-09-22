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

private val PeachPlumMaterialColors = lightColorScheme(
    primary = PeachPlumPalette.primary,
    onPrimary = PeachPlumPalette.onPrimary,
    primaryContainer = PeachPlumPalette.accentContainer,
    onPrimaryContainer = PeachPlumPalette.ink,
    secondary = PeachPlumPalette.accent,
    onSecondary = PeachPlumPalette.ink,
    secondaryContainer = PeachPlumPalette.accentContainer,
    onSecondaryContainer = PeachPlumPalette.ink,
    tertiary = PeachPlumPalette.positiveDisplay,
    onTertiary = PeachPlumPalette.positiveText,
    tertiaryContainer = PeachPlumPalette.positiveContainer,
    onTertiaryContainer = PeachPlumPalette.positiveText,
    background = PeachPlumPalette.background,
    onBackground = PeachPlumPalette.ink,
    surface = PeachPlumPalette.surface,
    onSurface = PeachPlumPalette.ink,
    surfaceVariant = PeachPlumPalette.surfaceSunken,
    onSurfaceVariant = PeachPlumPalette.inkMuted,
    outline = PeachPlumPalette.outline,
    outlineVariant = PeachPlumPalette.outlineStrong,
    error = PeachPlumPalette.overDisplay,
    onError = PeachPlumPalette.surface,
    errorContainer = PeachPlumPalette.overContainer,
    onErrorContainer = PeachPlumPalette.overText,
)

private val PeachPlumShapes = Shapes(
    extraSmall = PeachPlumRadius.icon,
    small = PeachPlumRadius.input,
    medium = PeachPlumRadius.card,
    large = PeachPlumRadius.large,
    extraLarge = PeachPlumRadius.large,
)

@Composable
fun ScreenTimeTheme(content: @Composable () -> Unit) {
    val typeScale = rememberPeachPlumTypeScale()
    // Plain instances, not routed through Hilt: both formatters are stateless
    // and safe to construct directly, and :shared has no Compose dependency
    // to host a CompositionLocal of its own. Non-Compose consumers (e.g. the
    // TV's EnforcementAccessibilityService) still get Hilt-managed
    // @Singleton instances of the same classes — harmless duplication since
    // there's no shared mutable state.
    val appContext = LocalContext.current.applicationContext
    val formats = remember(appContext) { Formats(DurationFormat(), ClockFormat(appContext)) }
    CompositionLocalProvider(
        LocalPeachPlumColors provides PeachPlumPalette,
        LocalPeachPlumTypography provides typeScale,
        LocalFormats provides formats,
    ) {
        MaterialTheme(
            colorScheme = PeachPlumMaterialColors,
            typography = materialTypeBridge(typeScale),
            shapes = PeachPlumShapes,
            content = content,
        )
    }
}

object PeachPlum {
    val colors: PeachPlumColors
        @Composable get() = LocalPeachPlumColors.current
    val typography: PeachPlumTypography
        @Composable get() = LocalPeachPlumTypography.current
    val spacing: PeachPlumSpacing = PeachPlumSpacing
    val radius: PeachPlumRadius = PeachPlumRadius
}
