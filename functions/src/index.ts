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
 * The family OWNER (main admin) claims a pairing code. Enforces a strict
 * one-to-one relationship: the TV must not already belong to another family,
 * and the family must not already have a TV paired.
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
  await db.runTransaction(async (tx) => {
    const pairing = await tx.get(pairingRef);
    if (!pairing.exists) throw new HttpsError("not-found", "Invalid code.");
    const expiresAt = pairing.get("expiresAt") as Timestamp | undefined;
    if (expiresAt && expiresAt.toMillis() < Date.now()) {
      tx.delete(pairingRef);
      throw new HttpsError("not-found", "This code has expired.");
    }

    const deviceId = pairing.get("deviceId") as string;
    const familyRef = db.collection("families").doc(familyId);
    const familySnap = await tx.get(familyRef);
    const devices = (familySnap.get("devices") as string[] | undefined) ?? [];

    // One-to-one: this family already controls a (different) TV.
    if (devices.length > 0 && !devices.includes(deviceId)) {
      throw new HttpsError(
        "failed-precondition",
        "This family is already paired with a TV.",
      );
    }
    // One-to-one: this TV already belongs to another family.
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
  });

  return { success: true };
});
