package com.screentime.tv.overlay

import android.os.SystemClock
import android.util.Log
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.limits.BonusStore
import com.screentime.shared.model.TimeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestController @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
    private val bonusStore: BonusStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watcherJob: Job? = null
    private var pendingRequestId: String? = null
    private var lastSubmitAt: Long? = null

    /** Monotonic clock (not settable by the child); swapped in tests. */
    internal var elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() }

    private val _requestStatus = MutableStateFlow<TimeRequest.Status?>(null)
    private val _approvedMinutes = MutableStateFlow<Int?>(null)

    /** Status of the most recently submitted request, observed by the block overlay so it can react to a parent's decision. */
    val requestStatus: StateFlow<TimeRequest.Status?> = _requestStatus.asStateFlow()
    
    /** Minutes granted in the most recently approved request. */
    val approvedMinutes: StateFlow<Int?> = _approvedMinutes.asStateFlow()

    /**
     * Sends a time request to the parents. Limits, so a child can't flood
     * the parents' phones with notifications:
     *  - while one is still pending, asking again reuses it (no new request);
     *  - otherwise at most one per [SUBMIT_COOLDOWN_MS] (the server rate-limits
     *    the same window — see onNewTimeRequest).
     * Returns the request id, or null if nothing was (re)submitted.
     */
    suspend fun submit(appPackage: String, requestedMinutes: Int): String? {
        val familyId = familyIdProvider.familyId.value ?: return null
        pendingRequestId?.let { pending ->
            if (_requestStatus.value == TimeRequest.Status.Pending) return pending
        }
        val now = elapsedRealtime()
        lastSubmitAt?.let { if (now - it < SUBMIT_COOLDOWN_MS) return null }

        _requestStatus.value = TimeRequest.Status.Pending
        _approvedMinutes.value = null
        val id = firestore.createRequest(familyId, appPackage, requestedMinutes)
        pendingRequestId = id
        lastSubmitAt = now
        watch(id, appPackage)
        return id
    }

    private fun watch(requestId: String, appPackage: String) {
        val familyId = familyIdProvider.familyId.value ?: return
        watcherJob?.cancel()
        watcherJob = scope.launch {
            // Stop at the first decision: a later snapshot re-emitting the same
            // Approved doc (metadata/cache refresh) must not add the bonus twice
            // or resurrect a decision that clearDecision() already consumed.
            val request = firestore.requestFlow(familyId, requestId)
                .filterNotNull()
                .firstOrNull { it.status != TimeRequest.Status.Pending }
                ?: return@launch
            when (request.status) {
                TimeRequest.Status.Approved -> {
                    // Apply the bonus BEFORE flipping requestStatus so the
                    // overlay (which dismisses on status=Approved) can't
                    // momentarily race with the accessibility-service tick
                    // and re-block the app between the dismiss and the
                    // bonus showing up in BonusStore.
                    val granted = request.approvedMinutes ?: request.requestedMinutes
                    bonusStore.addBonus(granted * 60_000L)
                    _approvedMinutes.value = granted
                    _requestStatus.value = request.status
                    Log.i(TAG, "Request $requestId approved for $granted min (device-wide), triggered by $appPackage")
                }
                TimeRequest.Status.Denied -> {
                    _requestStatus.value = request.status
                    Log.i(TAG, "Request $requestId denied")
                }
                TimeRequest.Status.Pending -> Unit
            }
        }
    }

    /**
     * Forgets the last decision once the overlay that displayed it is gone.
     * Without this, the next time the overlay is shown (e.g. when the granted
     * bonus runs out) it would read the stale Approved status and open on the
     * "You got N more minutes" screen instead of the block screen. A request
     * still awaiting a decision is left alone.
     */
    fun clearDecision() {
        if (_requestStatus.value == TimeRequest.Status.Pending) return
        _requestStatus.value = null
        _approvedMinutes.value = null
    }

    private companion object {
        const val TAG = "RequestController"
        const val SUBMIT_COOLDOWN_MS = 60_000L
    }
}
