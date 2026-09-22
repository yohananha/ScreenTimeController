package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.PeachPlum

/** Pending time request, inline on Today (design/i6c-peach-plum tokens.json#sizes.requestStrip). */
@Composable
fun RequestStrip(
    title: String,
    subtitle: String,
    approveAria: String,
    denyAria: String,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .background(PeachPlum.colors.ink, PeachPlum.radius.strip)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.background)
            Text(subtitle, style = PeachPlum.typography.caption, color = PeachPlum.colors.background.copy(alpha = 0.78f))
        }
        IconButton(
            onClick = onApprove,
            modifier = Modifier.size(44.dp).background(PeachPlum.colors.background, CircleShape),
        ) {
            Icon(Icons.Filled.Check, contentDescription = approveAria, tint = PeachPlum.colors.ink)
        }
        IconButton(
            onClick = onDeny,
            modifier = Modifier.size(44.dp).border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        ) {
            Icon(Icons.Filled.Close, contentDescription = denyAria, tint = PeachPlum.colors.background)
        }
    }
}
