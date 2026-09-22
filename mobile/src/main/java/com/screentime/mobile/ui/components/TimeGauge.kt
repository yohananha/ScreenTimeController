package com.screentime.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.PeachPlum

private val GaugeWidth = 300.dp
private val GaugeHeight = 170.dp
private val GaugeRadius = 120.dp
private val GaugeStroke = 16.dp

/**
 * Half-arc time gauge (design/i6c-peach-plum tokens.json#sizes.arc). The fill
 * represents time USED (not remaining) — it grows toward `over` as the day's
 * total is consumed, even though the center label shows time left.
 *
 * RTL: only the fill arc mirrors (drawn growing from the opposite end); the
 * track, and the h:mm/word text, never do — matching TimeGauge.tsx and
 * screens/Today-HE.html.
 */
@Composable
fun TimeGauge(
    centerLabel: String,
    word: String,
    progress: Float,
    overLimit: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val clamped = progress.coerceIn(0f, 1f)
    val trackColor = PeachPlum.colors.outline
    val overColor = PeachPlum.colors.overDisplay
    val peach = PeachPlum.colors.primary
    val rose = PeachPlum.colors.accent

    Box(modifier = modifier.width(GaugeWidth).height(GaugeHeight), contentAlignment = Alignment.BottomCenter) {
        Canvas(modifier = Modifier.width(GaugeWidth).height(GaugeHeight)) {
            val strokePx = GaugeStroke.toPx()
            val radiusPx = GaugeRadius.toPx()
            val diameter = radiusPx * 2
            val topLeft = Offset(size.width / 2f - radiusPx, size.height / 2f - radiusPx)
            val arcSize = Size(diameter, diameter)
            val style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round)

            // Track: always the top half-circle, west -> north -> east, never mirrored.
            drawArc(color = trackColor, startAngle = 180f, sweepAngle = -180f, useCenter = false, topLeft = topLeft, size = arcSize, style = style)

            if (clamped > 0f) {
                val sweep = 180f * clamped
                // LTR fills from the west end toward the east via the top; RTL
                // fills from the east end toward the west via the top instead
                // of literally mirroring the drawn path (matches the CSS
                // translate+scale(-1,1) applied only to the fill in the web version).
                val startAngle = if (isRtl) 0f else 180f
                val sweepAngle = if (isRtl) sweep else -sweep
                if (overLimit) {
                    drawArc(color = overColor, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
                } else {
                    val brush = Brush.horizontalGradient(listOf(peach, rose), startX = 0f, endX = size.width)
                    drawArc(brush = brush, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = topLeft, size = arcSize, style = style)
                }
            }
        }
        Column(modifier = Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = PeachPlum.typography.gauge, color = PeachPlum.colors.ink)
            if (word.isNotEmpty()) {
                Text(word, style = PeachPlum.typography.gaugeWord, color = PeachPlum.colors.inkMuted)
            }
        }
    }
}
