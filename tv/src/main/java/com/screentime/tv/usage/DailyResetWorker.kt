package com.screentime.tv.usage

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.screentime.shared.limits.BonusStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Runs once a day at the configured reset time and clears the bonus store.
 *
 * The Room usage table is keyed by date so it auto-rolls without explicit
 * archiving — yesterday's rows stay around for history but stop affecting
 * today's enforcement.
 */
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val bonusStore: BonusStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Daily reset: clearing bonus store.")
        bonusStore.clear()
        return Result.success()
    }

    companion object {
        private const val TAG = "DailyResetWorker"
        const val UNIQUE_NAME = "daily-reset"

        fun schedule(context: Context, resetTime: LocalTime = LocalTime.MIDNIGHT) {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            var next = now.with(resetTime)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val initialDelay = Duration.between(now, next).toMinutes().coerceAtLeast(1)

            val request = PeriodicWorkRequestBuilder<DailyResetWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
