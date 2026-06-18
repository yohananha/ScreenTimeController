package com.screentime.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius

@Composable
fun SproutPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SproutRadius.pill,
    shadow: Boolean = false,
    leading: @Composable (RowScope.() -> Unit)? = null,
) {
    val bg = if (enabled) Sprout.colors.primary else Sprout.colors.outline
    val shadowMod = if (shadow && enabled) Modifier.shadow(elevation = 10.dp, shape = shape) else Modifier
    Row(
        modifier = modifier
            .then(shadowMod)
            .background(bg, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) leading()
        Text(
            text = text,
            style = Sprout.typography.label,
            color = if (enabled) Sprout.colors.onPrimary else Sprout.colors.inkFaint,
        )
    }
}

@Composable
fun SproutGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 13.dp),
    leading: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .border(BorderStroke(1.5.dp, Sprout.colors.outline), SproutRadius.pill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) leading()
        Text(
            text = text,
            style = Sprout.typography.label,
            color = Sprout.colors.ink,
        )
    }
}

@Composable
fun SproutDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Sprout.colors.overContainer, SproutRadius.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = Sprout.typography.label,
            color = Sprout.colors.overText,
        )
    }
}
