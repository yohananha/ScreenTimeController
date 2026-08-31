package com.screentime.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.SproutDangerButton
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.family.FamilyViewModel
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius
import com.screentime.shared.R as SharedR
import com.screentime.shared.model.Family
import com.screentime.shared.model.FamilyRole

/**
 * Self-contained: owns its FamilyViewModel and observes it directly, so
 * SettingsScreen only has to call `FamilyMembersSection(familyId)` — moved
 * out of the old InviteScreen.kt (now deleted) with its content unchanged,
 * just relocated under Settings and given its own file.
 */
@Composable
fun FamilyMembersSection(
    familyId: String,
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(familyId) { viewModel.observeFamily(familyId) }

    val family = state.family
    val currentUid = state.currentUid
    if (family != null && currentUid != null) {
        MembersSection(
            family = family,
            currentUid = currentUid,
            onSetRole = { uid, role -> viewModel.setMemberRole(familyId, uid, role) },
            onRemove = { uid -> viewModel.removeMember(familyId, uid) },
            onGenerateInvite = { viewModel.generateInvite(familyId) },
            inviteCode = state.inviteCode,
        )
    }

    state.error?.let { err ->
        Text(
            stringResource(err),
            color = Sprout.colors.overText,
            style = Sprout.typography.caption,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(Sprout.colors.overContainer, SproutRadius.input)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MembersSection(
    family: Family,
    currentUid: String,
    onSetRole: (uid: String, role: FamilyRole) -> Unit,
    onRemove: (uid: String) -> Unit,
    onGenerateInvite: () -> Unit,
    inviteCode: String?,
) {
    var showInvitePanel by remember { mutableStateOf(false) }
    val currentIsAdmin = family.isAdmin(currentUid)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, SproutRadius.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.family_parents_title), style = Sprout.typography.headline, color = Sprout.colors.ink)
            Text(
                pluralStringResource(R.plurals.family_members, family.members.size, family.members.size),
                style = Sprout.typography.caption,
                color = Sprout.colors.inkMuted,
            )
        }

        family.members.entries
            .sortedWith(
                compareBy(
                    { if (it.key == currentUid) 0 else 1 },
                    { if (family.isOwner(it.key)) 0 else if (it.value == FamilyRole.ADMIN) 1 else 2 },
                )
            )
            .forEachIndexed { index, (uid, role) ->
                if (index > 0) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Sprout.colors.outline),
                    )
                }
                MemberRow(
                    initial = if (uid == currentUid) "P" else "C",
                    displayName = if (uid == currentUid) stringResource(R.string.family_you) else stringResource(R.string.family_role_co_parent),
                    isOwner = family.isOwner(uid),
                    isSelf = uid == currentUid,
                    role = role,
                    showActions = currentIsAdmin && uid != currentUid && !family.isOwner(uid),
                    onSetRole = { newRole -> onSetRole(uid, newRole) },
                    onRemove = { onRemove(uid) },
                )
            }

        // Invite row
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Sprout.colors.outline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFDDCFC2),
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable {
                    showInvitePanel = !showInvitePanel
                    if (!showInvitePanel) Unit else onGenerateInvite()
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Sprout.colors.accentContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = Sprout.colors.ink,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.family_invite_title), style = Sprout.typography.headline, color = Sprout.colors.ink)
                Text(
                    stringResource(R.string.family_invite_subtitle),
                    style = Sprout.typography.caption,
                    color = Sprout.colors.inkMuted,
                )
            }
        }

        if (showInvitePanel) {
            InvitePanel(
                inviteCode = inviteCode,
                onRefresh = onGenerateInvite,
            )
        }
    }
}

@Composable
private fun MemberRow(
    initial: String,
    displayName: String,
    isOwner: Boolean,
    isSelf: Boolean,
    role: FamilyRole,
    showActions: Boolean,
    onSetRole: (FamilyRole) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.family_remove_confirm_title), style = Sprout.typography.headline, color = Sprout.colors.ink) },
            text = { Text(stringResource(R.string.family_remove_confirm_body), style = Sprout.typography.body, color = Sprout.colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = { onRemove(); showConfirm = false }) {
                    Text(stringResource(SharedR.string.action_remove), color = Sprout.colors.overText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(SharedR.string.action_cancel)) }
            },
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Sprout.colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, style = Sprout.typography.headline, color = Sprout.colors.surface)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(displayName, style = Sprout.typography.bodyStrong, color = Sprout.colors.ink)
            }

            // Role badge
            val (badgeBg, badgeFg, badgeLabel) = when {
                isOwner && isSelf -> Triple(Sprout.colors.accent, Sprout.colors.ink, stringResource(R.string.family_role_owner_you))
                isOwner -> Triple(Sprout.colors.accent, Sprout.colors.ink, stringResource(R.string.family_role_owner))
                else -> Triple(Sprout.colors.accentContainer, Color(0xFF5B4D69), stringResource(R.string.family_role_co_parent))
            }
            Box(
                modifier = Modifier
                    .background(badgeBg, SproutRadius.pill)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    badgeLabel,
                    style = Sprout.typography.caption.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    ),
                    color = badgeFg,
                )
            }

            if (showActions) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Sprout.colors.surfaceSunken, CircleShape)
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.family_options), tint = Sprout.colors.inkMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (expanded && showActions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SproutDangerButton(
                    text = stringResource(R.string.family_remove_from_family),
                    onClick = { showConfirm = true; expanded = false },
                    modifier = Modifier.weight(1f),
                )
                SproutGhostButton(
                    text = stringResource(SharedR.string.action_cancel),
                    onClick = { expanded = false },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InvitePanel(inviteCode: String?, onRefresh: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.accentContainer, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.family_invite_share_hint),
            style = Sprout.typography.caption,
            color = Sprout.colors.inkMuted,
        )
        if (inviteCode != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Sprout.colors.surface, SproutRadius.pill)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        inviteCode,
                        style = Sprout.typography.bodyStrong,
                        color = Sprout.colors.ink,
                    )
                }
                SproutPrimaryButton(
                    text = if (copied) stringResource(R.string.family_invite_copied) else stringResource(R.string.family_invite_copy),
                    onClick = {
                        clipboard.setText(AnnotatedString(inviteCode))
                        copied = true
                    },
                )
            }
        } else {
            Text(stringResource(R.string.family_invite_generating), style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
        }
        SproutGhostButton(
            text = stringResource(R.string.family_invite_generate_new),
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
