package com.screentime.mobile.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.TimeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestsUiState(
    val pending: List<TimeRequest> = emptyList(),
    val active: List<TimeRequest> = emptyList(),
)

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<RequestsUiState> = familyIdProvider.familyId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else firestore.recentRequestsFlow(id)
        }
        .combine(tickerFlow()) { all, _ -> all }
        .map { all ->
            val now = Instant.now()
            RequestsUiState(
                pending = all.filter { it.status == TimeRequest.Status.Pending },
                // Denied requests and expired grants are dropped entirely — nothing left to act on or show.
                active = all.filter { it.isActiveGrant(now) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RequestsUiState())

    fun approve(request: TimeRequest, minutes: Int = request.requestedMinutes) {
        val familyId = familyIdProvider.familyId.value ?: return
        viewModelScope.launch {
            firestore.respondToRequest(familyId, request.id, minutes)
        }
    }

    fun deny(request: TimeRequest) {
        val familyId = familyIdProvider.familyId.value ?: return
        viewModelScope.launch {
            firestore.respondToRequest(familyId, request.id, null)
        }
    }
}
