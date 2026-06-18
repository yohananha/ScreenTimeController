package com.screentime.mobile.ui.requests

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.mobile.ui.theme.appAccentFor
import com.screentime.mobile.ui.components.ChipGroup
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.components.TopHeader
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.shared.model.TimeRequest
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RequestsScreen(viewModel: RequestsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val selectedAmounts = remember { mutableStateMapOf<String, Int>() }
    val hPad = rememberScreenPadding()

    Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TopHeader(familyName = "Family", parentInitial = "P") }
            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
                    Text("Requests", style = Sprout.typography.display, color = Sprout.colors.ink)
                    Text(
                        text = "Approve or deny extra time the TV asks for.",
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }

            if (state.pending.isEmpty() && state.active.isEmpty()) {
                item { EmptyState() }
            } else {
                if (state.pending.isNotEmpty()) {
                    items(state.pending, key = { it.id }) { req ->
                        val selected = selectedAmounts[req.id] ?: req.requestedMinutes
                        PendingCard(
                            request = req,
                            selectedAmount = selected,
                            onAmountChange = { selectedAmounts[req.id] = it },
                            onApprove = { viewModel.approve(req, selected) },
                            onDeny = { viewModel.deny(req) },
                        )
                    }
                }
                if (state.active.isNotEmpty()) {
                    item {
                        Text(
                            "Active",
                            style = Sprout.typography.title,
                            color = Sprout.colors.ink,
                            modifier = Modifier.padding(top = 12.dp, start = 2.dp),
                        )
                    }
                    items(state.active, key = { it.id }) { req -> ActiveGrantRow(req) }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(88.dp).background(Sprout.colors.positiveContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Sprout.colors.positiveText, modifier = Modifier.size(40.dp))
        }
        Text("You're all caught up", style = Sprout.typography.title, color = Sprout.colors.ink)
        Text(
            "No pending requests right now.",
            style = Sprout.typography.bodyStrong,
            color = Sprout.colors.inkMuted,
        )
    }
}


@Composable
private fun PendingCard(
    request: TimeRequest,
    selectedAmount: Int,
    onAmountChange: (Int) -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    val context = LocalContext.current
    val appLabel = remember(request.appPackage) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(request.appPackage, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            request.appPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(Sprout.colors.ink, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Tv, contentDescription = null, tint = Sprout.colors.background, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Living Room TV", style = Sprout.typography.headline, color = Sprout.colors.ink)
                val relativeTime = formatRelativeTime(request.createdAt)
                Text(relativeTime, style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
            }
            // App pill on the right
            Row(
                modifier = Modifier
                    .background(Sprout.colors.accentContainer, Sprout.radius.pill)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val appColor = appAccentFor(request.appPackage)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(appColor, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = appLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = Sprout.typography.label.copy(fontSize = 11.sp),
                        color = Color.White
                    )
                }
                Text(
                    text = appLabel,
                    style = Sprout.typography.label,
                    color = Sprout.colors.ink
                )
            }
        }
        Text(
            buildAnnotatedAsk(request.requestedMinutes),
            style = Sprout.typography.title,
        )

        Text(
            "GRANT HOW LONG?",
            style = Sprout.typography.label,
            color = Sprout.colors.inkFaint,
        )
        ChipGroup(
            options = listOf(15, 30, 60),
            selected = selectedAmount.coerceIn(15, 60),
            onSelect = onAmountChange,
            label = { "${it}m" },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SproutPrimaryButton(
                text = "Approve ${selectedAmount}m",
                onClick = onApprove,
                modifier = Modifier.weight(1f),
            )
            SproutGhostButton(
                text = "Deny",
                onClick = onDeny,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun formatRelativeTime(createdAt: java.time.Instant): String {
    val duration = java.time.Duration.between(createdAt, java.time.Instant.now())
    val seconds = duration.seconds
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        else -> "${seconds / 3600}h ago"
    }
}

@Composable
private fun buildAnnotatedAsk(minutes: Int) = buildAnnotatedString {
    append("Wants ")
    withStyle(SpanStyle(color = Sprout.colors.overDisplay)) {
        append("${minutes}m")
    }
    append(" more")
}

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
private fun ActiveGrantRow(request: TimeRequest) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surfaceSunken, Sprout.radius.input)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(Sprout.colors.outline, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Tv, contentDescription = null, tint = Sprout.colors.inkMuted, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            val granted = request.approvedMinutes ?: request.requestedMinutes
            val appLabel = remember(request.appPackage) {
                try {
                    val pm = context.packageManager
                    val info = pm.getApplicationInfo(request.appPackage, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    request.appPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                }
            }
            Text("Approved ${granted}m on $appLabel", style = Sprout.typography.bodyStrong, color = Sprout.colors.ink)
            request.grantExpiresAt()?.let { expiry ->
                val until = timeFormatter.withZone(ZoneId.systemDefault()).format(expiry)
                Text(
                    "Active until $until",
                    style = Sprout.typography.caption,
                    color = Sprout.colors.inkMuted,
                )
            }
        }
    }
}
