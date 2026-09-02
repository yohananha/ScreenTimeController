package com.screentime.tv.usage

import android.util.Log
import com.screentime.shared.firestore.FirestoreRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors the apps that can accrue screen time to /families/{id}/tvApps so the
 * mobile app can offer them as limit targets.
 *
 * The list comes from [CountablePackages], so the parent is never offered a
 * limit on something that will never accrue time — the home launcher, this
 * app, or a system package with no launcher entry.
 */
@Singleton
class InstalledAppsReporter @Inject constructor(
    private val countablePackages: CountablePackages,
    private val firestore: FirestoreRepository,
) {
    suspend fun sync(familyId: String) {
        val apps = countablePackages.labels()
        Log.d(TAG, "Syncing ${apps.size} TV apps for family $familyId")
        firestore.syncInstalledApps(familyId, apps)
    }

    companion object {
        private const val TAG = "InstalledAppsReporter"
    }
}
