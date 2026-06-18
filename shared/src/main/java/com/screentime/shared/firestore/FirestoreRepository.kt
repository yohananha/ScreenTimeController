package com.screentime.shared.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import com.screentime.shared.model.AppLimit
import com.screentime.shared.model.Family
import com.screentime.shared.model.FamilyRole
import com.screentime.shared.model.InstalledApp
import com.screentime.shared.model.Limits
import com.screentime.shared.model.LockoutMode
import com.screentime.shared.model.LockoutSettings
import com.screentime.shared.model.OneTimeCode
import com.screentime.shared.model.PairedDevice
import com.screentime.shared.model.TimeFrameSchedule
import com.screentime.shared.model.TimeFrameWindow
import com.screentime.shared.model.TimeRequest
import com.screentime.shared.model.UsageSnapshot
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Single entry point for all Firestore reads/writes under each family doc.
 *
 * Layout:
 *   families/{familyId}/limits/overall          (single doc)
 *   families/{familyId}/limits/perApp/apps/{pkg}
 *   families/{familyId}/usage/{yyyy-MM-dd}      (single doc per day)
 */
@Singleton
class FirestoreRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) {

    fun limitsFlow(familyId: String): Flow<Limits> = combine(
        overallLimitFlow(familyId),
        perAppLimitsFlow(familyId),
        timeFrameFlow(familyId),
        allowAllDayFlow(familyId),
        instantLockFlow(familyId),
    ) { overallMinutes, perApp, timeFrame, allowAllDayDate, instantLocked ->
        Limits(
            overallDailyMinutes = overallMinutes,
            perApp              = perApp,
            timeFrame           = timeFrame,
            allowAllDayDate     = allowAllDayDate,
            instantLocked       = instantLocked,
        )
    }

