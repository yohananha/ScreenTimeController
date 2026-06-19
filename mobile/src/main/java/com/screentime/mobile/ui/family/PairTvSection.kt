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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.CodeSlotInput
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius
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
            Text("TVs", style = Sprout.typography.headline, color = Sprout.colors.ink)
            Text(
                text = if (devices.isEmpty()) "None paired" else "${devices.size} paired",
                style = Sprout.typography.caption,
                color = Sprout.colors.inkMuted,
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
                        tint = Sprout.colors.inkFaint,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Text("No TV paired", style = Sprout.typography.title, color = Sprout.colors.ink)
                Text(
                    "Open ScreenTime on your Android TV and enter the 6-digit code it shows.",
                    style = Sprout.typography.body,
                    color = Sprout.colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                )
                SproutPrimaryButton(
                    text = "Pair a TV",
                    onClick = { showForm = true },
                )
            }
        } else {
            // Device list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Sprout.radius.card)
                    .background(Sprout.colors.ink, Sprout.radius.card),
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
                        .background(Sprout.colors.surface, Sprout.radius.card)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Pair a TV", style = Sprout.typography.headline, color = Sprout.colors.ink)
                    Text(
                        "Enter the 6-digit code shown on the TV's pairing screen.",
                        style = Sprout.typography.body,
                        color = Sprout.colors.inkMuted,
                    )
                    CodeSlotInput(value = code, onValueChange = { code = it })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SproutPrimaryButton(
                            text = if (state.busy) "Pairing…" else "Pair",
                            onClick = { viewModel.claim(code, familyId) },
                            enabled = code.length == 6 && !state.busy,
                            modifier = Modifier.weight(1f),
                        )
                        SproutGhostButton(
                            text = "Cancel",
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
                            it,
                            style = Sprout.typography.caption,
                            color = if (state.success) Sprout.colors.positiveText else Sprout.colors.overText,
                        )
                    }
                }
            } else {
                SproutGhostButton(
                    text = "+ Pair another TV",
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
                    tint = if (isExpanded) Color(0xFFB99AEF) else Sprout.colors.background,
                    modifier = Modifier.size(15.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = Sprout.typography.bodyStrong,
                    color = Sprout.colors.surface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Sprout.colors.positiveDisplay, CircleShape),
                    )
                    Text(
                        "Online · 0m today",
                        style = Sprout.typography.caption,
                        color = Color(0xFF9FE9CE),
                    )
                }
            }

            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
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
                    .background(Sprout.colors.darkSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatChip("Today", "0m", Modifier.weight(1f))
                    StatChip("Paired", "Active", Modifier.weight(1f))
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
                            "Unpair this TV? Limits stop applying until you pair it again.",
                            style = Sprout.typography.caption.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD9D4),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Sprout.colors.overDisplay, SproutRadius.pill)
                                    .clickable { onConfirmUnpair() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Unpair", style = Sprout.typography.label, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .border(BorderStroke(1.dp, Color(0xFF6A5A7E)), SproutRadius.pill)
                                    .clickable { onCancelUnpair() }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Cancel", style = Sprout.typography.label, color = Sprout.colors.background)
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
                                .border(BorderStroke(1.dp, Color(0xFF6A5A7E)), SproutRadius.pill)
                                .clickable { onRename() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Rename", style = Sprout.typography.label, color = Sprout.colors.background)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(BorderStroke(1.dp, Color(0xFF6A5A7E)), SproutRadius.pill)
                                .clickable { onRequestUnpair() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Unpair TV", style = Sprout.typography.label, color = Color(0xFFFFB7AF))
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
        Text(label, style = Sprout.typography.caption, color = Sprout.colors.darkMutedText)
        Text(value, style = Sprout.typography.bodyStrong, color = Sprout.colors.surface)
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
        containerColor = Sprout.colors.surface,
        title = { Text("Rename TV", style = Sprout.typography.headline, color = Sprout.colors.ink) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = Sprout.typography.bodyStrong,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Sprout.colors.primary,
                    unfocusedBorderColor = Sprout.colors.outline,
                    focusedTextColor = Sprout.colors.ink,
                    unfocusedTextColor = Sprout.colors.ink,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim().ifBlank { device.name }) }) {
                Text("Save", style = Sprout.typography.label, color = Sprout.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = Sprout.typography.label, color = Sprout.colors.inkMuted)
            }
        },
    )
}
