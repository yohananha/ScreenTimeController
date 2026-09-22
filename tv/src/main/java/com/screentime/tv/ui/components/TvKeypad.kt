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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.screentime.tv.R
import com.screentime.tv.ui.theme.PeachPlum

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
    keySize: Int = 150,
) {
    val rows = listOf(
        listOf(KeypadKey.Digit(1), KeypadKey.Digit(2), KeypadKey.Digit(3)),
        listOf(KeypadKey.Digit(4), KeypadKey.Digit(5), KeypadKey.Digit(6)),
        listOf(KeypadKey.Digit(7), KeypadKey.Digit(8), KeypadKey.Digit(9)),
        listOf(KeypadKey.Clear, KeypadKey.Digit(0), KeypadKey.Backspace),
    )
    // The keypad grid never mirrors (design/i6c-peach-plum README §5.3 —
    // screens/TV-Keypad-HE.html keeps 1-2-3/4-5-6/7-8-9/C-0-⌫ in the same
    // physical positions and the same "⌫" glyph as the English version).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
            rows.forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
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
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "scale")
    val ringWidth by animateDpAsState(if (focused) 6.dp else 0.dp, label = "ring")

    val bg = if (focused) PeachPlum.colors.tvCream else PeachPlum.colors.tvSurface
    val border = if (focused) PeachPlum.colors.tvCream else PeachPlum.colors.outline
    val textColor = when {
        focused -> PeachPlum.colors.tvBackground
        key is KeypadKey.Digit -> PeachPlum.colors.tvCream
        else -> PeachPlum.colors.primary // C / backspace are peach
    }
    Box(
        modifier = Modifier
            .border(BorderStroke(ringWidth, PeachPlum.colors.primary), RoundedCornerShape(26.dp))
            .padding(ringWidth)
            .scale(scale)
            .size(size.dp)
            .background(bg, RoundedCornerShape(26.dp))
            .border(BorderStroke(3.dp, border), RoundedCornerShape(26.dp))
            .focusable(interactionSource = interaction)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (key == KeypadKey.Backspace) {
            // AutoMirrored, but harmlessly so: the keypad is forced LTR above
            // (see the comment on TvKeypad), so this always renders its
            // default, non-flipped orientation — matching the "⌫" glyph the
            // Hebrew reference uses unchanged.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size((size * 0.32).dp),
            )
        } else {
            val label = when (key) {
                is KeypadKey.Digit -> key.value.toString()
                KeypadKey.Clear -> stringResource(R.string.keypad_clear)
                KeypadKey.Backspace -> "" // unreachable, handled above
            }
            Text(label, style = PeachPlum.typography.keypadDigit, color = textColor)
        }
    }
}
