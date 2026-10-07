package com.screentime.tv.time

import android.util.Log
import com.screentime.shared.auth.DeviceFamilyIdProvider
import com.screentime.shared.firestore.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The I/O half of [TvTrustedClock]:
 *  - [beat] stamps /devices/{id}.lastSeen with the server clock and anchors
 *    the TV's clock to it. It is called by the enforcement service, so
 *    `lastSeen` going stale tells parents protection has stopped (service
 *    disabled, app force-stopped/uninstalled, TV offline).
 *  - from construction, mirrors the family time zone into the clock.
 *
 * Construct at process start (ScreenTimeTvApp injects it) so the zone is
 * applied before anything asks for "today".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TvHeartbeat @Inject constructor(
    private val clock: TvTrustedClock,
    private val firestore: FirestoreRepository,
    private val deviceProvider: DeviceFamilyIdProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            deviceProvider.familyId
                .flatMapLatest { id -> if (id == null) flowOf(null) else firestore.timezoneFlow(id) }
                .collect { clock.setZone(it) }
        }
    }

    /** Best-effort; a failure (offline, unpaired) just leaves the previous anchor in place. */
    suspend fun beat() {
        val deviceId = deviceProvider.deviceId.value ?: return
        // An unpaired TV has no device doc to stamp (and nothing to enforce).
        if (deviceProvider.familyId.value == null) return
        try {
            clock.setAnchor(firestore.stampLastSeen(deviceId))
        } catch (t: Throwable) {
            Log.w(TAG, "Heartbeat failed; keeping previous clock anchor", t)
        }
    }

    private companion object {
        const val TAG = "TvHeartbeat"
    }
}
