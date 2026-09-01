package com.screentime.shared.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri

/**
 * Downloads the update APK via the system [DownloadManager] (no storage
 * permission needed when the destination is app-scoped external storage) and
 * hands the result to the system installer via the download provider's own
 * content:// URI — no FileProvider of our own required.
 */
class UpdateInstaller(private val context: Context) {

    private val downloadManager: DownloadManager
        get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueueDownload(downloadUrl: String, fileName: String): Long {
        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle(fileName)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        return downloadManager.enqueue(request)
    }

    /** True once the download finished successfully and can be installed. */
    fun isDownloadComplete(downloadId: Long): Boolean {
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return false
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            return status == DownloadManager.STATUS_SUCCESSFUL
        }
    }

    /** Whether the OS will let this app prompt the package installer at all. */
    fun canRequestInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Deep-links to the "Install unknown apps" toggle for this app. */
    fun requestInstallPermissionIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Builds the install intent for a finished download, or null if it isn't done yet. */
    fun installIntent(downloadId: Long): Intent? {
        if (!isDownloadComplete(downloadId)) return null
        val uri: Uri = downloadManager.getUriForDownloadedFile(downloadId) ?: return null
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
