package com.screentime.shared.model

import java.time.Instant

data class TimeRequest(
    val id: String,
    val appPackage: String,
    val requestedMinutes: Int,
    val status: Status = Status.Pending,
    val approvedMinutes: Int? = null,
    val createdAt: Instant = Instant.now(),
    val respondedAt: Instant? = null,
) {
    enum class Status { Pending, Approved, Denied }

    /** True while an approved grant's window ([respondedAt] + granted minutes) hasn't elapsed yet. */
    fun isActiveGrant(now: Instant = Instant.now()): Boolean {
        val expiry = grantExpiresAt() ?: return false
        return now.isBefore(expiry)
    }

    fun grantExpiresAt(): Instant? {
        if (status != Status.Approved) return null
        val grantedMinutes = approvedMinutes ?: requestedMinutes
        return respondedAt?.plusSeconds(grantedMinutes * 60L)
    }
}
