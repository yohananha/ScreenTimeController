package com.screentime.mobile.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.AppRow
import com.screentime.mobile.ui.components.SettingsRow
import com.screentime.mobile.ui.limits.EditLimitDialog
import com.screentime.mobile.ui.limits.EditOverallLimitDialog
import com.screentime.mobile.ui.limits.EditTarget
import com.screentime.mobile.ui.limits.LimitsViewModel
import com.screentime.mobile.ui.limits.PickAppDialog
import com.screentime.mobile.ui.limits.formatLimitLabel
import com.screentime.mobile.ui.limits.summarizeSchedule
import com.screentime.mobile.ui.settings.EditLockoutDialog
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.mobile.ui.theme.appAccentFor
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.Limits

@Composable
fun RulesScreen(
    onOpenFamily: () -> Unit,
    onOpenTimeFrame: () -> Unit,
    limitsViewModel: LimitsViewModel = hiltViewModel(),
) {
    val state by limitsViewModel.state.collectAsState()
    val lockout = state.lockout
    val hPad = rememberScreenPadding()

    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var picking by remember { mutableStateOf(false) }
    var editingOverall by remember { mutableStateOf(false) }
    var editingLockout by remember { mutableStateOf(false) }

    val overallValue = if (state.overallDailyMinutes == Limits.UNLIMITED) {
        stringResource(R.string.limits_overall_no_limit)
    } else {
        stringResource(R.string.rules_per_day, formatLimitLabel(state.overallDailyMinutes))
    }
    val lockoutValue = if (lockout.mode == LockoutMode.TIMER) {
        stringResource(R.string.limits_lockout_timer_duration, lockout.durationMinutes)
    } else {
        stringResource(R.string.limits_lockout_parent_unlock)
    }

    Box(modifier = Modifier.fillMaxSize().background(PeachPlum.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.today_nav_rules), style = PeachPlum.typography.display, color = PeachPlum.colors.ink)
                        Text(stringResource(R.string.rules_subtitle), style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
                    }
                    IconButton(
                        onClick = onOpenFamily,
                        modifier = Modifier.size(36.dp).background(PeachPlum.colors.ink, CircleShape),
                    ) {
                        Text("P", style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.background)
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PeachPlum.colors.surface, PeachPlum.radius.card)
                        .padding(horizontal = 16.dp),
                ) {
                    SettingsRow(
                        label = stringResource(R.string.limits_overall_title),
                        value = overallValue,
                        onClick = { editingOverall = true },
                    )
                    SettingsRow(
                        label = stringResource(R.string.limits_allowed_hours_title),
                        value = summarizeSchedule(state.timeFrame),
                        onClick = onOpenTimeFrame,
                        showTopBorder = true,
                    )
                    SettingsRow(
                        label = stringResource(R.string.limits_lockout_title),
                        value = lockoutValue,
                        onClick = { editingLockout = true },
                        showTopBorder = true,
                    )
                    if (lockout.locked && lockout.mode == LockoutMode.PARENT_UNLOCK) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.limits_lockout_locked_notice),
                                style = PeachPlum.typography.caption,
                                color = PeachPlum.colors.overDisplay,
                            )
                            TextButton(onClick = limitsViewModel::unlockNow) {
                                Text(stringResource(R.string.limits_lockout_unlock_now), style = PeachPlum.typography.label, color = PeachPlum.colors.ink)
                            }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.limits_app_section_title), style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.ink)
                    TextButton(onClick = { picking = true }) {
                        Text(stringResource(R.string.rules_add_app_link), style = PeachPlum.typography.label, color = PeachPlum.colors.ink)
                    }
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().background(PeachPlum.colors.surface, PeachPlum.radius.card).padding(horizontal = 16.dp)) {
                    state.limits.forEachIndexed { index, limit ->
                        val displayName = state.availableApps.firstOrNull { it.packageName == limit.packageName }?.label
                            ?: limit.packageName.substringAfterLast('.')
                        AppRow(
                            name = displayName,
                            swatchColor = appAccentFor(limit.packageName),
                            value = if (limit.dailyLimitMinutes == Limits.UNLIMITED) {
                                stringResource(R.string.limits_always_allowed)
                            } else {
                                stringResource(R.string.rules_per_day, formatLimitLabel(limit.dailyLimitMinutes))
                            },
                            onClick = { editing = EditTarget(limit.packageName, displayName, limit.dailyLimitMinutes) },
                            showTopBorder = index > 0,
                        )
                    }
                }
            }
        }
    }

    if (picking) {
        PickAppDialog(
            available = state.availableApps.filterNot { app -> state.limits.any { it.packageName == app.packageName } },
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
                limitsViewModel.setLimit(target.packageName, minutes)
                editing = null
            },
            onRemove = {
                limitsViewModel.removeLimit(target.packageName)
                editing = null
            },
        )
    }

    if (editingOverall) {
        EditOverallLimitDialog(
            currentMinutes = state.overallDailyMinutes,
            onDismiss = { editingOverall = false },
            onSave = { minutes ->
                limitsViewModel.setOverallLimit(minutes)
                editingOverall = false
            },
        )
    }

    if (editingLockout) {
        EditLockoutDialog(
            current = lockout,
            onDismiss = { editingLockout = false },
            onSave = { minutes, mode ->
                limitsViewModel.setLockoutConfig(minutes, mode)
                editingLockout = false
            },
        )
    }
}
