package com.screentime.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.shared.R as SharedR

internal enum class PendingAction { SIGN_OUT, DELETE_ACCOUNT, DELETE_FAMILY }

/**
 * The Account card: Sign out, Delete family (owner only), Delete my account.
 * Every action — including Sign out — goes through a confirm dialog that
 * says what happens and, for deletions, what is lost and for whom.
 */
@Composable
internal fun AccountSection(viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    AccountActions(
        isOwner = isOwner,
        busy = state.busy,
        error = state.error,
        onSignOut = viewModel::signOut,
        onDeleteFamily = viewModel::deleteFamily,
        onDeleteAccount = viewModel::deleteAccount,
    )
}

/**
 * Stateless apart from which confirm dialog is open (rendered directly by
 * AccountButtonsTest). The actions are plain text rows — regular weight, no
 * outlines — like the other settings rows; delete actions are red text so
 * they never read as Sign out.
 */
@Composable
internal fun AccountActions(
    isOwner: Boolean,
    busy: Boolean,
    error: Int?,
    onSignOut: () -> Unit,
    onDeleteFamily: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var pending by remember { mutableStateOf<PendingAction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PeachPlum.colors.surface, PeachPlum.radius.card)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.account_title),
            style = PeachPlum.typography.headline,
            color = PeachPlum.colors.ink,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        ActionRow(stringResource(R.string.account_sign_out), destructive = false) { pending = PendingAction.SIGN_OUT }
        if (isOwner) {
            ActionRow(stringResource(R.string.account_delete_family), destructive = true) { pending = PendingAction.DELETE_FAMILY }
        }
        ActionRow(stringResource(R.string.account_delete_account), destructive = true) { pending = PendingAction.DELETE_ACCOUNT }
    }

    pending?.let { action ->
        val title = when (action) {
            PendingAction.SIGN_OUT -> R.string.account_sign_out_title
            PendingAction.DELETE_FAMILY -> R.string.account_delete_family_title
            PendingAction.DELETE_ACCOUNT -> R.string.account_delete_account_title
        }
        val body = when (action) {
            PendingAction.SIGN_OUT -> R.string.account_sign_out_body
            PendingAction.DELETE_FAMILY -> R.string.account_delete_family_body
            PendingAction.DELETE_ACCOUNT ->
                if (isOwner) R.string.account_delete_account_body_owner else R.string.account_delete_account_body_member
        }
        val confirm = when {
            action == PendingAction.SIGN_OUT -> R.string.account_sign_out
            busy -> R.string.account_deleting
            else -> R.string.account_confirm_delete
        }
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            title = { Text(stringResource(title), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(body), style = PeachPlum.typography.body, color = PeachPlum.colors.inkMuted)
                    error?.let {
                        Text(stringResource(it), style = PeachPlum.typography.caption, color = PeachPlum.colors.overText)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        when (action) {
                            PendingAction.SIGN_OUT -> onSignOut()
                            PendingAction.DELETE_FAMILY -> onDeleteFamily()
                            PendingAction.DELETE_ACCOUNT -> onDeleteAccount()
                        }
                    },
                ) {
                    Text(
                        stringResource(confirm),
                        color = if (action == PendingAction.SIGN_OUT) PeachPlum.colors.ink else PeachPlum.colors.overText,
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pending = null }) {
                    Text(stringResource(SharedR.string.action_cancel))
                }
            },
        )
    }
}

/** A plain, full-width text row with a hairline above it — no outline, regular weight. */
@Composable
private fun ActionRow(label: String, destructive: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(PeachPlum.colors.outline))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                label,
                style = PeachPlum.typography.bodyL.copy(fontWeight = FontWeight.Normal),
                color = if (destructive) PeachPlum.colors.overText else PeachPlum.colors.ink,
            )
        }
    }
}
