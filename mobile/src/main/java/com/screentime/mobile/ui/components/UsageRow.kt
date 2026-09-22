package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.PeachPlum

/** "By app" row on Today (design/i6c-peach-plum README §3 "Usage row"). */
@Composable
fun UsageRow(
    name: String,
    swatchColor: Color,
    valueLabel: String,
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
    timesUp: Boolean = false,
    showBottomBorder: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PeachPlum.colors.outline))
        Row(
            modifier = Modifier.fillMaxWidth().height(47.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(10.dp).height(10.dp).background(swatchColor, RoundedCornerShape(3.dp)))
                Text(name, style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.ink)
            }
            if (timesUp) {
                Text(valueLabel, style = PeachPlum.typography.caption, color = PeachPlum.colors.overDisplay)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BoxWithConstraints(modifier = Modifier.width(72.dp).height(4.dp).background(PeachPlum.colors.outline, RoundedCornerShape(999.dp))) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(maxWidth * progress.coerceIn(0f, 1f))
                                .background(barColor, RoundedCornerShape(999.dp)),
                        )
                    }
                    Text(valueLabel, style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
                }
            }
        }
        if (showBottomBorder) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PeachPlum.colors.outline))
        }
    }
}
