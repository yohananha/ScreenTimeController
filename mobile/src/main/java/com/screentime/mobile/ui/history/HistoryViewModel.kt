package com.screentime.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.UsageSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    private val _snapshots = MutableStateFlow<List<UsageSnapshot>>(emptyList())
    val snapshots: StateFlow<List<UsageSnapshot>> = _snapshots.asStateFlow()

    init {
        viewModelScope.launch {
            familyIdProvider.familyId
                .flatMapLatest { familyId ->
                    if (familyId == null) return@flatMapLatest flowOf(emptyList())
                    val today = LocalDate.now()
                    val flows = (0..6).map { daysAgo ->
                        firestore.usageFlow(familyId, today.minusDays(daysAgo.toLong()))
                    }
                    combine(flows) { array -> array.sortedByDescending { it.date } }
                }
                .collect { _snapshots.value = it }
        }
    }
}
