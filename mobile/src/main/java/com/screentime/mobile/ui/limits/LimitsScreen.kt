package com.screentime.mobile.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.screentime.mobile.R
import com.screentime.shared.R as SharedR
import com.screentime.mobile.ui.theme.LocalFormats
import com.screentime.mobile.ui.components.PeachPlumDangerButton
import com.screentime.mobile.ui.components.PeachPlumGhostButton
import com.screentime.mobile.ui.components.PeachPlumPrimaryButton
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import com.screentime.shared.model.TimeFrameSchedule
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * The old Limits tab's screen composable was replaced by RulesScreen +
 * TodayScreen (see design/i6c-peach-plum). What's left here is the
 * edit-dialog machinery Rules still imports and reuses, mirroring how
 * RulesScreen.tsx imports PickAppDialog/EditLimitDialog/EditOverallLimitDialog
 * from LimitsScreen.tsx on web rather than duplicating them.
 */
internal data class EditTarget(val packageName: String, val displayName: String, val defaultMinutes: Int)

@Composable
internal fun PickAppDialog(
    available: List<InstalledApp>,
    tvHasNoApps: Boolean,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PeachPlum.colors.surface,
        title = { Text(stringResource(R.string.limits_pick_app_title), style = PeachPlum.typography.headline) },
        text = {
            if (available.isEmpty()) {
                Text(
                    if (tvHasNoApps) {
                        stringResource(R.string.limits_pick_app_no_apps)
                    } else {
                        stringResource(R.string.limits_pick_app_all_have_limits)
                    },
                )
            } else {
                LazyColumn {
                    items(available, key = { it.packageName }) { app ->
                        TextButton(
                            onClick = { onPick(app) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(app.label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(SharedR.string.action_cancel)) }
        },
    )
}

@Composable
internal fun EditLimitDialog(
    target: EditTarget,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var unlimited by remember(target) { mutableStateOf(target.defaultMinutes == Limits.UNLIMITED) }
    var minutes by remember(target) { mutableStateOf(target.defaultMinutes.coerceAtLeast(0)) }
    var minutesText by remember(target) { mutableStateOf(minutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PeachPlum.colors.surface,
        title = { Text(stringResource(R.string.limits_edit_title), style = PeachPlum.typography.headline) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(target.displayName, style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
                Text(
                    if (unlimited) stringResource(R.string.limits_always_allowed) else formatLimitLabel(minutes),
                    style = PeachPlum.typography.title,
                    color = PeachPlum.colors.ink,
                )
                if (!unlimited) {
                    Slider(
                        value = minutes.toFloat().coerceIn(0f, 240f),
                        onValueChange = {
                            minutes = it.toInt()
                            minutesText = minutes.toString()
                        },
                        valueRange = 0f..240f,
                        steps = 47,
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { text ->
                            val digits = text.filter(Char::isDigit).take(4)
                            minutesText = digits
                            digits.toIntOrNull()?.let { minutes = it }
                        },
                        label = { Text(stringResource(R.string.limits_minutes_field_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeachPlumGhostButton(text = stringResource(R.string.limits_action_block), onClick = {
                        unlimited = false
                        minutes = 0
                        minutesText = "0"
                    })
                    PeachPlumGhostButton(text = stringResource(R.string.limits_action_always_allow), onClick = { unlimited = true })
                }
            }
        },
        confirmButton = {
            PeachPlumPrimaryButton(
                text = stringResource(SharedR.string.action_save),
                onClick = { onSave(if (unlimited) Limits.UNLIMITED else minutes) },
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeachPlumDangerButton(text = stringResource(SharedR.string.action_remove), onClick = onRemove)
                PeachPlumGhostButton(text = stringResource(SharedR.string.action_cancel), onClick = onDismiss)
            }
        },
    )
}

@Composable
internal fun EditOverallLimitDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var unlimited by remember(currentMinutes) { mutableStateOf(currentMinutes == Limits.UNLIMITED) }
    var minutes by remember(currentMinutes) { mutableStateOf(currentMinutes.coerceAtLeast(0)) }
    var minutesText by remember(currentMinutes) { mutableStateOf(minutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PeachPlum.colors.surface,
        title = { Text(stringResource(R.string.limits_overall_title), style = PeachPlum.typography.headline) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.limits_overall_subtitle),
                    style = PeachPlum.typography.caption,
                    color = PeachPlum.colors.inkMuted,
                )
                Text(
                    if (unlimited) stringResource(R.string.limits_overall_no_limit) else formatLimitLabel(minutes),
                    style = PeachPlum.typography.title,
                    color = PeachPlum.colors.ink,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!unlimited) {
                    Slider(
                        value = minutes.toFloat().coerceIn(0f, 480f),
                        onValueChange = {
                            minutes = it.toInt()
                            minutesText = minutes.toString()
                        },
                        valueRange = 0f..480f,
                        steps = 95,
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { text ->
                            val digits = text.filter(Char::isDigit).take(4)
                            minutesText = digits
                            digits.toIntOrNull()?.let { minutes = it }
                        },
                        label = { Text(stringResource(R.string.limits_minutes_field_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeachPlumGhostButton(text = stringResource(R.string.limits_action_block_all), onClick = {
                        unlimited = false
                        minutes = 0
                        minutesText = "0"
                    })
                    PeachPlumGhostButton(text = stringResource(R.string.limits_action_no_limit), onClick = { unlimited = true })
                }
            }
        },
        confirmButton = {
            PeachPlumPrimaryButton(
                text = stringResource(SharedR.string.action_save),
                onClick = { onSave(if (unlimited) Limits.UNLIMITED else minutes) },
            )
        },
        dismissButton = {
            PeachPlumGhostButton(text = stringResource(SharedR.string.action_cancel), onClick = onDismiss)
        },
    )
}

// Delegates to the shared, locale-aware DurationFormat (see
// com.screentime.shared.format) rather than re-implementing duration
// formatting locally. Kept as a @Composable function with its original name
// so the call sites throughout this file didn't need to change.
@Composable
internal fun formatLimitLabel(minutes: Int): String =
    LocalFormats.current.duration.minutes(LocalContext.current.resources, minutes)

@Composable
private fun minutesToAmPm(minute: Int): String =
    LocalFormats.current.clock.timeOfDay(minute)

@Composable
internal fun summarizeSchedule(schedule: TimeFrameSchedule): String {
    if (!schedule.enabled) return stringResource(R.string.limits_schedule_none)
    val sorted = schedule.windowsByDay.entries
        .sortedBy { it.key.value }
        .filter { it.value.isNotEmpty() }
    if (sorted.isEmpty()) return stringResource(R.string.limits_schedule_on_no_windows)
    val clock = LocalFormats.current.clock
    val resources = LocalContext.current.resources
    val first = sorted.first().value.first()
    val allSame = sorted.all { it.value.size == 1 && it.value.first() == first }
    return if (allSame) {
        val windowStr = clock.range(resources, minutesToAmPm(first.startMinute), minutesToAmPm(first.endMinute))
        val firstDay = clock.dayName(sorted.first().key, TextStyle.SHORT)
        val lastDay = clock.dayName(sorted.last().key, TextStyle.SHORT)
        "${clock.range(resources, firstDay, lastDay)}, $windowStr"
    } else {
        val today = LocalDate.now().dayOfWeek
        val highlighted = sorted.firstOrNull { it.key == today }
            ?: sorted.firstOrNull { it.key.value > today.value }
            ?: sorted.first()
        val window = highlighted.value.first()
        val windowStr = clock.range(resources, minutesToAmPm(window.startMinute), minutesToAmPm(window.endMinute))
        val dayShort = clock.dayName(highlighted.key, TextStyle.SHORT)
        val more = sorted.size - 1
        "$dayShort: $windowStr" + if (more > 0) " · ${pluralStringResource(R.plurals.limits_more_days, more, more)}" else ""
    }
}
