package com.screentime.shared.model

/** A member's role within a family. */
enum class FamilyRole {
    /** The main admin (family creator) plus anyone they promote. Manages members. */
    ADMIN,

    /** A regular co-parent: can view and operate, but can't manage members or the TV. */
    USER,
}

/**
 * A family and its members. The [ownerUid] is the single "main admin" — the
 * only account allowed to pair/unpair the TV and delete the family.
 */
data class Family(
    val id: String,
    val ownerUid: String,
    val members: Map<String, FamilyRole>,
    val deviceIds: List<String>,
) {
    fun roleOf(uid: String): FamilyRole? = members[uid]
    fun isOwner(uid: String): Boolean = uid == ownerUid
    fun isAdmin(uid: String): Boolean = members[uid] == FamilyRole.ADMIN
}
