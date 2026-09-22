package com.screentime.tv.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import com.screentime.shared.format.ClockFormat
import com.screentime.shared.format.DurationFormat

@OptIn(ExperimentalTvMaterial3Api::class)
private val TvColors = darkColorScheme(
    primary = PeachPlumPalette.primary,
    onPrimary = PeachPlumPalette.onPrimary,
    primaryContainer = PeachPlumPalette.accentContainer,
    onPrimaryContainer = PeachPlumPalette.ink,
    secondary = PeachPlumPalette.accent,
    onSecondary = PeachPlumPalette.ink,
    secondaryContainer = PeachPlumPalette.tvSurface,
    onSecondaryContainer = PeachPlumPalette.tvCream,
    background = PeachPlumPalette.tvBackground,
    onBackground = PeachPlumPalette.tvCream,
    surface = PeachPlumPalette.tvSurface,
    onSurface = PeachPlumPalette.tvCream,
    error = PeachPlumPalette.overDisplay,
    onError = PeachPlumPalette.tvCream,
    errorContainer = PeachPlumPalette.overContainer,
    onErrorContainer = PeachPlumPalette.overText,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreenTimeTvTheme(content: @Composable () -> Unit) {
    val typeScale = rememberPeachPlumTypeScale()
    // Built from whatever LocalContext is in scope here — for the block
    // overlay that's the locale-wrapped Context TvLocaleController.wrap()
    // produced, so ClockFormat's is24HourFormat() check and locale defaults
    // line up with the rest of the overlay.
    val context = LocalContext.current
    val formats = remember(context) { Formats(DurationFormat(), ClockFormat(context)) }
    CompositionLocalProvider(
        LocalPeachPlumColors provides PeachPlumPalette,
        LocalPeachPlumTypography provides typeScale,
        LocalFormats provides formats,
    ) {
        MaterialTheme(colorScheme = TvColors, typography = materialTypeBridge(typeScale)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
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
