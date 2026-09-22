package com.screentime.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.screentime.shared.format.bidiWrap
import com.screentime.tv.R
import com.screentime.tv.ui.theme.PeachPlum

/**
 * Full-bleed tv.ground canvas with the two ambient peach/rose glows used on
 * every TV screen (design/i6c-peach-plum README §4 "Shared"), the
 * "ScreenTime" brand chip top-start, and a two-sided footer 64px from the
 * bottom: [footerContext] (step/reset info, left) and a remote-control hint
 * (right — [footerHint] overrides the default for screens with extra keys,
 * e.g. the keypad's delete key).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCanvas(
    footerContext: String? = null,
    footerHint: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachPlum.colors.tvBackground),
    ) {
        // Pinned to physical corners in both languages — screens/TV-Block-HE.html
        // keeps the same left/right as the EN version, unlike the rest of the
        // layout, which mirrors normally. Force LTR so Start/End resolve to
        // physical Left/Right regardless of the active locale.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-300).dp, y = (-420).dp)
                    .size(1200.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(Color(0x29FFB088), Color.Transparent)),
                        shape = CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 320.dp, y = 520.dp)
                    .size(1200.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(Color(0x24F79AC0), Color.Transparent)),
                        shape = CircleShape,
                    ),
            )
        }
        TvBrandChip(modifier = Modifier.align(Alignment.TopStart).padding(start = 96.dp, top = 56.dp))
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 96.dp)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(footerContext?.bidiWrap() ?: "", style = PeachPlum.typography.bodyMedium, color = PeachPlum.colors.tvMutedText)
            TvRemoteHint(text = (footerHint ?: stringResource(R.string.tv_remote_hint)).bidiWrap())
        }
        content()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvBrandChip(modifier: Modifier = Modifier) {
    Text("ScreenTime", modifier = modifier, style = PeachPlum.typography.label, color = PeachPlum.colors.tvMutedText)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvRemoteHint(modifier: Modifier = Modifier, text: String) {
    Text(text, modifier = modifier, style = PeachPlum.typography.bodyMedium, color = PeachPlum.colors.tvMutedText)
}

/**
 * The circular icon badge used on every non-Block overlay/pairing/permission
 * screen (design/i6c-peach-plum README §4 "TV" table — each state gives its
 * own fill/icon colour). [haloRing] adds the subtle white@6% 18px ring used
 * on Approved/Denied/Unlocked/Locked/Permission but not Waiting.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvStatusCircle(
    backgroundColor: Color,
    foregroundColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 200,
    iconSize: Int = 96,
    haloRing: Boolean = false,
) {
    val badge: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(size.dp).background(backgroundColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = foregroundColor, modifier = Modifier.size(iconSize.dp))
        }
    }
    if (haloRing) {
        Box(
            modifier = modifier.size((size + 36).dp).background(Color(0x0FFFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) { badge() }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { badge() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvStepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val filled = i < current
            Box(
                modifier = Modifier
                    .width(if (filled) 44.dp else 14.dp)
                    .height(12.dp)
                    .background(if (filled) PeachPlum.colors.primary else Color(0x2EFFFFFF), RoundedCornerShape(999.dp)),
            )
        }
    }
}
