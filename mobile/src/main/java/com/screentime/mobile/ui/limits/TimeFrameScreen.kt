package com.screentime.mobile.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.shared.R as SharedR
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.theme.LocalFormats
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.shared.model.TimeFrameSchedule
import com.screentime.shared.model.TimeFrameWindow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFrameScreen(
    onBack: () -> Unit,
    viewModel: TimeFrameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var editingDay by remember { mutableStateOf<DayOfWeek?>(null) }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Sprout.colors.surface, Sprout.radius.pill)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.timeframe_back),
                            tint = Sprout.colors.ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(stringResource(R.string.timeframe_title), style = Sprout.typography.title, color = Sprout.colors.ink)
                        Text(
                            stringResource(R.string.timeframe_subtitle),
                            style = Sprout.typography.caption,
                            color = Sprout.colors.inkMuted,
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Sprout.colors.surface, Sprout.radius.card)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.timeframe_enforce_schedule), style = Sprout.typography.headline, color = Sprout.colors.ink)
                        Switch(
                            checked = state.schedule.enabled,
                            onCheckedChange = viewModel::setEnabled,
                            thumbContent = { Box(Modifier.size(24.dp)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Sprout.colors.surface,
                                checkedTrackColor = Sprout.colors.primary,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = Sprout.colors.surface,
                                uncheckedTrackColor = Color(0xFFC9BCD0),
                                uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                    if (state.schedule.enabled) {
                        Text(
                            stringResource(R.string.timeframe_enforce_hint),
                            style = Sprout.typography.caption,
                            color = Sprout.colors.inkMuted,
                        )
                    }
                }
            }

            if (state.schedule.enabled) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Sprout.colors.surface, Sprout.radius.card),
                    ) {
                        val today = LocalDate.now().dayOfWeek
                        DayOfWeek.entries.forEachIndexed { index, day ->
                            DayRow(
                                day = day,
                                isToday = day == today,
                                windows = state.schedule.windowsByDay[day] ?: emptyList(),
                                onClick = { editingDay = day },
                            )
                            if (index < 6) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(Sprout.colors.outline)
                                        .size(height = 1.dp, width = 0.dp),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SproutGhostButton(
                                text = stringResource(R.string.timeframe_copy_weekdays),
                                onClick = { viewModel.copyToWeekdays(DayOfWeek.MONDAY) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            )
                            SproutGhostButton(
                                text = stringResource(R.string.timeframe_copy_weekend),
                                onClick = { viewModel.copyToWeekend(DayOfWeek.SATURDAY) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        if (state.pendingChanges) {
            SproutPrimaryButton(
                text = if (state.saving) stringResource(R.string.timeframe_saving) else stringResource(R.string.timeframe_save_schedule),
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }
    }

    editingDay?.let { day ->
        DayEditSheet(
            day = day,
            windows = state.schedule.windowsByDay[day] ?: emptyList(),
            onDismiss = { editingDay = null },
            onSave = { windows ->
                viewModel.setWindows(day, windows)
                editingDay = null
            },
        )
    }
}

@Composable
private fun DayRow(
    day: DayOfWeek,
    isToday: Boolean,
    windows: List<TimeFrameWindow>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(day.displayName, style = Sprout.typography.headline, color = Sprout.colors.ink)
            if (isToday) {
                Text(
                    stringResource(R.string.timeframe_today_badge),
                    style = Sprout.typography.caption,
                    color = Sprout.colors.ink,
                    modifier = Modifier
                        .background(Sprout.colors.accentContainer, Sprout.radius.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Text(
            text = windows.windowsSummary(),
            style = Sprout.typography.caption,
            color = when {
                windows.isEmpty() -> Sprout.colors.overText
                windows.isAllDay() -> Sprout.colors.positiveText
                else -> Sprout.colors.inkMuted
            },
            textAlign = TextAlign.End,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayEditSheet(
    day: DayOfWeek,
    windows: List<TimeFrameWindow>,
    onDismiss: () -> Unit,
    onSave: (List<TimeFrameWindow>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var localWindows by remember(windows) { mutableStateOf(windows.toMutableList()) }
    var addingWindow by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Sprout.colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(day.displayName, style = Sprout.typography.title, color = Sprout.colors.ink)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Sprout.colors.background, Sprout.radius.pill)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.timeframe_close),
                        tint = Sprout.colors.inkMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (localWindows.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = Sprout.colors.inkFaint,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        stringResource(R.string.timeframe_blocked_all_day_on, day.displayName),
                        style = Sprout.typography.headline,
                        color = Sprout.colors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.timeframe_add_window_hint),
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                localWindows.forEachIndexed { index, window ->
                    WindowRow(
                        window = window,
                        onDelete = {
                            localWindows = localWindows.toMutableList().also { it.removeAt(index) }
                        },
                    )
                }
            }

            SproutGhostButton(
                text = stringResource(R.string.timeframe_add_window_action),
                onClick = { addingWindow = true },
                modifier = Modifier.fillMaxWidth(),
            )

            SproutPrimaryButton(
                text = stringResource(SharedR.string.action_done),
                onClick = { onSave(localWindows) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (addingWindow) {
        AddWindowDialog(
            onDismiss = { addingWindow = false },
            onAdd = { window ->
                localWindows = localWindows.toMutableList().also { it.add(window) }
                addingWindow = false
            },
        )
    }
}

@Composable
private fun WindowRow(
    window: TimeFrameWindow,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.background, Sprout.radius.input)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.AccessTime,
                contentDescription = null,
                tint = Sprout.colors.accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                LocalFormats.current.clock.range(LocalContext.current.resources, window.startMinute.toTimeLabel(), window.endMinute.toTimeLabel()),
                style = Sprout.typography.bodyStrong,
                color = Sprout.colors.ink,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Sprout.colors.overContainer, Sprout.radius.pill)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(SharedR.string.action_remove),
                tint = Sprout.colors.overText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWindowDialog(
    onDismiss: () -> Unit,
    onAdd: (TimeFrameWindow) -> Unit,
) {
    var pickingEnd by remember { mutableStateOf(false) }
    // Respect the device's 12h/24h setting rather than hardcoding 12h — most
    // Hebrew-locale (and many English-locale) users expect 24-hour.
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val startState = rememberTimePickerState(initialHour = 16, initialMinute = 0, is24Hour = is24Hour)
    val endState = rememberTimePickerState(initialHour = 20, initialMinute = 0, is24Hour = is24Hour)
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sprout.colors.surface,
        title = {
            Text(
                if (pickingEnd) stringResource(R.string.timeframe_end_time) else stringResource(R.string.timeframe_start_time),
                style = Sprout.typography.headline,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimePicker(state = if (pickingEnd) endState else startState)
                errorMessage?.let {
                    Text(it, style = Sprout.typography.caption, color = Sprout.colors.warningText)
                }
            }
        },
        confirmButton = {
            val endAfterStartError = stringResource(R.string.timeframe_end_after_start)
            SproutPrimaryButton(
                text = if (pickingEnd) stringResource(R.string.timeframe_action_add) else stringResource(R.string.timeframe_action_next),
                onClick = {
                    if (!pickingEnd) {
                        pickingEnd = true
                        errorMessage = null
                    } else {
                        val startMin = startState.hour * 60 + startState.minute
                        val endMin = endState.hour * 60 + endState.minute
                        if (endMin <= startMin) {
                            errorMessage = endAfterStartError
                        } else {
                            onAdd(TimeFrameWindow(startMin, endMin))
                        }
                    }
                },
            )
        },
        dismissButton = {
            SproutGhostButton(
                text = if (pickingEnd) stringResource(SharedR.string.action_back) else stringResource(SharedR.string.action_cancel),
                onClick = {
                    if (pickingEnd) { pickingEnd = false; errorMessage = null } else onDismiss()
                },
            )
        },
    )
}

// ── helpers ─────────────────────────────────────────────────────────────────
// displayName/toTimeLabel/windowsSummary now delegate to the shared,
// locale-aware ClockFormat (see com.screentime.shared.format) instead of the
// English-only `name.lowercase().replaceFirstChar { ... }` and hardcoded
// "AM"/"PM" that used to live here. The surrounding sentence strings
// ("Blocked all day", the literal "s" suffix at the DayEditSheet empty
// state, etc.) are a Stage 2 concern (externalizing to string resources).

private val DayOfWeek.displayName: String
    @Composable get() = LocalFormats.current.clock.dayName(this, TextStyle.FULL)

@Composable
private fun Int.toTimeLabel(): String = LocalFormats.current.clock.timeOfDay(this)

@Composable
private fun List<TimeFrameWindow>.windowsSummary(): String {
    val clock = LocalFormats.current.clock
    val resources = LocalContext.current.resources
    return when {
        isEmpty() -> stringResource(R.string.timeframe_blocked_all_day)
        isAllDay() -> stringResource(R.string.timeframe_all_day)
        // joinToString() itself isn't inline, so its transform lambda can't
        // contain @Composable calls (toTimeLabel() is @Composable) — build
        // the list first with the inline map(), which is Compose-call-safe,
        // then join the resulting plain strings.
        else -> map { clock.range(resources, it.startMinute.toTimeLabel(), it.endMinute.toTimeLabel()) }
            .joinToString(" · ")
    }
}

private fun List<TimeFrameWindow>.isAllDay(): Boolean =
    size == 1 && first().startMinute == 0 && first().endMinute >= 1440
