package com.screentime.shared.room

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dao = AppDatabase.get(context).usageDao()

    suspend fun millisForToday(packageName: String): Long {
        val today = LocalDate.now().toString()
        return dao.loadForDate(today).firstOrNull { it.packageName == packageName }?.millis ?: 0L
    }

    suspend fun totalMillisForToday(): Long {
        val today = LocalDate.now().toString()
        return dao.loadForDate(today).sumOf { it.millis }
    }

    suspend fun upsertSamples(date: LocalDate, samples: Map<String, Long>) {
        val day = date.toString()
        dao.upsertAll(samples.map { UsageEntity(day, it.key, it.value) })
    }
}
