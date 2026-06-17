package com.screentime.shared.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UsageEntity::class, AppLimitEntity::class, BonusEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun limitsDao(): LimitsDao
    abstract fun bonusDao(): BonusDao

    companion object {
        const val DB_NAME = "screen_time.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME,
            ).fallbackToDestructiveMigration(dropAllTables = true)
                .build().also { instance = it }
        }
    }
}
