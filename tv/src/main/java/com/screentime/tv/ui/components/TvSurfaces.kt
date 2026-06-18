package com.screentime.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.screentime.tv.ui.theme.Sprout

/** Full-bleed deep-plum canvas with the two ambient radial gradients used on every TV screen. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCanvas(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Sprout.colors.tvBackground),
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF4A356A), Color(0x004A356A)),
                    ),
                    shape = CircleShape,
                )
                .align(Alignment.TopEnd),
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF3E2C57), Color(0x003E2C57)),
                    ),
                    shape = CircleShape,
                )
                .align(Alignment.BottomStart),
        )
        TvBrandChip(modifier = Modifier.align(Alignment.TopStart).padding(top = 24.dp, start = 32.dp))
        TvRemoteHint(modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp))
        content()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvBrandChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(19.dp).background(Sprout.colors.primary, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(7.5.dp).background(Sprout.colors.ink, CircleShape))
        }
        Text("ScreenTime", style = Sprout.typography.titleLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified), color = Color(0xFFEFE7F3))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvRemoteHint(modifier: Modifier = Modifier, text: String = "◀ ▶ ▲ ▼  +  OK") {
    Text(
        text,
        modifier = modifier,
        style = Sprout.typography.bodyMedium,
        color = Color(0xFF9785AC),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvStatusCircle(
    kind: StatusKind,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    size: Int = 105,
    backgroundColor: Color? = null,
    foregroundColor: Color? = null,
) {
    val (defaultBg, defaultFg) = when (kind) {
        StatusKind.Mint -> Sprout.colors.positiveDisplay to Color(0xFF0F1F18)
        StatusKind.Lilac -> Sprout.colors.accentContainer to Color(0xFF5B4D8C)
        StatusKind.Amber -> Color(0xFFF2C879) to Color(0xFF5A3E12)
    }
    val bg = backgroundColor ?: defaultBg
    val fg = foregroundColor ?: defaultFg
    val resolvedIcon = icon ?: when (kind) {
        StatusKind.Mint -> Icons.Filled.Check
        StatusKind.Lilac -> Icons.Filled.SentimentDissatisfied
        StatusKind.Amber -> Icons.Filled.WbIncandescent
    }
    if (kind == StatusKind.Mint) {
        Box(
            modifier = modifier
                .size(size.dp)
                .background(bg.copy(alpha = 0.18f), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(resolvedIcon, contentDescription = null, tint = fg, modifier = Modifier.size((size * 0.5).dp))
            }
        }
    } else {
        Box(
            modifier = modifier.size(size.dp).background(bg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(resolvedIcon, contentDescription = null, tint = fg, modifier = Modifier.size((size * 0.5).dp))
        }
    }
}

enum class StatusKind { Mint, Lilac, Amber }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvStepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val filled = i < current
            Box(
                modifier = Modifier
                    .width(if (filled) 22.dp else 7.dp)
                    .height(6.dp)
                    .background(if (filled) Sprout.colors.primary else Color(0xFF5A4A72), RoundedCornerShape(99.dp)),
            )
        }
    }
}
