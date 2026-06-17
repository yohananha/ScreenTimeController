package com.screentime.shared.limits

import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.Limits
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TV-side read-only LimitsProvider backed by the live Firestore snapshot.
 * Used by EnforcementAccessibilityService.
 */
@Singleton
class FirestoreLimitsProvider @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) : LimitsProvider {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun limits(): Flow<Limits> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(Limits()) else firestore.limitsFlow(id)
    }
}
