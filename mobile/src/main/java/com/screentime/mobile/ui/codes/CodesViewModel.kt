package com.screentime.mobile.ui.codes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.mobile.R
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.firestore.toErrorRes
import com.screentime.shared.model.OneTimeCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CodesUiState(
    val isGenerating: Boolean = false,
    val active: OneTimeCode? = null,
    /** A @StringRes to render, or null when there's no error to show. */
    val error: Int? = null,
)

@HiltViewModel
open class CodesViewModel @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(CodesUiState())
    open val state: StateFlow<CodesUiState> = _state.asStateFlow()

    fun generate(extraMinutes: Int) {
        val familyId = familyIdProvider.familyId.value ?: run {
            _state.value = _state.value.copy(error = R.string.error_no_family_linked)
            return
        }
        _state.value = _state.value.copy(isGenerating = true, error = null)
        viewModelScope.launch {
            runCatching { firestore.createCode(familyId, extraMinutes) }
                .onSuccess { _state.value = CodesUiState(active = it) }
                .onFailure { _state.value = CodesUiState(error = it.toErrorRes()) }
        }
    }

    fun dismiss() {
        _state.value = CodesUiState()
    }
}
