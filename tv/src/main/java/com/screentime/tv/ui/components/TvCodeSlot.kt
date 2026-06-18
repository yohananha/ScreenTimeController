package com.screentime.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.tv.ui.theme.Sprout

enum class TvCodeSlotState { Empty, Active, Filled, Error }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCodeSlot(
    digit: Char?,
    state: TvCodeSlotState,
    modifier: Modifier = Modifier,
    width: Int = 60,
    height: Int = 75,
) {
    val (bg, borderColor) = when (state) {
        TvCodeSlotState.Empty -> Color(0x1AFCF6F0) to Color(0x33FFFFFF)
        TvCodeSlotState.Active -> Color(0x1FFCF6F0) to Sprout.colors.primary
        TvCodeSlotState.Filled -> Sprout.colors.tvCream to Sprout.colors.tvCream
        TvCodeSlotState.Error -> Color(0x2EE5483A) to Sprout.colors.overDisplay
    }
    val textColor = if (state == TvCodeSlotState.Filled) Sprout.colors.ink else Sprout.colors.tvCream
    val shape = RoundedCornerShape(12.dp)
    val outerShape = RoundedCornerShape(15.dp)
    Box(
        modifier = modifier
            .width(width.dp)
            .height(height.dp)
            .background(bg, shape)
            .border(BorderStroke(if (state == TvCodeSlotState.Active) 2.dp else 0.75.dp, borderColor), shape)
            .then(
                if (state == TvCodeSlotState.Active) {
                    Modifier.border(BorderStroke(3.dp, Sprout.colors.primary.copy(alpha = 0.3f)), outerShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit?.toString() ?: "",
            style = Sprout.typography.displayLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
            color = textColor,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCodeSlotsRow(
    code: String,
    slots: Int = 6,
    errored: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(slots) { i ->
            val state = when {
                errored -> TvCodeSlotState.Error
                i < code.length -> TvCodeSlotState.Filled
                i == code.length -> TvCodeSlotState.Active
                else -> TvCodeSlotState.Empty
            }
            TvCodeSlot(digit = code.getOrNull(i), state = state)
        }
    }
}
