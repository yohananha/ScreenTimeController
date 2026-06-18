package com.screentime.mobile.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.ProgressBar
import com.screentime.mobile.ui.components.TopHeader
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.shared.model.UsageSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val snapshots by viewModel.snapshots.collectAsState()
    val nonEmpty = snapshots.filter { it.perAppMillis.isNotEmpty() }
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
                    Text("Usage", style = Sprout.typography.display, color = Sprout.colors.ink)
                    Text(
                        "Last 7 days",
                        style = Sprout.typography.caption,
                        color = Sprout.colors.inkMuted,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            if (nonEmpty.isEmpty()) {
                item { EmptyState() }
            } else {
                item { WeeklyBarChartCard(snapshots) }
                items(nonEmpty, key = { it.date }) { snapshot -> DayCard(snapshot) }
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
        Text("No usage yet", style = Sprout.typography.title, color = Sprout.colors.ink)
        Text(
            "Your family hasn't watched anything today. Enjoy the quiet!",
            style = Sprout.typography.bodyStrong,
            color = Sprout.colors.inkMuted,
        )
    }
}

@Composable
private fun WeeklyBarChartCard(snapshots: List<UsageSnapshot>) {
    val maxMinutes = (snapshots.maxOfOrNull { it.totalMinutes() } ?: 1).coerceAtLeast(1)
    val today = LocalDate.now()
    val limit = 240 // hardcoded 4h reference like the design

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.input)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            snapshots.reversed().forEach { snapshot ->
                val date = LocalDate.parse(snapshot.date)
                val isToday = date == today
                val mins = snapshot.totalMinutes()
                val fraction = (mins.toFloat() / maxMinutes).coerceIn(0f, 1f)
                val color = statusColorFor(mins)
                val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(color),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        dayLabel,
                        style = Sprout.typography.label,
                        color = if (isToday) Sprout.colors.ink else Sprout.colors.inkFaint,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(Sprout.colors.positiveDisplay, "On track")
            LegendDot(Sprout.colors.warningDisplay, "Almost up")
            LegendDot(Sprout.colors.overDisplay, "Over limit")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
    }
}

private fun statusColorFor(minutes: Int): Color = when {
    minutes > 240 -> Color(0xFFE5483A)
    minutes > 180 -> Color(0xFFF2A93B)
    else -> Color(0xFF4FCFA1)
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
    val topApps = snapshot.perAppMillis.entries.sortedByDescending { it.value }.take(5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(dayLabel, style = Sprout.typography.headline, color = Sprout.colors.ink)
                Text(dateLabel, style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
            }
            Text("${totalMinutes}m", style = Sprout.typography.headline, color = Sprout.colors.ink)
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

@Composable
private fun AppUsageRow(appName: String, minutes: Int, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(appName, style = Sprout.typography.body, color = Sprout.colors.ink)
            Text("${minutes}m", style = Sprout.typography.bodyStrong, color = Sprout.colors.inkMuted)
        }
        ProgressBar(
            progress = fraction.coerceIn(0f, 1f),
            fill = Sprout.colors.primary,
            track = Sprout.colors.outline,
            height = 6,
        )
    }
}

private fun UsageSnapshot.totalMinutes() = (perAppMillis.values.sum() / 60_000).toInt()
