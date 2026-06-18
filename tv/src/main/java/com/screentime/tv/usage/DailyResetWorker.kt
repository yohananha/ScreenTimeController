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
 *
 * Each successful reset stamps `last_reset_date`; [runIfOverdue] is called at
 * app startup so a device that was off across the scheduled time still gets
 * the reset on next launch (otherwise WorkManager would wait for the next
 * scheduled tick, leaving yesterday's bonus minutes alive).
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
        markReset(applicationContext, LocalDate.now(ZoneId.systemDefault()))
        return Result.success()
    }

    companion object {
        private const val TAG = "DailyResetWorker"
        const val UNIQUE_NAME = "daily-reset"
        private const val PREFS_NAME = "daily_reset"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"

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

        /**
         * Clears the bonus store immediately if we never recorded a reset for
         * today (TV was off across midnight, or this is the very first run).
         * Safe to call multiple times: the second call is a no-op once the
         * date has been stamped. Call from `Application.onCreate`.
         */
        fun runIfOverdue(context: Context, bonusStore: BonusStore) {
            val today = LocalDate.now(ZoneId.systemDefault())
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastReset = prefs.getString(KEY_LAST_RESET_DATE, null)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

            if (lastReset == null || lastReset.isBefore(today)) {
                Log.i(TAG, "Overdue daily reset (last=$lastReset, today=$today); clearing bonus store.")
                bonusStore.clear()
                markReset(context, today)
            }
        }

        private fun markReset(context: Context, date: LocalDate) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_RESET_DATE, date.toString())
                .apply()
        }
    }
}
