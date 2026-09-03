package com.screentime.tv.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.screentime.shared.format.ClockFormat
import com.screentime.shared.limits.BonusStore
import com.screentime.shared.limits.LimitsProvider
import com.screentime.shared.model.Limits
import com.screentime.tv.overlay.BlockOverlayController
import com.screentime.tv.usage.CountablePackages
import com.screentime.tv.usage.UsagePermission
import com.screentime.tv.usage.UsageRecorder
import com.screentime.tv.usage.UsageTracker
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
    @Inject lateinit var usageTracker: UsageTracker
    @Inject lateinit var countablePackages: CountablePackages
    @Inject lateinit var usageRecorder: UsageRecorder
    @Inject lateinit var overlay: BlockOverlayController
    @Inject lateinit var bonusStore: BonusStore
    @Inject lateinit var clockFormat: ClockFormat

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val foregroundPackage = MutableStateFlow<String?>(null)
    private val currentLimits = MutableStateFlow(Limits())

    // The package we last force-backgrounded via GLOBAL_ACTION_HOME because it
    // was blocked. While set, the launcher's own window-state event (a direct
    // side effect of that action) is suppressed in onAccessibilityEvent so it
    // doesn't overwrite foregroundPackage and cause the launcher — which is
    // never itself over-limit — to evaluate as unblocked and dismiss the
    // overlay for the app we just paused.
    private var sentHomeFor: String? = null

    private val homeLauncherPackage: String?
        get() = countablePackages.homeLauncherPackage()

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
            bonusStore.bonuses.collectLatest { expiresAt ->
                val pkg = foregroundPackage.value ?: return@collectLatest
                evaluate(pkg)
                expiresAt ?: return@collectLatest
                val remaining = Duration.between(Instant.now(), expiresAt).toMillis()
                if (remaining > 0) {
                    delay(remaining)
                    foregroundPackage.value?.let { evaluate(it) }
                }
            }
        }
        // Tick every minute so time-frame windows activate/deactivate without
        // needing an app switch event, and so the parent's view of usage is
        // roughly a minute fresh. WorkManager's periodic floor is 15 minutes,
        // so UsageWorker cannot do this — it stays on as the backstop for when
        // this service isn't running.
        scope.launch {
            minuteTicker().collect {
                val perPackage = sample()
                if (perPackage != null) usageRecorder.record(LocalDate.now(), perPackage)
                foregroundPackage.value?.let { pkg -> evaluate(pkg, perPackage) }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == homeLauncherPackage && sentHomeFor != null) {
            Log.d(TAG, "Ignoring launcher window from our own block of $sentHomeFor")
            return
        }
        foregroundPackage.value = pkg
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        overlay.hide()
    }

    /**
     * One usage read, shared by the minute ticker between recording and
     * evaluation so the tick costs a single UsageStatsManager query rather
     * than two. Null when usage access hasn't been granted.
     */
    private fun sample(): Map<String, Long>? {
        if (!UsagePermission.isGranted(this)) return null
        return usageTracker.millisPerPackage(alwaysCount = currentLimits.value.perApp.keys)
    }

    private suspend fun evaluate(pkg: String, sampled: Map<String, Long>? = null) {
        val limits = currentLimits.value
        val perAppLimit = limits.perApp[pkg]

        // Parent-initiated instant lock — absolute override, nothing pierces it.
        if (limits.instantLocked) {
            Log.d(TAG, "Eval $pkg: instant lock active")
            block(pkg, BlockReason.InstantLocked)
            return
        }

        // Parent toggled "Allow all day" for today — nothing blocks.
        if (limits.allowAllDayDate == LocalDate.now().toString()) {
            Log.d(TAG, "Eval $pkg: allow-all-day active")
            unblock()
            return
        }

        // The home launcher is never itself blocked outside an instant lock —
        // it's what's already on screen after backgrounding a blocked app,
        // not something the child chose to open, so it must not trigger its
        // own bonus/request flow on top of whatever app sent them here.
        if (pkg == homeLauncherPackage) {
            unblock()
            return
        }

        // "Always allow" — this app is exempt from per-app and overall limits.
        if (perAppLimit?.dailyLimitMinutes == Limits.UNLIMITED) {
            unblock()
            return
        }

        // A code redemption or approved request grants N minutes from now,
        // device-wide, bypassing both daily limits and time-frame schedule.
        if (bonusStore.isActive()) {
            Log.d(TAG, "Eval $pkg: device bonus active until ${bonusStore.expiryFor()}")
            unblock()
            return
        }

        // Outside allowed hours — block even if daily quota hasn't been used.
        val now = LocalDateTime.now()
        if (!limits.timeFrame.isAllowedAt(now)) {
            val nextLabel = limits.timeFrame.nextAllowedMinute(now)
                ?.let { clockFormat.timeOfDay(it.toLocalTime()) }
            Log.d(TAG, "Eval $pkg: outside time-frame schedule, next=$nextLabel")
            block(pkg, BlockReason.OutsideHours, nextLabel)
            return
        }

        // Computed live from UsageStatsManager rather than read back from the
        // Room cache: the cache is only ever as fresh as the last sample, and
        // an app switch has to be evaluated against the current second, not
        // the last tick. On the minute ticker [sampled] is that tick's own
        // read, so the cadence costs one query rather than two.
        val perPackage = sampled
            ?: usageTracker.millisPerPackage(alwaysCount = limits.perApp.keys)
        val usedMillis = perPackage[pkg] ?: 0L
        val perAppExceeded = perAppLimit != null &&
            usedMillis >= perAppLimit.dailyLimitMinutes * 60_000L

        val totalMillis = perPackage.values.sum()
        val overallExceeded = limits.overallDailyMinutes != Limits.UNLIMITED &&
            totalMillis >= limits.overallDailyMinutes * 60_000L

        Log.d(
            TAG,
            "Eval $pkg: used=${usedMillis}ms total=${totalMillis}ms " +
                "perAppLimit=${perAppLimit?.dailyLimitMinutes}min overall=${limits.overallDailyMinutes}min",
        )

        if (perAppExceeded || overallExceeded) {
            block(pkg, BlockReason.DailyLimitReached)
        } else {
            unblock()
        }
    }

    // Backgrounds the blocked app (so playback actually pauses instead of
    // just being visually covered by the overlay) before showing the block
    // screen. Only fires once per block activation — the minute-ticker
    // re-evaluates the same blocked pkg every 60s, and repeating
    // GLOBAL_ACTION_HOME on every tick would be redundant and could yank the
    // user out of the launcher's own menus if they're navigating it.
    private fun block(pkg: String, reason: BlockReason, nextWindow: String? = null) {
        if (sentHomeFor != pkg && pkg != homeLauncherPackage) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            sentHomeFor = pkg
        }
        overlay.show(pkg, reason, nextWindow)
    }

    private fun unblock() {
        sentHomeFor = null
        overlay.hide()
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
