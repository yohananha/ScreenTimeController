package com.screentime.mobile.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.EmptyState
import com.screentime.mobile.ui.components.IconBadge
import com.screentime.shared.model.AppLimit
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitsScreen(viewModel: LimitsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var picking by remember { mutableStateOf(false) }
    var editingOverall by remember { mutableStateOf(false) }
    var editingLockout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_limits)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { picking = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_app_limit))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OverallLimitCard(
                minutes = state.overallDailyMinutes,
                onClick = { editingOverall = true },
            )
            LockoutCard(
                lockout = state.lockout,
                onClick = { editingLockout = true },
                onUnlockNow = viewModel::unlockNow,
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.limits.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.HourglassEmpty,
                        text = stringResource(R.string.no_limits_set),
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.limits, key = { it.packageName }) { limit ->
                            LimitRow(
                                limit = limit,
                                displayName = state.availableApps
                                    .firstOrNull { it.packageName == limit.packageName }
                                    ?.label ?: limit.packageName,
                                onClick = { editing = EditTarget(limit.packageName, limit.dailyLimitMinutes) },
                            )
                        }
                    }
                }
            }
        }
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
                editing = EditTarget(app.packageName, defaultMinutes = 60)
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

private data class EditTarget(val packageName: String, val defaultMinutes: Int)

@Composable
private fun OverallLimitCard(minutes: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.AccessTime)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Overall daily limit", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Applies across all apps combined",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(formatLimitLabel(minutes), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LockoutCard(
    lockout: LockoutSettings,
    onClick: () -> Unit,
    onUnlockNow: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = if (lockout.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        containerColor = if (lockout.locked) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (lockout.locked) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Code lockout", fontWeight = FontWeight.SemiBold)
                        Text(
                            "After 5 wrong codes in 1 minute",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    when (lockout.mode) {
                        LockoutMode.TIMER -> "${lockout.durationMinutes} min lock"
                        LockoutMode.PARENT_UNLOCK -> "Parent unlock"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (lockout.locked) {
                Text(
                    "TV code entry is currently locked.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (lockout.mode == LockoutMode.PARENT_UNLOCK) {
                    TextButton(onClick = onUnlockNow) { Text("Unlock now") }
                }
            }
        }
    }
}

@Composable
private fun LimitRow(limit: AppLimit, displayName: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = Icons.Filled.Apps,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(displayName, fontWeight = FontWeight.SemiBold)
                    Text(limit.packageName, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(formatLimitLabel(limit.dailyLimitMinutes), style = MaterialTheme.typography.titleMedium)
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
        title = { Text(stringResource(R.string.add_app_limit)) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
        title = { Text(stringResource(R.string.edit_app_limit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(target.packageName, style = MaterialTheme.typography.bodySmall)
                if (unlimited) {
                    Text("Always allowed", style = MaterialTheme.typography.headlineSmall)
                } else {
                    Text(formatLimitLabel(minutes), style = MaterialTheme.typography.headlineSmall)
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
                    TextButton(onClick = {
                        unlimited = false
                        minutes = 0
                        minutesText = "0"
                    }) { Text("Block app") }
                    TextButton(onClick = { unlimited = true }) { Text("Always allow") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(if (unlimited) Limits.UNLIMITED else minutes) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
        title = { Text("Overall daily limit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Once today's total usage across all apps reaches this, " +
                        "the TV blocks no matter which app is open.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (unlimited) {
                    Text("No overall limit", style = MaterialTheme.typography.headlineSmall)
                } else {
                    Text(formatLimitLabel(minutes), style = MaterialTheme.typography.headlineSmall)
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
                    TextButton(onClick = {
                        unlimited = false
                        minutes = 0
                        minutesText = "0"
                    }) { Text("Block everything") }
                    TextButton(onClick = { unlimited = true }) { Text("No overall limit") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(if (unlimited) Limits.UNLIMITED else minutes) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
        title = { Text("Code lockout") },
        text = {
            Column {
                Text(
                    "After 5 incorrect codes within 1 minute, the TV's code " +
                        "entry will:",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == LockoutMode.TIMER,
                        onClick = { mode = LockoutMode.TIMER },
                    )
                    Text("Lock for a set time")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == LockoutMode.PARENT_UNLOCK,
                        onClick = { mode = LockoutMode.PARENT_UNLOCK },
                    )
                    Text("Require a parent to unlock")
                }
                if (mode == LockoutMode.TIMER) {
                    Text(formatDurationLabel(minutes.toInt()), style = MaterialTheme.typography.headlineSmall)
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
            TextButton(onClick = { onSave(minutes.toInt(), mode) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun formatDurationLabel(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0 -> "$mins min"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}min"
    }
}

private fun formatLimitLabel(minutes: Int): String = when {
    minutes == Limits.UNLIMITED -> "Always allowed"
    minutes <= 0 -> "No time today"
    else -> formatDurationLabel(minutes)
}
