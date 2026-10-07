package com.screentime.shared.model

import java.time.Instant

enum class LockoutMode { TIMER, PARENT_UNLOCK }

/**
 * Configures what happens when too many wrong codes are entered on the TV.
 * [locked]/[lockedUntil] reflect the TV's current lockout state; the rest is
 * parent-configurable from the mobile app.
 */
data class LockoutSettings(
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val mode: LockoutMode = LockoutMode.TIMER,
    val locked: Boolean = false,
    val lockedUntil: Instant? = null,
) {
    /**
     * A lock with no [lockedUntil] — parent mode, or a timer lock the server
     * escalated after repeated lockouts — only ends when a parent unlocks.
     */
    val needsParent: Boolean
        get() = locked && (mode == LockoutMode.PARENT_UNLOCK || lockedUntil == null)

    companion object {
        const val DEFAULT_DURATION_MINUTES = 15
        const val MAX_ATTEMPTS = 5
        const val ATTEMPT_WINDOW_SECONDS = 60L
    }
}
