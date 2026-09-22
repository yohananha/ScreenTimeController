package com.screentime.mobile.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.CodeSlotInput
import com.screentime.mobile.ui.components.PeachPlumPrimaryButton
import com.screentime.mobile.ui.components.mirrorInRtl
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.mobile.ui.theme.rememberScreenPadding

@Composable
fun FamilyOnboardingScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var joinExpanded by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(PeachPlum.colors.background), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = hPad)
                .padding(top = 12.dp),
        ) {
        // Brand chip + parent avatar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(PeachPlum.colors.primary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(12.dp).background(PeachPlum.colors.ink, CircleShape))
                }
                Text("ScreenTime", style = PeachPlum.typography.headline, color = PeachPlum.colors.ink)
            }
            Box(
                modifier = Modifier.size(38.dp).background(PeachPlum.colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("P", style = PeachPlum.typography.label, color = PeachPlum.colors.ink)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.family_onboarding_title), style = PeachPlum.typography.display, color = PeachPlum.colors.ink)
        Text(
            stringResource(R.string.family_onboarding_subtitle),
            style = PeachPlum.typography.bodyStrong,
            color = PeachPlum.colors.inkMuted,
            modifier = Modifier.padding(top = 7.dp),
        )

        Spacer(Modifier.height(20.dp))

        // Create card
        Card(
            iconBg = PeachPlum.colors.accentContainer,
            iconContent = {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color5B4D8C, modifier = Modifier.size(24.dp))
            },
            title = stringResource(R.string.family_onboarding_create_title),
            subtitle = stringResource(R.string.family_onboarding_create_subtitle),
            onClick = { viewModel.createFamily() },
        )

        Spacer(Modifier.height(14.dp))

        // Join card (expandable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PeachPlum.colors.surface, PeachPlum.radius.card)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    joinExpanded = !joinExpanded
                    if (!joinExpanded) code = ""
                },
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(52.dp).background(PeachPlum.colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = PeachPlum.colors.ink, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.family_onboarding_join_title), style = PeachPlum.typography.headline, color = PeachPlum.colors.ink)
                    Text(
                        stringResource(R.string.family_onboarding_join_subtitle),
                        style = PeachPlum.typography.body,
                        color = PeachPlum.colors.inkMuted,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = PeachPlum.colors.primary,
                    modifier = Modifier.size(22.dp).mirrorInRtl(),
                )
            }
            if (joinExpanded) {
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.family_onboarding_invite_code_label),
                    style = PeachPlum.typography.label,
                    color = PeachPlum.colors.inkFaint,
                )
                Spacer(Modifier.height(10.dp))
                CodeSlotInput(value = code, onValueChange = { code = it })
                Spacer(Modifier.height(14.dp))
                PeachPlumPrimaryButton(
                    text = if (state.joining) stringResource(R.string.family_onboarding_joining) else stringResource(R.string.action_continue),
                    onClick = { viewModel.joinByCode(code) },
                    enabled = code.length == 6 && !state.joining,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(it), color = PeachPlum.colors.overText, style = PeachPlum.typography.bodyStrong)
        }
        }
    }
}

@Composable
private fun Card(
    iconBg: androidx.compose.ui.graphics.Color,
    iconContent: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PeachPlum.colors.surface, PeachPlum.radius.card)
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(52.dp).background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            iconContent()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = PeachPlum.typography.headline, color = PeachPlum.colors.ink)
            Text(subtitle, style = PeachPlum.typography.body, color = PeachPlum.colors.inkMuted)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = PeachPlum.colors.primary,
            modifier = Modifier.size(22.dp).mirrorInRtl(),
        )
    }
}

private val Color5B4D8C = androidx.compose.ui.graphics.Color(0xFF5B4D8C)
