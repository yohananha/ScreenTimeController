package com.screentime.shared.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes the current family id as a StateFlow.
 *
 * Production bindings:
 * - TV: [DeviceFamilyIdProvider] (reads /devices/{deviceId}.familyId).
 * - Mobile: [FirestoreFamilyIdProvider] (reads /users/{uid}.familyId).
 */
interface FamilyIdProvider {
    val familyId: StateFlow<String?>
}
