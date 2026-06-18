package com.screentime.mobile.ui.codes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.mobile.ui.components.ChipGroup
import com.screentime.mobile.ui.components.CodeTilesRow
import com.screentime.mobile.ui.components.HeroCard
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.components.TopHeader
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
fun CodesScreen(viewModel: CodesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedMinutes by remember { mutableStateOf(30) }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { TopHeader(familyName = "Family", parentInitial = "P") }
            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
                    Text("Unlock codes", style = Sprout.typography.display, color = Sprout.colors.ink)
                    Text(
                        "Single-use codes the kid types on the TV.",
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }

            val active = state.active
            if (active != null) {
                item {
                    ActiveCodeHero(
                        code = active.code,
                        extraMinutes = active.extraMinutes,
                        expiresAt = active.expiresAt,
                        onDismiss = viewModel::dismiss,
                    )
                }
            } else {
                item { NoCodeHero() }
                item {
                    SettingsCard(
                        selectedMinutes = selectedMinutes,
                        onSelectMinutes = { selectedMinutes = it },
                        isGenerating = state.isGenerating,
                        onGenerate = { viewModel.generate(selectedMinutes) },
                    )
                }
            }

            state.error?.let {
                item { Text(it, color = Sprout.colors.overText, style = Sprout.typography.bodyStrong) }
            }
        }
    }
}

@Composable
private fun ActiveCodeHero(code: String, extraMinutes: Int, expiresAt: Instant, onDismiss: () -> Unit) {
    var remaining by remember(code) { mutableStateOf(secondsUntil(expiresAt)) }
    LaunchedEffect(code) {
        while (remaining > 0) {
            delay(1_000)
            remaining = secondsUntil(expiresAt)
        }
    }
    HeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("UNLOCK CODE", style = Sprout.typography.label, color = Sprout.colors.tvMutedText)
                Box(
                    modifier = Modifier
                        .background(Sprout.colors.accent, Sprout.radius.pill)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("Single-use", style = Sprout.typography.caption, color = Sprout.colors.ink)
                }
            }
            CodeTilesRow(code = code)
            Text(
                "Unlocks Everything for $extraMinutes min",
                style = Sprout.typography.bodyStrong,
                color = Sprout.colors.background,
            )
            Row(
                modifier = Modifier
                    .background(Sprout.colors.tvSurface, Sprout.radius.pill)
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Sprout.colors.primary, modifier = Modifier.size(14.dp))
                Text(
                    if (remaining > 0) "Expires in ${formatRemaining(remaining)}" else "Expired",
                    style = Sprout.typography.label,
                    color = Sprout.colors.background,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SproutPrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NoCodeHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, SproutRadius.large)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Sprout.colors.surfaceSunken, RoundedCornerShape(18.dp))
                        .padding(vertical = 26.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "–",
                        style = Sprout.typography.display.copy(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
                        color = Sprout.colors.inkFaint,
                    )
                }
            }
        }
        Text(
            "Generate a code below.",
            style = Sprout.typography.body,
            color = Sprout.colors.inkMuted,
        )
    }
}

@Composable
private fun SettingsCard(
    selectedMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("What it unlocks", style = Sprout.typography.headline, color = Sprout.colors.ink)
        Text(
            "HOW MUCH TIME",
            style = Sprout.typography.label,
            color = Sprout.colors.inkFaint,
        )
        ChipGroup(
            options = listOf(15, 30, 60, -1),
            selected = selectedMinutes,
            onSelect = onSelectMinutes,
            label = { if (it == -1) "Rest of day" else "$it min" },
        )
        SproutPrimaryButton(
            text = if (isGenerating) "Generating…" else "Generate code",
            onClick = { if (!isGenerating) onGenerate() },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatRemaining(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun secondsUntil(instant: Instant): Long =
    (instant.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
