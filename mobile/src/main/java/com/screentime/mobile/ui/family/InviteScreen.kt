package com.screentime.mobile.ui.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.IconBadge
import com.screentime.shared.model.Family
import com.screentime.shared.model.FamilyRole

@Composable
fun InviteScreen(familyId: String, viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(familyId) { viewModel.observeFamily(familyId) }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        val family = state.family
        val currentUid = state.currentUid
        if (family != null && currentUid != null) {
            item {
                MembersSection(
                    family = family,
                    currentUid = currentUid,
                    onSetRole = { uid, role -> viewModel.setMemberRole(familyId, uid, role) },
                    onRemove = { uid -> viewModel.removeMember(familyId, uid) },
                )
            }
            item { HorizontalDivider() }
        }

        item {
            InviteSection(
                state = state,
                onGenerateInvite = { viewModel.generateInvite(familyId) },
            )
        }

        item { PairTvSection(familyId = familyId) }
    }
}

@Composable
private fun InviteSection(
    state: FamilyUiState,
    onGenerateInvite: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconBadge(Icons.Filled.GroupAdd, size = 56.dp)
        Text("Invite a parent", style = MaterialTheme.typography.titleLarge)
        Text(
            "Generate a 6-digit code and share it. It expires in 48 hours.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        val invite = state.inviteCode
        if (invite != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text(
                    text = invite,
                    modifier = Modifier.padding(24.dp),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                )
            }
        }
        Button(onClick = onGenerateInvite) {
            Text(if (invite == null) "Generate code" else "Generate another")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun MembersSection(
    family: Family,
    currentUid: String,
    onSetRole: (uid: String, role: FamilyRole) -> Unit,
    onRemove: (uid: String) -> Unit,
) {
    val currentIsAdmin = family.isAdmin(currentUid)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Family members", style = MaterialTheme.typography.titleLarge)
        family.members.entries
            .sortedWith(
                compareBy(
                    { if (it.key == currentUid) 0 else 1 },
                    { if (family.isOwner(it.key)) 0 else if (it.value == FamilyRole.ADMIN) 1 else 2 },
                )
            )
            .forEach { (uid, role) ->
                MemberRow(
                    label = if (uid == currentUid) "You" else "Parent",
                    uidSuffix = uid.takeLast(8),
                    role = role,
                    isOwner = family.isOwner(uid),
                    showActions = currentIsAdmin && uid != currentUid && !family.isOwner(uid),
                    onSetRole = { newRole -> onSetRole(uid, newRole) },
                    onRemove = { onRemove(uid) },
                )
            }
    }
}

@Composable
private fun MemberRow(
    label: String,
    uidSuffix: String,
    role: FamilyRole,
    isOwner: Boolean,
    showActions: Boolean,
    onSetRole: (FamilyRole) -> Unit,
    onRemove: () -> Unit,
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove $label?") },
            text = { Text("They will lose access to this family immediately.") },
            confirmButton = {
                TextButton(onClick = { onRemove(); showRemoveDialog = false }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
            },
        )
    }

    val (roleLabel, roleColor) = when {
        isOwner -> "Owner" to MaterialTheme.colorScheme.tertiary
        role == FamilyRole.ADMIN -> "Admin" to MaterialTheme.colorScheme.primary
        else -> "Member" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "···$uidSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = roleColor.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        roleLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = roleColor,
                    )
                }
            }
            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onSetRole(if (role == FamilyRole.USER) FamilyRole.ADMIN else FamilyRole.USER)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (role == FamilyRole.USER) "Make admin" else "Remove admin",
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = { showRemoveDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    ) {
                        Text("Remove", maxLines = 1)
                    }
                }
            }
        }
    }
}
