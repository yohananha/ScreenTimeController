package com.screentime.tv.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The set of packages whose foreground time counts as screen use, plus their
 * labels for the parent-facing limits list.
 *
 * A package qualifies by exposing a launcher activity — the only signal that
 * survives across OEM Android TV builds, where a hand-maintained denylist of
 * system packages would rot. The resolved home launcher and this app are
 * removed: both are launcher-visible (see the LEANBACK_LAUNCHER filter in the
 * TV manifest) but neither is something a child chose to watch.
 *
 * The `queryIntentActivities` scan is cached because [packages] is read on
 * every enforcement evaluation, which runs once a minute plus on every app
 * switch. [invalidate] is called when a package is installed or removed; the
 * TTL is only a backstop for a missed broadcast.
 */
@Singleton
class CountablePackages @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private class Snapshot(val labels: Map<String, String>, val builtAt: Long)

    @Volatile private var snapshot: Snapshot? = null

    // Resolved separately from the snapshot: the accessibility service reads
    // this on every window-state change, and resolveActivity is far cheaper
    // than the MATCH_ALL scan below.
    @Volatile private var homeLauncher: String? = null
    @Volatile private var homeLauncherResolved = false

    /** Package name to launcher label, for the apps that count. */
    fun labels(): Map<String, String> {
        val existing = snapshot
        if (existing != null && SystemClock.elapsedRealtime() - existing.builtAt < TTL_MILLIS) {
            return existing.labels
        }
        return build().also { snapshot = it }.labels
    }

    /** The packages allowed to accrue screen time. */
    fun packages(): Set<String> = labels().keys

    /** The device's home app, which is never counted and never blocked. */
    fun homeLauncherPackage(): String? {
        if (!homeLauncherResolved) {
            homeLauncher = context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0,
            )?.activityInfo?.packageName
            homeLauncherResolved = true
        }
        return homeLauncher
    }

    fun invalidate() {
        snapshot = null
        homeLauncherResolved = false
    }

    private fun build(): Snapshot {
        val pm = context.packageManager
        val home = homeLauncherPackage()

        // Most TV apps expose CATEGORY_LEANBACK_LAUNCHER, but sideloaded or
        // phone-ported apps often only declare the standard CATEGORY_LAUNCHER
        // (or lack a leanback banner entirely). Query both and merge so the
        // limits list matches what's actually installed on the device.
        @Suppress("DEPRECATION")
        val labels = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            .flatMap { category ->
                val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            .associate { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filterKeys { it != context.packageName && it != home }

        return Snapshot(labels, SystemClock.elapsedRealtime())
    }

    companion object {
        private val TTL_MILLIS = 60 * 60 * 1000L
    }
}
