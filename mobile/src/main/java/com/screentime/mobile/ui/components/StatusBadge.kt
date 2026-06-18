package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius

enum class Status { OnTrack, AlmostUp, TimesUp, Paused }

@Composable
fun StatusBadge(status: Status, modifier: Modifier = Modifier) {
    val (bg, fg, dot, label) = with(Sprout.colors) {
        when (status) {
            Status.OnTrack -> Quad(positiveContainer, positiveText, positiveDisplay, "On track")
            Status.AlmostUp -> Quad(warningContainer, warningText, warningDisplay, "Almost up")
            Status.TimesUp -> Quad(overContainer, overText, overDisplay, "Time's up")
            Status.Paused -> Quad(accentContainer, ink, accent, "Paused")
        }
    }
    Row(
        modifier = modifier
            .background(bg, SproutRadius.pill)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (status != Status.Paused) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(7.dp).background(dot, CircleShape),
            )
        }
        Text(text = label, style = Sprout.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = fg)
    }
}

private data class Quad(val bg: Color, val fg: Color, val dot: Color, val label: String)
