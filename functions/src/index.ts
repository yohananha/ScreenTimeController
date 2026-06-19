import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";

initializeApp();

const INVITE_TTL_MS = 48 * 60 * 60 * 1000;
const PAIRING_TTL_MS = 10 * 60 * 1000;
const MAX_CODE_ATTEMPTS = 10;
const DEFAULT_DEVICE_NAME = "Android TV";

// Must stay in sync with LockoutSettings.kt companion constants.
const MAX_WRONG_CODE_ATTEMPTS = 5;
const ATTEMPT_WINDOW_MS = 60 * 1000;
const DEFAULT_LOCKOUT_MINUTES = 15;

// Bounds for /requests/{id}.requestedMinutes. Anything outside is rejected.
const MAX_REQUESTED_MINUTES = 240;

// ---------------------------------------------------------------------------
// Notification trigger
// ---------------------------------------------------------------------------

/**
 * Triggered when the TV writes a new request under
 * /families/{familyId}/requests/{requestId}. Fans out an FCM push to every
 * registered parent token for that family.
 */
export const onNewTimeRequest = onDocumentCreated(
  "families/{familyId}/requests/{requestId}",
  async (event) => {
    const data = event.data?.data();
    if (!data) {
      logger.warn("Request document has no data.");
      return;
    }

    const familyId = event.params.familyId;
    const requestId = event.params.requestId;
    const appPackage = data.appPackage as string;
    const requestedMinutes = data.requestedMinutes as number;

    // Defense-in-depth: the Firestore rule already bounds this on create,
    // but if anything writes an out-of-range value we drop the notification
    // and mark the request denied so the TV doesn't silently get more time.
    if (
      typeof requestedMinutes !== "number" ||
      !Number.isFinite(requestedMinutes) ||
      requestedMinutes < 1 ||
      requestedMinutes > MAX_REQUESTED_MINUTES
    ) {
      logger.warn(
        `Rejecting request ${requestId}: requestedMinutes=${requestedMinutes} out of range.`,
      );
      await event.data?.ref.set(
        { status: "denied", deniedReason: "invalid_minutes" },
        { merge: true },
      );
      return;
    }

    const tokens = await loadMemberTokens(familyId);
    if (tokens.length === 0) {
      logger.info(`No FCM tokens registered for family ${familyId}; skipping.`);
      return;
    }

    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "Time request",
        body: `+${requestedMinutes} min for ${appPackage}`,
      },
      data: {
        familyId,
        requestId,
        appPackage,
        requestedMinutes: String(requestedMinutes),
      },
      android: {
        priority: "high",
      },
    });

    logger.info(
      `Sent ${response.successCount}/${tokens.length} pushes for ${requestId}.`,
    );
  },
);

/**
 * Collects FCM tokens for everyone who should be notified about a family's
 * requests: every member in the family's `roles` map (admins + users).
 * Falls back to the legacy `fcmTokens` array and legacy `admins` array.
 */
async function loadMemberTokens(familyId: string): Promise<string[]> {
  const db = getFirestore();
  const familyDoc = await db.collection("families").doc(familyId).get();

  // Legacy demo-family path: tokens stored directly on the family doc.
  const direct = (familyDoc.get("fcmTokens") as string[] | undefined) ?? [];
  if (direct.length > 0) return Array.from(new Set(direct));

  const roles = (familyDoc.get("roles") as Record<string, string> | undefined) ?? {};
  const uids = Object.keys(roles);
  if (uids.length === 0) {
    // Legacy schema fallback.
    const admins = (familyDoc.get("admins") as string[] | undefined) ?? [];
    uids.push(...admins);
  }
  if (uids.length === 0) return [];

  const tokens: string[] = [];
  for (const uid of uids) {
    const userDoc = await db.collection("users").doc(uid).get();
    const userTokens = (userDoc.get("fcmTokens") as string[] | undefined) ?? [];
    tokens.push(...userTokens);
  }
  return Array.from(new Set(tokens));
}

