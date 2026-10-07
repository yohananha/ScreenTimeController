package com.screentime.shared.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant

/*
 * Type-tolerant field readers. DocumentSnapshot.getLong/getBoolean/getString
 * THROW when a field holds a different type, and a throw inside a snapshot
 * listener crashes the process — on the TV that takes the enforcement
 * service down with it, so one malformed value written by any family member
 * (or a buggy client) switched blocking off. These return null instead, and
 * callers fall back to their default.
 */

internal fun DocumentSnapshot.longOrNull(field: String): Long? = (get(field) as? Number)?.toLong()

internal fun DocumentSnapshot.intOrNull(field: String): Int? =
    longOrNull(field)?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt()

internal fun DocumentSnapshot.boolOrNull(field: String): Boolean? = get(field) as? Boolean

internal fun DocumentSnapshot.stringOrNull(field: String): String? = get(field) as? String

internal fun DocumentSnapshot.instantOrNull(field: String): Instant? =
    (get(field) as? Timestamp)?.toDate()?.toInstant()
