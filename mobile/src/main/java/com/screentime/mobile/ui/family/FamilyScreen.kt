package com.screentime.mobile.ui.family

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.settings.AboutSection
import com.screentime.mobile.ui.settings.EditLockoutDialog
import com.screentime.mobile.ui.settings.FamilyMembersSection
import com.screentime.mobile.ui.settings.LanguageSection
import com.screentime.mobile.ui.settings.LockoutCard
import com.screentime.mobile.ui.settings.NotificationsSection
import com.screentime.mobile.ui.settings.SettingsViewModel
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.mobile.ui.theme.rememberScreenPadding

@Composable
fun FamilyScreen(
    familyId: String,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val lockout by settingsViewModel.lockout.collectAsState()
    val writeError by settingsViewModel.writeError.collectAsState()
    var editingLockout by remember { mutableStateOf(false) }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(PeachPlum.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column {
                    Text(stringResource(R.string.today_nav_family), style = PeachPlum.typography.display, color = PeachPlum.colors.ink)
                    Text(stringResource(R.string.family_subtitle), style = PeachPlum.typography.caption, color = PeachPlum.colors.inkMuted)
                }
            }
            writeError?.let { err ->
                item {
                    Text(
                        err,
                        color = PeachPlum.colors.overText,
                        style = PeachPlum.typography.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PeachPlum.colors.overContainer, PeachPlum.radius.input)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            item { FamilyMembersSection(familyId = familyId) }
            item { PairTvSection(familyId = familyId) }
            item { LanguageSection() }
            item { NotificationsSection() }
            item {
                LockoutCard(
                    lockout = lockout,
                    onClick = { editingLockout = true },
                    onUnlockNow = settingsViewModel::unlockNow,
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
                settingsViewModel.setLockoutConfig(minutes, mode)
                editingLockout = false
            },
        )
    }
}
