package com.screentime.tv.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import com.screentime.shared.format.ClockFormat
import com.screentime.shared.format.DurationFormat
import kotlin.math.min

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
    // Every TV dimension in this app (dp and sp alike) is a design/i6c-peach-plum
    // reference pixel on a 1920x1080 canvas. A real TV reports ~960x540dp
    // (1080p at xhdpi, or 4K at xxxhdpi), so using those values as plain dp
    // drew everything twice the intended size and pushed layouts off-screen.
    // Redefine the density so 1dp == 1 reference px of the actual window.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val system = LocalDensity.current
        val referenceDensity = referenceDensity(constraints, fallback = system.density)
        CompositionLocalProvider(LocalDensity provides Density(referenceDensity, system.fontScale)) {
            ThemedContent(content)
        }
    }
}

internal const val TV_REFERENCE_WIDTH = 1920f
internal const val TV_REFERENCE_HEIGHT = 1080f

/** Px-per-reference-px that fits the whole 1920x1080 canvas inside [constraints]. */
internal fun referenceDensity(constraints: Constraints, fallback: Float): Float {
    val byWidth = if (constraints.hasBoundedWidth) constraints.maxWidth / TV_REFERENCE_WIDTH else null
    val byHeight = if (constraints.hasBoundedHeight) constraints.maxHeight / TV_REFERENCE_HEIGHT else null
    val scale = when {
        byWidth != null && byHeight != null -> min(byWidth, byHeight)
        else -> byWidth ?: byHeight ?: fallback
    }
    return if (scale > 0f) scale else fallback
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ThemedContent(content: @Composable () -> Unit) {
    val typeScale = PeachPlumTypeScale
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
