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

    /** Status of the most recently submitted request, observed by the block overlay so it can react to a parent's decision. */
    val requestStatus: StateFlow<TimeRequest.Status?> = _requestStatus.asStateFlow()

    suspend fun submit(appPackage: String, requestedMinutes: Int): String? {
        val familyId = familyIdProvider.familyId.value ?: return null
        _requestStatus.value = TimeRequest.Status.Pending
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
                _requestStatus.value = request.status
                when (request.status) {
                    TimeRequest.Status.Approved -> {
                        val granted = request.approvedMinutes ?: request.requestedMinutes
                        Log.i(TAG, "Request $requestId approved for $granted min on $appPackage")
                        bonusStore.addBonus(appPackage, granted * 60_000L)
                    }
                    TimeRequest.Status.Denied -> {
                        Log.i(TAG, "Request $requestId denied")
                    }
                    TimeRequest.Status.Pending -> Unit
                }
            }
        }
    }

    private companion object {
        const val TAG = "RequestController"
    }
}
