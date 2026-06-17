package com.screentime.shared.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bonus")
data class BonusEntity(
    @PrimaryKey val packageName: String,
    val expiresAt: Long,
)
