package com.screentime.mobile.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.screentime.mobile.MainActivity
import com.screentime.mobile.R
import com.screentime.shared.auth.FamilyIdProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class PushService : FirebaseMessagingService() {

    @Inject lateinit var familyIdProvider: FamilyIdProvider
    @Inject lateinit var firestore: FirebaseFirestore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        ensureChannel()

        val title = message.notification?.title ?: message.data["title"] ?: "Time request"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("openTab", "requests")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            REQ_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            // Wait for sign-in; if the user signs out before a token arrives the
            // refresh will be re-issued on next sign-in via FCM.
            val resolved = familyIdProvider.familyId.filterNotNull().first()
            try {
                firestore.collection("families")
                    .document(resolved)
                    .set(
                        mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                        SetOptions.merge(),
                    )
                    .await()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to register FCM token for family $resolved", t)
            }
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Time requests",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Notifications when the TV asks for more screen time." },
        )
    }

    private companion object {
        const val TAG = "PushService"
        const val CHANNEL_ID = "time_requests"
        const val NOTIFICATION_ID = 1
        const val REQ_OPEN = 0
    }
}
