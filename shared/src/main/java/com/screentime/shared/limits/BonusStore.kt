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
 * Device-wide "bonus time" exemption window granted by code redemption or
 * parent approval. Room-backed so an active bonus survives a TV reboot.
 *
 * A grant is deliberately not scoped to the app that triggered it: after a
 * blocked app is backgrounded, the launcher (and whatever the child opens
 * next) would otherwise need its own separate grant, forcing the parent to
 * approve the same "let them use the TV" request once per app.
 *
 * The in-memory [StateFlow] is the fast path for enforcement checks; Room is
 * loaded on startup and written asynchronously on every mutation.
 */
@Singleton
class BonusStore @Inject constructor(db: AppDatabase) {
    private val dao = db.bonusDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow<Instant?>(null)
    val bonuses: StateFlow<Instant?> = state.asStateFlow()

    init {
        scope.launch {
            val now = Instant.now()
            state.value = dao.getAll()
                .map { Instant.ofEpochMilli(it.expiresAt) }
                .maxOrNull()
                ?.takeIf { it.isAfter(now) }
        }
    }

    /** Extends the device's exemption by [millis] from now or its current expiry, whichever is later. */
    fun addBonus(millis: Long) {
        val now = Instant.now()
        val base = state.value?.takeIf { it.isAfter(now) } ?: now
        val expiry = base.plusMillis(millis)
        state.value = expiry
        scope.launch { dao.upsert(BonusEntity(DEVICE_KEY, expiry.toEpochMilli())) }
    }

    fun isActive(now: Instant = Instant.now()): Boolean = state.value?.isAfter(now) == true

    fun expiryFor(): Instant? = state.value

    fun clear() {
        state.value = null
        scope.launch { dao.deleteAll() }
    }

    private companion object {
        const val DEVICE_KEY = "__device__"
    }
}
