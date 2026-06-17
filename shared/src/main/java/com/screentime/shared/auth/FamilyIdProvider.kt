package com.screentime.shared.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposes the current family id as a StateFlow.
 *
 * Phase 4 uses a stubbed value so the sync layer can be tested end-to-end
 * without auth. Phase 7 swaps the binding to read from
 * /users/{uid}.familyId on Firestore once the user signs in.
 */
interface FamilyIdProvider {
    val familyId: StateFlow<String?>
}

@Singleton
class StubFamilyIdProvider @Inject constructor() : FamilyIdProvider {
    private val state = MutableStateFlow<String?>(DEFAULT_FAMILY_ID)
    override val familyId: StateFlow<String?> = state.asStateFlow()

    companion object {
        const val DEFAULT_FAMILY_ID = "demo-family"
    }
}
