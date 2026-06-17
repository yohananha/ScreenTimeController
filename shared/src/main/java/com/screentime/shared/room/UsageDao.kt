package com.screentime.shared.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage WHERE date = :date")
    fun observeForDate(date: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage WHERE date = :date")
    suspend fun loadForDate(date: String): List<UsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<UsageEntity>)

    @Query("DELETE FROM usage WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
}
