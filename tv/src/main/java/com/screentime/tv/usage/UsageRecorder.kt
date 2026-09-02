package com.screentime.tv.usage

import android.util.Log
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.room.AppDatabase
import com.screentime.shared.room.UsageEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists a usage sample to Room and Firestore.
 *
 * Shared by the two things that sample: the accessibility service's minute
 * ticker, which is what makes the parent's view roughly a minute fresh, and
 * [UsageWorker], which is the 15-minute backstop for when the accessibility
 * service isn't running.
 *
 * A sample identical to the last one written is dropped. Now that idle time on
 * the launcher and in the screensaver no longer accrues (see [UsageAccounting]),
 * a TV sitting on the home screen produces the same map every minute, so this
 * turns "write every minute" into "write every minute *while something is
 * actually being watched*" — which is what keeps a per-minute cadence from
 * costing 1440 Firestore writes per TV per day.
 */
@Singleton
class UsageRecorder @Inject constructor(
    private val database: AppDatabase,
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) {
    private val mutex = Mutex()
    private var lastDate: String? = null
    private var lastUploaded: Map<String, Long>? = null

    /**
     * @return false if the Firestore upload failed and is worth retrying. Room
     *   is written regardless — it is local and cannot fail for network reasons.
     */
    suspend fun record(date: LocalDate, perPackage: Map<String, Long>): Boolean = mutex.withLock {
        val key = date.toString()
        if (key == lastDate && perPackage == lastUploaded) return@withLock true

        val dao = database.usageDao()

        // Packages recorded earlier today that no longer count — before the
        // launcher and other system chrome were excluded, they had real rows.
        // Both stores merge rather than replace, so they have to be deleted
        // explicitly or they keep inflating the day's total.
        val stale = dao.loadForDate(key)
            .map { it.packageName }
            .filterNot { it in perPackage }
        if (stale.isNotEmpty()) {
            Log.d(TAG, "Dropping ${stale.size} no-longer-counted packages: $stale")
            dao.deleteForDate(key, stale)
        }

        val rows = perPackage.map { (pkg, millis) ->
            UsageEntity(date = key, packageName = pkg, millis = millis)
        }
        if (rows.isNotEmpty()) dao.upsertAll(rows)

        val familyId = familyIdProvider.familyId.value
        if (familyId != null && (perPackage.isNotEmpty() || stale.isNotEmpty())) {
            try {
                firestore.recordUsage(familyId, date, perPackage, stale.toSet())
            } catch (t: Throwable) {
                // Leave the dedupe snapshot untouched so the next tick retries
                // instead of mistaking this sample for already uploaded.
                Log.w(TAG, "Firestore upload failed; will retry next sample", t)
                return@withLock false
            }
        }

        lastDate = key
        lastUploaded = perPackage
        true
    }

    companion object {
        private const val TAG = "UsageRecorder"
    }
}
