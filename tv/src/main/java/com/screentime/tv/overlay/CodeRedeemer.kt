package com.screentime.tv.overlay

import com.google.firebase.functions.FirebaseFunctionsException
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.limits.BonusStore
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CodeRedeemer @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
    private val bonusStore: BonusStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Live lockout config + state, shared with the block overlay UI. */
    val lockout: StateFlow<LockoutSettings> = familyIdProvider.familyId
        .flatMapLatest { id ->
            if (id == null) flowOf(LockoutSettings()) else firestore.lockoutFlow(id)
        }
        .stateIn(scope, SharingStarted.Eagerly, LockoutSettings())

    /**
     * Submits [code] to the server for validation. Returns true if the code
     * was accepted and bonus minutes were added for [currentlyBlocked].
     *
     * Failure tracking and lockout triggering are fully server-side: the
     * [redeemCode] Cloud Function increments the failure counter and sets
     * [LockoutSettings.locked] when the threshold is reached. The UI reacts
     * via [lockout] which observes Firestore in real time.
     */
    suspend fun redeem(code: String, currentlyBlocked: String?): Boolean {
        val familyId = familyIdProvider.familyId.value ?: return false
        val minutes = try {
            firestore.redeemCode(familyId, code)
        } catch (e: FirebaseFunctionsException) {
            // NOT_FOUND = wrong/expired code; FAILED_PRECONDITION = locked.
            // In both cases the server has already updated Firestore, so the
            // lockout StateFlow will reflect the new state automatically.
            return false
        }
        if (currentlyBlocked != null) {
            bonusStore.addBonus(currentlyBlocked, minutes * 60_000L)
        }
        return true
    }

    /**
     * Clears a TIMER lockout. Called by [LockedView] once its monotonic-clock
     * countdown reaches zero; the local wall-clock is intentionally NOT
     * consulted here, so a child editing the device clock cannot bypass the
     * timer (`SystemClock.elapsedRealtime()` is monotonic from boot).
     */
    suspend fun clearExpiredLockout() {
        val current = lockout.value
        if (!current.locked || current.mode != LockoutMode.TIMER) return
        val familyId = familyIdProvider.familyId.value ?: return
        firestore.clearLockout(familyId)
    }
}
