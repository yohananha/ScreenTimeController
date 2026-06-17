package com.screentime.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.TimeRequest
import com.screentime.tv.ui.components.IconBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private enum class OverlayView { Main, NumPad, RequestTime, RequestCustom, RequestSent }

/** Lets the hosting [android.view.ViewGroup] route hardware Back presses into this composable. */
class BackPressHandler {
    var onBackPressed: (() -> Unit)? = null
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BlockOverlayContent(
    blockedPackage: String,
    lockout: LockoutSettings,
    requestStatus: TimeRequest.Status?,
    backPressHandler: BackPressHandler,
    onSubmitCode: suspend (String) -> Boolean,
    onSubmitRequest: suspend (Int) -> Boolean,
    onLockoutTick: suspend () -> Unit,
) {
    var view by remember { mutableStateOf(OverlayView.Main) }

    // Back always steps to the previous screen rather than dismissing the
    // overlay — the block can only be lifted via a code or parent approval.
    DisposableEffect(view, lockout.locked) {
        backPressHandler.onBackPressed = {
            if (!lockout.locked) {
                view = when (view) {
                    OverlayView.RequestCustom -> OverlayView.RequestTime
                    OverlayView.Main -> OverlayView.Main
                    else -> OverlayView.Main
                }
            }
        }
        onDispose { backPressHandler.onBackPressed = null }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0B1226)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (lockout.locked) {
                LockedView(lockout = lockout, onTick = onLockoutTick)
            } else {
                when (view) {
                    OverlayView.Main -> MainView(
                        blockedPackage = blockedPackage,
                        onRequestMore = { view = OverlayView.RequestTime },
                        onEnterCode = { view = OverlayView.NumPad },
                    )
                    OverlayView.NumPad -> NumPadView(
                        onCancel = { view = OverlayView.Main },
                        onSubmit = onSubmitCode,
                    )
                    OverlayView.RequestTime -> RequestTimeView(
                        onCancel = { view = OverlayView.Main },
                        onOther = { view = OverlayView.RequestCustom },
                        onSubmit = { minutes ->
                            val ok = onSubmitRequest(minutes)
                            if (ok) view = OverlayView.RequestSent
                            ok
                        },
                    )
                    OverlayView.RequestCustom -> RequestCustomView(
                        onCancel = { view = OverlayView.RequestTime },
                        onSubmit = { minutes ->
                            val ok = onSubmitRequest(minutes)
                            if (ok) view = OverlayView.RequestSent
                            ok
                        },
                    )
                    OverlayView.RequestSent -> RequestSentView(
                        requestStatus = requestStatus,
                        onBack = { view = OverlayView.Main },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LockedView(lockout: LockoutSettings, onTick: suspend () -> Unit) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(lockout) {
        while (true) {
            now = Instant.now()
            onTick()
            delay(1000)
        }
    }
    IconBadge(
        icon = Icons.Filled.Lock,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        size = 64.dp,
    )
    Text(
        text = "Code entry locked",
        style = MaterialTheme.typography.headlineLarge,
        color = Color.White,
    )
    when (lockout.mode) {
        LockoutMode.PARENT_UNLOCK -> Text(
            text = "Ask a parent to unlock from the mobile app.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
        )
        LockoutMode.TIMER -> {
            val remaining = lockout.lockedUntil?.let { until ->
                Duration.between(now, until).let { if (it.isNegative) Duration.ZERO else it }
            } ?: Duration.ZERO
            Text(
                text = "Too many wrong codes. Try again in ${formatRemaining(remaining)}.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatRemaining(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MainView(
    blockedPackage: String,
    onRequestMore: () -> Unit,
    onEnterCode: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    IconBadge(
        icon = Icons.Filled.HourglassBottom,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        size = 64.dp,
    )
    Text(
        text = "Time's up for now",
        style = MaterialTheme.typography.headlineLarge,
        color = Color.White,
    )
    if (blockedPackage.isNotBlank()) {
        Text(
            text = blockedPackage,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = onRequestMore, modifier = Modifier.focusRequester(focusRequester)) {
            Text("Request more time")
        }
        Button(onClick = onEnterCode) { Text("Enter code") }
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    IconBadge(
        icon = Icons.Filled.Schedule,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        size = 64.dp,
    )
    Text("How much time do you need?", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(15, 30, 60).forEachIndexed { index, mins ->
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch { onSubmit(mins); busy = false }
                },
                modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
            ) { Text("$mins min") }
        }
        Button(enabled = !busy, onClick = onOther) { Text("Other amount") }
    }
    Button(onClick = onCancel, enabled = !busy) { Text("Cancel") }
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    IconBadge(
        icon = Icons.Filled.Schedule,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        size = 64.dp,
    )
    Text("How many minutes do you need?", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    Text(
        text = if (entered.isEmpty()) "— min" else "$entered min",
        color = Color.White,
        fontFamily = FontFamily.Monospace,
        fontSize = 40.sp,
    )

    NumericKeypad(
        value = entered,
        maxLength = 3,
        enabled = !busy,
        submitLabel = "OK",
        submitEnabled = minutes in 1..240,
        onValueChange = { entered = it },
        onSubmit = {
            busy = true
            scope.launch { onSubmit(minutes); busy = false }
        },
        initialFocusRequester = focusRequester,
    )
    Button(onClick = onCancel, enabled = !busy) { Text("Cancel") }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RequestSentView(requestStatus: TimeRequest.Status?, onBack: () -> Unit) {
    // A denial doesn't dismiss the overlay (unlike approval, which grants a
    // bonus and lets the accessibility service re-evaluate) — show a brief
    // acknowledgement, then return to the main "time's up" screen ourselves.
    val denied = requestStatus == TimeRequest.Status.Denied
    LaunchedEffect(denied) {
        if (denied) {
            delay(2000)
            onBack()
        }
    }

    if (denied) {
        IconBadge(
            icon = Icons.Filled.Cancel,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            size = 64.dp,
        )
        Text(
            "Request denied",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
    } else {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        IconBadge(
            icon = Icons.Filled.CheckCircle,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            size = 64.dp,
        )
        Text(
            "Request sent! Waiting for a parent to respond…",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onBack, modifier = Modifier.focusRequester(focusRequester)) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumPadView(
    onCancel: () -> Unit,
    onSubmit: suspend (String) -> Boolean,
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    IconBadge(
        icon = Icons.Filled.Dialpad,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        size = 64.dp,
    )
    Text("Enter 4-digit code", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    Text(
        text = entered.padEnd(4, '·'),
        color = Color.White,
        fontFamily = FontFamily.Monospace,
        fontSize = 40.sp,
    )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    NumericKeypad(
        value = entered,
        maxLength = 4,
        enabled = !busy,
        submitLabel = "OK",
        submitEnabled = entered.length == 4,
        onValueChange = { entered = it },
        onSubmit = {
            busy = true
            error = null
            scope.launch {
                val ok = onSubmit(entered)
                if (!ok) {
                    error = "Code is invalid or expired."
                    entered = ""
                }
                busy = false
            }
        },
        initialFocusRequester = focusRequester,
    )
    Button(onClick = onCancel, enabled = !busy) { Text("Cancel") }
}

/** Compact digit grid shared by code entry and custom-minutes entry. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumericKeypad(
    value: String,
    maxLength: Int,
    enabled: Boolean,
    submitLabel: String,
    submitEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    initialFocusRequester: FocusRequester? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("⌫", "0", submitLabel),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { label ->
                    val isSubmit = label == submitLabel
                    val isFirstKey = label == "1"
                    Button(
                        onClick = {
                            when {
                                label == "⌫" -> if (value.isNotEmpty()) onValueChange(value.dropLast(1))
                                isSubmit -> onSubmit()
                                value.length < maxLength -> onValueChange(value + label)
                            }
                        },
                        modifier = Modifier.size(width = 84.dp, height = 56.dp).let {
                            if (isFirstKey && initialFocusRequester != null) {
                                it.focusRequester(initialFocusRequester)
                            } else {
                                it
                            }
                        },
                        enabled = enabled && (!isSubmit || submitEnabled),
                    ) { Text(label) }
                }
            }
        }
    }
}
