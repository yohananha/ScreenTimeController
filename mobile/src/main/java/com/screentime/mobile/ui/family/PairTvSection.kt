package com.screentime.mobile.ui.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.EmptyState
import com.screentime.mobile.ui.components.IconBadge
import com.screentime.shared.model.PairedDevice

@Composable
fun PairTvSection(
    familyId: String,
    viewModel: PairTvViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val devices by viewModel.pairedDevices.collectAsState()
    var code by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PairedDevice?>(null) }
    var unpairing by remember { mutableStateOf<PairedDevice?>(null) }

    LaunchedEffect(state.success) {
        if (state.success) {
            code = ""
            showForm = false
            viewModel.reset()
        }
    }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TVs", style = MaterialTheme.typography.titleMedium)
            if (devices.isEmpty() && !showForm) {
                EmptyState(
                    icon = Icons.Filled.Tv,
                    text = "No TVs paired yet — pair one to start managing screen time.",
                    iconSize = 32.dp,
                )
            }
            devices.forEach { device ->
                PairedDeviceRow(
                    device = device,
                    onRename = { renaming = device },
                    onUnpair = { unpairing = device },
                )
            }
            if (showForm) {
                Text(
                    "Enter the 6-digit code shown on the TV's pairing screen.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit TV code") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = code.length == 6 && !state.busy,
                        onClick = { viewModel.claim(code, familyId) },
                    ) { Text(if (state.busy) "Pairing…" else "Pair") }
                    TextButton(onClick = {
                        showForm = false
                        code = ""
                        viewModel.reset()
                    }) { Text("Cancel") }
                }
                state.message?.let {
                    Text(
                        it,
                        color = if (state.success) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                TextButton(onClick = { showForm = true }) { Text("Pair another TV") }
            }
        }
    }

    renaming?.let { device ->
        RenameDeviceDialog(
            device = device,
            onDismiss = { renaming = null },
            onSave = { name ->
                viewModel.rename(device.id, name)
                renaming = null
            },
        )
    }

    unpairing?.let { device ->
        UnpairDeviceDialog(
            device = device,
            onDismiss = { unpairing = null },
            onConfirm = {
                viewModel.unpair(familyId, device.id)
                unpairing = null
            },
        )
    }
}

@Composable
private fun PairedDeviceRow(
    device: PairedDevice,
    onRename: () -> Unit,
    onUnpair: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = Icons.Filled.Tv,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(device.name, modifier = Modifier.padding(start = 12.dp))
        }
        Row {
            IconButton(onClick = onRename) {
                Icon(Icons.Filled.Edit, contentDescription = "Rename")
            }
            IconButton(onClick = onUnpair) {
                Icon(Icons.Filled.LinkOff, contentDescription = "Unpair")
            }
        }
    }
}

@Composable
private fun RenameDeviceDialog(
    device: PairedDevice,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(device) { mutableStateOf(device.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename TV") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim().ifBlank { device.name }) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun UnpairDeviceDialog(
    device: PairedDevice,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unpair ${device.name}?") },
        text = { Text("The TV will need to be paired again to receive limits and enforce screen time.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Unpair") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
