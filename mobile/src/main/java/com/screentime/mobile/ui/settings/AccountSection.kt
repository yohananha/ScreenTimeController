package com.screentime.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.PeachPlumGhostButton
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.shared.R as SharedR

private enum class PendingDeletion { ACCOUNT, FAMILY }

/**
 * "Delete my account" (everyone) and "Delete family" (owner only), each
 * behind a confirm dialog that spells out what is lost and for whom.
 */
@Composable
internal fun AccountSection(viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    var pending by remember { mutableStateOf<PendingDeletion?>(null) }

    AccountButtons(
        isOwner = isOwner,
        busy = state.busy,
        onSignOut = viewModel::signOut,
        onDeleteFamily = { pending = PendingDeletion.FAMILY },
        onDeleteAccount = { pending = PendingDeletion.ACCOUNT },
    )

    pending?.let { which ->
        val body = when {
            which == PendingDeletion.FAMILY -> R.string.account_delete_family_body
            isOwner -> R.string.account_delete_account_body_owner
            else -> R.string.account_delete_account_body_member
        }
        AlertDialog(
            onDismissRequest = { if (!state.busy) pending = null },
            title = {
                Text(
                    stringResource(
                        if (which == PendingDeletion.FAMILY) R.string.account_delete_family_title else R.string.account_delete_account_title,
                    ),
                    style = PeachPlum.typography.headline,
                    color = PeachPlum.colors.ink,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(body), style = PeachPlum.typography.body, color = PeachPlum.colors.inkMuted)
                    state.error?.let {
                        Text(stringResource(it), style = PeachPlum.typography.caption, color = PeachPlum.colors.overText)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = { if (which == PendingDeletion.FAMILY) viewModel.deleteFamily() else viewModel.deleteAccount() },
                ) {
                    Text(
                        stringResource(if (state.busy) R.string.account_deleting else R.string.account_confirm_delete),
                        color = PeachPlum.colors.overText,
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !state.busy, onClick = { pending = null }) {
                    Text(stringResource(SharedR.string.action_cancel))
                }
            },
        )
    }
}

/**
 * The Account card's buttons, stateless (rendered by the screenshot test).
 *
 * Full-width and stacked rather than side by side: the Hebrew labels are long
 * enough to wrap or clip in a shared row on a phone, and a co-parent (no
 * "Delete family") would get a lopsided row. Delete actions are red so they
 * never look like Sign out.
 */
@Composable
internal fun AccountButtons(
    isOwner: Boolean,
    busy: Boolean,
    onSignOut: () -> Unit,
    onDeleteFamily: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PeachPlum.colors.surface, PeachPlum.radius.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.account_title), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink)
        PeachPlumGhostButton(
            text = stringResource(R.string.account_sign_out),
            onClick = onSignOut,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isOwner) {
            PeachPlumGhostButton(
                text = stringResource(R.string.account_delete_family),
                onClick = onDeleteFamily,
                destructive = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PeachPlumGhostButton(
            text = stringResource(R.string.account_delete_account),
            onClick = onDeleteAccount,
            destructive = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
