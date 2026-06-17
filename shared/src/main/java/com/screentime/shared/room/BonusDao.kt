package com.screentime.shared.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BonusDao {
    @Query("SELECT * FROM bonus")
    suspend fun getAll(): List<BonusEntity>

    @Upsert
    suspend fun upsert(entity: BonusEntity)

    @Query("DELETE FROM bonus")
    suspend fun deleteAll()
}
