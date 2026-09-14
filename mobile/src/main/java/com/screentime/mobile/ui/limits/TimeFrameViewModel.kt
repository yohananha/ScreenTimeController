package com.screentime.mobile.ui.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.model.TimeFrameSchedule
import com.screentime.shared.model.TimeFrameWindow
import com.screentime.shared.room.LimitsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class TimeFrameUiState(
    val schedule: TimeFrameSchedule = TimeFrameSchedule.DEFAULT,
    val pendingChanges: Boolean = false,
    val saving: Boolean = false,
)

@HiltViewModel
class TimeFrameViewModel @Inject constructor(
    private val repo: LimitsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TimeFrameUiState())
    val state: StateFlow<TimeFrameUiState> = _state

    private val saved: StateFlow<TimeFrameSchedule> = repo.observeTimeFrame()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimeFrameSchedule.DEFAULT)

    init {
        viewModelScope.launch {
            saved.collect { serverSchedule ->
                _state.update { it.copy(schedule = serverSchedule, pendingChanges = false) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(schedule = it.schedule.copy(enabled = enabled), pendingChanges = true) }
    }

    fun setWindows(day: DayOfWeek, windows: List<TimeFrameWindow>, applyAlsoTo: Set<DayOfWeek> = emptySet()) {
        _state.update { state ->
            val updated = state.schedule.windowsByDay.toMutableMap()
            updated[day] = windows
            applyAlsoTo.forEach { updated[it] = windows }
            state.copy(schedule = state.schedule.copy(windowsByDay = updated), pendingChanges = true)
        }
    }

    fun save() {
        val schedule = _state.value.schedule
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repo.setTimeFrame(schedule)
            _state.update { it.copy(saving = false, pendingChanges = false) }
        }
    }
}
