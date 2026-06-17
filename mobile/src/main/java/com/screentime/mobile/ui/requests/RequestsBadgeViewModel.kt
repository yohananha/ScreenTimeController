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
import javax.inject.Inject

/**
 * Activity-scoped, always-on listener that drives the "Requests" tab badge —
 * so a new pending request or a freshly-granted bonus is visible from any
 * tab, on either parent's device, in real time.
 */
@HiltViewModel
class RequestsBadgeViewModel @Inject constructor(
    firestore: FirestoreRepository,
    familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasUpdates: StateFlow<Boolean> = familyIdProvider.familyId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else firestore.recentRequestsFlow(id)
        }
        .combine(tickerFlow()) { all, _ -> all }
        .map { all ->
            val now = Instant.now()
            all.any { it.status == TimeRequest.Status.Pending || it.isActiveGrant(now) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
