package com.screentime.tv.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.screentime.shared.firestore.FirestoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enumerates the apps shown on the TV's launcher and mirrors them to
 * /families/{id}/tvApps so the mobile app can offer them as limit targets.
 */
@Singleton
class InstalledAppsReporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirestoreRepository,
) {
    suspend fun sync(familyId: String) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

        @Suppress("DEPRECATION")
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .associate { it.activityInfo.packageName to it.loadLabel(pm).toString() }

        Log.d(TAG, "Syncing ${apps.size} TV apps for family $familyId")
        firestore.syncInstalledApps(familyId, apps)
    }

    companion object {
        private const val TAG = "InstalledAppsReporter"
    }
}
