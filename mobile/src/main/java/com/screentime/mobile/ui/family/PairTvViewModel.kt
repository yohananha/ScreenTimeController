package com.screentime.mobile.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.PairedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairTvUiState(
    val busy: Boolean = false,
    val success: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class PairTvViewModel @Inject constructor(
    private val firestore: FirestoreRepository,
    familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(PairTvUiState())
    val state: StateFlow<PairTvUiState> = _state.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pairedDevices: StateFlow<List<PairedDevice>> = familyIdProvider.familyId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else firestore.pairedDevicesFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun claim(code: String, familyId: String) {
        _state.value = PairTvUiState(busy = true)
        viewModelScope.launch {
            runCatching { firestore.claimPairing(code, familyId) }
                .onSuccess { ok ->
                    _state.value = PairTvUiState(
                        success = ok,
                        message = if (ok) "TV paired." else "Invalid or expired code.",
                    )
                }
                .onFailure {
                    _state.value = PairTvUiState(message = it.message ?: "Failed.")
                }
        }
    }

    fun reset() {
        _state.value = PairTvUiState()
    }

    fun rename(deviceId: String, name: String) {
        viewModelScope.launch { runCatching { firestore.renameDevice(deviceId, name) } }
    }

    fun unpair(familyId: String, deviceId: String) {
        viewModelScope.launch { runCatching { firestore.unpairDevice(familyId, deviceId) } }
    }
}
