package com.screentime.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.AuthRepository
import com.screentime.shared.auth.FamilyIdProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object NeedsSignIn : AuthState
    data object NeedsFamily : AuthState
    data class Authenticated(val uid: String, val familyId: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    val state: StateFlow<AuthState> = combine(
        auth.currentSession,
        familyIdProvider.familyId,
    ) { session, family ->
        when {
            session == null -> AuthState.NeedsSignIn
            family == null -> AuthState.NeedsFamily
            else -> AuthState.Authenticated(session.uid, family)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        _error.value = null
        viewModelScope.launch {
            runCatching { auth.signInWithGoogle(idToken) }
                .onFailure { _error.value = it.message }
        }
    }

    fun reportError(message: String) {
        _error.value = message
    }

    fun signOut() = auth.signOut()
}
