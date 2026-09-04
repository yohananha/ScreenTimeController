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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.shared.R as SharedR
import com.screentime.mobile.ui.theme.LocalFormats
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
                    Text(stringResource(R.string.codes_title), style = Sprout.typography.display, color = Sprout.colors.ink)
                    Text(
                        stringResource(R.string.codes_subtitle),
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
                item { Text(stringResource(it), color = Sprout.colors.overText, style = Sprout.typography.bodyStrong) }
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
                Text(stringResource(R.string.codes_hero_label), style = Sprout.typography.label, color = Sprout.colors.darkMutedText)
                Box(
                    modifier = Modifier
                        .background(Sprout.colors.accent, Sprout.radius.pill)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.codes_hero_single_use), style = Sprout.typography.caption, color = Sprout.colors.ink)
                }
            }
            CodeTilesRow(code = code)
            Text(
                stringResource(
                    R.string.codes_unlocks_for,
                    LocalFormats.current.duration.minutes(LocalContext.current.resources, extraMinutes),
                ),
                style = Sprout.typography.bodyStrong,
                color = Sprout.colors.background,
            )
            Row(
                modifier = Modifier
                    .background(Sprout.colors.darkSurface, Sprout.radius.pill)
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Sprout.colors.primary, modifier = Modifier.size(14.dp))
                Text(
                    if (remaining > 0) stringResource(R.string.codes_expires_in, formatRemaining(remaining)) else stringResource(R.string.codes_expired),
                    style = Sprout.typography.label,
                    color = Sprout.colors.background,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SproutPrimaryButton(text = stringResource(SharedR.string.action_done), onClick = onDismiss, modifier = Modifier.weight(1f))
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
        CodeTilesRow(code = "")
        Text(
            stringResource(R.string.codes_generate_below),
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
    val restOfDayLabel = stringResource(R.string.codes_rest_of_day)
    val minutesLabel = stringResource(R.string.codes_chip_minutes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.codes_what_it_unlocks), style = Sprout.typography.headline, color = Sprout.colors.ink)
        Text(
            stringResource(R.string.codes_how_much_time),
            style = Sprout.typography.label,
            color = Sprout.colors.inkFaint,
        )
        ChipGroup(
            options = listOf(15, 30, 60, -1),
            selected = selectedMinutes,
            onSelect = onSelectMinutes,
            label = { if (it == -1) restOfDayLabel else minutesLabel.format(it) },
        )
        SproutPrimaryButton(
            text = if (isGenerating) stringResource(R.string.codes_generating) else stringResource(R.string.codes_generate_action),
            onClick = { if (!isGenerating) onGenerate() },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun formatRemaining(seconds: Long): String =
    LocalFormats.current.duration.countdown(LocalContext.current.resources, seconds)

private fun secondsUntil(instant: Instant): Long =
    (instant.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
