package com.screentime.tv.usage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * Drops [CountablePackages]' cache when an app is installed, removed or
 * replaced, so a newly sideloaded app starts counting immediately instead of
 * waiting out the cache TTL.
 *
 * Registered at runtime rather than in the manifest: package broadcasts are
 * not on the implicit-broadcast exemption list, so a manifest declaration
 * would never fire on API 26+. The process stays alive for the accessibility
 * service's lifetime, which is exactly as long as any of this matters.
 */
class PackageChangeReceiver(
    private val countablePackages: CountablePackages,
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Package change (${intent?.action}); invalidating countable set")
        countablePackages.invalidate()
    }

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(this, filter)
        }
    }

    companion object {
        private const val TAG = "PackageChangeReceiver"
    }
}
