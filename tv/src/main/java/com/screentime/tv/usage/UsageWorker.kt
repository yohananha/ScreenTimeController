package com.screentime.tv.usage

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.limits.LimitsProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Backstop sampler. The accessibility service records a sample every minute
 * while it is running (see EnforcementAccessibilityService's minute ticker);
 * this covers the case where it isn't — permission not granted yet, service
 * disabled, or the process restarted between ticks.
 *
 * WorkManager's periodic floor is 15 minutes, which is why this cannot itself
 * be the per-minute path.
 */
@HiltWorker
class UsageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val tracker: UsageTracker,
    private val recorder: UsageRecorder,
    private val familyIdProvider: FamilyIdProvider,
    private val installedAppsReporter: InstalledAppsReporter,
    private val limitsProvider: LimitsProvider,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!UsagePermission.isGranted(applicationContext)) {
            Log.w(TAG, "Skipping usage sample — PACKAGE_USAGE_STATS not granted.")
            return Result.success()
        }

        val today = LocalDate.now()
        // FirestoreLimitsProvider awaits its first snapshot, which never
        // arrives offline. A worker must not hang on that; missing the
        // always-count set for one sample only risks under-counting a
        // non-launcher app that has an explicit limit, and live enforcement
        // uses its own already-collected limits either way.
        val alwaysCount = withTimeoutOrNull(LIMITS_TIMEOUT_MILLIS) {
            limitsProvider.limits().first()
        }?.perApp?.keys.orEmpty()

        val perPackage = tracker.millisPerPackage(date = today, alwaysCount = alwaysCount)
        Log.d(TAG, "Usage sample $today: $perPackage")

        if (!recorder.record(today, perPackage)) return Result.retry()

        val familyId = familyIdProvider.familyId.value
        if (familyId != null) {
            try {
                installedAppsReporter.sync(familyId)
            } catch (t: Throwable) {
                Log.w(TAG, "Installed-apps sync failed; will retry next cycle", t)
            }
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "UsageWorker"
        private const val LIMITS_TIMEOUT_MILLIS = 5_000L
        const val UNIQUE_NAME = "usage-sampler"

        fun schedule(context: Context, intervalMinutes: Long) {
            val request = PeriodicWorkRequestBuilder<UsageWorker>(
                intervalMinutes, TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
