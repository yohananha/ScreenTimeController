package com.screentime.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.shared.R as SharedR
import com.screentime.mobile.ui.codes.CodesViewModel
import com.screentime.mobile.ui.theme.LocalFormats
import com.screentime.mobile.ui.theme.PeachPlum
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Unlock bottom sheet — choose duration then show the generated code
 * (design/i6c-peach-plum README §4 "Unlock — choose" / "Unlock — code ready").
 * Wired to the existing CodesViewModel; folds in what was CodesScreen.
 *
 * The design's "Applies to: Everything / {app} only" chips assume per-app
 * scoped codes, but OneTimeCode/createCode only ever unlock everything — there
 * is no scope field to honor a per-app choice. Rather than show a chip that
 * silently does nothing, this only offers "Everything" (see ChooseState).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockSheet(onDismiss: () -> Unit, viewModel: CodesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedMinutes by remember { mutableStateOf(30) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = PeachPlum.colors.background) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val active = state.active
            if (active != null) {
                ReadyState(code = active.code, extraMinutes = active.extraMinutes, expiresAt = active.expiresAt, onNewCode = viewModel::dismiss)
            } else {
                ChooseState(
                    selectedMinutes = selectedMinutes,
                    onSelectMinutes = { selectedMinutes = it },
                    isGenerating = state.isGenerating,
                    onGenerate = { viewModel.generate(selectedMinutes) },
                )
            }
            state.error?.let {
                Text(stringResource(it), style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.overDisplay)
            }
        }
    }
}

@Composable
private fun SheetHeading(subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(stringResource(R.string.today_unlock_aria), style = PeachPlum.typography.title, color = PeachPlum.colors.ink)
        Text(subtitle, style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
    }
}

@Composable
private fun ChooseState(
    selectedMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    val restOfDayLabel = stringResource(R.string.codes_rest_of_day)
    val minutesLabel = stringResource(R.string.codes_chip_minutes)
    SheetHeading(subtitle = stringResource(R.string.unlock_choose_subtitle))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.codes_how_much_time), style = PeachPlum.typography.label, color = PeachPlum.colors.inkFaint)
        ChipGroup(
            options = listOf(15, 30, 60, -1),
            selected = selectedMinutes,
            onSelect = onSelectMinutes,
            label = { if (it == -1) restOfDayLabel else minutesLabel.format(it) },
        )
    }
    PeachPlumPrimaryButton(
        text = if (isGenerating) stringResource(R.string.codes_generating) else stringResource(R.string.codes_generate_action),
        onClick = { if (!isGenerating) onGenerate() },
        enabled = !isGenerating,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReadyState(code: String, extraMinutes: Int, expiresAt: Instant, onNewCode: () -> Unit) {
    var remaining by remember(code) { mutableStateOf(secondsUntil(expiresAt)) }
    var copied by remember(code) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(code) {
        while (remaining > 0) {
            delay(1_000)
            remaining = secondsUntil(expiresAt)
        }
    }

    SheetHeading(subtitle = stringResource(R.string.unlock_ready_subtitle))
    CodeTilesRow(code = code)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.unlock_scope_line, LocalFormats.current.duration.minutes(LocalContext.current.resources, extraMinutes)),
            style = PeachPlum.typography.bodyStrong,
            color = PeachPlum.colors.ink,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = PeachPlum.colors.inkMuted, modifier = Modifier.size(16.dp))
            Text(
                if (remaining > 0) stringResource(R.string.codes_expires_in, formatRemaining(remaining)) else stringResource(R.string.codes_expired),
                style = PeachPlum.typography.caption,
                color = PeachPlum.colors.inkMuted,
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        PeachPlumPrimaryButton(
            text = if (copied) stringResource(R.string.family_invite_copied) else stringResource(R.string.family_invite_copy),
            onClick = {
                clipboard.setText(AnnotatedString(code))
                copied = true
            },
            modifier = Modifier.weight(1f),
        )
        PeachPlumGhostButton(text = stringResource(R.string.unlock_new_code), onClick = onNewCode, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun formatRemaining(seconds: Long): String =
    LocalFormats.current.duration.countdown(LocalContext.current.resources, seconds)

private fun secondsUntil(instant: Instant): Long =
    (instant.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
