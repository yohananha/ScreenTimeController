package com.screentime.shared.model

import java.time.Duration
import java.time.Instant

/**
 * [lastSeen] is the TV's last heartbeat (server time). The enforcement
 * service stamps it every few minutes while running, so a stale value means
 * protection is off — service disabled, app force-stopped or uninstalled,
 * or the TV is offline/powered down.
 */
data class PairedDevice(
    val id: String,
    val name: String,
    val lastSeen: Instant? = null,
) {
    enum class Heartbeat { ACTIVE, NOT_RESPONDING, NEVER_SEEN }

    /** Whether the TV's enforcement service has checked in recently, as of [now]. */
    fun heartbeatAt(now: Instant): Heartbeat = when {
        lastSeen == null -> Heartbeat.NEVER_SEEN
        Duration.between(lastSeen, now) > STALE_AFTER -> Heartbeat.NOT_RESPONDING
        else -> Heartbeat.ACTIVE
    }

    companion object {
        const val DEFAULT_NAME = "Android TV"

        /** Three missed 5-minute heartbeats (see TvHeartbeat). */
        val STALE_AFTER: Duration = Duration.ofMinutes(15)
    }
}
