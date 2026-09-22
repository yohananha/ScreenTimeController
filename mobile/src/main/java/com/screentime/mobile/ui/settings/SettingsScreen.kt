package com.screentime.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.PeachPlumGhostButton
import com.screentime.mobile.ui.components.PeachPlumPrimaryButton
import com.screentime.mobile.ui.theme.PeachPlum

/**
 * The old Settings/Family tab's screen composable was replaced by
 * FamilyScreen (see design/i6c-peach-plum), which restacks
 * LanguageSection/NotificationsSection/FamilyMembersSection/PairTvSection/
 * LockoutCard plus this file's AboutSection under the new page header. This
 * file now only holds AboutSection, which FamilyScreen imports directly.
 */
@Composable
internal fun AboutSection(viewModel: UpdateViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    val updateState by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PeachPlum.colors.surface, PeachPlum.radius.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(stringResource(R.string.settings_about_title), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink)
            Text(
                stringResource(R.string.settings_about_version, versionName),
                style = PeachPlum.typography.caption,
                color = PeachPlum.colors.inkMuted,
            )
        }

        when (val state = updateState) {
            is UpdateUiState.Idle ->
                PeachPlumGhostButton(text = stringResource(R.string.update_check_button), onClick = viewModel::checkForUpdate)

            is UpdateUiState.Checking ->
                Text(stringResource(R.string.update_checking), style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)

            is UpdateUiState.UpToDate ->
                Text(stringResource(R.string.update_up_to_date), style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)

            is UpdateUiState.Available -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.update_available, state.versionName),
                    style = PeachPlum.typography.bodyStrong,
                    color = PeachPlum.colors.ink,
                )
                PeachPlumPrimaryButton(
                    text = stringResource(R.string.update_download_button),
                    onClick = { viewModel.startDownload(state.downloadUrl, state.versionName) },
                )
            }

            is UpdateUiState.Downloading ->
                Text(stringResource(R.string.update_downloading), style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)

            is UpdateUiState.ReadyToInstall -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!viewModel.canRequestInstall()) {
                    Text(
                        stringResource(R.string.update_install_permission_needed),
                        style = PeachPlum.typography.caption,
                        color = PeachPlum.colors.inkMuted,
                    )
                }
                PeachPlumPrimaryButton(
                    text = stringResource(R.string.update_install_button),
                    onClick = {
                        if (viewModel.canRequestInstall()) {
                            viewModel.installIntent(state.downloadId)?.let { context.startActivity(it) }
                        } else {
                            context.startActivity(viewModel.requestInstallPermissionIntent())
                        }
                    },
                )
            }

            is UpdateUiState.Failed ->
                Text(
                    stringResource(R.string.update_failed, state.message),
                    style = PeachPlum.typography.caption,
                    color = PeachPlum.colors.overText,
                )
        }
    }
}
