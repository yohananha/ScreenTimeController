package com.screentime.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.Sprout

@Composable
fun AppLimitRow(
    appName: String,
    initial: String,
    accent: Color,
    usedLabel: String,
    progress: Float,
    status: Status,
    paused: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val statusColor = when (status) {
        Status.OnTrack -> Sprout.colors.positiveDisplay
        Status.AlmostUp -> Sprout.colors.warningDisplay
        Status.TimesUp -> Sprout.colors.overDisplay
        Status.Paused -> Sprout.colors.accent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.input)
            .border(BorderStroke(1.dp, Sprout.colors.outline), Sprout.radius.input)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp)
            .alpha(if (paused) 0.78f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(accent, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, style = Sprout.typography.headline, color = Color.White)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(appName, style = Sprout.typography.headline, color = Sprout.colors.ink)
                if (!paused) {
                    Text(usedLabel, style = Sprout.typography.bodyStrong, color = Sprout.colors.ink)
                }
            }
            if (!paused) {
                ProgressBar(progress = progress, fill = statusColor, track = Sprout.colors.outline, height = 8)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(status = if (paused) Status.Paused else status)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Sprout.colors.inkFaint, modifier = Modifier.size(20.dp))
            }
        }
    }
}
