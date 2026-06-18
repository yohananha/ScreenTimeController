package com.screentime.shared.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.concurrent.atomic.AtomicBoolean

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
        // New filename for the SQLCipher-encrypted DB. The pre-encryption file
        // ("screen_time.db") is intentionally orphaned: bonus / usage rows are
        // either ephemeral or re-synced from Firestore, so a clean recreate is
        // simpler than a copy-decrypt-reinsert migration path.
        const val DB_NAME = "screen_time_enc.db"
        private const val LEGACY_DB_NAME = "screen_time.db"

        private val nativeLoaded = AtomicBoolean(false)

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

        private fun build(context: Context): AppDatabase {
            val appContext = context.applicationContext
            if (nativeLoaded.compareAndSet(false, true)) {
                System.loadLibrary("sqlcipher")
            }
            // Best-effort cleanup of the unencrypted legacy file. Safe to delete:
            // any state it held will be repopulated by the Firestore sync layer.
            runCatching { appContext.deleteDatabase(LEGACY_DB_NAME) }

            val passphrase = DbKeyProvider.getOrCreatePassphrase(appContext)
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
