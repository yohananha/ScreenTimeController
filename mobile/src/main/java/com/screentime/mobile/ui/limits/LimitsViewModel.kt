package com.screentime.mobile.ui.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.AppLimit
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.TimeFrameSchedule
import com.screentime.shared.model.UsageSnapshot
import com.screentime.shared.room.LimitsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LimitsUiState(
    val limits: List<AppLimit> = emptyList(),
    val overallDailyMinutes: Int = Limits.DEFAULT_OVERALL_MINUTES,
    val availableApps: List<InstalledApp> = emptyList(),
    val lockout: LockoutSettings = LockoutSettings(),
    val timeFrame: TimeFrameSchedule = TimeFrameSchedule.DEFAULT,
    val allowAllDayActive: Boolean = false,
    val instantLocked: Boolean = false,
    val usagePerApp: Map<String, Long> = emptyMap(),
    val totalUsageMillis: Long = 0L,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
open class LimitsViewModel @Inject constructor(
    private val repo: LimitsRepository,
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    private val _writeError = MutableStateFlow<String?>(null)
    open val writeError: StateFlow<String?> = _writeError.asStateFlow()

    private val _todayUsage: StateFlow<UsageSnapshot> = familyIdProvider.familyId
        .flatMapLatest { familyId ->
            if (familyId == null) return@flatMapLatest flowOf(UsageSnapshot(LocalDate.now().toString(), emptyMap()))
            firestore.usageFlow(familyId, LocalDate.now())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsageSnapshot(LocalDate.now().toString(), emptyMap()))

    open val state: StateFlow<LimitsUiState> = combine(
        combine(repo.observe(), repo.observeInstalledApps(), repo.observeLockout()) { limits, apps, lockout ->
            Triple(limits, apps, lockout)
        },
        combine(repo.observeTimeFrame(), repo.observeAllowAllDay(), repo.observeInstantLock()) { tf, allDay, lock ->
            Triple(tf, allDay, lock)
        },
        _todayUsage,
    ) { (limits, apps, lockout), (timeFrame, allDayDate, instantLocked), usage ->
        LimitsUiState(
            limits = limits.perApp.values.sortedBy { it.packageName },
            overallDailyMinutes = limits.overallDailyMinutes,
            availableApps = apps,
            lockout = lockout,
            timeFrame = timeFrame,
            allowAllDayActive = allDayDate == LocalDate.now().toString(),
            instantLocked = instantLocked,
            usagePerApp = usage.perAppMillis,
            totalUsageMillis = usage.totalMillis(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LimitsUiState())

    fun clearWriteError() {
        _writeError.value = null
    }

    fun setLimit(packageName: String, minutes: Int) = write { repo.setLimit(packageName, minutes) }
    fun removeLimit(packageName: String) = write { repo.removeLimit(packageName) }
    fun setOverallLimit(minutes: Int) = write { repo.setOverallLimit(minutes) }
    fun setLockoutConfig(durationMinutes: Int, mode: LockoutMode) = write { repo.setLockoutConfig(durationMinutes, mode) }
    fun unlockNow() = write { repo.unlockNow() }
    fun allowAllDay() = write { repo.setAllowAllDay(LocalDate.now().toString()) }
    fun disallowAllDay() = write { repo.setAllowAllDay(null) }
    fun lockInstantly() = write { repo.setInstantLock(true) }
    fun unlockInstantly() = write { repo.setInstantLock(false) }

    fun selectInstantLock() = write {
        repo.setAllowAllDay(null)
        repo.setInstantLock(true)
    }
    fun selectAllowAllDay() = write {
        repo.setInstantLock(false)
        repo.setAllowAllDay(LocalDate.now().toString())
    }
    fun selectDefaultLimits() = write {
        repo.setInstantLock(false)
        repo.setAllowAllDay(null)
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { _writeError.value = it.message ?: "Something went wrong." }
        }
    }
}
