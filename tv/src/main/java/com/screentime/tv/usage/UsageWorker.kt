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
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.room.AppDatabase
import com.screentime.shared.room.UsageEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class UsageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val tracker: UsageTracker,
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
    private val installedAppsReporter: InstalledAppsReporter,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!UsagePermission.isGranted(applicationContext)) {
            Log.w(TAG, "Skipping usage sample — PACKAGE_USAGE_STATS not granted.")
            return Result.success()
        }

        val today = LocalDate.now()
        val perPackage = tracker.millisPerPackage(today)
        Log.d(TAG, "Usage sample $today: $perPackage")

        val rows = perPackage.map { (pkg, millis) ->
            UsageEntity(date = today.toString(), packageName = pkg, millis = millis)
        }
        if (rows.isNotEmpty()) {
            AppDatabase.get(applicationContext).usageDao().upsertAll(rows)
        }

        val familyId = familyIdProvider.familyId.value
        if (familyId != null && perPackage.isNotEmpty()) {
            try {
                firestore.recordUsage(familyId, today, perPackage)
            } catch (t: Throwable) {
                Log.w(TAG, "Firestore upload failed; will retry next cycle", t)
                return Result.retry()
            }
        }

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
