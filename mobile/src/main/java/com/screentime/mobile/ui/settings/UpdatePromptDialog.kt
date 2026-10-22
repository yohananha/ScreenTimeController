package com.screentime.mobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.PeachPlumGhostButton
import com.screentime.mobile.ui.components.PeachPlumPrimaryButton
import com.screentime.mobile.ui.theme.PeachPlum

/**
 * Checks for a newer release once per app launch and, if there is one, asks
 * the user to update. It reappears on every launch until they do.
 * Silent when offline or when the check fails.
 */
@Composable
fun UpdatePromptDialog(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var dismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.state.value is UpdateUiState.Idle) viewModel.checkForUpdate()
    }

    if (dismissed) return
    val current = state
    if (current !is UpdateUiState.Available && current !is UpdateUiState.Downloading && current !is UpdateUiState.ReadyToInstall) return

    AlertDialog(
        onDismissRequest = { dismissed = true },
        containerColor = PeachPlum.colors.surface,
        title = { Text(stringResource(R.string.update_prompt_title), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (current is UpdateUiState.Available) {
                    Text(stringResource(R.string.update_available, current.versionName), style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.ink)
                }
                Text(
                    text = when (current) {
                        is UpdateUiState.Downloading -> stringResource(R.string.update_downloading)
                        is UpdateUiState.ReadyToInstall ->
                            if (viewModel.canRequestInstall()) stringResource(R.string.update_prompt_ready)
                            else stringResource(R.string.update_install_permission_needed)
                        else -> stringResource(R.string.update_prompt_body)
                    },
                    style = PeachPlum.typography.body,
                    color = PeachPlum.colors.inkMuted,
                )
            }
        },
        confirmButton = {
            when (current) {
                is UpdateUiState.Available -> PeachPlumPrimaryButton(
                    text = stringResource(R.string.update_prompt_now),
                    onClick = { viewModel.startDownload(current.downloadUrl, current.versionName) },
                )
                is UpdateUiState.ReadyToInstall -> PeachPlumPrimaryButton(
                    text = stringResource(R.string.update_install_button),
                    onClick = {
                        if (viewModel.canRequestInstall()) {
                            viewModel.installIntent(current.downloadId)?.let { context.startActivity(it) }
                        } else {
                            context.startActivity(viewModel.requestInstallPermissionIntent())
                        }
                    },
                )
                else -> Unit
            }
        },
        dismissButton = {
            PeachPlumGhostButton(text = stringResource(R.string.update_prompt_later), onClick = { dismissed = true })
        },
    )
}
