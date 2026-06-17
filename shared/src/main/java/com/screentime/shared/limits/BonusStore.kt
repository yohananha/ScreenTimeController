package com.screentime.shared.limits

import com.screentime.shared.room.AppDatabase
import com.screentime.shared.room.BonusEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-package "bonus time" exemption windows granted by code redemption or
 * parent approval. Room-backed so active bonuses survive a TV reboot.
 *
 * The in-memory [StateFlow] is the fast path for enforcement checks;
 * Room is loaded on startup and written asynchronously on every mutation.
 */
@Singleton
class BonusStore @Inject constructor(db: AppDatabase) {
    private val dao = db.bonusDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow<Map<String, Instant>>(emptyMap())
    val bonuses: StateFlow<Map<String, Instant>> = state.asStateFlow()

    init {
        scope.launch {
            val now = Instant.now()
            state.value = dao.getAll()
                .filter { Instant.ofEpochMilli(it.expiresAt).isAfter(now) }
                .associate { it.packageName to Instant.ofEpochMilli(it.expiresAt) }
        }
    }

    /** Extends [packageName]'s exemption by [millis] from now or its current expiry, whichever is later. */
    fun addBonus(packageName: String, millis: Long) {
        val now = Instant.now()
        var expiry = now
        state.value = state.value.toMutableMap().apply {
            val base = this[packageName]?.takeIf { it.isAfter(now) } ?: now
            expiry = base.plusMillis(millis)
            this[packageName] = expiry
        }
        scope.launch { dao.upsert(BonusEntity(packageName, expiry.toEpochMilli())) }
    }

    fun isActive(packageName: String, now: Instant = Instant.now()): Boolean =
        state.value[packageName]?.isAfter(now) == true

    fun expiryFor(packageName: String): Instant? = state.value[packageName]

    fun clear() {
        state.value = emptyMap()
        scope.launch { dao.deleteAll() }
    }
}
