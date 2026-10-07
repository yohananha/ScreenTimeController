package com.screentime.mobile.fcm

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves this install's FCM token to users/{uid}/private/push — the same doc
 * the web app writes and the onNewTimeRequest function reads. That doc is
 * readable only by its own user (see firestore.rules); tokens used to sit on
 * the family doc, where every member and the TV could read them and nothing
 * ever removed them.
 *
 * onNewToken alone isn't enough: it fires once per install, often before
 * sign-in. So this also runs on every signed-in launch with the current
 * token (arrayUnion makes that a no-op when it's already saved), and
 * [unregister] runs on sign-out so the phone stops getting the family's
 * notifications.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val messaging: FirebaseMessaging get() = FirebaseMessaging.getInstance()

    suspend fun registerCurrentToken(uid: String) {
        val token = currentToken() ?: return
        register(uid, token)
    }

    suspend fun register(uid: String, token: String) {
        try {
            pushDoc(uid).set(mapOf(FIELD_TOKENS to FieldValue.arrayUnion(token)), SetOptions.merge()).await()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register FCM token", t)
        }
    }

    /**
     * Removes this install's token from [uid]'s push doc, then invalidates it
     * with FCM. Must run while [uid] is still signed in — the write needs
     * their credentials. Best-effort: never throws, so sign-out can't be
     * blocked by a network failure.
     */
    suspend fun unregister(uid: String) {
        val token = currentToken() ?: return
        try {
            pushDoc(uid).set(mapOf(FIELD_TOKENS to FieldValue.arrayRemove(token)), SetOptions.merge()).await()
            messaging.deleteToken().await()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to unregister FCM token", t)
        }
    }

    private suspend fun currentToken(): String? = try {
        messaging.token.await()
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to fetch FCM token", t)
        null
    }

    private fun pushDoc(uid: String): DocumentReference =
        firestore.collection("users").document(uid).collection("private").document("push")

    private companion object {
        const val TAG = "PushTokenRegistrar"
        const val FIELD_TOKENS = "tokens"
    }
}