// ---------------------------------------------------------------------------
// Callable helpers
// ---------------------------------------------------------------------------

function requireAuth(req: CallableRequest): string {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
  return uid;
}

function sixDigit(): string {
  return Math.floor(Math.random() * 1_000_000).toString().padStart(6, "0");
}

/** Allocates a collision-free 6-digit code in [collection]. */
async function allocateCode(
  collection: string,
  build: (code: string) => Record<string, unknown>,
): Promise<string> {
  const db = getFirestore();
  for (let i = 0; i < MAX_CODE_ATTEMPTS; i++) {
    const code = sixDigit();
    const ref = db.collection(collection).doc(code);
    const created = await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (snap.exists) return false;
      tx.set(ref, build(code));
      return true;
    });
    if (created) return code;
  }
  throw new HttpsError(
    "resource-exhausted",
    `Could not allocate a unique code in ${collection}.`,
  );
}

// ---------------------------------------------------------------------------
// Family invites (parent → parent)
// ---------------------------------------------------------------------------

/** An admin creates a 6-digit invite for their family (48h TTL). */
export const createFamilyInvite = onCall(async (req) => {
  const uid = requireAuth(req);
  const familyId = req.data?.familyId as string | undefined;
  if (!familyId) throw new HttpsError("invalid-argument", "familyId is required.");

  const db = getFirestore();
  const fam = await db.collection("families").doc(familyId).get();
  if (!fam.exists) throw new HttpsError("not-found", "Family not found.");
  const roles = (fam.get("roles") as Record<string, string> | undefined) ?? {};
  if (roles[uid] !== "admin") {
    throw new HttpsError("permission-denied", "Only admins can invite members.");
  }

  const code = await allocateCode("invites", () => ({
    familyId,
    createdBy: uid,
    createdAt: FieldValue.serverTimestamp(),
    expiresAt: Timestamp.fromMillis(Date.now() + INVITE_TTL_MS),
    used: false,
  }));
  return { code };
});

/**
 * The invited user redeems a code and joins the family as a regular "user"
 * (not admin). Validates existence/expiry/used server-side.
 */
export const joinFamilyWithInvite = onCall(async (req) => {
  const uid = requireAuth(req);
  const code = req.data?.code as string | undefined;
  if (!code) throw new HttpsError("invalid-argument", "code is required.");

  const db = getFirestore();
  const inviteRef = db.collection("invites").doc(code);

  const familyId = await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists) throw new HttpsError("not-found", "Invalid code.");
    if (invite.get("used") === true) {
      throw new HttpsError("not-found", "This code has already been used.");
    }
    const expiresAt = invite.get("expiresAt") as Timestamp | undefined;
    if (expiresAt && expiresAt.toMillis() < Date.now()) {
      throw new HttpsError("not-found", "This code has expired.");
    }
    const fid = invite.get("familyId") as string;
    const familyRef = db.collection("families").doc(fid);
    const fam = await tx.get(familyRef);
    if (!fam.exists) throw new HttpsError("not-found", "Family no longer exists.");

    tx.update(familyRef, { [`roles.${uid}`]: "user" });
    tx.set(db.collection("users").doc(uid), { familyId: fid }, { merge: true });
    tx.update(inviteRef, { used: true, usedBy: uid });
    return fid;
  });

  return { familyId };
});

// ---------------------------------------------------------------------------
// TV pairing (one TV ↔ one family, owner-controlled)
// ---------------------------------------------------------------------------

/** The TV (anonymous auth) requests a 6-digit pairing code (10m TTL). */
export const createTvPairing = onCall(async (req) => {
  const deviceId = requireAuth(req); // TV's anonymous uid == deviceId
  const code = await allocateCode("pairings", () => ({
    deviceId,
    createdAt: FieldValue.serverTimestamp(),
    expiresAt: Timestamp.fromMillis(Date.now() + PAIRING_TTL_MS),
  }));
  return { code };
});

