package com.screentime.shared.room

import androidx.room.Entity

@Entity(tableName = "usage", primaryKeys = ["date", "packageName"])
data class UsageEntity(
    val date: String,
    val packageName: String,
    val millis: Long,
)
