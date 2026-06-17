package com.screentime.mobile.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.EmptyState
import com.screentime.shared.model.UsageSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val snapshots by viewModel.snapshots.collectAsState()
    val nonEmpty = snapshots.filter { it.perAppMillis.isNotEmpty() }

    if (nonEmpty.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.BarChart,
            text = "No usage yet — data will appear here once the TV has been active.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Last 7 days", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            WeeklyBarChart(snapshots)
        }

        items(nonEmpty, key = { it.date }) { snapshot ->
            DayCard(snapshot)
        }
    }
}

@Composable
private fun WeeklyBarChart(snapshots: List<UsageSnapshot>) {
    val maxMinutes = snapshots.maxOfOrNull { it.totalMinutes() } ?: 1
    val today = LocalDate.now()
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        snapshots.reversed().forEach { snapshot ->
            val date = LocalDate.parse(snapshot.date)
            val isToday = date == today
            val fraction = (snapshot.totalMinutes().toFloat() / maxMinutes).coerceIn(0f, 1f)
            val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.03f))
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(if (isToday) primary else primary.copy(alpha = 0.45f)),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) primary else muted,
                )
            }
        }
    }
}

@Composable
private fun DayCard(snapshot: UsageSnapshot) {
    val date = LocalDate.parse(snapshot.date)
    val today = LocalDate.now()
    val dayLabel = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    val dateLabel = date.format(DateTimeFormatter.ofPattern("MMM d"))
    val totalMinutes = snapshot.totalMinutes()
    val topApps = snapshot.perAppMillis.entries
        .sortedByDescending { it.value }
        .take(5)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dayLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    "$dateLabel · ${totalMinutes}min total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            topApps.forEach { (pkg, millis) ->
                AppUsageRow(
                    appName = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    minutes = (millis / 60_000).toInt(),
                    fraction = millis.toFloat() / snapshot.perAppMillis.values.sum().coerceAtLeast(1L),
                )
            }
        }
    }
}

@Composable
private fun AppUsageRow(appName: String, minutes: Int, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                appName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                "${minutes}min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

private fun UsageSnapshot.totalMinutes() =
    (perAppMillis.values.sum() / 60_000).toInt()
