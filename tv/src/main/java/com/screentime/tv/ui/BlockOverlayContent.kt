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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.TimeRequest
import com.screentime.shared.model.Limits
import com.screentime.tv.R
import com.screentime.tv.service.BlockReason
import com.screentime.tv.ui.components.KeypadKey
import com.screentime.tv.ui.components.TvCanvas
import com.screentime.tv.ui.components.TvCodeSlotsRow
import com.screentime.tv.ui.components.TvGhostButton
import com.screentime.tv.ui.components.TvKeypad
import com.screentime.tv.ui.components.TvPrimaryButton
import com.screentime.tv.ui.components.TvStatusCircle
import com.screentime.tv.ui.theme.LocalFormats
import com.screentime.tv.ui.theme.PeachPlum
import com.screentime.tv.ui.theme.atReferenceSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.screentime.shared.format.bidiWrap

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

    // Footer left-side context (design/i6c-peach-plum README §4 "TV"): only the
    // main Block screen shows one ("Resets at midnight · in 3h 12m"), so the
    // countdown lives up here where TvCanvas's footer can see it too.
    val durationFormat = LocalFormats.current.duration
    val resources = LocalContext.current.resources
    var timeRemainingStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val nowTime = LocalTime.now()
            val secondsToMidnight = ChronoUnit.SECONDS.between(nowTime, LocalTime.MAX) + 1
            timeRemainingStr = durationFormat.minutes(resources, (secondsToMidnight / 60).toInt())
            delay(1000)
        }
    }
    val isLockedNow = lockout.locked && view != OverlayView.RequestTime && view != OverlayView.RequestCustom && view != OverlayView.Waiting
    val footerContext = when {
        isLockedNow -> null
        view == OverlayView.Main && blockReason == BlockReason.DailyLimitReached ->
            stringResource(R.string.overlay_resets_midnight, timeRemainingStr)
        else -> null
    }
    val footerHint = if (view == OverlayView.NumPad && !isLockedNow) stringResource(R.string.tv_remote_hint_keypad) else null

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

    TvCanvas(footerContext = footerContext, footerHint = footerHint) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLockedNow) {
                LockedView(
                    lockout = lockout,
                    onTimerExpired = onLockoutTick,
                    onAskParent = { view = OverlayView.RequestTime },
                )
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

/** "That's enough " + peach "for today." (design/i6c-peach-plum README §4 "Block"). */
@Composable
private fun TwoToneHeadline(first: String, accented: String, style: androidx.compose.ui.text.TextStyle) {
    Text(
        buildAnnotatedString {
            append(first)
            withStyle(SpanStyle(color = PeachPlum.colors.primary)) { append(accented) }
        },
        style = style,
        color = PeachPlum.colors.tvCream,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InstantLockedView() {
    Column(
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Text(
            stringResource(R.string.overlay_instant_locked_title),
            style = PeachPlum.typography.displayHero,
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.overlay_instant_locked_body),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
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
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Text(
            stringResource(R.string.overlay_outside_hours_title),
            style = PeachPlum.typography.displayHero,
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            if (nextWindowAt != null) {
                stringResource(R.string.overlay_opens_again_at, nextWindowAt)
            } else {
                stringResource(R.string.overlay_no_more_today)
            },
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            TvPrimaryButton(text = stringResource(R.string.overlay_ask_parent_more_time), onClick = onRequestMore, focusRequester = focus)
            TvGhostButton(text = stringResource(R.string.overlay_enter_unlock_code), onClick = onEnterCode)
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

    val durationFormat = LocalFormats.current.duration
    val resources = LocalContext.current.resources

    Column(
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        if (blockedPackage.isNotBlank()) {
            val usedMinutes = (usedMillis / 60000).toInt()
            Text(
                text = stringResource(R.string.overlay_watched_today, appLabel, durationFormat.minutes(resources, usedMinutes)).bidiWrap(),
                style = PeachPlum.typography.label,
                color = PeachPlum.colors.tvMutedText,
            )
        }

        TwoToneHeadline(
            first = stringResource(R.string.overlay_main_title),
            accented = stringResource(R.string.overlay_main_title_accent),
            style = PeachPlum.typography.displayHero,
        )

        Text(
            stringResource(R.string.overlay_used_all_time),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            TvPrimaryButton(text = stringResource(R.string.overlay_ask_parent_more_time), onClick = onRequestMore, focusRequester = focus)
            TvGhostButton(text = stringResource(R.string.overlay_enter_unlock_code), onClick = onEnterCode)
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
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Text(
            stringResource(R.string.overlay_request_time_title),
            style = PeachPlum.typography.displayLarge.atReferenceSize(132),
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.overlay_request_time_body),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            TvPrimaryButton(
                text = stringResource(R.string.overlay_request_15),
                onClick = {
                    if (busy) return@TvPrimaryButton
                    busy = true
                    scope.launch { onSubmit(15); busy = false }
                },
                focusRequester = focus,
            )
            TvPrimaryButton(
                text = stringResource(R.string.overlay_request_30),
                onClick = {
                    if (busy) return@TvPrimaryButton
                    busy = true
                    scope.launch { onSubmit(30); busy = false }
                },
            )
            TvGhostButton(text = stringResource(R.string.overlay_maybe_later), onClick = onCancel)
        }
        // Extra beyond the drawn spec (design/i6c-peach-plum only shows 15/30/Maybe
        // later) — kept so the existing custom-amount request flow stays reachable.
        TvGhostButton(text = stringResource(R.string.overlay_request_other_amount), onClick = onOther)
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
        modifier = Modifier.width(1500.dp),
        horizontalArrangement = Arrangement.spacedBy(120.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(760.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Text(
                stringResource(R.string.overlay_request_custom_title),
                style = PeachPlum.typography.titleLarge,
                color = PeachPlum.colors.tvCream,
            )
            Text(
                stringResource(R.string.overlay_request_custom_body),
                style = PeachPlum.typography.bodyLarge,
                color = PeachPlum.colors.tvMutedText,
            )
            Box(
                modifier = Modifier
                    .background(if (entered.isEmpty()) PeachPlum.colors.tvSurface else PeachPlum.colors.tvCream, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (entered.isEmpty()) stringResource(R.string.overlay_request_custom_placeholder) else entered,
                    style = PeachPlum.typography.displayHero,
                    color = if (entered.isEmpty()) PeachPlum.colors.tvMutedText else PeachPlum.colors.tvBackground,
                )
            }
            if (entered.isNotEmpty() && !valid) {
                Text(
                    stringResource(R.string.overlay_pick_number_range),
                    color = PeachPlum.colors.overDisplay,
                    style = PeachPlum.typography.label,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TvPrimaryButton(
                    text = stringResource(R.string.overlay_send_to_parent),
                    onClick = {
                        if (!valid || busy) return@TvPrimaryButton
                        busy = true
                        scope.launch { onSubmit(minutes); busy = false }
                    },
                )
                TvGhostButton(text = stringResource(R.string.overlay_cancel), onClick = onCancel)
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
        modifier = Modifier.width(1500.dp),
        horizontalArrangement = Arrangement.spacedBy(120.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(760.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Text(
                stringResource(R.string.overlay_numpad_title),
                style = PeachPlum.typography.titleLarge,
                color = PeachPlum.colors.tvCream,
            )
            Text(
                stringResource(R.string.overlay_numpad_body),
                style = PeachPlum.typography.bodyLarge,
                color = PeachPlum.colors.tvMutedText,
            )
            TvCodeSlotsRow(code = entered, errored = errored)
            if (errored) {
                // Real CLDR plural rules — the original English literal had
                // no singular form at all ("1 tries left"), a free bug fix.
                Row(
                    modifier = Modifier
                        .background(PeachPlum.colors.overContainer, RoundedCornerShape(18.dp))
                        .border(BorderStroke(2.dp, PeachPlum.colors.overDisplay), RoundedCornerShape(18.dp))
                        .padding(horizontal = 26.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.tv.material3.Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = PeachPlum.colors.overText,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        pluralStringResource(R.plurals.overlay_code_wrong, triesLeft, triesLeft),
                        color = PeachPlum.colors.overText,
                        style = PeachPlum.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    )
                }
            }
            TvGhostButton(text = stringResource(R.string.overlay_cancel), onClick = onCancel)
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
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        TvStatusCircle(
            backgroundColor = Color(0x2EF79AC0),
            foregroundColor = PeachPlum.colors.accent,
            icon = Icons.Outlined.Smartphone,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        )
        Text(
            stringResource(R.string.overlay_waiting_title),
            style = PeachPlum.typography.displayLarge,
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.overlay_waiting_body),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            TvGhostButton(text = stringResource(R.string.overlay_enter_code_instead), onClick = onEnterCode, focusRequester = focus)
            TvGhostButton(text = stringResource(R.string.overlay_never_mind), onClick = onCancel)
        }
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
        stringResource(R.string.overlay_approved_headline, approvedMinutes)
    } else {
        stringResource(R.string.overlay_approved_headline_unknown)
    }

    Column(
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        TvStatusCircle(
            backgroundColor = PeachPlum.colors.positiveDisplay,
            foregroundColor = Color(0xFF062A1E),
            icon = Icons.Filled.Check,
            haloRing = true,
        )
        Text(
            headlineText,
            style = PeachPlum.typography.displayLarge.atReferenceSize(128),
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.overlay_approved_body),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
        )
        TvPrimaryButton(text = stringResource(R.string.overlay_keep_watching), onClick = onBack, focusRequester = focus)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UnlockedView(onBack: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    Column(
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        TvStatusCircle(
            backgroundColor = PeachPlum.colors.positiveDisplay,
            foregroundColor = Color(0xFF062A1E),
            icon = Icons.Filled.LockOpen,
            haloRing = true,
        )
        Text(
            stringResource(R.string.overlay_unlocked_title),
            style = PeachPlum.typography.displayLarge.atReferenceSize(128),
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.overlay_unlocked_body),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
        )
        TvPrimaryButton(text = stringResource(R.string.overlay_keep_watching), onClick = onBack, focusRequester = focus)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeniedView(onOkay: () -> Unit, onEnterCode: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (_: Exception) {} }

    Column(
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        TvStatusCircle(
            backgroundColor = Color(0x1AFFFFFF),
            foregroundColor = PeachPlum.colors.tvCream,
            icon = Icons.Filled.SentimentDissatisfied,
            haloRing = true,
        )
        Text(
            stringResource(R.string.overlay_denied_title),
            style = PeachPlum.typography.displayLarge.atReferenceSize(128),
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.overlay_denied_body),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 550.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            TvGhostButton(text = stringResource(R.string.overlay_enter_unlock_code), onClick = onEnterCode)
            // Default focus = Okay (design/i6c-peach-plum README §4 "Denied").
            TvPrimaryButton(text = stringResource(R.string.overlay_okay), onClick = onOkay, focusRequester = focus)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LockedView(lockout: LockoutSettings, onTimerExpired: suspend () -> Unit, onAskParent: () -> Unit) {
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
    val focus = remember { FocusRequester() }
    LaunchedEffect(lockout.mode) { try { focus.requestFocus() } catch (_: Exception) {} }

    Column(
        modifier = Modifier.width(1280.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        TvStatusCircle(
            backgroundColor = Color(0x2EFFB088),
            foregroundColor = PeachPlum.colors.primary,
            icon = Icons.Filled.Pause,
            haloRing = true,
        )
        Text(
            if (lockout.mode == LockoutMode.PARENT_UNLOCK) stringResource(R.string.overlay_locked_parent_title) else stringResource(R.string.overlay_locked_timer_title),
            style = PeachPlum.typography.displayLarge.atReferenceSize(112),
            color = PeachPlum.colors.tvCream,
            textAlign = TextAlign.Center,
        )
        Text(
            if (lockout.mode == LockoutMode.PARENT_UNLOCK)
                stringResource(R.string.overlay_locked_parent_body)
            else
                stringResource(R.string.overlay_locked_timer_body),
            style = PeachPlum.typography.bodyLarge,
            color = PeachPlum.colors.tvMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 1000.dp),
        )
        when (lockout.mode) {
            LockoutMode.PARENT_UNLOCK -> {
                TvGhostButton(text = stringResource(R.string.overlay_locked_ask_parent), onClick = onAskParent, focusRequester = focus)
            }
            LockoutMode.TIMER -> {
                val remaining = anchor?.let {
                    val elapsed = nowElapsed - it.startElapsedMs
                    Duration.ofMillis((it.totalRemainingMs - elapsed).coerceAtLeast(0))
                } ?: Duration.ZERO
                Text(
                    text = LocalFormats.current.duration.countdown(LocalContext.current.resources, remaining.seconds),
                    style = PeachPlum.typography.displayHero,
                    color = PeachPlum.colors.primary,
                )
                TvGhostButton(text = stringResource(R.string.overlay_locked_ask_parent), onClick = onAskParent, focusRequester = focus)
            }
        }
    }
}

private data class LockoutAnchor(val startElapsedMs: Long, val totalRemainingMs: Long)
