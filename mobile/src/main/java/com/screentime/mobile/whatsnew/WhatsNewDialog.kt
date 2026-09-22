package com.screentime.mobile.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.PeachPlumPrimaryButton
import com.screentime.mobile.ui.theme.PeachPlum

@Composable
fun WhatsNewDialog(entry: WhatsNewEntry, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PeachPlum.colors.surface,
        title = { Text(stringResource(entry.titleRes), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entry.bulletRes.forEach { bulletRes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = PeachPlum.typography.body, color = PeachPlum.colors.inkMuted)
                        Text(stringResource(bulletRes), style = PeachPlum.typography.body, color = PeachPlum.colors.inkMuted)
                    }
                }
            }
        },
        confirmButton = {
            PeachPlumPrimaryButton(text = stringResource(R.string.whats_new_dismiss), onClick = onDismiss)
        },
    )
}
