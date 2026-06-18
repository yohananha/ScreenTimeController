package com.screentime.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius

@Composable
fun TopHeader(
    familyName: String,
    parentInitial: String,
    modifier: Modifier = Modifier,
    onFamilyClick: () -> Unit = {},
    onParentClick: () -> Unit = {},
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Family switcher pill
        Row(
            modifier = Modifier
                .background(Sprout.colors.surface, SproutRadius.pill)
                .border(BorderStroke(1.dp, Sprout.colors.outline), SproutRadius.pill)
                .clickable(onClick = onFamilyClick)
                .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            InitialCircle(
                initial = familyName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                size = 30,
                bg = Sprout.colors.accent,
            )
            Text(
                familyName,
                style = Sprout.typography.label.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = Sprout.colors.ink,
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Sprout.colors.inkMuted, modifier = Modifier.size(16.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (trailing != null) trailing()
            InitialCircle(
                initial = parentInitial,
                size = 38,
                bg = Sprout.colors.primary,
                onClick = onParentClick,
            )
        }
    }
}

@Composable
fun InitialCircle(initial: String, size: Int, bg: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(bg, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, style = Sprout.typography.headline, color = Sprout.colors.ink)
    }
}
