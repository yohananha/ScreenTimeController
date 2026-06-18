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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.tv.ui.components.TvCanvas
import com.screentime.tv.ui.components.TvGhostButton
import com.screentime.tv.ui.theme.Sprout

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
                "Pair with a parent's phone",
                style = Sprout.typography.displayLarge,
                color = Sprout.colors.tvCream,
                textAlign = TextAlign.Center,
            )
            Text(
                "In the ScreenTime app, open Family & devices → Pair a TV and enter this code.",
                style = Sprout.typography.bodyLarge,
                color = Sprout.colors.tvMutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp).widthIn(max = 600.dp),
            )

            val code = state.code
            if (code != null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val slotGap = 9.dp
                    val slotWidth = ((maxWidth - slotGap * (code.length - 1)) / code.length)
                        .coerceAtMost(59.dp)
                    val slotHeight = (slotWidth.value * 1.254f).dp
                    Row(horizontalArrangement = Arrangement.spacedBy(slotGap)) {
                        code.forEach { ch ->
                            Box(
                                modifier = Modifier
                                    .width(slotWidth)
                                    .height(slotHeight)
                                    .background(Sprout.colors.tvCream, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = ch.toString(),
                                    style = Sprout.typography.displayHero,
                                    color = Sprout.colors.ink,
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.padding(top = 28.dp)) {
                    Text(
                        "Generating your code…",
                        style = Sprout.typography.bodyLarge,
                        color = Sprout.colors.tvMutedText,
                    )
                }
            }

            // Waiting indicator
            Row(
                modifier = Modifier.padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        .size(7.dp)
                        .alpha(alpha)
                        .background(Sprout.colors.positiveDisplay, CircleShape),
                )
                Text(
                    "Waiting for the phone…",
                    style = Sprout.typography.bodyMedium,
                    color = Sprout.colors.positiveDisplay,
                )
            }

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {}
            }
            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TvGhostButton(
                    text = "Get a new code",
                    onClick = { viewModel.ensureCode() },
                    focusRequester = focusRequester
                )
                TvGhostButton(
                    text = "How to pair",
                    onClick = {}
                )
            }

            state.error?.let {
                Text(
                    it,
                    color = Sprout.colors.overDisplay,
                    modifier = Modifier.padding(top = 6.dp),
                    style = Sprout.typography.bodyMedium,
                )
            }
        }
    }
}
