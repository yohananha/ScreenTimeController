package com.screentime.tv.ui.pairing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.tv.R
import com.screentime.tv.ui.components.TvCanvas
import com.screentime.tv.ui.components.TvGhostButton
import com.screentime.tv.ui.theme.PeachPlum
import com.screentime.tv.ui.theme.atReferenceSize

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PairingScreen(viewModel: PairingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.ensureCode() }

    TvCanvas {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 100.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.pairing_headline),
                style = PeachPlum.typography.displayLarge.atReferenceSize(104),
                color = PeachPlum.colors.tvCream,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.pairing_instructions),
                style = PeachPlum.typography.bodyLarge,
                color = PeachPlum.colors.tvMutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp).widthIn(max = 1000.dp),
            )

            val code = state.code
            if (code != null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val slotGap = 18.dp
                    val slotWidth = ((maxWidth - slotGap * (code.length - 1)) / code.length)
                        .coerceAtMost(118.dp)
                    val slotHeight = (slotWidth.value * 1.254f).dp
                    // Forced LTR — a Row honors layout direction, so under
                    // RTL the pairing code's digits would render reversed.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(horizontalArrangement = Arrangement.spacedBy(slotGap)) {
                            code.forEach { ch ->
                                Box(
                                    modifier = Modifier
                                        .width(slotWidth)
                                        .height(slotHeight)
                                        .background(PeachPlum.colors.tvCream, RoundedCornerShape(24.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = ch.toString(),
                                        style = PeachPlum.typography.codeTile,
                                        color = PeachPlum.colors.tvBackground,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.padding(top = 28.dp)) {
                    Text(
                        stringResource(R.string.pairing_generating_code),
                        style = PeachPlum.typography.bodyLarge,
                        color = PeachPlum.colors.tvMutedText,
                    )
                }
            }

            // Waiting indicator
            Row(
                modifier = Modifier.padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val transition = rememberInfiniteTransition(label = "waitingDot")
                val alpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
                    label = "alpha",
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(alpha)
                        .background(PeachPlum.colors.positiveDisplay, CircleShape),
                )
                Text(
                    stringResource(R.string.pairing_waiting),
                    style = PeachPlum.typography.label,
                    color = Color(0xFF9FE9CE),
                )
            }

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {}
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                TvGhostButton(
                    text = stringResource(R.string.pairing_get_new_code),
                    onClick = { viewModel.ensureCode() },
                    focusRequester = focusRequester
                )
            }

            state.error?.let {
                Text(
                    stringResource(it),
                    color = PeachPlum.colors.overDisplay,
                    modifier = Modifier.padding(top = 6.dp),
                    style = PeachPlum.typography.bodyMedium,
                )
            }
        }
    }
}
