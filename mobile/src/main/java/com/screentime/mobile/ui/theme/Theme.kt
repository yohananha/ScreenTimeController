package com.screentime.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E4FF),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFF00BFA5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5F6EF),
    onSecondaryContainer = Color(0xFF00504A),
    tertiary = Color(0xFFFF8A65),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0D6),
    onTertiaryContainer = Color(0xFF5C2A1A),
    background = Color(0xFFFAFBFF),
    surface = Color(0xFFFAFBFF),
)

private val Dark = darkColorScheme(
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

/** Slightly rounder than Material defaults for a softer, friendlier feel. */
private val FriendlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ScreenTimeTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) Dark else Light
    MaterialTheme(colorScheme = colors, shapes = FriendlyShapes, content = content)
}
