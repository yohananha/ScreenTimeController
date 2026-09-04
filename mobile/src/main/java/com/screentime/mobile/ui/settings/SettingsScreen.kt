package com.screentime.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.components.TopHeader
import com.screentime.mobile.ui.family.PairTvSection
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.rememberScreenPadding

/**
 * Replaces the old Family tab (InviteScreen, now deleted). Gathers every
 * account-level setting in one place: language, family members + invite,
 * paired TVs, code lockout, and About — see the Hebrew-localization plan for
 * why these were consolidated here instead of staying spread across Limits
 * and a bare "Family" screen.
 */
@Composable
fun SettingsScreen(
    familyId: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val lockout by viewModel.lockout.collectAsState()
    val writeError by viewModel.writeError.collectAsState()
    var editingLockout by remember { mutableStateOf(false) }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TopHeader(familyName = "Family", parentInitial = "P") }
            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
                    Text(stringResource(R.string.settings_title), style = Sprout.typography.display, color = Sprout.colors.ink)
                }
            }

            writeError?.let { err ->
                item {
                    Text(
                        err,
                        color = Sprout.colors.overText,
                        style = Sprout.typography.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Sprout.colors.overContainer, Sprout.radius.input)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            item { LanguageSection() }

            item { NotificationsSection() }

            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                    Text(stringResource(R.string.family_devices_title), style = Sprout.typography.title, color = Sprout.colors.ink)
                    Text(
                        stringResource(R.string.family_devices_subtitle),
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            item { FamilyMembersSection(familyId = familyId) }
            item { PairTvSection(familyId = familyId) }

            item {
                LockoutCard(
                    lockout = lockout,
                    onClick = { editingLockout = true },
                    onUnlockNow = viewModel::unlockNow,
                )
            }

            item { AboutSection() }
        }
    }

    if (editingLockout) {
        EditLockoutDialog(
            current = lockout,
            onDismiss = { editingLockout = false },
            onSave = { minutes, mode ->
                viewModel.setLockoutConfig(minutes, mode)
                editingLockout = false
            },
        )
    }
}

@Composable
private fun AboutSection(viewModel: UpdateViewModel = hiltViewModel()) {
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
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(stringResource(R.string.settings_about_title), style = Sprout.typography.headline, color = Sprout.colors.ink)
            Text(
                stringResource(R.string.settings_about_version, versionName),
                style = Sprout.typography.caption,
                color = Sprout.colors.inkMuted,
            )
        }

        when (val state = updateState) {
            is UpdateUiState.Idle ->
                SproutGhostButton(text = stringResource(R.string.update_check_button), onClick = viewModel::checkForUpdate)

            is UpdateUiState.Checking ->
                Text(stringResource(R.string.update_checking), style = Sprout.typography.caption, color = Sprout.colors.inkMuted)

            is UpdateUiState.UpToDate ->
                Text(stringResource(R.string.update_up_to_date), style = Sprout.typography.caption, color = Sprout.colors.inkMuted)

            is UpdateUiState.Available -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.update_available, state.versionName),
                    style = Sprout.typography.bodyStrong,
                    color = Sprout.colors.ink,
                )
                SproutPrimaryButton(
                    text = stringResource(R.string.update_download_button),
                    onClick = { viewModel.startDownload(state.downloadUrl, state.versionName) },
                )
            }

            is UpdateUiState.Downloading ->
                Text(stringResource(R.string.update_downloading), style = Sprout.typography.caption, color = Sprout.colors.inkMuted)

            is UpdateUiState.ReadyToInstall -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!viewModel.canRequestInstall()) {
                    Text(
                        stringResource(R.string.update_install_permission_needed),
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                    )
                }
                SproutPrimaryButton(
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
                    style = Sprout.typography.caption,
                    color = Sprout.colors.overText,
                )
        }
    }
}
