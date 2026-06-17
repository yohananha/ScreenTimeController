package com.screentime.tv.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme

/** Matches the mobile app's dark palette so both apps feel like the same product. */
private val TvColors = darkColorScheme(
    primary = Color(0xFF8C9EFF),
    onPrimary = Color(0xFF0B1226),
    primaryContainer = Color(0xFF2A3470),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF64FFDA),
    onSecondary = Color(0xFF00352F),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFF9CF6E8),
    tertiary = Color(0xFFFFB59D),
    onTertiary = Color(0xFF5C2A1A),
    tertiaryContainer = Color(0xFF7A3B26),
    onTertiaryContainer = Color(0xFFFFDBCD),
    background = Color(0xFF10131C),
    surface = Color(0xFF181B25),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreenTimeTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TvColors) {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
