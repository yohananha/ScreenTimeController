package com.screentime.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.tv.ui.theme.PeachPlum
import com.screentime.tv.ui.theme.PeachPlumRadius

/**
 * The double-ring focus halo shared by every focusable TV control (design/i6c-peach-plum
 * tokens.json#motion.tvFocus: "0 0 0 6px tv.ground, 0 0 0 12px peach", scale 1.04).
 * A CSS box-shadow spread can't translate directly to Compose, but the same picture
 * comes from a peach border drawn *outside* a same-colour-as-background gap: the gap
 * is invisible because it matches whatever's already behind the control.
 */
@Composable
private fun Modifier.tvFocusRing(focused: Boolean, shape: androidx.compose.ui.graphics.Shape) = this
    .border(BorderStroke(if (focused) 6.dp else 0.dp, PeachPlum.colors.primary), shape)
    .padding(if (focused) 6.dp else 0.dp)

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
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "scale")
    Row(
        modifier = modifier
            .tvFocusRing(focused, PeachPlumRadius.pill)
            .scale(scale)
            .height(92.dp)
            .background(PeachPlum.colors.tvCream, PeachPlumRadius.pill)
            .focusable(interactionSource = interaction)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 56.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = PeachPlum.typography.button, color = PeachPlum.colors.tvBackground)
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
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "scale")
    Row(
        modifier = modifier
            .tvFocusRing(focused, PeachPlumRadius.pill)
            .scale(scale)
            .height(92.dp)
            .background(PeachPlum.colors.tvSurface, PeachPlumRadius.pill)
            .border(BorderStroke(3.dp, PeachPlum.colors.outline), PeachPlumRadius.pill)
            .focusable(interactionSource = interaction)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = PeachPlum.typography.button, color = PeachPlum.colors.tvCream)
    }
}
