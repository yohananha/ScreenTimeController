package com.screentime.shared.limits

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store of per-package "bonus time" exemption windows granted by
 * code redemption or parent approval. A grant means "this app is unblocked
 * for N more minutes starting now" — while [isActive] is true, enforcement
 * ignores the package's usage entirely, regardless of how much usage already
 * accumulated today. Phase 9 persists this so it survives the TV rebooting
 * and resets at the configured daily reset time.
 */
@Singleton
class BonusStore @Inject constructor() {
    private val state = MutableStateFlow<Map<String, Instant>>(emptyMap())
    val bonuses: StateFlow<Map<String, Instant>> = state.asStateFlow()

    /** Extends [packageName]'s exemption window by [millis], from now or its current expiry, whichever is later. */
    fun addBonus(packageName: String, millis: Long) {
        val now = Instant.now()
        state.value = state.value.toMutableMap().apply {
            val base = this[packageName]?.takeIf { it.isAfter(now) } ?: now
            this[packageName] = base.plusMillis(millis)
        }
    }

    fun isActive(packageName: String, now: Instant = Instant.now()): Boolean =
        state.value[packageName]?.isAfter(now) == true

    fun expiryFor(packageName: String): Instant? = state.value[packageName]

    fun clear() {
        state.value = emptyMap()
    }
}
