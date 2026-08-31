package com.screentime.shared.firestore

import androidx.annotation.StringRes
import com.google.firebase.functions.FirebaseFunctionsException
import com.screentime.shared.R

/**
 * Maps a caught error to a localized [StringRes] instead of showing the
 * server's English-only [Throwable.message] straight in the UI.
 *
 * Prefers the machine-readable `details.reason` tag that functions/src/index.ts
 * attaches to each [HttpsError][com.google.firebase.functions.FirebaseFunctionsException] —
 * the gRPC status code alone is ambiguous (NOT_FOUND alone covers five
 * distinct conditions across the callable functions: invalid code, used
 * code, expired code, missing family, missing pairing).
 */
@StringRes
fun Throwable.toErrorRes(): Int {
    val fe = this as? FirebaseFunctionsException ?: return R.string.error_generic
    val reason = (fe.details as? Map<*, *>)?.get("reason") as? String
    return when (reason) {
        "family_not_found" -> R.string.error_family_not_found
        "not_admin" -> R.string.error_not_admin
        "not_owner" -> R.string.error_not_owner
        "code_invalid" -> R.string.error_code_invalid
        "code_used" -> R.string.error_code_used
        "code_expired" -> R.string.error_code_expired
        "code_invalid_or_expired" -> R.string.error_code_invalid_or_expired
        "tv_already_paired" -> R.string.error_tv_already_paired
        "device_not_paired" -> R.string.error_device_not_paired
        "locked_out" -> R.string.error_locked_out
        else -> when (fe.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> R.string.error_signin_required
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> R.string.error_permission_denied
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> R.string.error_network
            else -> R.string.error_generic
        }
    }
}
