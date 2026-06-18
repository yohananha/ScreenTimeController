package com.screentime.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import com.screentime.mobile.ui.theme.Sprout

/** Phone-side 6-digit invite-code entry. Hidden text field accepts input; visible slots render code. */
@Composable
fun CodeSlotInput(
    value: String,
    onValueChange: (String) -> Unit,
    slots: Int = 6,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var tfv by remember { mutableStateOf(TextFieldValue(value)) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(slots) { i ->
                val filled = i < value.length
                val active = i == value.length
                val bg = if (filled || active) Sprout.colors.background else Sprout.colors.surfaceSunken
                val border = when {
                    active -> BorderStroke(2.dp, Sprout.colors.primary)
                    filled -> BorderStroke(1.5.dp, Sprout.colors.outline)
                    else -> BorderStroke(1.5.dp, Sprout.colors.outline)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp)
                        .background(bg, RoundedCornerShape(14.dp))
                        .border(border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = value.getOrNull(i)?.toString() ?: "",
                        style = TextStyle(
                            fontFamily = com.screentime.mobile.ui.theme.RubikFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 32.sp,
                            color = Sprout.colors.ink,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
        BasicTextField(
            value = tfv,
            onValueChange = { v ->
                val digits = v.text.filter { it.isDigit() }.take(slots)
                tfv = TextFieldValue(digits, selection = androidx.compose.ui.text.TextRange(digits.length))
                onValueChange(digits)
            },
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
@Suppress("unused")
private fun PreviewHook() {
    // ensure size import retained
    Box(Modifier.size(0.dp))
}
