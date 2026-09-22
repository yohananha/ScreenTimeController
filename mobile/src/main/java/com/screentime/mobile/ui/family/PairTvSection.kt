package com.screentime.mobile.ui.family

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.shared.R as SharedR
import com.screentime.mobile.ui.components.CodeSlotInput
import com.screentime.mobile.ui.components.PeachPlumGhostButton
import com.screentime.mobile.ui.components.PeachPlumPrimaryButton
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.mobile.ui.theme.PeachPlumRadius
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
    var confirmUnpairDevice by remember { mutableStateOf<PairedDevice?>(null) }
    var expandedDeviceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.success) {
        if (state.success) {
            code = ""
            showForm = false
            viewModel.reset()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.pairtv_section_title), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink)
            Text(
                text = if (devices.isEmpty()) stringResource(R.string.pairtv_none_paired) else pluralStringResource(R.plurals.pairtv_count_paired, devices.size, devices.size),
                style = PeachPlum.typography.caption,
                color = PeachPlum.colors.inkMuted,
            )
        }

        if (devices.isEmpty() && !showForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val stroke = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                        )
                        drawRoundRect(
                            color = Color(0xFFDDCFC2),
                            style = stroke,
                            cornerRadius = CornerRadius(24.dp.toPx()),
                        )
                    }
                    .padding(26.dp, 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .background(Color(0xFFF2EAE2), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Tv,
                        contentDescription = null,
                        tint = PeachPlum.colors.inkFaint,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Text(stringResource(R.string.pairtv_empty_title), style = PeachPlum.typography.title, color = PeachPlum.colors.ink)
                Text(
                    stringResource(R.string.pairtv_empty_subtitle),
                    style = PeachPlum.typography.body,
                    color = PeachPlum.colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                )
                PeachPlumPrimaryButton(
                    text = stringResource(R.string.pairtv_action_pair),
                    onClick = { showForm = true },
                )
            }
        } else {
            // Device list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PeachPlum.radius.card)
                    .background(PeachPlum.colors.ink, PeachPlum.radius.card),
            ) {
                devices.forEachIndexed { index, device ->
                    val isExpanded = expandedDeviceId == device.id
                    val isConfirmingUnpair = confirmUnpairDevice?.id == device.id

                    if (index > 0) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.07f),
                            thickness = 0.5.dp,
                        )
                    }

                    DeviceListRow(
                        device = device,
                        isExpanded = isExpanded,
                        isConfirmingUnpair = isConfirmingUnpair,
                        onToggle = {
                            expandedDeviceId = if (isExpanded) null else device.id
                            if (isExpanded && isConfirmingUnpair) confirmUnpairDevice = null
                        },
                        onRename = { renaming = device },
                        onRequestUnpair = { confirmUnpairDevice = device },
                        onCancelUnpair = { confirmUnpairDevice = null },
                        onConfirmUnpair = {
                            viewModel.unpair(familyId, device.id)
                            confirmUnpairDevice = null
                            expandedDeviceId = null
                        },
                    )
                }
            }

            if (showForm) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PeachPlum.colors.surface, PeachPlum.radius.card)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(stringResource(R.string.pairtv_action_pair), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink)
                    Text(
                        stringResource(R.string.pairtv_form_subtitle),
                        style = PeachPlum.typography.body,
                        color = PeachPlum.colors.inkMuted,
                    )
                    CodeSlotInput(value = code, onValueChange = { code = it })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PeachPlumPrimaryButton(
                            text = if (state.busy) stringResource(R.string.pairtv_pairing) else stringResource(R.string.pairtv_pair),
                            onClick = { viewModel.claim(code, familyId) },
                            enabled = code.length == 6 && !state.busy,
                            modifier = Modifier.weight(1f),
                        )
                        PeachPlumGhostButton(
                            text = stringResource(SharedR.string.action_cancel),
                            onClick = {
                                showForm = false
                                code = ""
                                viewModel.reset()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    state.message?.let {
                        Text(
                            stringResource(it),
                            style = PeachPlum.typography.caption,
                            color = if (state.success) PeachPlum.colors.positiveText else PeachPlum.colors.overText,
                        )
                    }
                }
            } else {
                PeachPlumGhostButton(
                    text = stringResource(R.string.pairtv_add_another),
                    onClick = { showForm = true },
                    modifier = Modifier.fillMaxWidth(),
                )
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
}

