package com.screentime.tv.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.screentime.shared.limits.BonusStore
import com.screentime.shared.limits.LimitsProvider
import com.screentime.shared.model.Limits
import com.screentime.shared.room.UsageRepository
import com.screentime.tv.overlay.BlockOverlayController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class EnforcementAccessibilityService : AccessibilityService() {

    @Inject lateinit var limitsProvider: LimitsProvider
    @Inject lateinit var usage: UsageRepository
    @Inject lateinit var overlay: BlockOverlayController
    @Inject lateinit var bonusStore: BonusStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val foregroundPackage = MutableStateFlow<String?>(null)
    private val currentLimits = MutableStateFlow(Limits())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connected.")

        // Re-evaluate when limits change — so a tightened limit (or "Block
        // everything") takes effect immediately without an app switch.
        scope.launch {
            limitsProvider.limits().collectLatest {
                currentLimits.value = it
                foregroundPackage.value?.let { pkg -> evaluate(pkg) }
            }
        }
        scope.launch {
            foregroundPackage.collectLatest { pkg ->
                if (pkg == null) return@collectLatest
                evaluate(pkg)
            }
        }
        // Re-evaluate when a bonus is granted — so dismiss happens
        // immediately after a code redemption or an approved request — and
        // again when that bonus expires, so the block returns without
        // needing an app switch.
        scope.launch {
            bonusStore.bonuses.collectLatest { expiries ->
                val pkg = foregroundPackage.value ?: return@collectLatest
                evaluate(pkg)
                val expiresAt = expiries[pkg] ?: return@collectLatest
                val remaining = Duration.between(Instant.now(), expiresAt).toMillis()
                if (remaining > 0) {
                    delay(remaining)
                    foregroundPackage.value?.let { evaluate(it) }
                }
            }
        }
        // Tick every minute so time-frame windows activate/deactivate without
        // needing an app switch event.
        scope.launch {
            minuteTicker().collect {
                foregroundPackage.value?.let { pkg -> evaluate(pkg) }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        foregroundPackage.value = pkg
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        overlay.hide()
    }

    private suspend fun evaluate(pkg: String) {
        val limits = currentLimits.value
        val perAppLimit = limits.perApp[pkg]

        // Parent-initiated instant lock — absolute override, nothing pierces it.
        if (limits.instantLocked) {
            Log.d(TAG, "Eval $pkg: instant lock active")
            overlay.show(pkg, BlockReason.InstantLocked)
            return
        }

        // Parent toggled "Allow all day" for today — nothing blocks.
        if (limits.allowAllDayDate == LocalDate.now().toString()) {
            Log.d(TAG, "Eval $pkg: allow-all-day active")
            overlay.hide()
            return
        }

        // "Always allow" — this app is exempt from per-app and overall limits.
        if (perAppLimit?.dailyLimitMinutes == Limits.UNLIMITED) {
            overlay.hide()
            return
        }

        // A code redemption or approved request grants N minutes from now,
        // bypassing both daily limits and time-frame schedule.
        if (bonusStore.isActive(pkg)) {
            Log.d(TAG, "Eval $pkg: bonus active until ${bonusStore.expiryFor(pkg)}")
            overlay.hide()
            return
        }

        // Outside allowed hours — block even if daily quota hasn't been used.
        val now = LocalDateTime.now()
        if (!limits.timeFrame.isAllowedAt(now)) {
            val nextLabel = limits.timeFrame.nextAllowedMinute(now)
                ?.let { java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(it) }
            Log.d(TAG, "Eval $pkg: outside time-frame schedule, next=$nextLabel")
            overlay.show(pkg, BlockReason.OutsideHours, nextLabel)
            return
        }

        val usedMillis = usage.millisForToday(pkg)
        val perAppExceeded = perAppLimit != null &&
            usedMillis >= perAppLimit.dailyLimitMinutes * 60_000L

        val totalMillis = usage.totalMillisForToday()
        val overallExceeded = limits.overallDailyMinutes != Limits.UNLIMITED &&
            totalMillis >= limits.overallDailyMinutes * 60_000L

        Log.d(
            TAG,
            "Eval $pkg: used=${usedMillis}ms total=${totalMillis}ms " +
                "perAppLimit=${perAppLimit?.dailyLimitMinutes}min overall=${limits.overallDailyMinutes}min",
        )

        if (perAppExceeded || overallExceeded) {
            overlay.show(pkg, BlockReason.DailyLimitReached)
        } else {
            overlay.hide()
        }
    }

    private fun minuteTicker() = flow<Unit> {
        while (true) {
            val now = LocalDateTime.now()
            val secondsUntilNextMinute = 60L - now.second
            delay(secondsUntilNextMinute * 1_000L)
            emit(Unit)
        }
    }

    companion object {
        private const val TAG = "EnforcementSvc"
    }
}

enum class BlockReason { DailyLimitReached, OutsideHours, InstantLocked }
