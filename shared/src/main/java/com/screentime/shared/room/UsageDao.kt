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

    /**
     * Removes rows for [date] whose package is no longer counted. [upsertAll]
     * only ever replaces keys it is given, so without this a package that used
     * to accrue time — the launcher, before it was excluded — would keep its
     * stale row and inflate the day's total forever.
     */
    @Query("DELETE FROM usage WHERE date = :date AND packageName IN (:packages)")
    suspend fun deleteForDate(date: String, packages: List<String>)
}
