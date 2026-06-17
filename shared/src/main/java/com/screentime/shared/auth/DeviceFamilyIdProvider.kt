package com.screentime.shared.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.screentime.shared.firestore.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TV-side FamilyIdProvider. The TV signs in anonymously to get a stable
 * uid (which we call deviceId), then watches /devices/{deviceId}.familyId.
 *
 * Pre-pairing this emits null and the pairing screen is shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DeviceFamilyIdProvider @Inject constructor(
    private val firestore: FirestoreRepository,
    private val auth: FirebaseAuth,
) : FamilyIdProvider {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val deviceIdState = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> get() = deviceIdState

    /** Set if anonymous sign-in fails (e.g. Anonymous auth disabled for this Firebase project). */
    private val authErrorState = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> get() = authErrorState

    init {
        scope.launch { ensureAnonymousAuth() }
    }

    override val familyId: StateFlow<String?> = deviceIdState
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else firestore.deviceFamilyFlow(id)
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private suspend fun ensureAnonymousAuth() {
        try {
            val current = auth.currentUser
            val uid = current?.uid ?: auth.signInAnonymously().await().user?.uid
            deviceIdState.value = uid
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            authErrorState.value = e.message ?: "Sign-in failed"
        }
    }

    private companion object {
        const val TAG = "DeviceFamilyIdProvider"
    }
}
