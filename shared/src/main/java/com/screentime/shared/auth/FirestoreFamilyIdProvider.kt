package com.screentime.shared.auth

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves familyId from /users/{uid}.familyId, refreshed by the auth state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreFamilyIdProvider @Inject constructor(
    authRepository: AuthRepository,
    private val db: FirebaseFirestore,
) : FamilyIdProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val familyId: StateFlow<String?> = authRepository.currentSession
        .flatMapLatest { session ->
            if (session == null) flowOf(null) else userFamilyIdFlow(session.uid)
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private fun userFamilyIdFlow(uid: String): Flow<String?> = callbackFlow {
        val ref = db.collection("users").document(uid)
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "userFamilyIdFlow($uid) listener failed", error)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.getString("familyId"))
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val TAG = "FirestoreFamilyIdProvider"
    }
}
