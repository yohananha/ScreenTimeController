package com.screentime.shared.model

import java.time.Instant

data class OneTimeCode(
    val code: String,
    val extraMinutes: Int,
    val expiresAt: Instant,
) {
    val isExpired: Boolean get() = Instant.now().isAfter(expiresAt)
}
