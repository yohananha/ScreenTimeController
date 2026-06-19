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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.mobile.ui.theme.appAccentFor
import com.screentime.mobile.ui.components.AppLimitRow
import com.screentime.mobile.ui.components.DailyTotalHero
import com.screentime.mobile.ui.components.SproutDangerButton
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.components.Status
import com.screentime.mobile.ui.components.TopHeader
import com.screentime.mobile.ui.theme.Sprout
import androidx.compose.foundation.shape.RoundedCornerShape
import com.screentime.shared.model.AppLimit
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.TimeFrameSchedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun LimitsScreen(
    onOpenHistory: () -> Unit = {},
    onOpenTimeFrame: () -> Unit = {},
    viewModel: LimitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val writeError by viewModel.writeError.collectAsState()
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var picking by remember { mutableStateOf(false) }
    var editingOverall by remember { mutableStateOf(false) }
    var editingLockout by remember { mutableStateOf(false) }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TopHeader(
                    familyName = "Family",
                    parentInitial = "P",
                    onFamilyClick = {},
                    onParentClick = {},
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Sprout.colors.surface, Sprout.radius.pill)
                                .clickable { onOpenHistory() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = "History",
                                tint = Sprout.colors.ink,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )
            }
            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
                    Text(
                        "Limits",
                        style = Sprout.typography.display.copy(fontSize = 30.sp),
                        color = Sprout.colors.ink,
                    )
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            writeError?.let { err ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Sprout.colors.overContainer, Sprout.radius.input)
                            .clickable { viewModel.clearWriteError() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            err,
                            color = Sprout.colors.overText,
                            style = Sprout.typography.caption,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Sprout.colors.overText,
                            modifier = Modifier.size(16.dp).padding(start = 8.dp),
                        )
                    }
                }
            }
            item {
                LimitModeCard(
                    mode = when {
                        state.instantLocked -> LimitMode.Lock
                        state.allowAllDayActive -> LimitMode.Allow
                        else -> LimitMode.Default
                    },
                    onSelect = { selected ->
                        when (selected) {
                            LimitMode.Lock -> viewModel.selectInstantLock()
                            LimitMode.Default -> viewModel.selectDefaultLimits()
                            LimitMode.Allow -> viewModel.selectAllowAllDay()
                        }
                    },
                )
            }
            item {
                val totalUsedMs = state.totalUsageMillis
                val overallMs = state.overallDailyMinutes * 60_000L
                val overallProgress = if (overallMs > 0) (totalUsedMs / overallMs.toFloat()).coerceIn(0f, 1f) else 0f
                val overallStatus = when {
                    overallMs > 0 && totalUsedMs >= overallMs        -> Status.TimesUp
                    overallMs > 0 && totalUsedMs >= overallMs * 0.8f -> Status.AlmostUp
                    else                                               -> Status.OnTrack
                }
                val usedMin = (totalUsedMs / 60_000L).toInt()
                val leftMin = (state.overallDailyMinutes - usedMin).coerceAtLeast(0)
                DailyTotalHero(
                    usedLabel = formatLimitLabel(usedMin),
                    ofLabel = "of ${formatLimitLabel(state.overallDailyMinutes)} daily",
                    progress = overallProgress,
                    timeLeft = "${formatLimitLabel(leftMin)} left",
                    resetLabel = "Resets at midnight",
                    status = overallStatus,
                    modifier = Modifier.clickable { editingOverall = true },
                )
            }
            item {
                AllowedHoursRow(
                    schedule = state.timeFrame,
                    onClick = onOpenTimeFrame,
                )
            }
            item {
                LockoutCard(
                    lockout = state.lockout,
                    onClick = { editingLockout = true },
                    onUnlockNow = viewModel::unlockNow,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp, start = 2.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("App limits", style = Sprout.typography.title, color = Sprout.colors.ink)
                    Text(
                        text = "${state.limits.size} ${if (state.limits.size == 1) "app" else "apps"}",
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                    )
                }
            }
            if (state.limits.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Sprout.colors.surface, Sprout.radius.card)
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "No app limits yet",
                            style = Sprout.typography.headline,
                            color = Sprout.colors.ink,
                        )
                        Text(
                            "Tap the + to add one.",
                            style = Sprout.typography.body,
                            color = Sprout.colors.inkMuted,
                        )
                    }
                }
            } else {
                items(state.limits, key = { it.packageName }) { limit ->
                    val displayName = state.availableApps
                        .firstOrNull { it.packageName == limit.packageName }
                        ?.label ?: limit.packageName.substringAfterLast(".")
                    val usedMs = state.usagePerApp[limit.packageName] ?: 0L
                    val limitMs = limit.dailyLimitMinutes * 60_000L
                    val rowProgress = when {
                        limit.dailyLimitMinutes <= 0 || limit.dailyLimitMinutes == Limits.UNLIMITED -> 0f
                        else -> (usedMs / limitMs.toFloat()).coerceIn(0f, 1f)
                    }
                    val rowStatus = when {
                        limit.dailyLimitMinutes == 0               -> Status.TimesUp
                        limit.dailyLimitMinutes == Limits.UNLIMITED -> Status.Paused
                        limitMs > 0 && usedMs >= limitMs           -> Status.TimesUp
                        limitMs > 0 && usedMs >= limitMs * 0.8f    -> Status.AlmostUp
                        else                                        -> Status.OnTrack
                    }
                    val usedMinutes = (usedMs / 60_000L).toInt()
                    AppLimitRow(
                        appName = displayName,
                        initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        accent = appAccentFor(limit.packageName),
                        usedLabel = "${formatLimitLabel(usedMinutes)} / ${formatLimitLabel(limit.dailyLimitMinutes)}",
                        progress = rowProgress,
                        status = rowStatus,
                        paused = limit.dailyLimitMinutes == Limits.UNLIMITED,
                        onClick = { editing = EditTarget(limit.packageName, displayName, limit.dailyLimitMinutes) },
                    )
                }
            }
        }
        // FAB
        SproutPrimaryButton(
            text = "+ Add limit",
            onClick = { picking = true },
            shape = Sprout.radius.large,
            shadow = true,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp),
        )
    }

    if (picking) {
        PickAppDialog(
            available = state.availableApps.filterNot { app ->
                state.limits.any { it.packageName == app.packageName }
            },
            tvHasNoApps = state.availableApps.isEmpty(),
            onDismiss = { picking = false },
            onPick = { app ->
                picking = false
                editing = EditTarget(app.packageName, displayName = app.label, defaultMinutes = 60)
            },
        )
    }

    editing?.let { target ->
        EditLimitDialog(
            target = target,
            onDismiss = { editing = null },
            onSave = { minutes ->
                viewModel.setLimit(target.packageName, minutes)
                editing = null
            },
            onRemove = {
                viewModel.removeLimit(target.packageName)
                editing = null
            },
        )
    }

    if (editingOverall) {
        EditOverallLimitDialog(
            currentMinutes = state.overallDailyMinutes,
            onDismiss = { editingOverall = false },
            onSave = { minutes ->
                viewModel.setOverallLimit(minutes)
                editingOverall = false
            },
        )
    }

    if (editingLockout) {
        EditLockoutDialog(
            current = state.lockout,
            onDismiss = { editingLockout = false },
            onSave = { minutes, mode ->
                viewModel.setLockoutConfig(minutes, mode)
                editingLockout = false
            },
        )
    }
}

