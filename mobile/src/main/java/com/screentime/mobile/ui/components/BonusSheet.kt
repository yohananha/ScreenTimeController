package com.screentime.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.screentime.mobile.R
import com.screentime.mobile.ui.theme.PeachPlum

private const val CUSTOM = -1
private const val MAX_CUSTOM_MINUTES = 240

/**
 * "Add bonus time" bottom sheet: pick 15 / 30 / 60 minutes or a custom amount.
 * The grant applies to today only and takes effect on the TV immediately —
 * there is no approval step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusSheet(onDismiss: () -> Unit, onAdd: (minutes: Int) -> Unit) {
    var selected by remember { mutableStateOf(30) }
    var customText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val minutes = if (selected == CUSTOM) customText.toIntOrNull() ?: 0 else selected
    val valid = minutes in 1..MAX_CUSTOM_MINUTES
    val minutesLabel = stringResource(R.string.codes_chip_minutes)
    val customLabel = stringResource(R.string.bonus_custom)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = PeachPlum.colors.background) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.bonus_title), style = PeachPlum.typography.title, color = PeachPlum.colors.ink)
                Text(stringResource(R.string.bonus_subtitle), style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
            }
            ChipGroup(
                options = listOf(15, 30, 60, CUSTOM),
                selected = selected,
                onSelect = { selected = it },
                label = { if (it == CUSTOM) customLabel else minutesLabel.format(it) },
            )
            if (selected == CUSTOM) {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { text -> customText = text.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.bonus_custom_field, MAX_CUSTOM_MINUTES)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PeachPlumPrimaryButton(
                text = if (valid) stringResource(R.string.bonus_add_action, minutes) else stringResource(R.string.bonus_add),
                onClick = {
                    onAdd(minutes)
                    onDismiss()
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
