package com.screentime.shared.room

import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.TimeFrameSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mobile-facing read/write store for limits, backed by Firestore.
 *
 * Note: this class still lives in the `room` package for now (it used to be
 * Room-backed in Phase 3). Phase 9 will move it.
 */
@Singleton
class LimitsRepository @Inject constructor(
    private val firestore: FirestoreRepository,
    private val familyIdProvider: FamilyIdProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<Limits> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(Limits()) else firestore.limitsFlow(id)
    }

    suspend fun setLimit(packageName: String, dailyLimitMinutes: Int) {
        val id = familyIdProvider.familyId.value ?: return
        firestore.setLimit(id, packageName, dailyLimitMinutes)
    }

    suspend fun removeLimit(packageName: String) {
        val id = familyIdProvider.familyId.value ?: return
        firestore.removeLimit(id, packageName)
    }

    suspend fun setOverallLimit(minutes: Int) {
        val id = familyIdProvider.familyId.value ?: return
        firestore.setOverallLimit(id, minutes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeInstalledApps(): Flow<List<InstalledApp>> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else firestore.installedAppsFlow(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLockout(): Flow<LockoutSettings> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(LockoutSettings()) else firestore.lockoutFlow(id)
    }

    suspend fun setLockoutConfig(durationMinutes: Int, mode: LockoutMode) {
        val id = familyIdProvider.familyId.value ?: return
        firestore.setLockoutConfig(id, durationMinutes, mode)
    }

    suspend fun unlockNow() {
        val id = familyIdProvider.familyId.value ?: return
        firestore.clearLockout(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTimeFrame(): Flow<TimeFrameSchedule> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(TimeFrameSchedule.DEFAULT) else firestore.timeFrameFlow(id)
    }

    suspend fun setTimeFrame(schedule: TimeFrameSchedule) {
        val id = familyIdProvider.familyId.value ?: return
        firestore.setTimeFrame(id, schedule)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAllowAllDay(): Flow<String?> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(null) else firestore.allowAllDayFlow(id)
    }

    suspend fun setAllowAllDay(date: String?) {
        val id = familyIdProvider.familyId.value ?: return
        firestore.setAllowAllDay(id, date)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeInstantLock(): Flow<Boolean> = familyIdProvider.familyId.flatMapLatest { id ->
        if (id == null) flowOf(false) else firestore.instantLockFlow(id)
    }

    suspend fun setInstantLock(locked: Boolean) {
        val id = familyIdProvider.familyId.value ?: return
        firestore.setInstantLock(id, locked)
    }
}
