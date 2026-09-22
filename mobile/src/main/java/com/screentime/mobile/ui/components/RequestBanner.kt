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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screentime.mobile.R
import com.screentime.mobile.ui.theme.PeachPlum

@Composable
fun RequestBanner(
    title: String,
    context: String,
    actionLabel: String = stringResource(R.string.requests_action_review),
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PeachPlum.colors.accentContainer, PeachPlum.radius.input)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(PeachPlum.colors.ink, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Tv, contentDescription = null, tint = PeachPlum.colors.background, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.ink)
            Text(context, style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
        }
        Row(
            modifier = Modifier
                .background(PeachPlum.colors.surface, PeachPlum.radius.pill)
                .padding(horizontal = 15.dp, vertical = 9.dp),
        ) {
            Text(actionLabel, style = PeachPlum.typography.label, color = PeachPlum.colors.ink)
        }
    }
}