/**
 * The family OWNER (main admin) claims a pairing code. A family may pair any
 * number of TVs; each TV may only belong to one family at a time.
 */
export const claimTvPairing = onCall(async (req) => {
  const uid = requireAuth(req);
  const code = req.data?.code as string | undefined;
  const familyId = req.data?.familyId as string | undefined;
  if (!code || !familyId) {
    throw new HttpsError("invalid-argument", "code and familyId are required.");
  }

  const db = getFirestore();
  const fam = await db.collection("families").doc(familyId).get();
  if (!fam.exists) throw new HttpsError("not-found", "Family not found.");
  // Only the main admin (owner) may pair/control the TV.
  if (fam.get("ownerUid") !== uid) {
    throw new HttpsError("permission-denied", "Only the family owner can pair a TV.");
  }

  const pairingRef = db.collection("pairings").doc(code);
  // Returning true from the transaction means the code was expired; we throw
  // AFTER the transaction so the cleanup delete actually commits (throwing
  // inside a transaction callback aborts it and rolls back all writes).
  const isExpired = await db.runTransaction(async (tx) => {
    const pairing = await tx.get(pairingRef);
    if (!pairing.exists) throw new HttpsError("not-found", "Invalid code.");
    const expiresAt = pairing.get("expiresAt") as Timestamp | undefined;
    if (expiresAt && expiresAt.toMillis() < Date.now()) {
      tx.delete(pairingRef);
      return true;
    }

    const deviceId = pairing.get("deviceId") as string;
    const familyRef = db.collection("families").doc(familyId);
    const familySnap = await tx.get(familyRef);
    const devices = (familySnap.get("devices") as string[] | undefined) ?? [];

    // Idempotent: already paired to this same family — nothing to do.
    if (devices.includes(deviceId)) {
      tx.delete(pairingRef);
      return;
    }

    // One TV ↔ one family: this TV already belongs to another family.
    const deviceRef = db.collection("devices").doc(deviceId);
    const deviceSnap = await tx.get(deviceRef);
    if (deviceSnap.exists && deviceSnap.get("familyId") !== familyId) {
      throw new HttpsError(
        "failed-precondition",
        "This TV is already paired with another family.",
      );
    }

    tx.set(deviceRef, { familyId, name: DEFAULT_DEVICE_NAME });
    tx.update(familyRef, { devices: FieldValue.arrayUnion(deviceId) });
    tx.delete(pairingRef);
    return false;
  });

  if (isExpired) throw new HttpsError("not-found", "This code has expired.");
  return { success: true };
});

// ---------------------------------------------------------------------------
// Code redemption (TV-only, server-side lockout enforcement)
// ---------------------------------------------------------------------------

/**
 * The TV submits a 6-digit unlock code. The pairing check, lockout check,
 * code validation, and failure-counter update all run in ONE transaction so
 * a paired device cannot:
 *   - race with a concurrent lockout-clear to enter a code while locked,
 *   - cause a failure counter to be incremented against a family it isn't
 *     paired to (the pairing check happens inside the same transaction),
 *   - exploit a torn read between the pairing/lockout reads and the
 *     code-consume write.
 *
 * On wrong/expired code: increments a server-side counter; triggers lockout
 * when MAX_WRONG_CODE_ATTEMPTS failures occur within ATTEMPT_WINDOW_MS.
 * On success: resets the failure counter and returns extraMinutes.
 *
 * Errors: FAILED_PRECONDITION = locked, NOT_FOUND = wrong/expired code,
 *         PERMISSION_DENIED = device not paired to this family.
 */
