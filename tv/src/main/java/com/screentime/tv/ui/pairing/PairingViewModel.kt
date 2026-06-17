package com.screentime.tv.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.DeviceFamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val code: String? = null,
    val error: String? = null,
    val generating: Boolean = false,
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val firestore: FirestoreRepository,
    private val deviceProvider: DeviceFamilyIdProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    fun ensureCode() {
        if (_state.value.code != null || _state.value.generating) return
        _state.value = _state.value.copy(generating = true)
        viewModelScope.launch {
            runCatching {
                val (deviceId, error) = combine(deviceProvider.deviceId, deviceProvider.authError) { id, err ->
                    id to err
                }.first { (id, err) -> id != null || err != null }
                deviceId ?: throw IllegalStateException(error ?: "Sign-in failed")
                firestore.createPairing(deviceId)
            }.onSuccess {
                _state.value = PairingUiState(code = it)
            }.onFailure {
                _state.value = PairingUiState(error = it.message ?: "Failed to create code.")
            }
        }
    }
}
