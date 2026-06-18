package com.screentime.tv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.tv.ui.theme.Sprout
import com.screentime.tv.ui.theme.SproutRadius

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "scale")
    val haloWidth by animateDpAsState(if (focused) 3.dp else 0.dp, label = "halo")
    Row(
        modifier = modifier
            .scale(scale)
            .background(Sprout.colors.primary, SproutRadius.pill)
            .border(BorderStroke(haloWidth, Sprout.colors.tvCream), SproutRadius.pill)
            .focusable(interactionSource = interaction)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 23.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = Sprout.typography.button, color = Sprout.colors.onPrimary)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "scale")
    val bg = if (focused) Color(0x29FF6B5E) else Color(0x14FFFFFF)
    val borderColor = if (focused) Sprout.colors.primary else Color(0x3DFFFFFF)
    Row(
        modifier = modifier
            .scale(scale)
            .background(bg, SproutRadius.pill)
            .border(BorderStroke(1.5.dp, borderColor), SproutRadius.pill)
            .focusable(interactionSource = interaction)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 23.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = Sprout.typography.button, color = Sprout.colors.tvCream)
    }
}
