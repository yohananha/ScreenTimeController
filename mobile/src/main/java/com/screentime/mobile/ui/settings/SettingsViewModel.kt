package com.screentime.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.room.LimitsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lean on purpose: only the lockout config lives here. Settings doesn't need
 * LimitsViewModel's usage/installed-apps/overall-limit combine — reusing it
 * would mean this screen re-subscribes to flows it never reads.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: LimitsRepository,
) : ViewModel() {

    val lockout: StateFlow<LockoutSettings> = repo.observeLockout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockoutSettings())

    private val _writeError = MutableStateFlow<String?>(null)
    val writeError: StateFlow<String?> = _writeError.asStateFlow()

    fun clearWriteError() {
        _writeError.value = null
    }

    fun setLockoutConfig(durationMinutes: Int, mode: LockoutMode) = write {
        repo.setLockoutConfig(durationMinutes, mode)
    }

    fun unlockNow() = write { repo.unlockNow() }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { _writeError.value = it.message ?: "Something went wrong." }
        }
    }
}
