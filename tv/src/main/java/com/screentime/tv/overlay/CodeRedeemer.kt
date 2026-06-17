package com.screentime.tv.overlay

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
import java.time.Instant
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
    private val recentFailures = mutableListOf<Instant>()

    /** Live lockout config + state, shared with the block overlay. */
    val lockout: StateFlow<LockoutSettings> = familyIdProvider.familyId
        .flatMapLatest { id -> if (id == null) flowOf(LockoutSettings()) else firestore.lockoutFlow(id) }
        .stateIn(scope, SharingStarted.Eagerly, LockoutSettings())

    /**
     * Returns true if [code] granted bonus minutes for [currentlyBlocked].
     * Tracks repeated wrong codes and triggers a lockout per
     * [LockoutSettings] once [LockoutSettings.MAX_ATTEMPTS] failures land
     * within [LockoutSettings.ATTEMPT_WINDOW_SECONDS].
     */
    suspend fun redeem(code: String, currentlyBlocked: String?): Boolean {
        val familyId = familyIdProvider.familyId.value ?: return false
        val minutes = firestore.redeemCode(familyId, code)
        if (minutes == null) {
            recordFailure(familyId)
            return false
        }
        recentFailures.clear()
        if (currentlyBlocked != null) {
            bonusStore.addBonus(currentlyBlocked, minutes * 60_000L)
        }
        return true
    }

    /** Clears a TIMER lockout once its [LockoutSettings.lockedUntil] has passed. */
    suspend fun clearExpiredLockout() {
        val current = lockout.value
        if (!current.locked || current.mode != LockoutMode.TIMER) return
        val until = current.lockedUntil
        if (until != null && Instant.now().isBefore(until)) return
        val familyId = familyIdProvider.familyId.value ?: return
        firestore.clearLockout(familyId)
    }

    private suspend fun recordFailure(familyId: String) {
        val now = Instant.now()
        recentFailures.add(now)
        recentFailures.removeAll { it.isBefore(now.minusSeconds(LockoutSettings.ATTEMPT_WINDOW_SECONDS)) }
        if (recentFailures.size >= LockoutSettings.MAX_ATTEMPTS) {
            recentFailures.clear()
            val config = lockout.value
            firestore.triggerLockout(familyId, config.durationMinutes, config.mode)
        }
    }
}
