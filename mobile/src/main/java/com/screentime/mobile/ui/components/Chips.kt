package com.screentime.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.Sprout

/** Selectable chip row used in Requests amount chooser and Codes settings. */
@Composable
fun <T> ChipGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val isSel = opt == selected
            val bg = if (isSel) Sprout.colors.ink else Sprout.colors.surface
            val fg = if (isSel) Sprout.colors.background else Sprout.colors.ink
            val border = if (isSel) Sprout.colors.ink else Sprout.colors.outline
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(bg, Sprout.radius.input)
                    .border(BorderStroke(1.dp, border), Sprout.radius.input)
                    .clickable { onSelect(opt) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label(opt), style = Sprout.typography.label, color = fg)
            }
        }
    }
}