private data class EditTarget(val packageName: String, val displayName: String, val defaultMinutes: Int)

@Composable
private fun LockoutCard(
    lockout: LockoutSettings,
    onClick: () -> Unit,
    onUnlockNow: () -> Unit,
) {
    val statusText = when (lockout.mode) {
        LockoutMode.TIMER -> "${lockout.durationMinutes} min lock"
        LockoutMode.PARENT_UNLOCK -> "Parent unlock"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Code lockout", style = Sprout.typography.headline, color = Sprout.colors.ink)
                Text(
                    "After 5 wrong codes in 1 minute",
                    style = Sprout.typography.caption,
                    color = Sprout.colors.inkMuted,
                )
            }
            Text(statusText, style = Sprout.typography.bodyStrong, color = Sprout.colors.ink)
        }
        if (lockout.locked) {
            Text(
                "TV code entry is currently locked.",
                color = Sprout.colors.overText,
                style = Sprout.typography.caption,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (lockout.mode == LockoutMode.PARENT_UNLOCK) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    SproutPrimaryButton(text = "Unlock now", onClick = onUnlockNow)
                }
            }
        }
    }
}

@Composable
private fun PickAppDialog(
    available: List<InstalledApp>,
    tvHasNoApps: Boolean,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sprout.colors.surface,
        title = { Text("Add app limit", style = Sprout.typography.headline) },
        text = {
            if (available.isEmpty()) {
                Text(
                    if (tvHasNoApps) {
                        "No apps found on the TV yet. Make sure the TV is paired and online."
                    } else {
                        "All apps already have limits."
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EditLimitDialog(
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
        containerColor = Sprout.colors.surface,
        title = { Text("Edit limit", style = Sprout.typography.headline) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(target.displayName, style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
                Text(
                    if (unlimited) "Always allowed" else formatLimitLabel(minutes),
                    style = Sprout.typography.title,
                    color = Sprout.colors.ink,
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
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SproutGhostButton(text = "Block", onClick = {
                        unlimited = false
                        minutes = 0
                        minutesText = "0"
                    })
                    SproutGhostButton(text = "Always allow", onClick = { unlimited = true })
                }
            }
        },
        confirmButton = {
            SproutPrimaryButton(
                text = "Save",
                onClick = { onSave(if (unlimited) Limits.UNLIMITED else minutes) },
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SproutDangerButton(text = "Remove", onClick = onRemove)
                SproutGhostButton(text = "Cancel", onClick = onDismiss)
            }
        },
    )
}

@Composable
private fun EditOverallLimitDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var unlimited by remember(currentMinutes) { mutableStateOf(currentMinutes == Limits.UNLIMITED) }
    var minutes by remember(currentMinutes) { mutableStateOf(currentMinutes.coerceAtLeast(0)) }
    var minutesText by remember(currentMinutes) { mutableStateOf(minutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sprout.colors.surface,
        title = { Text("Overall daily limit", style = Sprout.typography.headline) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Once today's total usage across all apps reaches this, the TV blocks no matter which app is open.",
                    style = Sprout.typography.caption,
                    color = Sprout.colors.inkMuted,
                )
                Text(
                    if (unlimited) "No overall limit" else formatLimitLabel(minutes),
                    style = Sprout.typography.title,
                    color = Sprout.colors.ink,
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
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SproutGhostButton(text = "Block all", onClick = {
                        unlimited = false
                        minutes = 0
                        minutesText = "0"
                    })
                    SproutGhostButton(text = "No limit", onClick = { unlimited = true })
                }
            }
        },
        confirmButton = {
            SproutPrimaryButton(
                text = "Save",
                onClick = { onSave(if (unlimited) Limits.UNLIMITED else minutes) },
            )
        },
        dismissButton = {
            SproutGhostButton(text = "Cancel", onClick = onDismiss)
        },
    )
}

@Composable
private fun EditLockoutDialog(
    current: LockoutSettings,
    onDismiss: () -> Unit,
    onSave: (Int, LockoutMode) -> Unit,
) {
    var minutes by remember(current) { mutableStateOf(current.durationMinutes.toFloat()) }
    var mode by remember(current) { mutableStateOf(current.mode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sprout.colors.surface,
        title = { Text("Code lockout", style = Sprout.typography.headline) },
        text = {
            Column {
                Text(
                    "After 5 incorrect codes within 1 minute, the TV's code entry will:",
                    style = Sprout.typography.caption,
                    color = Sprout.colors.inkMuted,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == LockoutMode.TIMER, onClick = { mode = LockoutMode.TIMER })
                    Text("Lock for a set time")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == LockoutMode.PARENT_UNLOCK, onClick = { mode = LockoutMode.PARENT_UNLOCK })
                    Text("Require a parent to unlock")
                }
                if (mode == LockoutMode.TIMER) {
                    Text(
                        formatDurationLabel(minutes.toInt()),
                        style = Sprout.typography.title,
                        color = Sprout.colors.ink,
                    )
                    Slider(
                        value = minutes,
                        onValueChange = { minutes = it },
                        valueRange = 5f..60f,
                        steps = 10,
                    )
                }
            }
        },
        confirmButton = {
            SproutPrimaryButton(text = "Save", onClick = { onSave(minutes.toInt(), mode) })
        },
        dismissButton = {
            SproutGhostButton(text = "Cancel", onClick = onDismiss)
        },
    )
}

private enum class LimitMode { Lock, Default, Allow }

@Composable
private fun LimitModeCard(
    mode: LimitMode,
    onSelect: (LimitMode) -> Unit,
) {
    val (bg, titleColor, iconBg, iconTint, icon, title, caption) = when (mode) {
        LimitMode.Lock -> LimitModeVisuals(
            bg = Sprout.colors.overContainer,
            titleColor = Sprout.colors.overText,
            iconBg = Sprout.colors.overDisplay.copy(alpha = 0.15f),
            iconTint = Sprout.colors.overDisplay,
            icon = Icons.Filled.Lock,
            title = "Instant lock",
            caption = "TV is locked — tap Default to release",
        )
        LimitMode.Default -> LimitModeVisuals(
            bg = Sprout.colors.surface,
            titleColor = Sprout.colors.ink,
            iconBg = Sprout.colors.surfaceSunken,
            iconTint = Sprout.colors.inkMuted,
            icon = Icons.Filled.Check,
            title = "Limits active",
            caption = "Today's schedule and app limits apply",
        )
        LimitMode.Allow -> LimitModeVisuals(
            bg = Sprout.colors.positiveContainer,
            titleColor = Sprout.colors.positiveText,
            iconBg = Sprout.colors.positiveDisplay.copy(alpha = 0.15f),
            iconTint = Sprout.colors.positiveDisplay,
            icon = Icons.Filled.WbSunny,
            title = "Allow all day",
            caption = "All limits paused until midnight",
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, Sprout.radius.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, Sprout.radius.icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = Sprout.typography.headline, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(caption, style = Sprout.typography.caption, color = Sprout.colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        LimitModeSegmented(selected = mode, onSelect = onSelect)
    }
}

private data class LimitModeVisuals(
    val bg: Color,
    val titleColor: Color,
    val iconBg: Color,
    val iconTint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val caption: String,
)

@Composable
private fun LimitModeSegmented(
    selected: LimitMode,
    onSelect: (LimitMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surfaceSunken, Sprout.radius.pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LimitModeSegment(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Lock,
            label = "Lock",
            selected = selected == LimitMode.Lock,
            selectedBg = Sprout.colors.overDisplay,
            selectedFg = Sprout.colors.surface,
            onClick = { onSelect(LimitMode.Lock) },
        )
        LimitModeSegment(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Check,
            label = "Default",
            selected = selected == LimitMode.Default,
            selectedBg = Sprout.colors.surface,
            selectedFg = Sprout.colors.ink,
            onClick = { onSelect(LimitMode.Default) },
        )
        LimitModeSegment(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.WbSunny,
            label = "Allow",
            selected = selected == LimitMode.Allow,
            selectedBg = Sprout.colors.positiveDisplay,
            selectedFg = Sprout.colors.surface,
            onClick = { onSelect(LimitMode.Allow) },
        )
    }
}

@Composable
private fun LimitModeSegment(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    selectedBg: Color,
    selectedFg: Color,
    onClick: () -> Unit,
) {
    val bg = if (selected) selectedBg else Color.Transparent
    val fg = if (selected) selectedFg else Sprout.colors.inkMuted
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(bg, Sprout.radius.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(18.dp))
        Text(
            label,
            style = Sprout.typography.caption,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AllowedHoursRow(schedule: TimeFrameSchedule, onClick: () -> Unit) {
    val subtitle = summarizeSchedule(schedule)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.input)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Sprout.colors.accentContainer, Sprout.radius.icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = Sprout.colors.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text("Allowed hours", style = Sprout.typography.headline, color = Sprout.colors.ink)
                Text(subtitle, style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Sprout.colors.inkMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun formatDurationLabel(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}

private fun formatLimitLabel(minutes: Int): String = when {
    minutes == Limits.UNLIMITED -> "No limit"
    minutes <= 0 -> "0m"
    else -> formatDurationLabel(minutes)
}

private fun minutesToAmPm(minute: Int): String {
    val h = minute / 60
    val m = minute % 60
    val period = if (h < 12) "AM" else "PM"
    val displayHour = when {
        h == 0 -> 12
        h <= 12 -> h
        else -> h - 12
    }
    return if (m == 0) "$displayHour $period" else "$displayHour:${m.toString().padStart(2, '0')} $period"
}

private fun summarizeSchedule(schedule: TimeFrameSchedule): String {
    if (!schedule.enabled) return "No schedule set"
    val sorted = schedule.windowsByDay.entries
        .sortedBy { it.key.value }
        .filter { it.value.isNotEmpty() }
    if (sorted.isEmpty()) return "Schedule on — no windows set"
    val first = sorted.first().value.first()
    val windowStr = "${minutesToAmPm(first.startMinute)}–${minutesToAmPm(first.endMinute)}"
    val allSame = sorted.all { it.value.size == 1 && it.value.first() == first }
    return if (allSame) {
        val days = sorted.map { it.key.name.take(1) + it.key.name.drop(1).lowercase().take(2) }
        "${days.first()}–${days.last()}, $windowStr"
    } else {
        val dayShort = sorted.first().key.name.take(1) + sorted.first().key.name.drop(1).lowercase().take(2)
        val more = sorted.size - 1
        "$dayShort: $windowStr" + if (more > 0) " · $more more ${if (more == 1) "day" else "days"}" else ""
    }
}