@Composable
private fun DeviceListRow(
    device: PairedDevice,
    isExpanded: Boolean,
    isConfirmingUnpair: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onRequestUnpair: () -> Unit,
    onCancelUnpair: () -> Unit,
    onConfirmUnpair: () -> Unit,
) {
    Column {
        // Collapsed header — always visible, full row is tappable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        if (isExpanded) Color(0xFF7C5CBF).copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Tv,
                    contentDescription = null,
                    tint = if (isExpanded) Color(0xFFB99AEF) else PeachPlum.colors.background,
                    modifier = Modifier.size(15.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = PeachPlum.typography.bodyStrong,
                    color = PeachPlum.colors.surface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(PeachPlum.colors.positiveDisplay, CircleShape),
                    )
                    Text(
                        stringResource(R.string.pairtv_online_status, "0m"),
                        style = PeachPlum.typography.caption,
                        color = Color(0xFF9FE9CE),
                    )
                }
            }

            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.pairtv_collapse) else stringResource(R.string.pairtv_expand),
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp),
            )
        }

        // Expanded detail panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PeachPlum.colors.darkSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatChip(stringResource(R.string.pairtv_stat_today_label), "0m", Modifier.weight(1f))
                    StatChip(stringResource(R.string.pairtv_stat_paired_label), stringResource(R.string.pairtv_stat_paired_value), Modifier.weight(1f))
                }

                if (isConfirmingUnpair) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4A2230), RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color(0xFF7A3A48), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.pairtv_unpair_confirm),
                            style = PeachPlum.typography.caption.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD9D4),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(PeachPlum.colors.overDisplay, PeachPlumRadius.pill)
                                    .clickable { onConfirmUnpair() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(stringResource(R.string.pairtv_action_unpair), style = PeachPlum.typography.label, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .border(BorderStroke(1.dp, Color(0xFF6A5A7E)), PeachPlumRadius.pill)
                                    .clickable { onCancelUnpair() }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(stringResource(SharedR.string.action_cancel), style = PeachPlum.typography.label, color = PeachPlum.colors.background)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(BorderStroke(1.dp, Color(0xFF6A5A7E)), PeachPlumRadius.pill)
                                .clickable { onRename() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.pairtv_action_rename), style = PeachPlum.typography.label, color = PeachPlum.colors.background)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(BorderStroke(1.dp, Color(0xFF6A5A7E)), PeachPlumRadius.pill)
                                .clickable { onRequestUnpair() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.pairtv_action_unpair_tv), style = PeachPlum.typography.label, color = Color(0xFFFFB7AF))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, style = PeachPlum.typography.caption, color = PeachPlum.colors.darkMutedText)
        Text(value, style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.surface)
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
        containerColor = PeachPlum.colors.surface,
        title = { Text(stringResource(R.string.pairtv_rename_title), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = PeachPlum.typography.bodyStrong,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PeachPlum.colors.primary,
                    unfocusedBorderColor = PeachPlum.colors.outline,
                    focusedTextColor = PeachPlum.colors.ink,
                    unfocusedTextColor = PeachPlum.colors.ink,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim().ifBlank { device.name }) }) {
                Text(stringResource(SharedR.string.action_save), style = PeachPlum.typography.label, color = PeachPlum.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(SharedR.string.action_cancel), style = PeachPlum.typography.label, color = PeachPlum.colors.inkMuted)
            }
        },
    )
}