export const redeemCode = onCall(async (req) => {
  const deviceId = requireAuth(req);
  const familyId = req.data?.familyId as string | undefined;
  const code = req.data?.code as string | undefined;
  if (!familyId || !code) {
    throw new HttpsError("invalid-argument", "familyId and code are required.");
  }

  const db = getFirestore();
  const familyRef = db.collection("families").doc(familyId);
  const lockoutRef = familyRef.collection("settings").doc("lockout");
  const codeRef = familyRef.collection("codes").doc(code);

  type Outcome =
    | { kind: "ok"; extraMinutes: number }
    | { kind: "locked" }
    | { kind: "wrong" };

  const outcome: Outcome = await db.runTransaction(async (tx) => {
    // (1) Pairing — must be inside the txn so it sees a fresh devices array.
    const famSnap = await tx.get(familyRef);
    if (!famSnap.exists) {
      throw new HttpsError("not-found", "Family not found.");
    }
    const devices = (famSnap.get("devices") as string[] | undefined) ?? [];
    if (!devices.includes(deviceId)) {
      throw new HttpsError("permission-denied", "Device not paired to this family.");
    }

    // (2) Lockout — refuse before consuming the code.
    const lockoutSnap = await tx.get(lockoutRef);
    if (lockoutSnap.exists && lockoutSnap.get("locked") === true) {
      return { kind: "locked" };
    }

    // (3) Code validation + consumption.
    const codeSnap = await tx.get(codeRef);
    const expiresAt = codeSnap.exists
      ? (codeSnap.get("expiresAt") as Timestamp | undefined)
      : undefined;
    const expired =
      expiresAt !== undefined && expiresAt.toMillis() < Date.now();
    const minutes = codeSnap.exists
      ? (codeSnap.get("extraMinutes") as number | undefined)
      : undefined;

    if (!codeSnap.exists || expired || minutes == null) {
      // Wrong / expired — increment counter inside the same transaction.
      if (codeSnap.exists && expired) tx.delete(codeRef);

      const now = Date.now();
      const rawCount =
        (lockoutSnap.get("failureCount") as number | undefined) ?? 0;
      const rawWindowStart = lockoutSnap.get("failureWindowStart") as
        | Timestamp
        | undefined;
      const windowExpired =
        !rawWindowStart ||
        now - rawWindowStart.toMillis() > ATTEMPT_WINDOW_MS;
      const newCount = windowExpired ? 1 : rawCount + 1;

      if (newCount >= MAX_WRONG_CODE_ATTEMPTS) {
        const durationMinutes =
          (lockoutSnap.get("durationMinutes") as number | undefined) ??
          DEFAULT_LOCKOUT_MINUTES;
        const isParentMode = lockoutSnap.get("mode") === "parent";
        const update: Record<string, unknown> = {
          locked: true,
          failureCount: 0,
          failureWindowStart: FieldValue.delete(),
          lockedAt: FieldValue.serverTimestamp(),
        };
        if (!isParentMode) {
          update.lockedUntil = Timestamp.fromMillis(
            now + durationMinutes * 60_000,
          );
        }
        tx.set(lockoutRef, update, { merge: true });
      } else {
        const windowStart: Timestamp = windowExpired
          ? Timestamp.now()
          : (rawWindowStart ?? Timestamp.now());
        tx.set(
          lockoutRef,
          { failureCount: newCount, failureWindowStart: windowStart },
          { merge: true },
        );
      }
      return { kind: "wrong" };
    }

    // Success path — consume code and reset failure counter atomically.
    tx.delete(codeRef);
    tx.set(
      lockoutRef,
      { failureCount: 0, failureWindowStart: FieldValue.delete() },
      { merge: true },
    );
    return { kind: "ok", extraMinutes: minutes };
  });

  switch (outcome.kind) {
    case "locked":
      throw new HttpsError("failed-precondition", "Code entry is locked.");
    case "wrong":
      throw new HttpsError("not-found", "Invalid or expired code.");
    case "ok":
      return { extraMinutes: outcome.extraMinutes };
  }
});