    private fun overallLimitFlow(familyId: String): Flow<Int> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("limits").document("overall")
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "overallLimitFlow($familyId) listener failed", error)
                trySend(Limits.DEFAULT_OVERALL_MINUTES)
                return@addSnapshotListener
            }
            trySend(snap?.getLong(FIELD_OVERALL_MINUTES)?.toInt() ?: Limits.DEFAULT_OVERALL_MINUTES)
        }
        awaitClose { registration.remove() }
    }

    private fun perAppLimitsFlow(familyId: String): Flow<Map<String, AppLimit>> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("limits").document("perApp")
            .collection("apps")
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "perAppLimitsFlow($familyId) listener failed", error)
                trySend(emptyMap())
                return@addSnapshotListener
            }
            val entries = snap?.documents.orEmpty().mapNotNull { doc ->
                val pkg = doc.id
                val minutes = doc.getLong(FIELD_MINUTES)?.toInt() ?: return@mapNotNull null
                pkg to AppLimit(pkg, minutes)
            }
            trySend(entries.toMap())
        }
        awaitClose { registration.remove() }
    }

    suspend fun setLimit(familyId: String, packageName: String, dailyLimitMinutes: Int) {
        db.collection("families").document(familyId)
            .collection("limits").document("perApp")
            .collection("apps").document(packageName)
            .set(mapOf(FIELD_MINUTES to dailyLimitMinutes))
            .await()
    }

    /**
     * Sets the cross-app daily cap: once today's total usage across all apps
     * reaches this many minutes, the TV blocks regardless of per-app limits.
     */
    suspend fun setOverallLimit(familyId: String, minutes: Int) {
        db.collection("families").document(familyId)
            .collection("limits").document("overall")
            .set(mapOf(FIELD_OVERALL_MINUTES to minutes))
            .await()
    }

    fun timeFrameFlow(familyId: String): Flow<TimeFrameSchedule> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("limits").document("timeFrame")
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "timeFrameFlow($familyId) listener failed", error)
                trySend(TimeFrameSchedule.DEFAULT)
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                trySend(TimeFrameSchedule.DEFAULT)
                return@addSnapshotListener
            }
            val enabled = snap.getBoolean(FIELD_TF_ENABLED) ?: false
            @Suppress("UNCHECKED_CAST")
            val rawDays = snap.get(FIELD_TF_WINDOWS_BY_DAY) as? Map<String, List<Map<String, Long>>>
                ?: emptyMap()
            val windowsByDay = rawDays.mapNotNull { (dayName, windows) ->
                val day = runCatching { DayOfWeek.valueOf(dayName) }.getOrNull() ?: return@mapNotNull null
                val parsed = windows.mapNotNull { w ->
                    val start = w[FIELD_TF_START]?.toInt() ?: return@mapNotNull null
                    val end = w[FIELD_TF_END]?.toInt() ?: return@mapNotNull null
                    TimeFrameWindow(start, end)
                }
                day to parsed
            }.toMap()
            trySend(TimeFrameSchedule(enabled, windowsByDay))
        }
        awaitClose { registration.remove() }
    }

    suspend fun setTimeFrame(familyId: String, schedule: TimeFrameSchedule) {
        val windowsByDay = schedule.windowsByDay.entries.associate { (day, windows) ->
            day.name to windows.map { mapOf(FIELD_TF_START to it.startMinute, FIELD_TF_END to it.endMinute) }
        }
        db.collection("families").document(familyId)
            .collection("limits").document("timeFrame")
            .set(mapOf(FIELD_TF_ENABLED to schedule.enabled, FIELD_TF_WINDOWS_BY_DAY to windowsByDay))
            .await()
    }

    fun allowAllDayFlow(familyId: String): Flow<String?> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("limits").document("allDay")
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "allowAllDayFlow($familyId) listener failed", error)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.getString(FIELD_ALLDAY_DATE))
        }
        awaitClose { registration.remove() }
    }

    suspend fun setAllowAllDay(familyId: String, date: String?) {
        val ref = db.collection("families").document(familyId)
            .collection("limits").document("allDay")
        if (date == null) {
            ref.set(mapOf(FIELD_ALLDAY_DATE to FieldValue.delete()), SetOptions.merge()).await()
        } else {
            ref.set(mapOf(FIELD_ALLDAY_DATE to date)).await()
        }
    }

    fun instantLockFlow(familyId: String): Flow<Boolean> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("limits").document("instantLock")
        var lastKnown: Boolean? = null
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "instantLockFlow($familyId) listener failed", error)
                // Fail-secure: re-emit the last known value, or default to locked
                // if we never received one. Defaulting to false here would allow
                // a child to bypass an active instant-lock by forcing listener errors.
                trySend(lastKnown ?: true)
                return@addSnapshotListener
            }
            val value = snap?.getBoolean(FIELD_INSTANT_LOCKED) ?: false
            lastKnown = value
            trySend(value)
        }
        awaitClose { registration.remove() }
    }

    suspend fun setInstantLock(familyId: String, locked: Boolean) {
        db.collection("families").document(familyId)
            .collection("limits").document("instantLock")
            .set(mapOf(FIELD_INSTANT_LOCKED to locked))
            .await()
    }

    suspend fun removeLimit(familyId: String, packageName: String) {
        db.collection("families").document(familyId)
            .collection("limits").document("perApp")
            .collection("apps").document(packageName)
            .delete()
            .await()
    }

    /**
     * Overwrites /families/{id}/tvApps with the TV's current set of
     * launchable apps (packageName -> display label), so mobile can offer
     * them when setting limits. Removes entries for uninstalled apps.
     */
    suspend fun syncInstalledApps(familyId: String, apps: Map<String, String>) {
        val collection = db.collection("families").document(familyId).collection("tvApps")
        val existing = collection.get().await().documents
        db.runBatch { batch ->
            apps.forEach { (pkg, label) ->
                batch.set(collection.document(pkg), mapOf(FIELD_LABEL to label))
            }
            existing.filter { it.id !in apps.keys }.forEach { doc ->
                batch.delete(collection.document(doc.id))
            }
        }.await()
    }

    fun installedAppsFlow(familyId: String): Flow<List<InstalledApp>> = callbackFlow {
        val ref = db.collection("families").document(familyId).collection("tvApps")
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "installedAppsFlow($familyId) listener failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val apps = snap?.documents.orEmpty().mapNotNull { doc ->
                val label = doc.getString(FIELD_LABEL) ?: return@mapNotNull null
                InstalledApp(doc.id, label)
            }.sortedBy { it.label }
            trySend(apps)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Watches /families/{id}/settings/lockout — the configurable response to
     * repeated wrong codes (see [LockoutSettings]) plus the TV's live
     * locked/lockedUntil state.
     */
    fun lockoutFlow(familyId: String): Flow<LockoutSettings> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("settings").document("lockout")
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "lockoutFlow($familyId) listener failed", error)
                trySend(LockoutSettings())
                return@addSnapshotListener
            }
            val durationMinutes = snap?.getLong("durationMinutes")?.toInt()
                ?: LockoutSettings.DEFAULT_DURATION_MINUTES
            val mode = if (snap?.getString("mode") == "parent") {
                LockoutMode.PARENT_UNLOCK
            } else {
                LockoutMode.TIMER
            }
            val locked = snap?.getBoolean("locked") ?: false
            val lockedUntil = snap?.getTimestamp("lockedUntil")?.toDate()?.toInstant()
            trySend(LockoutSettings(durationMinutes, mode, locked, lockedUntil))
        }
        awaitClose { registration.remove() }
    }

    /** Parent-configurable lockout duration/mode (does not change live lock state). */
    suspend fun setLockoutConfig(familyId: String, durationMinutes: Int, mode: LockoutMode) {
        db.collection("families").document(familyId)
            .collection("settings").document("lockout")
            .set(
                mapOf(
                    "durationMinutes" to durationMinutes,
                    "mode" to if (mode == LockoutMode.PARENT_UNLOCK) "parent" else "timer",
                ),
                SetOptions.merge(),
            ).await()
    }

    /** Called by the TV after [LockoutSettings.MAX_ATTEMPTS] wrong codes within [LockoutSettings.ATTEMPT_WINDOW_SECONDS]. */
    suspend fun triggerLockout(familyId: String, durationMinutes: Int, mode: LockoutMode) {
        val update = mutableMapOf<String, Any>("locked" to true)
        update["lockedUntil"] = if (mode == LockoutMode.TIMER) {
            Timestamp(Instant.now().plusSeconds(durationMinutes * 60L).epochSecond, 0)
        } else {
            FieldValue.delete()
        }
        db.collection("families").document(familyId)
            .collection("settings").document("lockout")
            .set(update, SetOptions.merge())
            .await()
    }

    /** Called by the TV when a timer-based lockout expires, or by a parent unlocking remotely. */
    suspend fun clearLockout(familyId: String) {
        db.collection("families").document(familyId)
            .collection("settings").document("lockout")
            .set(mapOf("locked" to false, "lockedUntil" to FieldValue.delete()), SetOptions.merge())
            .await()
    }

    fun usageFlow(familyId: String, date: LocalDate): Flow<UsageSnapshot> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("usage").document(date.toString())
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "usageFlow($familyId, $date) listener failed", error)
                trySend(UsageSnapshot(date.toString(), emptyMap()))
                return@addSnapshotListener
            }
            val raw = snap?.get(FIELD_PER_APP_MILLIS) as? Map<*, *>
            val parsed = raw.orEmpty()
                .mapNotNull { (k, v) ->
                    val key = k as? String ?: return@mapNotNull null
                    val value = (v as? Number)?.toLong() ?: return@mapNotNull null
                    key to value
                }.toMap()
            trySend(UsageSnapshot(date.toString(), parsed))
        }
        awaitClose { registration.remove() }
    }

    /**
     * Merge-writes per-package millis. The map under [FIELD_PER_APP_MILLIS]
     * is keyed by package name; we set each entry rather than overwriting
     * the whole map so concurrent updates from multiple TV samples coexist.
     */
    suspend fun recordUsage(
        familyId: String,
        date: LocalDate,
        perPackageMillis: Map<String, Long>,
    ) {
        if (perPackageMillis.isEmpty()) return
        val payload = mapOf(
            FIELD_PER_APP_MILLIS to perPackageMillis,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        db.collection("families").document(familyId)
            .collection("usage").document(date.toString())
            .set(payload, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    /**
     * Generates a random 6-digit code, writes /families/{id}/codes/{code},
     * returns the created [OneTimeCode]. 1 000 000-code space combined with
     * the server-side lockout (5 wrong / 60 s) makes online brute force
     * infeasible. Retries on the (very rare) collision until a free code is
     * found.
     */
    suspend fun createCode(familyId: String, extraMinutes: Int): OneTimeCode {
        val expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS)
        val codesCollection = db.collection("families").document(familyId).collection("codes")
        repeat(MAX_CODE_ATTEMPTS) {
            val candidate = "%06d".format(Random.nextInt(0, 1_000_000))
            val docRef = codesCollection.document(candidate)
            val snap = docRef.get().await()
            if (snap.exists()) return@repeat
            docRef.set(
                mapOf(
                    "extraMinutes" to extraMinutes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to Timestamp(expiresAt.epochSecond, 0),
                ),
            ).await()
            return OneTimeCode(candidate, extraMinutes, expiresAt)
        }
        error("Could not allocate a unique 6-digit code after $MAX_CODE_ATTEMPTS attempts.")
    }

    /**
     * Redeems a 4-digit code via the [redeemCode] Cloud Function, which
     * enforces the lockout server-side. Returns the granted extra minutes.
     * Throws [FirebaseFunctionsException] with:
     *  - NOT_FOUND → wrong or expired code (server already incremented the
     *    failure counter and may have triggered lockout),
     *  - FAILED_PRECONDITION → the device is currently locked out.
     */
    suspend fun redeemCode(familyId: String, code: String): Int {
        val result = functions.getHttpsCallable("redeemCode")
            .call(mapOf("familyId" to familyId, "code" to code))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?>
        return (data?.get("extraMinutes") as? Number)?.toInt()
            ?: error("redeemCode: extraMinutes missing in response.")
    }

    suspend fun createRequest(
        familyId: String,
        appPackage: String,
        requestedMinutes: Int,
    ): String {
        val id = UUID.randomUUID().toString()
        db.collection("families").document(familyId)
            .collection("requests").document(id)
            .set(
                mapOf(
                    "appPackage" to appPackage,
                    "requestedMinutes" to requestedMinutes,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
        return id
    }

    fun requestFlow(familyId: String, requestId: String): Flow<TimeRequest?> = callbackFlow {
        val ref = db.collection("families").document(familyId)
            .collection("requests").document(requestId)
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "requestFlow($familyId, $requestId) listener failed", error)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.let(::snapToRequest))
        }
        awaitClose { registration.remove() }
    }

    fun recentRequestsFlow(familyId: String, limit: Long = 20): Flow<List<TimeRequest>> = callbackFlow {
        val query = db.collection("families").document(familyId)
            .collection("requests")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
        val registration = query.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "recentRequestsFlow($familyId) listener failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snap?.documents.orEmpty().mapNotNull(::snapToRequest))
        }
        awaitClose { registration.remove() }
    }

    suspend fun respondToRequest(
        familyId: String,
        requestId: String,
        approvedMinutes: Int?,
    ) {
        val status = if (approvedMinutes == null) "denied" else "approved"
        val update = buildMap<String, Any> {
            put("status", status)
            put("respondedAt", FieldValue.serverTimestamp())
            if (approvedMinutes != null) put("approvedMinutes", approvedMinutes)
        }
        db.collection("families").document(familyId)
            .collection("requests").document(requestId)
            .update(update)
            .await()
    }

    /**
     * Creates /families/{newId} with [uid] as the owner (the single main
     * admin) and sole member, and sets /users/{uid}.familyId. The schema is
     * enforced by security rules: ownerUid + a roles map + an empty devices
     * list. Returns the new familyId.
     */
    suspend fun createFamily(uid: String): String {
        val familyRef = db.collection("families").document()
        val userRef = db.collection("users").document(uid)
        db.runBatch { batch ->
            batch.set(
                familyRef,
                mapOf(
                    "ownerUid" to uid,
                    "roles" to mapOf(uid to ROLE_ADMIN),
                    "devices" to emptyList<String>(),
                ),
            )
            batch.set(userRef, mapOf("familyId" to familyRef.id), SetOptions.merge())
        }.await()
        return familyRef.id
    }

    /**
     * Issues a family invite via the `createFamilyInvite` Cloud Function (only
     * admins may invite; the code is written server-side so /invites stays
     * closed to clients). Returns the 6-digit code.
     */
    suspend fun generateInvite(familyId: String, createdByUid: String): String {
        val result = functions.getHttpsCallable("createFamilyInvite")
            .call(mapOf("familyId" to familyId))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?>
        return data?.get("code") as? String
            ?: error("Invite creation failed: no code returned.")
    }

    /**
     * Redeems an invite via the `joinFamilyWithInvite` Cloud Function, which
     * adds the caller to the family as a regular "user". Returns the joined
     * familyId, or null if the code is invalid/expired/used.
     */
    suspend fun joinFamilyByInvite(code: String, uid: String): String? {
        return try {
            val result = functions.getHttpsCallable("joinFamilyWithInvite")
                .call(mapOf("code" to code))
                .await()
            @Suppress("UNCHECKED_CAST")
            (result.getData() as? Map<String, Any?>)?.get("familyId") as? String
        } catch (e: FirebaseFunctionsException) {
            if (e.isInvalidCode()) null else throw e
        }
    }

    fun familyFlow(familyId: String): Flow<Family?> = callbackFlow {
        val ref = db.collection("families").document(familyId)
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "familyFlow($familyId) listener failed", error)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.let(::snapToFamily))
        }
        awaitClose { registration.remove() }
    }

    /** Admin-only: promote/demote a member. Rules forbid touching the owner. */
    suspend fun setMemberRole(familyId: String, uid: String, role: FamilyRole) {
        db.collection("families").document(familyId)
            .update("roles.$uid", if (role == FamilyRole.ADMIN) ROLE_ADMIN else ROLE_USER)
            .await()
    }

    /** Admin-only: remove a member from the family (revokes their access). */
    suspend fun removeMember(familyId: String, uid: String) {
        db.collection("families").document(familyId)
            .update("roles.$uid", FieldValue.delete())
            .await()
    }

    /**
     * The TV requests a pairing code via the `createTvPairing` Cloud Function
     * (the code is written server-side keyed to the caller's anonymous uid =
     * [deviceId]). Returns the 6-digit code.
     */
    suspend fun createPairing(deviceId: String): String {
        val result = functions.getHttpsCallable("createTvPairing").call().await()
        @Suppress("UNCHECKED_CAST")
        return (result.getData() as? Map<String, Any?>)?.get("code") as? String
            ?: error("Pairing code creation failed: no code returned.")
    }

    /**
     * The family OWNER claims a pairing code for [familyId] via the
     * `claimTvPairing` Cloud Function, which binds the TV (enforcing the
     * one-TV-per-family relationship) and deletes the pairing doc. Returns
     * true on success, false if the code is invalid/expired or the one-to-one
     * constraint is violated.
     */
    suspend fun claimPairing(code: String, familyId: String): Boolean {
        return try {
            val result = functions.getHttpsCallable("claimTvPairing")
                .call(mapOf("code" to code, "familyId" to familyId))
                .await()
            @Suppress("UNCHECKED_CAST")
            (result.getData() as? Map<String, Any?>)?.get("success") as? Boolean ?: false
        } catch (e: FirebaseFunctionsException) {
            if (e.isInvalidCode()) false else throw e
        }
    }

    fun deviceFamilyFlow(deviceId: String): Flow<String?> = callbackFlow {
        val ref = db.collection("devices").document(deviceId)
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "deviceFamilyFlow($deviceId) listener failed", error)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snap?.getString("familyId"))
        }
        awaitClose { registration.remove() }
    }

    /**
     * Watches /families/{id}.devices (paired TV device ids) and resolves
     * each device's display name from /devices/{deviceId}.
     */
    fun pairedDevicesFlow(familyId: String): Flow<List<PairedDevice>> = callbackFlow {
        val ref = db.collection("families").document(familyId)
        val registration = ref.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "pairedDevicesFlow($familyId) listener failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val ids = (snap?.get(FIELD_DEVICES) as? List<*>).orEmpty().filterIsInstance<String>()
            launch {
                val devices = ids.map { id ->
                    val doc = runCatching { db.collection("devices").document(id).get().await() }.getOrNull()
                    PairedDevice(id, doc?.getString(FIELD_DEVICE_NAME) ?: PairedDevice.DEFAULT_NAME)
                }
                trySend(devices)
            }
        }
        awaitClose { registration.remove() }
    }

    /** Renames a paired TV as shown on the mobile pairing screen. */
    suspend fun renameDevice(deviceId: String, name: String) {
        db.collection("devices").document(deviceId)
            .set(mapOf(FIELD_DEVICE_NAME to name), SetOptions.merge())
            .await()
    }

    /**
     * Removes [deviceId] from the family and deletes its device doc. The TV
     * loses its familyId and falls back to the pairing screen.
     */
    suspend fun unpairDevice(familyId: String, deviceId: String) {
        db.collection("families").document(familyId)
            .update(FIELD_DEVICES, FieldValue.arrayRemove(deviceId))
            .await()
        db.collection("devices").document(deviceId).delete().await()
    }

    private fun snapToFamily(doc: com.google.firebase.firestore.DocumentSnapshot): Family? {
        val ownerUid = doc.getString("ownerUid") ?: return null
        val rawRoles = doc.get("roles") as? Map<*, *> ?: emptyMap<Any, Any>()
        val members = rawRoles.mapNotNull { (k, v) ->
            val uid = k as? String ?: return@mapNotNull null
            val role = if (v == ROLE_ADMIN) FamilyRole.ADMIN else FamilyRole.USER
            uid to role
        }.toMap()
        val devices = (doc.get(FIELD_DEVICES) as? List<*>).orEmpty().filterIsInstance<String>()
        return Family(doc.id, ownerUid, members, devices)
    }

    /** Cloud Function error codes we treat as "code invalid/expired/used". */
    private fun FirebaseFunctionsException.isInvalidCode(): Boolean =
        code == FirebaseFunctionsException.Code.NOT_FOUND ||
            code == FirebaseFunctionsException.Code.FAILED_PRECONDITION

    private fun snapToRequest(doc: com.google.firebase.firestore.DocumentSnapshot): TimeRequest? {
        val appPackage = doc.getString("appPackage") ?: return null
        val requestedMinutes = doc.getLong("requestedMinutes")?.toInt() ?: return null
        val status = when (doc.getString("status")) {
            "approved" -> TimeRequest.Status.Approved
            "denied" -> TimeRequest.Status.Denied
            else -> TimeRequest.Status.Pending
        }
        val approvedMinutes = doc.getLong("approvedMinutes")?.toInt()
        val createdAt = doc.getTimestamp("createdAt")?.toDate()?.toInstant() ?: Instant.EPOCH
        val respondedAt = doc.getTimestamp("respondedAt")?.toDate()?.toInstant()
        return TimeRequest(doc.id, appPackage, requestedMinutes, status, approvedMinutes, createdAt, respondedAt)
    }

    private companion object {
        const val TAG = "FirestoreRepository"
        const val FIELD_MINUTES = "dailyLimitMinutes"
        const val FIELD_OVERALL_MINUTES = "overallDailyMinutes"
        const val FIELD_LABEL = "label"
        const val FIELD_PER_APP_MILLIS = "perAppMillis"
        const val FIELD_DEVICES = "devices"
        const val FIELD_DEVICE_NAME = "name"
        const val ROLE_ADMIN = "admin"
        const val ROLE_USER = "user"
        const val CODE_TTL_SECONDS = 5 * 60L
        const val MAX_CODE_ATTEMPTS = 10
        const val FIELD_TF_ENABLED = "enabled"
        const val FIELD_TF_WINDOWS_BY_DAY = "windowsByDay"
        const val FIELD_TF_START = "start"
        const val FIELD_TF_END = "end"
        const val FIELD_ALLDAY_DATE = "date"
        const val FIELD_INSTANT_LOCKED = "locked"
    }
}
