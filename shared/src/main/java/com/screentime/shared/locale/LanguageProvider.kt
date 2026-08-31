package com.screentime.shared.locale

import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only view of the family's language (families/{id}/settings/language)
 * — what a paired TV shows. Mirrors [com.screentime.shared.limits.FirestoreLimitsProvider]'s
 * shape. Null = not set; the caller falls back to its own device locale.
 */
@Singleton
class LanguageProvider @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val languageTag: Flow<String?> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(null) else firestore.languageFlow(id)
    }
}
