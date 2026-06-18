package com.screentime.tv.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.TimeRequest
import com.screentime.shared.model.Limits
import com.screentime.tv.service.BlockReason
import com.screentime.tv.ui.components.KeypadKey
import com.screentime.tv.ui.components.StatusKind
import com.screentime.tv.ui.components.TvCanvas
import com.screentime.tv.ui.components.TvCodeSlotsRow
import com.screentime.tv.ui.components.TvGhostButton
import com.screentime.tv.ui.components.TvKeypad
import com.screentime.tv.ui.components.TvPrimaryButton
import com.screentime.tv.ui.components.TvStatusCircle
import com.screentime.tv.ui.theme.Sprout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.unit.sp

private enum class OverlayView { Main, NumPad, RequestTime, RequestCustom, Waiting, Approved, Denied, Unlocked }

class BackPressHandler {
    var onBackPressed: (() -> Unit)? = null
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BlockOverlayContent(
    blockedPackage: String,
    blockReason: BlockReason = BlockReason.DailyLimitReached,
    nextWindowAt: String? = null,
    lockout: LockoutSettings,
    requestStatus: TimeRequest.Status?,
    approvedMinutes: Int?,
    limits: Limits,
    usedMillis: Long,
    backPressHandler: BackPressHandler,
    onSubmitCode: suspend (String) -> Boolean,
    onSubmitRequest: suspend (Int) -> Boolean,
    onLockoutTick: suspend () -> Unit,
) {
    var view by remember { mutableStateOf(OverlayView.Main) }

    val context = LocalContext.current
    val appLabel = remember(blockedPackage) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(blockedPackage, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            blockedPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    LaunchedEffect(requestStatus) {
        when (requestStatus) {
            TimeRequest.Status.Approved -> view = OverlayView.Approved
            TimeRequest.Status.Denied -> view = OverlayView.Denied
            else -> Unit
        }
    }

    DisposableEffect(view, lockout.locked) {
        backPressHandler.onBackPressed = {
            if (!lockout.locked) {
                view = when (view) {
                    OverlayView.RequestCustom -> OverlayView.RequestTime
                    else -> OverlayView.Main
                }
            }
        }
        onDispose { backPressHandler.onBackPressed = null }
    }

    TvCanvas {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (lockout.locked) {
                LockedView(lockout = lockout, onTimerExpired = onLockoutTick)
            } else {
                when (view) {
                    OverlayView.Main -> when (blockReason) {
                        BlockReason.InstantLocked -> InstantLockedView()
                        BlockReason.OutsideHours -> OutsideHoursView(
                            nextWindowAt = nextWindowAt,
                            onRequestMore = { view = OverlayView.RequestTime },
                            onEnterCode = { view = OverlayView.NumPad },
                        )
                        else -> MainView(
                            blockedPackage = blockedPackage,
                            appLabel = appLabel,
                            usedMillis = usedMillis,
                            limits = limits,
                            onRequestMore = { view = OverlayView.RequestTime },
                            onEnterCode = { view = OverlayView.NumPad },
                        )
                    }
                    OverlayView.NumPad -> NumPadView(
                        onCancel = { view = OverlayView.Main },
                        onSubmit = onSubmitCode,
                        onSuccess = { view = OverlayView.Unlocked },
                    )
                    OverlayView.RequestTime -> RequestTimeView(
                        onCancel = { view = OverlayView.Main },
                        onOther = { view = OverlayView.RequestCustom },
                        onSubmit = { minutes ->
                            val ok = onSubmitRequest(minutes)
                            if (ok) view = OverlayView.Waiting
                            ok
                        },
                    )
                    OverlayView.RequestCustom -> RequestCustomView(
                        onCancel = { view = OverlayView.RequestTime },
                        onSubmit = { minutes ->
                            val ok = onSubmitRequest(minutes)
                            if (ok) view = OverlayView.Waiting
                            ok
                        },
                    )
                    OverlayView.Waiting -> WaitingView(
                        onEnterCode = { view = OverlayView.NumPad },
                        onCancel = { view = OverlayView.Main },
                    )
                    OverlayView.Approved -> ApprovedView(approvedMinutes = approvedMinutes, onBack = { view = OverlayView.Main })
                    OverlayView.Denied -> DeniedView(
                        onOkay = { view = OverlayView.Main },
                        onEnterCode = { view = OverlayView.NumPad },
                    )
                    OverlayView.Unlocked -> UnlockedView(onBack = { view = OverlayView.Main })
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InstantLockedView() {
    Column(
        modifier = Modifier.padding(horizontal = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvStatusCircle(kind = StatusKind.Amber, size = 100)
        Text(
            "TV is locked",
            style = Sprout.typography.displayHero,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            "A parent locked the TV from the Sprout app. Ask them to unlock it when you're ready.",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 590.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OutsideHoursView(
    nextWindowAt: String?,
    onRequestMore: () -> Unit,
    onEnterCode: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }
    Column(
        modifier = Modifier.padding(horizontal = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvStatusCircle(kind = StatusKind.Amber, size = 100)
        Text(
            "TV time is paused right now",
            style = Sprout.typography.displayHero,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            if (nextWindowAt != null) {
                "Screen time opens again at $nextWindowAt — hang tight!"
            } else {
                "No more screen time scheduled for today — see you tomorrow!"
            },
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 590.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvPrimaryButton(text = "Ask a parent for more time", onClick = onRequestMore, focusRequester = focus)
            TvGhostButton(text = "Enter an unlock code", onClick = onEnterCode)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MainView(
    blockedPackage: String,
    appLabel: String,
    usedMillis: Long,
    limits: Limits,
    onRequestMore: () -> Unit,
    onEnterCode: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    var timeRemainingStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val nowTime = LocalTime.now()
            val secondsToMidnight = ChronoUnit.SECONDS.between(nowTime, LocalTime.MAX) + 1
            val hours = secondsToMidnight / 3600
            val minutes = (secondsToMidnight % 3600) / 60
            timeRemainingStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (blockedPackage.isNotBlank()) {
            Row(
                modifier = Modifier
                    .background(Color(0x14FFFFFF), Sprout.radius.pill)
                    .border(1.dp, Color(0x29FFFFFF), Sprout.radius.pill)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val appColor = appAccentFor(blockedPackage)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(appColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = appLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = Sprout.typography.label.copy(fontSize = 13.sp),
                        color = Color.White,
                    )
                }
                val usedMinutes = (usedMillis / 60000).toInt()
                Text(
                    text = "$appLabel · ${formatDuration(usedMinutes)} watched today",
                    style = Sprout.typography.label,
                    color = Color(0xFFEFE7F3),
                )
            }
        }
        
        TvStatusCircle(kind = StatusKind.Mint, size = 105)
        
        Text(
            "That's a wrap for today!",
            style = Sprout.typography.displayHero,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        
        Text(
            "Nice watching. You've used all your $appLabel time — see you tomorrow, or ask for a little more.",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 590.dp),
        )

        Row(
            modifier = Modifier
                .background(Color(0x12FFFFFF), Sprout.radius.pill)
                .padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.tv.material3.Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Color(0xFF9785AC),
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Resets at midnight · $timeRemainingStr",
                style = Sprout.typography.bodyMedium,
                color = Color(0xFF9785AC),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvPrimaryButton(text = "Ask a parent for more time", onClick = onRequestMore, focusRequester = focus)
            TvGhostButton(text = "Enter an unlock code", onClick = onEnterCode)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RequestTimeView(
    onCancel: () -> Unit,
    onOther: () -> Unit,
    onSubmit: suspend (Int) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }
    Column(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "How much more?",
            style = Sprout.typography.displayLarge,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            "We'll send a quick request to a parent's phone.",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvPrimaryButton(
                text = "15 more minutes",
                onClick = {
                    if (busy) return@TvPrimaryButton
                    busy = true
                    scope.launch { onSubmit(15); busy = false }
                },
                focusRequester = focus,
            )
            TvPrimaryButton(
                text = "30 more minutes",
                onClick = {
                    if (busy) return@TvPrimaryButton
                    busy = true
                    scope.launch { onSubmit(30); busy = false }
                },
            )
            TvGhostButton(text = "Other amount", onClick = onOther)
        }
        TvGhostButton(text = "Maybe later", onClick = onCancel)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RequestCustomView(
    onCancel: () -> Unit,
    onSubmit: suspend (Int) -> Boolean,
) {
    var entered by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val minutes = entered.toIntOrNull() ?: 0
    val valid = minutes in 1..240
 
    Row(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(310.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "How many minutes?",
                style = Sprout.typography.titleLarge,
                color = Sprout.colors.tvCream,
            )
            Text(
                "Anywhere from 1 to 240 minutes. We'll send the request to a parent's phone.",
                style = Sprout.typography.bodyLarge,
                color = Sprout.colors.tvMutedText,
            )
            Box(
                modifier = Modifier
                    .background(if (entered.isEmpty()) Color(0x1AFCF6F0) else Sprout.colors.tvCream, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (entered.isEmpty()) "—" else entered,
                    style = Sprout.typography.displayHero,
                    color = if (entered.isEmpty()) Sprout.colors.tvMutedText else Sprout.colors.ink,
                )
            }
            if (entered.isNotEmpty() && !valid) {
                Text(
                    "Pick a number between 1 and 240",
                    color = Sprout.colors.overDisplay,
                    style = Sprout.typography.label,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvPrimaryButton(
                    text = "Send to parent",
                    onClick = {
                        if (!valid || busy) return@TvPrimaryButton
                        busy = true
                        scope.launch { onSubmit(minutes); busy = false }
                    },
                )
                TvGhostButton(text = "Cancel", onClick = onCancel)
            }
        }
        TvKeypad(
            onKey = { key ->
                when (key) {
                    is KeypadKey.Digit -> if (entered.length < 3) entered += key.value.toString()
                    KeypadKey.Backspace -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                    KeypadKey.Clear -> entered = ""
                }
            },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumPadView(
    onCancel: () -> Unit,
    onSubmit: suspend (String) -> Boolean,
    onSuccess: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    var errored by remember { mutableStateOf(false) }
    var triesLeft by remember { mutableStateOf(LockoutSettings.MAX_ATTEMPTS) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(entered) {
        if (entered.length == CODE_LENGTH && !busy) {
            busy = true
            val ok = onSubmit(entered)
            if (ok) {
                onSuccess()
            } else {
                errored = true
                triesLeft = (triesLeft - 1).coerceAtLeast(0)
                delay(900)
                entered = ""
                errored = false
            }
            busy = false
        }
    }

    Row(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(310.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Enter the unlock code",
                style = Sprout.typography.titleLarge,
                color = Sprout.colors.tvCream,
            )
            Text(
                "Ask a parent to read it to you, or check their phone.",
                style = Sprout.typography.bodyLarge,
                color = Sprout.colors.tvMutedText,
            )
            TvCodeSlotsRow(code = entered, errored = errored)
            if (errored) {
                Text(
                    "That code didn't work — $triesLeft tries left",
                    color = Sprout.colors.overDisplay,
                    style = Sprout.typography.label,
                )
            }
            TvGhostButton(text = "Cancel", onClick = onCancel)
        }
        TvKeypad(
            onKey = { key ->
                if (busy) return@TvKeypad
                when (key) {
                    is KeypadKey.Digit -> if (entered.length < CODE_LENGTH) entered += key.value.toString()
                    KeypadKey.Backspace -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                    KeypadKey.Clear -> entered = ""
                }
            },
        )
    }
}

private const val CODE_LENGTH = 6

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WaitingView(onEnterCode: () -> Unit, onCancel: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvStatusCircle(
            kind = StatusKind.Lilac,
            icon = Icons.Outlined.Smartphone,
            backgroundColor = Color(0xFFB9A8F0),
            foregroundColor = Color(0xFF3A2A4D),
            size = 100,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
        )
        Text(
            "Asked your parent!",
            style = Sprout.typography.displayLarge,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            "They just got a notification. Hang tight — or punch in an unlock code if they gave you one.",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 550.dp),
        )
        TvGhostButton(text = "Enter a code instead", onClick = onEnterCode, focusRequester = focus)
        TvGhostButton(text = "Never mind", onClick = onCancel)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ApprovedView(
    approvedMinutes: Int?,
    onBack: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    val headlineText = if (approvedMinutes != null) {
        "You got $approvedMinutes more!"
    } else {
        "You got more time!"
    }

    Column(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvStatusCircle(kind = StatusKind.Mint)
        Text(
            headlineText,
            style = Sprout.typography.displayLarge,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            "Make it count — your timer's running again.",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
        )
        TvPrimaryButton(text = "Keep watching", onClick = onBack, focusRequester = focus)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UnlockedView(onBack: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    Column(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvStatusCircle(kind = StatusKind.Mint, icon = Icons.Filled.LockOpen)
        Text(
            "Code worked!",
            style = Sprout.typography.displayLarge,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            "Limit's paused for a bit. Enjoy!",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
        )
        TvPrimaryButton(text = "Keep watching", onClick = onBack, focusRequester = focus)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeniedView(onOkay: () -> Unit, onEnterCode: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    Column(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvStatusCircle(kind = StatusKind.Lilac, size = 100)
        Text(
            "Not right now",
            style = Sprout.typography.displayLarge,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            "Your parent said maybe later — and that's okay. There's always tomorrow.",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 550.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvGhostButton(text = "Enter an unlock code", onClick = onEnterCode, focusRequester = focus)
            TvPrimaryButton(text = "Okay", onClick = onOkay)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LockedView(lockout: LockoutSettings, onTimerExpired: suspend () -> Unit) {
    // Use the monotonic clock so manipulating the device's wall clock can't
    // shorten a lockout. We pin (startElapsedMs, totalRemainingMs) once when
    // we first observe `lockedUntil` and count down from there. The server's
    // `lockedUntil` is still the source of truth for the initial duration;
    // we anchor to it once via Instant.now(), then tick on elapsedRealtime
    // (which the user can't fast-forward by editing settings).
    val lockedUntil = lockout.lockedUntil
    val anchor = remember(lockedUntil) {
        lockedUntil?.let {
            val initialRemainingMs = Duration.between(Instant.now(), it)
                .toMillis().coerceAtLeast(0)
            LockoutAnchor(SystemClock.elapsedRealtime(), initialRemainingMs)
        }
    }
    var nowElapsed by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(anchor) {
        var expiredFired = false
        while (true) {
            nowElapsed = SystemClock.elapsedRealtime()
            if (!expiredFired && anchor != null &&
                nowElapsed - anchor.startElapsedMs >= anchor.totalRemainingMs) {
                expiredFired = true
                onTimerExpired()
            }
            delay(1000)
        }
    }
    Column(
        modifier = Modifier.padding(horizontal = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TvStatusCircle(kind = StatusKind.Amber, size = 100)
        Text(
            if (lockout.mode == LockoutMode.PARENT_UNLOCK) "Ask a parent to unlock" else "Let's take a short break",
            style = Sprout.typography.displayLarge,
            color = Sprout.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            if (lockout.mode == LockoutMode.PARENT_UNLOCK)
                "That's a few wrong codes. A parent needs to unlock the TV from the mobile app."
            else
                "That's a few wrong codes. The TV unlocks again on its own — grab a snack or stretch!",
            style = Sprout.typography.bodyLarge,
            color = Sprout.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 550.dp),
        )
        when (lockout.mode) {
            LockoutMode.PARENT_UNLOCK -> {}
            LockoutMode.TIMER -> {
                val remaining = anchor?.let {
                    val elapsed = nowElapsed - it.startElapsedMs
                    Duration.ofMillis((it.totalRemainingMs - elapsed).coerceAtLeast(0))
                } ?: Duration.ZERO
                Text(
                    text = formatRemaining(remaining),
                    style = Sprout.typography.displayHero,
                    color = Color(0xFFF2C879),
                )
            }
        }
    }
}

private data class LockoutAnchor(val startElapsedMs: Long, val totalRemainingMs: Long)

private fun formatRemaining(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private val appAccents = listOf(
    Color(0xFFE5483A), Color(0xFF5B6B7B), Color(0xFF2A2730), Color(0xFF4FA98C),
    Color(0xFF8E86D9), Color(0xFFF2A93B), Color(0xFFB9A8F0),
)

private fun appAccentFor(packageName: String): Color =
    appAccents[(packageName.hashCode().let { if (it < 0) -it else it }) % appAccents.size]

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}
