package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.Sprout

@Composable
fun RequestBanner(
    title: String,
    context: String,
    actionLabel: String = "Review",
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Sprout.colors.accentContainer, Sprout.radius.input)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(Sprout.colors.ink, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Tv, contentDescription = null, tint = Sprout.colors.background, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = Sprout.typography.bodyStrong, color = Sprout.colors.ink)
            Text(context, style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
        }
        Row(
            modifier = Modifier
                .background(Sprout.colors.surface, Sprout.radius.pill)
                .padding(horizontal = 15.dp, vertical = 9.dp),
        ) {
            Text(actionLabel, style = Sprout.typography.label, color = Sprout.colors.ink)
        }
    }
}
