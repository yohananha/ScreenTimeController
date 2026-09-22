package com.screentime.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.tv.ui.theme.PeachPlum

enum class TvCodeSlotState { Empty, Active, Filled, Error }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCodeSlot(
    digit: Char?,
    state: TvCodeSlotState,
    modifier: Modifier = Modifier,
    height: Int = 150,
) {
    // design/i6c-peach-plum README §4 "Keypad": filled = cream / active = peach
    // 4px border + glow / empty = white@8%.
    val (bg, borderColor, borderWidth) = when (state) {
        TvCodeSlotState.Empty -> Triple(PeachPlum.colors.tvSurface, PeachPlum.colors.outline, 3.dp)
        TvCodeSlotState.Active -> Triple(Color(0x1AFFF6EE), PeachPlum.colors.primary, 4.dp)
        TvCodeSlotState.Filled -> Triple(PeachPlum.colors.tvCream, PeachPlum.colors.tvCream, 0.dp)
        TvCodeSlotState.Error -> Triple(PeachPlum.colors.overContainer, PeachPlum.colors.overDisplay, 2.dp)
    }
    val textColor = if (state == TvCodeSlotState.Filled) PeachPlum.colors.tvBackground else PeachPlum.colors.tvCream
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(height.dp)
            .background(bg, shape)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .then(
                if (state == TvCodeSlotState.Active) {
                    Modifier.border(BorderStroke(6.dp, PeachPlum.colors.primary.copy(alpha = 0.3f)), shape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = digit?.toString() ?: "", style = PeachPlum.typography.codeTile, color = textColor)
    }
}

/** Forced LTR — see CodeTilesRow (mobile) for why: digits must not reverse under RTL. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCodeSlotsRow(
    code: String,
    slots: Int = 6,
    errored: Boolean = false,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(slots) { i ->
                val state = when {
                    errored -> TvCodeSlotState.Error
                    i < code.length -> TvCodeSlotState.Filled
                    i == code.length -> TvCodeSlotState.Active
                    else -> TvCodeSlotState.Empty
                }
                TvCodeSlot(digit = code.getOrNull(i), state = state, modifier = Modifier.weight(1f))
            }
        }
    }
}
