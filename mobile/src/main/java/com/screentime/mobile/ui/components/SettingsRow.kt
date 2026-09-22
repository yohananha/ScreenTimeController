package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.PeachPlum

/** Settings row used on Rules and Family (design/i6c-peach-plum README §3 "Settings row"). */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showTopBorder: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTopBorder) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp, max = 1.dp).background(PeachPlum.colors.outline),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = PeachPlum.typography.bodyL, color = PeachPlum.colors.ink)
                if (value != null) {
                    Text(value, style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
                }
            }
            if (onClick != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = PeachPlum.colors.inkFaint,
                    modifier = Modifier.mirrorInRtl(),
                )
            }
        }
    }
}
