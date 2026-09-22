package com.screentime.mobile.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.ModeTiles
import com.screentime.mobile.ui.components.RequestStrip
import com.screentime.mobile.ui.components.TimeGauge
import com.screentime.mobile.ui.components.TvMode
import com.screentime.mobile.ui.components.UsageRow
import com.screentime.mobile.ui.family.PairTvViewModel
import com.screentime.mobile.ui.limits.LimitsViewModel
import com.screentime.mobile.ui.requests.RequestsViewModel
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.mobile.ui.theme.appAccentFor
import com.screentime.mobile.ui.theme.rememberScreenPadding
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    onOpenRules: () -> Unit,
    onOpenFamily: () -> Unit,
    limitsViewModel: LimitsViewModel = hiltViewModel(),
    requestsViewModel: RequestsViewModel = hiltViewModel(),
    pairTvViewModel: PairTvViewModel = hiltViewModel(),
) {
    val state by limitsViewModel.state.collectAsState()
    val requestsState by requestsViewModel.state.collectAsState()
    val devices by pairTvViewModel.pairedDevices.collectAsState()
    val hPad = rememberScreenPadding()
    val context = LocalContext.current

    val mode = if (state.instantLocked) TvMode.Lock else if (state.allowAllDayActive) TvMode.Allow else TvMode.Limits
    val noLimit = state.overallDailyMinutes == Limits.UNLIMITED
    val overallMs = if (noLimit) 0L else state.overallDailyMinutes * 60_000L
    val usedMs = state.totalUsageMillis
    val leftMinutes = if (noLimit) 0 else ((overallMs - usedMs) / 60_000L).coerceAtLeast(0).toInt()
    val overLimit = !noLimit && overallMs > 0 && usedMs >= overallMs
    val progress = if (noLimit || overallMs <= 0) 0f else (usedMs.toFloat() / overallMs.toFloat())

    val pendingRequest = requestsState.pending.firstOrNull()
    val tvName = devices.firstOrNull()?.name ?: stringResource(R.string.today_no_tv_paired)
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(locale) {
        DateTimeFormatter.ofPattern("EEEE, MMM d", locale).format(java.time.LocalDate.now())
    }

    Box(modifier = Modifier.fillMaxSize().background(PeachPlum.colors.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = hPad, end = hPad, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(dateLabel.replaceFirstChar { it.titlecase(locale) }, style = PeachPlum.typography.body, color = PeachPlum.colors.inkMuted)
                    IconButton(
                        onClick = onOpenFamily,
                        modifier = Modifier.size(36.dp).background(PeachPlum.colors.ink, CircleShape),
                    ) {
                        Text("P", style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.background)
                    }
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimeGauge(
                        centerLabel = if (noLimit) "—" else formatHoursMinutesClock(leftMinutes),
                        word = if (noLimit) "" else if (overLimit) stringResource(R.string.today_gauge_word_over) else stringResource(R.string.today_gauge_word_left),
                        progress = progress,
                        overLimit = overLimit,
                    )
                }
            }
            item {
                Text(
                    text = when {
                        mode == TvMode.Allow -> stringResource(R.string.today_mode_allow_caption)
                        noLimit -> stringResource(R.string.today_no_daily_limit)
                        else -> stringResource(R.string.today_gauge_caption, formatHoursMinutesClock(state.overallDailyMinutes))
                    },
                    style = PeachPlum.typography.caption,
                    color = PeachPlum.colors.inkMuted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .background(PeachPlum.colors.surface, PeachPlum.radius.pill)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (devices.isNotEmpty()) {
                        Box(modifier = Modifier.size(8.dp).background(PeachPlum.colors.positiveDisplay, CircleShape))
                    }
                    Text(tvName, style = PeachPlum.typography.caption, color = PeachPlum.colors.ink)
                }
            }
            item {
                ModeTiles(
                    active = mode,
                    disabled = devices.isEmpty(),
                    onSelect = { next ->
                        when (next) {
                            TvMode.Lock -> limitsViewModel.selectInstantLock()
                            TvMode.Allow -> limitsViewModel.selectAllowAllDay()
                            TvMode.Limits -> limitsViewModel.selectDefaultLimits()
                        }
                    },
                )
            }
            if (pendingRequest != null) {
                item {
                    val relativeTime = formatRelativeTime(
                        createdAt = pendingRequest.createdAt,
                        justNow = stringResource(R.string.requests_just_now),
                        minutesAgo = stringResource(R.string.requests_minutes_ago),
                        hoursAgo = stringResource(R.string.requests_hours_ago),
                    )
                    val requestedLabel = stringResource(R.string.requests_chip_minutes, pendingRequest.requestedMinutes)
                    RequestStrip(
                        title = stringResource(R.string.today_request_title, requestedLabel, packageDisplayName(pendingRequest.appPackage, state.availableApps, context)),
                        subtitle = stringResource(R.string.today_request_subtitle, relativeTime),
                        approveAria = stringResource(R.string.today_approve_aria, requestedLabel),
                        denyAria = stringResource(R.string.today_deny_aria),
                        onApprove = { requestsViewModel.approve(pendingRequest) },
                        onDeny = { requestsViewModel.deny(pendingRequest) },
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.today_by_app), style = PeachPlum.typography.bodyStrong, color = PeachPlum.colors.ink)
                    TextButton(onClick = onOpenRules) {
                        Text(stringResource(R.string.today_rules_link), style = PeachPlum.typography.label, color = PeachPlum.colors.ink)
                    }
                }
            }
            itemsIndexed(state.limits, key = { _, limit -> limit.packageName }) { index, limit ->
                val usedMsForApp = state.usagePerApp[limit.packageName] ?: 0L
                val usedMinutes = usedMsForApp / 60_000L
                val isUnlimited = limit.dailyLimitMinutes == Limits.UNLIMITED
                val limitMs = limit.dailyLimitMinutes * 60_000L
                val timesUp = !isUnlimited && limit.dailyLimitMinutes >= 0 && limitMs > 0 && usedMsForApp >= limitMs
                val rowProgress = if (isUnlimited || limitMs <= 0) 0f else (usedMsForApp.toFloat() / limitMs.toFloat()).coerceAtMost(1f)
                val swatch = appAccentFor(limit.packageName)
                UsageRow(
                    name = packageDisplayName(limit.packageName, state.availableApps, context),
                    swatchColor = if (timesUp) PeachPlum.colors.overDisplay else swatch,
                    valueLabel = when {
                        timesUp -> stringResource(R.string.status_times_up)
                        isUnlimited -> "${usedMinutes}m"
                        else -> "${usedMinutes}m / ${limit.dailyLimitMinutes}m"
                    },
                    progress = rowProgress,
                    barColor = swatch,
                    timesUp = timesUp,
                    showBottomBorder = index == state.limits.lastIndex,
                )
            }
        }
    }
}

private fun formatHoursMinutesClock(totalMinutes: Int): String {
    val m = totalMinutes.coerceAtLeast(0)
    val hours = m / 60
    val mins = m % 60
    return "$hours:${mins.toString().padStart(2, '0')}"
}

private fun formatRelativeTime(createdAt: Instant, justNow: String, minutesAgo: String, hoursAgo: String): String {
    val seconds = Duration.between(createdAt, Instant.now()).seconds
    return when {
        seconds < 60 -> justNow
        seconds < 3600 -> minutesAgo.format(seconds / 60)
        else -> hoursAgo.format(seconds / 3600)
    }
}

private fun packageDisplayName(pkg: String, availableApps: List<InstalledApp>, context: android.content.Context): String {
    availableApps.find { it.packageName == pkg }?.let { return it.label }
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}
