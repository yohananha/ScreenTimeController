package com.screentime.tv.overlay

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
import kotlinx.coroutines.flow.collectLatest
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

    private val _requestStatus = MutableStateFlow<TimeRequest.Status?>(null)
    private val _approvedMinutes = MutableStateFlow<Int?>(null)

    /** Status of the most recently submitted request, observed by the block overlay so it can react to a parent's decision. */
    val requestStatus: StateFlow<TimeRequest.Status?> = _requestStatus.asStateFlow()
    
    /** Minutes granted in the most recently approved request. */
    val approvedMinutes: StateFlow<Int?> = _approvedMinutes.asStateFlow()

    suspend fun submit(appPackage: String, requestedMinutes: Int): String? {
        val familyId = familyIdProvider.familyId.value ?: return null
        _requestStatus.value = TimeRequest.Status.Pending
        _approvedMinutes.value = null
        val id = firestore.createRequest(familyId, appPackage, requestedMinutes)
        watch(id, appPackage)
        return id
    }

    private fun watch(requestId: String, appPackage: String) {
        val familyId = familyIdProvider.familyId.value ?: return
        watcherJob?.cancel()
        watcherJob = scope.launch {
            firestore.requestFlow(familyId, requestId).collectLatest { request ->
                request ?: return@collectLatest
                when (request.status) {
                    TimeRequest.Status.Approved -> {
                        // Apply the bonus BEFORE flipping requestStatus so the
                        // overlay (which dismisses on status=Approved) can't
                        // momentarily race with the accessibility-service tick
                        // and re-block the app between the dismiss and the
                        // bonus showing up in BonusStore.
                        val granted = request.approvedMinutes ?: request.requestedMinutes
                        bonusStore.addBonus(appPackage, granted * 60_000L)
                        _approvedMinutes.value = granted
                        _requestStatus.value = request.status
                        Log.i(TAG, "Request $requestId approved for $granted min on $appPackage")
                    }
                    TimeRequest.Status.Denied -> {
                        _requestStatus.value = request.status
                        Log.i(TAG, "Request $requestId denied")
                    }
                    TimeRequest.Status.Pending -> {
                        _requestStatus.value = request.status
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "RequestController"
    }
}
