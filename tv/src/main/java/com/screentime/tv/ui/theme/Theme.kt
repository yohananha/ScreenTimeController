package com.screentime.tv.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val TvColors = darkColorScheme(
    primary = SproutPalette.primary,
    onPrimary = SproutPalette.onPrimary,
    primaryContainer = SproutPalette.accentContainer,
    onPrimaryContainer = SproutPalette.ink,
    secondary = SproutPalette.accent,
    onSecondary = SproutPalette.ink,
    secondaryContainer = SproutPalette.tvSurface,
    onSecondaryContainer = SproutPalette.tvCream,
    background = SproutPalette.tvBackground,
    onBackground = SproutPalette.tvCream,
    surface = SproutPalette.tvSurface,
    onSurface = SproutPalette.tvCream,
    error = SproutPalette.overDisplay,
    onError = SproutPalette.tvCream,
    errorContainer = SproutPalette.overContainer,
    onErrorContainer = SproutPalette.overText,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreenTimeTvTheme(content: @Composable () -> Unit) {
    val typeScale = rememberSproutTypeScale()
    CompositionLocalProvider(
        LocalSproutColors provides SproutPalette,
        LocalSproutTypography provides typeScale,
    ) {
        MaterialTheme(colorScheme = TvColors, typography = materialTypeBridge(typeScale)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
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
