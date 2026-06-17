package com.screentime.mobile.ui.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.model.AppLimit
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.room.LimitsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LimitsUiState(
    val limits: List<AppLimit> = emptyList(),
    val overallDailyMinutes: Int = Limits.DEFAULT_OVERALL_MINUTES,
    val availableApps: List<InstalledApp> = emptyList(),
    val lockout: LockoutSettings = LockoutSettings(),
)

@HiltViewModel
class LimitsViewModel @Inject constructor(
    private val repo: LimitsRepository,
) : ViewModel() {

    val state: StateFlow<LimitsUiState> = combine(
        repo.observe(),
        repo.observeInstalledApps(),
        repo.observeLockout(),
    ) { limits, apps, lockout ->
        LimitsUiState(
            limits = limits.perApp.values.sortedBy { it.packageName },
            overallDailyMinutes = limits.overallDailyMinutes,
            availableApps = apps,
            lockout = lockout,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LimitsUiState())

    fun setLimit(packageName: String, minutes: Int) {
        viewModelScope.launch { repo.setLimit(packageName, minutes) }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch { repo.removeLimit(packageName) }
    }

    fun setOverallLimit(minutes: Int) {
        viewModelScope.launch { repo.setOverallLimit(minutes) }
    }

    fun setLockoutConfig(durationMinutes: Int, mode: LockoutMode) {
        viewModelScope.launch { repo.setLockoutConfig(durationMinutes, mode) }
    }

    fun unlockNow() {
        viewModelScope.launch { repo.unlockNow() }
    }
}
