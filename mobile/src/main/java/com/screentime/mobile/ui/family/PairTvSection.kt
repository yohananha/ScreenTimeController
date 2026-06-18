package com.screentime.mobile.ui.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(state.success) {
        if (state.success) {
            code = ""
            showForm = false
            viewModel.reset()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section Title: TV | 1 of 1 paired (or 0 of 1 paired)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TV", style = Sprout.typography.headline, color = Sprout.colors.ink)
            Text(
                text = "${devices.size} of 1 paired",
                style = Sprout.typography.caption,
                color = Sprout.colors.inkMuted,
            )
        }

        if (devices.isNotEmpty()) {
            val device = devices.first()
            // TV paired dark card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Sprout.radius.card)
                    .background(Sprout.colors.ink, Sprout.radius.card),
            ) {
                // Decorative offset circle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 26.dp, y = (-26).dp)
                        .size(110.dp)
                        .background(Sprout.colors.tvSurface.copy(alpha = 0.45f), CircleShape),
                )

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        // Icon Area
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Sprout.colors.tvSurface, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Tv,
                                contentDescription = null,
                                tint = Sprout.colors.background,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, style = Sprout.typography.headline, color = Sprout.colors.surface)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(Sprout.colors.positiveDisplay, CircleShape),
                                )
                                Text(
                                    "Online",
                                    style = Sprout.typography.caption,
                                    color = Color(0xFF9FE9CE),
                                )
                            }
                        }
                    }

                    // Stat boxes: Today: 0m, Paired: Active
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Sprout.colors.tvSurface, RoundedCornerShape(14.dp))
                                .padding(horizontal = 13.dp, vertical = 11.dp),
                        ) {
                            Text("Today", style = Sprout.typography.caption, color = Sprout.colors.tvMutedText)
                            Text("0m", style = Sprout.typography.bodyStrong, color = Sprout.colors.surface)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Sprout.colors.tvSurface, RoundedCornerShape(14.dp))
                                .padding(horizontal = 13.dp, vertical = 11.dp),
                        ) {
                            Text("Paired", style = Sprout.typography.caption, color = Sprout.colors.tvMutedText)
                            Text("Active", style = Sprout.typography.bodyStrong, color = Sprout.colors.surface)
                        }
                    }

                    if (confirmUnpairDevice != null) {
                        // Confirmation unpair card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4A2230), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF7A3A48), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Unpair this TV? Limits stop applying until you pair it again.",
                                style = Sprout.typography.caption.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD9D4),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Sprout.colors.overDisplay, SproutRadius.pill)
                                        .clickable {
                                            viewModel.unpair(familyId, device.id)
                                            confirmUnpairDevice = null
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Unpair", style = Sprout.typography.label, color = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.Transparent, SproutRadius.pill)
                                        .border(BorderStroke(1.5.dp, Color(0xFF6A5A7E)), SproutRadius.pill)
                                        .clickable { confirmUnpairDevice = null }
                                        .padding(horizontal = 20.dp, vertical = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Cancel", style = Sprout.typography.label, color = Sprout.colors.background)
                                }
                            }
                        }
                    } else {
                        // Rename / Unpair Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Transparent, SproutRadius.pill)
                                    .border(BorderStroke(1.5.dp, Color(0xFF6A5A7E)), SproutRadius.pill)
                                    .clickable { renaming = device }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Rename", style = Sprout.typography.label, color = Sprout.colors.background)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.Transparent, SproutRadius.pill)
                                    .border(BorderStroke(1.5.dp, Color(0xFF6A5A7E)), SproutRadius.pill)
                                    .clickable { confirmUnpairDevice = device }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Unpair TV", style = Sprout.typography.label, color = Color(0xFFFFB7AF))
                            }
                        }
                    }
                }
            }
        } else if (showForm) {
            // Pairing setup state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Sprout.colors.surface, Sprout.radius.card)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Pair with TV", style = Sprout.typography.headline, color = Sprout.colors.ink)
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
            // Unpaired dashed card
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                )
                SproutPrimaryButton(
                    text = "Pair a TV",
                    onClick = { showForm = true },
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
