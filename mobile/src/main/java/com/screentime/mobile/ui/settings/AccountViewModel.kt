package com.screentime.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.mobile.fcm.PushTokenRegistrar
import com.screentime.shared.auth.AuthRepository
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.firestore.toErrorRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val busy: Boolean = false,
    /** A @StringRes, or null when there's no error to show. */
    val error: Int? = null,
)

/**
 * Account / family deletion (Cloud Functions deleteAccount / deleteFamily).
 * On success there is nothing to navigate to: deleting the account signs
 * out (AuthGate shows sign-in), and deleting the family clears this user's
 * familyId (AuthGate shows onboarding).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: FirestoreRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val familyIdProvider: FamilyIdProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    /** Whether the signed-in user owns the current family (only the owner may delete it). */
    val isOwner: StateFlow<Boolean> = familyIdProvider.familyId
        .flatMapLatest { id -> if (id == null) flowOf(null) else firestore.familyFlow(id) }
        .map { family -> family != null && family.ownerUid == auth.currentUid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Unregisters this phone's push token while still signed in (so it stops
     * getting the family's notifications), then signs out.
     */
    fun signOut() = run {
        auth.currentUid?.let { pushTokenRegistrar.unregister(it) }
        auth.signOut()
    }

    /** Unregisters push while the user still exists, deletes server-side, then signs out. */
    fun deleteAccount() = run {
        auth.currentUid?.let { pushTokenRegistrar.unregister(it) }
        firestore.deleteAccount()
        auth.signOut()
    }

    fun deleteFamily() = run {
        val familyId = familyIdProvider.familyId.value ?: return@run
        firestore.deleteFamily(familyId)
    }

    private fun run(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.value = AccountUiState(busy = true)
        viewModelScope.launch {
            _state.value = runCatching { action() }.fold(
                onSuccess = { AccountUiState() },
                onFailure = { AccountUiState(error = it.toErrorRes()) },
            )
        }
    }
}
