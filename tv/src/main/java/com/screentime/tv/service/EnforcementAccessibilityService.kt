package com.screentime.tv.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.screentime.shared.format.ClockFormat
import com.screentime.shared.limits.BonusStore
import com.screentime.shared.limits.LimitsProvider
import com.screentime.shared.model.Limits
import com.screentime.shared.time.TrustedClock
import com.screentime.tv.overlay.BlockOverlayController
import com.screentime.tv.time.TvHeartbeat
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
    @Inject lateinit var trustedClock: TrustedClock
    @Inject lateinit var heartbeat: TvHeartbeat

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

        // Anchor the trusted clock (and stamp lastSeen) before anything is
        // evaluated, and floor today's usage with what Room already holds.
        scope.launch {
            heartbeat.beat()
            usageTracker.restoreToday()
        }

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
                val remaining = Duration.between(trustedClock.now(), expiresAt).toMillis()
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
            var ticks = 0
            var lastActiveApp: String? = null
            var lastTickAt = 0L
            minuteTicker().collect {
                // Credit the minute to the app whose window was active at
                // both ends of it — a floor under the event-log count, so
                // time on screen is counted even when the platform's usage
                // events are incomplete. Capped so a stalled tick can't
                // credit a long stretch it didn't observe.
                val activeApp = activeAppPackage()
                val tickAt = SystemClock.elapsedRealtime()
                if (activeApp != null && activeApp == lastActiveApp && lastTickAt != 0L) {
                    usageTracker.creditObservedForeground(
                        pkg = activeApp,
                        millis = (tickAt - lastTickAt).coerceAtMost(MAX_OBSERVED_CREDIT_MILLIS),
                        alwaysCount = currentLimits.value.perApp.keys,
                    )
                }
                lastActiveApp = activeApp
                lastTickAt = tickAt

                // Every HEARTBEAT_EVERY_MINUTES: re-anchor the clock and tell
                // parents enforcement is still running (devices/{id}.lastSeen).
                if (++ticks % HEARTBEAT_EVERY_MINUTES == 0) heartbeat.beat()
                val perPackage = sample()
                if (perPackage != null) usageRecorder.record(trustedClock.today(), perPackage)
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

    /**
     * The package of the application window that has focus right now, or null
     * when the active window isn't an app (screensaver, a system panel, our
     * own block overlay) or windows can't be read. Transient system windows
     * like the volume bar are not application windows, so they don't hide the
     * app playing underneath.
     */
    private fun activeAppPackage(): String? = try {
        windows
            .firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isActive || it.isFocused) }
            ?.root
            ?.packageName
            ?.toString()
    } catch (t: RuntimeException) {
        Log.w(TAG, "Could not read the active window", t)
        null
    }

    private suspend fun evaluate(pkg: String, sampled: Map<String, Long>? = null) {
        val limits = currentLimits.value
        val perAppLimit = limits.perApp[pkg]

        // Parent-initiated instant lock — absolute override, nothing pierces
        // it, but it self-clears at midnight rather than staying locked
        // indefinitely (mirrors allowAllDayDate below).
        // "Today" and "now" are trusted time (TrustedClock), not the device
        // clock — moving the TV's date used to lift an instant lock, re-open
        // allowed hours, or start a fresh daily quota.
        val today = trustedClock.today().toString()
        if (limits.instantLockedDate == today) {
            Log.d(TAG, "Eval $pkg: instant lock active")
            block(pkg, BlockReason.InstantLocked)
            return
        }

        // Parent toggled "Allow all day" for today, or turned on "keep
        // allowing every day" — nothing blocks.
        if (limits.allowAllDayIndefinite || limits.allowAllDayDate == today) {
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
        val now = trustedClock.localNow()
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
        // A parent's same-day bonus raises both caps (stale-dated bonus = 0).
        val bonusMillis = limits.bonusFor(today) * 60_000L
        val perAppExceeded = perAppLimit != null &&
            usedMillis >= perAppLimit.dailyLimitMinutes * 60_000L + bonusMillis

        val totalMillis = perPackage.values.sum()
        val overallExceeded = limits.overallDailyMinutes != Limits.UNLIMITED &&
            totalMillis >= limits.overallDailyMinutes * 60_000L + bonusMillis

        Log.d(
            TAG,
            "Eval $pkg: used=${usedMillis}ms total=${totalMillis}ms " +
                "perAppLimit=${perAppLimit?.dailyLimitMinutes}min overall=${limits.overallDailyMinutes}min",
        )

        if (perAppExceeded || overallExceeded) {
            // Only an app-specific block offers "Go home": the child can still
            // use other apps. An exhausted overall limit leaves nowhere to go.
            block(pkg, BlockReason.DailyLimitReached, canGoHome = perAppExceeded && !overallExceeded)
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
    private fun block(pkg: String, reason: BlockReason, nextWindow: String? = null, canGoHome: Boolean = false) {
        if (sentHomeFor != pkg && pkg != homeLauncherPackage) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            sentHomeFor = pkg
        }
        overlay.onGoHome = ::goHome
        overlay.show(pkg, reason, nextWindow, canGoHome)
    }

    // The overlay's "Go home" button: leave the blocked app for the launcher
    // and drop the overlay. The foreground package is moved to the launcher
    // explicitly because block() suppresses the launcher's window event while
    // sentHomeFor is set, so nothing else would clear the block.
    private fun goHome() {
        sentHomeFor = null
        performGlobalAction(GLOBAL_ACTION_HOME)
        homeLauncherPackage?.let { foregroundPackage.value = it }
        overlay.hide()
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
        private const val HEARTBEAT_EVERY_MINUTES = 5
        private const val MAX_OBSERVED_CREDIT_MILLIS = 2 * 60_000L
    }
}

enum class BlockReason { DailyLimitReached, OutsideHours, InstantLocked }
