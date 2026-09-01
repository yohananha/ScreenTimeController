package com.screentime.shared.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateCheckResult {
    data class Available(val versionName: String, val downloadUrl: String, val sizeBytes: Long) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Polls this repo's GitHub Releases for a build newer than [currentVersionName].
 * Releases are published manually (see release notes), one APK asset per app
 * (e.g. "tv-debug.apk", "mobile-debug.apk") attached to a "vX.Y.Z[-suffix]"
 * tag — [assetName] picks the asset for the calling app.
 */
class UpdateChecker(
    private val assetName: String,
    private val repoSlug: String = "yohananha/ScreenTimeController",
) {
    suspend fun check(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL("https://api.github.com/repos/$repoSlug/releases/latest")
                .openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val latestVersion = json.getString("tag_name").removePrefix("v").substringBefore("-")

            val assets = json.getJSONArray("assets")
            var downloadUrl: String? = null
            var sizeBytes = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name") == assetName) {
                    downloadUrl = asset.getString("browser_download_url")
                    sizeBytes = asset.getLong("size")
                    break
                }
            }

            when {
                downloadUrl == null -> UpdateCheckResult.Error("No $assetName in the latest release")
                isNewer(latestVersion, currentVersionName) ->
                    UpdateCheckResult.Available(latestVersion, downloadUrl, sizeBytes)
                else -> UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed", e)
            UpdateCheckResult.Error(e.message ?: "Update check failed")
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    companion object {
        private const val TAG = "UpdateChecker"
    }
}
