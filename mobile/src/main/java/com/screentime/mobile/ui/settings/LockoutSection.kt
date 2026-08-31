package com.screentime.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.theme.LocalFormats
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.shared.R as SharedR
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings

/**
 * Moved out of LimitsScreen.kt into Settings — the lockout config isn't a
 * per-day limit, it's a device-security setting, so it belongs with the
 * other account-level settings rather than cluttering the Limits home tab.
 */
@Composable
internal fun LockoutCard(
    lockout: LockoutSettings,
    onClick: () -> Unit,
    onUnlockNow: () -> Unit,
) {
    val statusText = when (lockout.mode) {
        LockoutMode.TIMER -> stringResource(R.string.limits_lockout_timer_duration, lockout.durationMinutes)
        LockoutMode.PARENT_UNLOCK -> stringResource(R.string.limits_lockout_parent_unlock)
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
                Text(stringResource(R.string.limits_lockout_title), style = Sprout.typography.headline, color = Sprout.colors.ink)
                Text(
                    stringResource(R.string.limits_lockout_subtitle),
                    style = Sprout.typography.caption,
                    color = Sprout.colors.inkMuted,
                )
            }
            Text(statusText, style = Sprout.typography.bodyStrong, color = Sprout.colors.ink)
        }
        if (lockout.locked) {
            Text(
                stringResource(R.string.limits_lockout_locked_notice),
                color = Sprout.colors.overText,
                style = Sprout.typography.caption,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (lockout.mode == LockoutMode.PARENT_UNLOCK) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    SproutPrimaryButton(text = stringResource(R.string.limits_lockout_unlock_now), onClick = onUnlockNow)
                }
            }
        }
    }
}

@Composable
internal fun EditLockoutDialog(
    current: LockoutSettings,
    onDismiss: () -> Unit,
    onSave: (Int, LockoutMode) -> Unit,
) {
    var minutes by remember(current) { mutableStateOf(current.durationMinutes.toFloat()) }
    var mode by remember(current) { mutableStateOf(current.mode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sprout.colors.surface,
        title = { Text(stringResource(R.string.limits_lockout_title), style = Sprout.typography.headline) },
        text = {
            Column {
                Text(
                    stringResource(R.string.limits_lockout_dialog_subtitle),
                    style = Sprout.typography.caption,
                    color = Sprout.colors.inkMuted,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == LockoutMode.TIMER, onClick = { mode = LockoutMode.TIMER })
                    Text(stringResource(R.string.limits_lockout_mode_timer))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == LockoutMode.PARENT_UNLOCK, onClick = { mode = LockoutMode.PARENT_UNLOCK })
                    Text(stringResource(R.string.limits_lockout_mode_parent))
                }
                if (mode == LockoutMode.TIMER) {
                    Text(
                        LocalFormats.current.duration.minutes(LocalContext.current.resources, minutes.toInt()),
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
            SproutPrimaryButton(text = stringResource(SharedR.string.action_save), onClick = { onSave(minutes.toInt(), mode) })
        },
        dismissButton = {
            SproutGhostButton(text = stringResource(SharedR.string.action_cancel), onClick = onDismiss)
        },
    )
}
