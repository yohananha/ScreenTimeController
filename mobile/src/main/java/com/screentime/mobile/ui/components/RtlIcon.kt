package com.screentime.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Horizontally flips a directional icon under RTL. `Icons.AutoMirrored.*`
 * doesn't cover every icon — `ChevronRight` has no auto-mirrored variant in
 * this version of material-icons-extended (confirmed by a real compile
 * failure: "Unresolved reference 'ChevronRight'" under
 * androidx.compose.material.icons.automirrored.filled) — so this is the
 * fallback for those.
 */
@Composable
fun Modifier.mirrorInRtl(): Modifier =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) scale(scaleX = -1f, scaleY = 1f) else this
