package com.screentime.mobile.ui.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.EmptyState
import com.screentime.shared.model.TimeRequest
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(viewModel: RequestsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_requests)) }) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.pending.isEmpty() && state.active.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    text = "All caught up — no requests right now.",
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.pending.isNotEmpty()) {
                        item { SectionHeader("Pending", Icons.Filled.HourglassEmpty) }
                        items(state.pending, key = { it.id }) { req ->
                            PendingCard(req, viewModel::approve, viewModel::deny)
                        }
                    }
                    if (state.active.isNotEmpty()) {
                        item { SectionHeader("Extra time granted", Icons.Filled.CheckCircle) }
                        items(state.active, key = { it.id }) { req -> ActiveGrantCard(req) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PendingCard(
    request: TimeRequest,
    onApprove: (TimeRequest, Int) -> Unit,
    onDeny: (TimeRequest) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(request.appPackage, fontWeight = FontWeight.SemiBold)
            Text("Requested ${request.requestedMinutes} min")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onApprove(request, request.requestedMinutes) }) {
                    Text("Approve ${request.requestedMinutes}")
                }
                listOf(15, 30, 60).forEach { minutes ->
                    AssistChip(
                        onClick = { onApprove(request, minutes) },
                        label = { Text("$minutes") },
                    )
                }
            }
            TextButton(onClick = { onDeny(request) }) { Text("Deny") }
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
private fun ActiveGrantCard(request: TimeRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(request.appPackage, fontWeight = FontWeight.SemiBold)
            val granted = request.approvedMinutes ?: request.requestedMinutes
            Text("Extra time granted: $granted min", style = MaterialTheme.typography.bodyMedium)
            request.grantExpiresAt()?.let { expiry ->
                val until = timeFormatter.withZone(ZoneId.systemDefault()).format(expiry)
                Text(
                    "Active until $until",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
