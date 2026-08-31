package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screentime.mobile.ui.theme.RubikFont
import com.screentime.mobile.ui.theme.Sprout

/**
 * 6 cream-on-dark digit tiles used in the active unlock-code hero card.
 *
 * Forced LTR: a `Row` honors layout direction, so under RTL the digits would
 * render in reverse order ("123456" as "654321"). Codes are always a fixed
 * left-to-right numeral sequence regardless of locale, so the direction is
 * pinned here rather than left to the caller — this is the single place all
 * callers go through.
 */
@Composable
fun CodeTilesRow(code: String, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        repeat(6) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(78.dp)
                    .background(Sprout.colors.background, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = code.getOrNull(i)?.toString() ?: "-",
                    style = TextStyle(
                        fontFamily = RubikFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 44.sp,
                    ),
                    color = Sprout.colors.ink,
                )
            }
        }
    }
    }
}
