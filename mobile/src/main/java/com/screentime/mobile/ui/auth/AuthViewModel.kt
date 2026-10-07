package com.screentime.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.mobile.fcm.PushTokenRegistrar
import com.screentime.shared.auth.AuthRepository
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
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
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val firestore: FirestoreRepository,
) : ViewModel() {

    init {
        // Re-save the FCM token on every signed-in launch (and whenever a
        // different account signs in), not only when FCM hands out a new one.
        viewModelScope.launch {
            auth.currentSession.map { it?.uid }.filterNotNull().distinctUntilChanged().collect { uid ->
                pushTokenRegistrar.registerCurrentToken(uid)
            }
        }
        // The TV measures "today" in the family's zone, not its own (which
        // the child can change). Seed it from this phone if nobody has yet.
        viewModelScope.launch {
            familyIdProvider.familyId.filterNotNull().distinctUntilChanged().collect { familyId ->
                runCatching { firestore.ensureFamilyTimezone(familyId, ZoneId.systemDefault().id) }
            }
        }
    }

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

}
