package com.screentime.shared.limits

import com.screentime.shared.model.AppLimit
import com.screentime.shared.model.Limits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Source of truth for [Limits]. Phase 2 ships a hardcoded implementation so
 * enforcement can be tested without a mobile app or Firestore. Phase 4
 * swaps the binding for a Firestore-backed one.
 */
interface LimitsProvider {
    fun limits(): Flow<Limits>
}

class HardcodedLimitsProvider(
    initial: Limits = DEFAULT,
) : LimitsProvider {
    private val state = MutableStateFlow(initial)
    override fun limits(): Flow<Limits> = state.asStateFlow()

    companion object {
        val DEFAULT = Limits(
            overallDailyMinutes = 120,
            perApp = mapOf(
                "com.google.android.youtube.tv" to AppLimit(
                    packageName = "com.google.android.youtube.tv",
                    dailyLimitMinutes = 5,
                ),
                "com.netflix.ninja" to AppLimit(
                    packageName = "com.netflix.ninja",
                    dailyLimitMinutes = 45,
                ),
            ),
        )
    }
}
