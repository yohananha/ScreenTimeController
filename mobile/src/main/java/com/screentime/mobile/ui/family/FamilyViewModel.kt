package com.screentime.mobile.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.AuthRepository
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.Family
import com.screentime.shared.model.FamilyRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val inviteCode: String? = null,
    val joining: Boolean = false,
    val error: String? = null,
    val family: Family? = null,
    val currentUid: String? = null,
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val firestore: FirestoreRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FamilyUiState())
    val state: StateFlow<FamilyUiState> = _state.asStateFlow()

    private var observingFamilyId: String? = null

    fun observeFamily(familyId: String) {
        if (observingFamilyId == familyId) return
        observingFamilyId = familyId
        viewModelScope.launch {
            val session = auth.currentSession.first() ?: return@launch
            _state.value = _state.value.copy(currentUid = session.uid)
            firestore.familyFlow(familyId).collect { family ->
                _state.value = _state.value.copy(family = family)
            }
        }
    }

    fun createFamily() {
        viewModelScope.launch {
            val session = auth.currentSession.first() ?: return@launch
            runCatching { firestore.createFamily(session.uid) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun generateInvite(familyId: String) {
        viewModelScope.launch {
            val session = auth.currentSession.first() ?: return@launch
            runCatching { firestore.generateInvite(familyId, session.uid) }
                .onSuccess { _state.value = _state.value.copy(inviteCode = it, error = null) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun joinByCode(code: String) {
        _state.value = _state.value.copy(joining = true, error = null)
        viewModelScope.launch {
            val session = auth.currentSession.first() ?: return@launch
            runCatching { firestore.joinFamilyByInvite(code, session.uid) }
                .onSuccess { joined ->
                    _state.value = if (joined == null) {
                        _state.value.copy(joining = false, error = "Invalid or expired code.")
                    } else _state.value.copy(joining = false)
                }
                .onFailure { _state.value = _state.value.copy(joining = false, error = it.message) }
        }
    }

    fun setMemberRole(familyId: String, uid: String, role: FamilyRole) {
        viewModelScope.launch {
            runCatching { firestore.setMemberRole(familyId, uid, role) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun removeMember(familyId: String, uid: String) {
        viewModelScope.launch {
            runCatching { firestore.removeMember(familyId, uid) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}
