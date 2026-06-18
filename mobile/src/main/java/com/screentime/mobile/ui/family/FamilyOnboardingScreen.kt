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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.CodeSlotInput
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.rememberScreenPadding

@Composable
fun FamilyOnboardingScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var joinExpanded by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background), contentAlignment = Alignment.TopCenter) {
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
                        .background(Sprout.colors.primary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(12.dp).background(Sprout.colors.ink, CircleShape))
                }
                Text("ScreenTime", style = Sprout.typography.headline, color = Sprout.colors.ink)
            }
            Box(
                modifier = Modifier.size(38.dp).background(Sprout.colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("P", style = Sprout.typography.label, color = Sprout.colors.ink)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Set up your family", style = Sprout.typography.display, color = Sprout.colors.ink)
        Text(
            "Create a new family or join one a co-parent set up.",
            style = Sprout.typography.bodyStrong,
            color = Sprout.colors.inkMuted,
            modifier = Modifier.padding(top = 7.dp),
        )

        Spacer(Modifier.height(20.dp))

        // Create card
        Card(
            iconBg = Sprout.colors.accentContainer,
            iconContent = {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color5B4D8C, modifier = Modifier.size(24.dp))
            },
            title = "Create a family",
            subtitle = "Start fresh. You'll be the owner.",
            onClick = { viewModel.createFamily() },
        )

        Spacer(Modifier.height(14.dp))

        // Join card (expandable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sprout.colors.surface, Sprout.radius.card)
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
                    modifier = Modifier.size(52.dp).background(Sprout.colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = Sprout.colors.ink, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Join a family", style = Sprout.typography.headline, color = Sprout.colors.ink)
                    Text(
                        "Use the invite code from your co-parent.",
                        style = Sprout.typography.body,
                        color = Sprout.colors.inkMuted,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Sprout.colors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (joinExpanded) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "ENTER THE 6-DIGIT INVITE CODE",
                    style = Sprout.typography.label,
                    color = Sprout.colors.inkFaint,
                )
                Spacer(Modifier.height(10.dp))
                CodeSlotInput(value = code, onValueChange = { code = it })
                Spacer(Modifier.height(14.dp))
                SproutPrimaryButton(
                    text = if (state.joining) "Joining…" else "Continue",
                    onClick = { viewModel.joinByCode(code) },
                    enabled = code.length == 6 && !state.joining,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Sprout.colors.overText, style = Sprout.typography.bodyStrong)
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
            .background(Sprout.colors.surface, Sprout.radius.card)
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
            Text(title, style = Sprout.typography.headline, color = Sprout.colors.ink)
            Text(subtitle, style = Sprout.typography.body, color = Sprout.colors.inkMuted)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Sprout.colors.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

private val Color5B4D8C = androidx.compose.ui.graphics.Color(0xFF5B4D8C)
