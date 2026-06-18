package com.screentime.tv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.tv.ui.theme.Sprout

sealed class KeypadKey {
    data class Digit(val value: Int) : KeypadKey()
    data object Clear : KeypadKey()
    data object Backspace : KeypadKey()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvKeypad(
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
    firstKeyFocus: FocusRequester? = null,
    keySize: Int = 75,
) {
    val rows = listOf(
        listOf(KeypadKey.Digit(1), KeypadKey.Digit(2), KeypadKey.Digit(3)),
        listOf(KeypadKey.Digit(4), KeypadKey.Digit(5), KeypadKey.Digit(6)),
        listOf(KeypadKey.Digit(7), KeypadKey.Digit(8), KeypadKey.Digit(9)),
        listOf(KeypadKey.Clear, KeypadKey.Digit(0), KeypadKey.Backspace),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(11.dp)) {
        rows.forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                row.forEachIndexed { colIdx, key ->
                    val first = rowIdx == 0 && colIdx == 0
                    KeypadButton(
                        key = key,
                        onClick = { onKey(key) },
                        size = keySize,
                        focusRequester = if (first) firstKeyFocus else null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    size: Int,
    focusRequester: FocusRequester?,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "scale")
    val haloWidth by animateDpAsState(if (focused) 3.dp else 0.dp, label = "halo")

    val bg = if (focused) Sprout.colors.primary else Color(0x12FFFFFF)
    val border = if (focused) Sprout.colors.primary else Color(0x24FFFFFF)
    val textColor = when {
        focused -> Sprout.colors.onPrimary
        key is KeypadKey.Digit -> Sprout.colors.tvCream
        else -> Sprout.colors.accent
    }
    val label = when (key) {
        is KeypadKey.Digit -> key.value.toString()
        KeypadKey.Clear -> "C"
        KeypadKey.Backspace -> "⌫"
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .background(bg, RoundedCornerShape(13.dp))
            .border(BorderStroke(1.5.dp, border), RoundedCornerShape(13.dp))
            .border(BorderStroke(haloWidth, Sprout.colors.tvCream), RoundedCornerShape(16.dp))
            .focusable(interactionSource = interaction)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Sprout.typography.keypadDigit, color = textColor)
    }
}
