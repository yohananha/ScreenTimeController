package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.screentime.mobile.R
import com.screentime.mobile.ui.theme.PeachPlum

enum class TvMode { Lock, Limits, Allow }

/**
 * Lock · Limits · Allow tile row (design/i6c-peach-plum tokens.json#sizes.modeTile).
 * `disabled` is the "no TV paired" state — there's nothing to switch modes on,
 * so the tiles go inert rather than silently no-op when tapped.
 */
@Composable
fun ModeTiles(
    active: TvMode,
    onSelect: (TvMode) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
) {
    val icons: Map<TvMode, ImageVector> = mapOf(
        TvMode.Lock to Icons.Filled.Lock,
        TvMode.Limits to Icons.Filled.AccessTime,
        TvMode.Allow to Icons.Filled.WbSunny,
    )
    val labels: Map<TvMode, String> = mapOf(
        TvMode.Lock to stringResource(R.string.today_mode_lock),
        TvMode.Limits to stringResource(R.string.today_mode_limits),
        TvMode.Allow to stringResource(R.string.today_mode_allow),
    )
    val groupLabel = stringResource(R.string.today_mode_group_aria)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TvMode.entries.forEach { mode ->
            val selected = active == mode
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(84.dp)
                    .background(if (selected) PeachPlum.colors.ink else PeachPlum.colors.surface, PeachPlum.radius.card)
                    .border(1.5.dp, if (selected) PeachPlum.colors.ink else PeachPlum.colors.outline, PeachPlum.radius.card)
                    .selectable(selected = selected, enabled = !disabled, role = Role.RadioButton, onClick = { onSelect(mode) })
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                Icon(
                    imageVector = icons.getValue(mode),
                    contentDescription = null,
                    tint = if (selected) PeachPlum.colors.primary else PeachPlum.colors.inkMuted,
                )
                Text(labels.getValue(mode), style = PeachPlum.typography.bodyStrong, color = if (selected) PeachPlum.colors.surface else PeachPlum.colors.ink)
            }
        }
    }
}
