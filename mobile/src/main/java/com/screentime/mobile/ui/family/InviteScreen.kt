package com.screentime.mobile.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.SproutDangerButton
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.components.TopHeader
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.shared.model.Family
import com.screentime.shared.model.FamilyRole

@Composable
fun InviteScreen(familyId: String, viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(familyId) { viewModel.observeFamily(familyId) }

    val hPad = rememberScreenPadding()
    Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(start = hPad, end = hPad, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
        ) {
            item { TopHeader(familyName = "Family", parentInitial = "P") }
            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
                    Text(
                        "Family & devices",
                        style = Sprout.typography.display.copy(
                            fontSize = with(Sprout.typography.display) { fontSize * (30f / 34f) }
                        ),
                        color = Sprout.colors.ink,
                    )
                    Text(
                        "Co-parents and your paired TV.",
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }

            val family = state.family
            val currentUid = state.currentUid
            if (family != null && currentUid != null) {
                item {
                    MembersSection(
                        family = family,
                        currentUid = currentUid,
                        onSetRole = { uid, role -> viewModel.setMemberRole(familyId, uid, role) },
                        onRemove = { uid -> viewModel.removeMember(familyId, uid) },
                        onGenerateInvite = { viewModel.generateInvite(familyId) },
                        inviteCode = state.inviteCode,
                    )
                }
            }

            item { PairTvSection(familyId = familyId) }

            state.error?.let { err ->
                item {
                    Text(
                        err,
                        color = Sprout.colors.overText,
                        style = Sprout.typography.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Sprout.colors.overContainer, SproutRadius.input)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
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
            Text("Parents", style = Sprout.typography.headline, color = Sprout.colors.ink)
            Text(
                "${family.members.size} ${if (family.members.size == 1) "member" else "members"}",
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
                    displayName = if (uid == currentUid) "You" else "Co-parent",
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
                Text("Invite a parent", style = Sprout.typography.headline, color = Sprout.colors.ink)
                Text(
                    "They join as a co-parent",
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
            title = { Text("Remove co-parent?", style = Sprout.typography.headline, color = Sprout.colors.ink) },
            text = { Text("They will lose access to this family immediately.", style = Sprout.typography.body, color = Sprout.colors.inkMuted) },
            confirmButton = {
                TextButton(onClick = { onRemove(); showConfirm = false }) {
                    Text("Remove", color = Sprout.colors.overText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
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
                isOwner && isSelf -> Triple(Sprout.colors.accent, Sprout.colors.ink, "Owner · you")
                isOwner -> Triple(Sprout.colors.accent, Sprout.colors.ink, "Owner")
                else -> Triple(Sprout.colors.accentContainer, Color(0xFF5B4D69), "Co-parent")
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
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Sprout.colors.inkMuted, modifier = Modifier.size(18.dp))
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
                    text = "Remove from family",
                    onClick = { showConfirm = true; expanded = false },
                    modifier = Modifier.weight(1f),
                )
                SproutGhostButton(
                    text = "Cancel",
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
            "Share this code with the parent you're inviting. It expires in 24 hours.",
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
                    text = if (copied) "Copied!" else "Copy",
                    onClick = {
                        clipboard.setText(AnnotatedString(inviteCode))
                        copied = true
                    },
                )
            }
        } else {
            Text("Generating code…", style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
        }
        SproutGhostButton(
            text = "Generate a new code",
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
