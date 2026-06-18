package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo

/** Dark plum hero card used for the daily total on Limits and the active unlock code on Codes. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SproutRadius.large)
            .background(Sprout.colors.ink, SproutRadius.large),
    ) {
        // Decorative offset circle — top-right, partially clipped by the card edge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(120.dp)
                .background(Sprout.colors.darkSurface.copy(alpha = 0.5f), CircleShape),
        )
        Box(modifier = Modifier.padding(22.dp)) {
            content()
        }
    }
}

@Composable
fun DailyTotalHero(
    usedLabel: String,
    ofLabel: String,
    progress: Float,
    timeLeft: String,
    resetLabel: String,
    status: Status,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (status) {
        Status.OnTrack -> Sprout.colors.positiveDisplay
        Status.AlmostUp -> Sprout.colors.warningDisplay
        Status.TimesUp -> Sprout.colors.overDisplay
        Status.Paused -> Sprout.colors.accent
    }
    HeroCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "TODAY'S SCREEN TIME",
                    style = Sprout.typography.label,
                    color = Sprout.colors.darkMutedText,
                )
                StatusBadge(status = status)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    usedLabel,
                    style = Sprout.typography.display.copy(fontSize = 46.sp, fontWeight = FontWeight.SemiBold),
                    color = Sprout.colors.background,
                )
                Text(
                    ofLabel,
                    style = Sprout.typography.bodyL,
                    color = Sprout.colors.darkMutedText,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            ProgressBar(progress = progress.coerceIn(0f, 1f), fill = statusColor, track = Sprout.colors.darkSurface)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(timeLeft, style = Sprout.typography.caption, color = Sprout.colors.darkMutedText)
                Text(resetLabel, style = Sprout.typography.caption, color = Sprout.colors.darkMutedText)
            }
        }
    }
}

@Composable
fun ProgressBar(progress: Float, fill: Color, track: Color, height: Int = 12) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(track, SproutRadius.pill)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(fill, SproutRadius.pill),
        )
    }
}
