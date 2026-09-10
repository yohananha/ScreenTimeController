package com.screentime.shared.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) {
    data class Session(val uid: String, val email: String?)

    /**
     * AuthStateListener fires immediately with whatever session is currently
     * active — including a persisted sign-in restored on app cold start, not
     * just a fresh interactive sign-in — so syncing the display name here
     * (rather than only inside signInWithGoogle) also backfills it for
     * members who were already signed in before this shipped, the next time
     * they open the app.
     */
    val currentSession: Flow<Session?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            fa.currentUser?.let { syncDisplayName(it) }
            trySend(fa.currentUser?.let { Session(it.uid, it.email) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun syncDisplayName(user: FirebaseUser) {
        db.collection("users").document(user.uid)
            .set(mapOf("displayName" to user.displayName), SetOptions.merge())
            .addOnFailureListener { Log.e(TAG, "syncDisplayName(${user.uid}) failed", it) }
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    fun signOut() {
        auth.signOut()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
