package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.PeachPlum

/** Per-app row on Rules' "App limits" card (design/i6c-peach-plum README §3 "App row"). */
@Composable
fun AppRow(
    name: String,
    swatchColor: Color,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBorder: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTopBorder) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp, max = 1.dp).background(PeachPlum.colors.outline))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(10.dp).height(10.dp).background(swatchColor, RoundedCornerShape(3.dp)))
                Text(name, style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = PeachPlum.typography.body, color = PeachPlum.colors.inkMuted)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PeachPlum.colors.inkFaint, modifier = Modifier.mirrorInRtl())
            }
        }
    }
}
