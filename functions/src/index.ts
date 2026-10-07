import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { PUSH_TIME_REQUEST, resolveLang } from "./strings";
import {
  FAMILY_ID_RE,
  SIX_DIGIT_RE,
  inviteCode,
  isInviteCode,
  normalizeInviteCode,
  numericCode,
  requireNonAnonymous,
  requireString,
} from "./codes";
import { consume } from "./rateLimit";

export { deleteAccount, deleteFamily } from "./deletion";

initializeApp();

const INVITE_TTL_MS = 48 * 60 * 60 * 1000;
const PAIRING_TTL_MS = 10 * 60 * 1000;
const MAX_CODE_ATTEMPTS = 10;
const DEFAULT_DEVICE_NAME = "Android TV";

// Per-uid attempt budgets for the guessable endpoints. Counted on every call
// (not just failures) so the check-and-increment is one atomic transaction;
// a real parent joins or pairs a handful of times, far below these.
const JOIN_LIMIT = { max: 10, windowMs: 60 * 60 * 1000 };
const CLAIM_LIMIT = { max: 10, windowMs: 10 * 60 * 1000 };
const CREATE_PAIRING_LIMIT = { max: 5, windowMs: 10 * 60 * 1000 };

// Must stay in sync with LockoutSettings.kt companion constants.
const MAX_WRONG_CODE_ATTEMPTS = 5;
const ATTEMPT_WINDOW_MS = 60 * 1000;
const DEFAULT_LOCKOUT_MINUTES = 15;
// The Nth timer lockout within LOCKOUT_ESCALATION_WINDOW_MS escalates to a
// lock only a parent can lift.
const ESCALATE_AFTER_LOCKOUTS = 3;
const LOCKOUT_ESCALATION_WINDOW_MS = 24 * 60 * 60 * 1000;

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
    const appPackage = data.appPackage as unknown;
    const requestedMinutes = data.requestedMinutes as number;

    // appPackage comes from the TV and is used in a Firestore path below
    // (tvApps/{appPackage}) and in the push payload — accept only a real
    // Android package name.
    if (typeof appPackage !== "string" || !PACKAGE_NAME_RE.test(appPackage)) {
      logger.warn(`Rejecting request ${requestId}: malformed appPackage.`);
      await event.data?.ref.set({ status: "denied", deniedReason: "invalid_package" }, { merge: true });
      return;
    }

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

    // One push per family per REQUEST_COOLDOWN_MS: a child (or a tampered
    // TV) spamming "ask for more time" must not flood the parents' phones.
    // This request is itself in the window, so a second doc means a recent one.
    const db = getFirestore();
    const recent = await db
      .collection(`families/${familyId}/requests`)
      .where("createdAt", ">", Timestamp.fromMillis(Date.now() - REQUEST_COOLDOWN_MS))
      .limit(2)
      .get();
    if (recent.size > 1) {
      logger.info(`Rate-limiting request ${requestId} for family ${familyId}.`);
      await event.data?.ref.set({ status: "denied", deniedReason: "rate_limited" }, { merge: true });
      return;
    }

    const owners = await loadMemberTokens(familyId);
    const tokens = Array.from(owners.keys());
    if (tokens.length === 0) {
      logger.info(`No FCM tokens registered for family ${familyId}; skipping.`);
      return;
    }

    const [langSnap, appSnap] = await Promise.all([
      db.doc(`families/${familyId}/settings/language`).get(),
      db.doc(`families/${familyId}/tvApps/${appPackage}`).get(),
    ]);
    const lang = resolveLang(langSnap.get("code") as string | undefined);
    // Falls back to the raw package name only if the TV hasn't reported a
    // label yet — previously this was the *only* path, so a notification
    // always read "+30 min for com.google.android.youtube".
    const appLabel = sanitizeLabel(appSnap.get("label")) ?? appPackage;
    const copy = PUSH_TIME_REQUEST[lang];

    let sent = 0;
    const dead: string[] = [];
    for (let i = 0; i < tokens.length; i += FCM_MULTICAST_LIMIT) {
      const batch = tokens.slice(i, i + FCM_MULTICAST_LIMIT);
      const response = await getMessaging().sendEachForMulticast({
        tokens: batch,
        notification: {
          title: copy.title,
          body: copy.body(requestedMinutes, appLabel),
        },
        data: {
          familyId,
          requestId,
          appPackage,
          appLabel,
          requestedMinutes: String(requestedMinutes),
        },
        android: {
          priority: "high",
        },
      });
      sent += response.successCount;
      response.responses.forEach((r, idx) => {
        if (r.error && DEAD_TOKEN_ERRORS.has(r.error.code)) dead.push(batch[idx]);
      });
    }
    await pruneTokens(owners, dead);

    logger.info(`Sent ${sent}/${tokens.length} pushes for ${requestId}.`);
  },
);

const FCM_MULTICAST_LIMIT = 500;

/** Same shape the Firestore rules accept for package-name doc ids. */
const PACKAGE_NAME_RE = /^[A-Za-z0-9_.]{1,255}$/;
const REQUEST_COOLDOWN_MS = 60 * 1000;
const MAX_LABEL_CHARS = 60;

/**
 * The app label is written by the TV and shown in the parents' notification.
 * Strip control and bidi-override characters (which can visually reorder or
 * hide text, e.g. to disguise a message as a system alert) and cap the length.
 * Returns null when nothing usable is left, so the caller falls back to the
 * package name.
 */
export function sanitizeLabel(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const cleaned = raw
    // Whitespace (incl. newlines/tabs) first becomes single spaces, so
    // "Kids\nTV" stays two words; then the remaining C0/C1 controls and
    // Unicode bidi overrides/isolates are dropped.
    .replace(/\s+/g, " ")
    .replace(/[\u0000-\u001F\u007F-\u009F\u202A-\u202E\u2066-\u2069\u200E\u200F\u061C]/g, "")
    .trim();
  if (!cleaned) return null;
  return cleaned.length > MAX_LABEL_CHARS ? `${cleaned.slice(0, MAX_LABEL_CHARS - 1)}…` : cleaned;
}

/** FCM errors meaning the token will never work again (app uninstalled, token rotated). */
const DEAD_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

/** Where a member's FCM tokens live — readable/writable only by that user (see firestore.rules). */
function pushDoc(uid: string) {
  return getFirestore().collection("users").doc(uid).collection("private").doc("push");
}

/**
 * Maps each FCM token to the member who registered it, for everyone who
 * should hear about a family's requests: each uid in the family's `roles`
 * whose own users/{uid}.familyId still points here. A removed member drops
 * out of `roles`; a member who switched family fails the familyId check —
 * either way their devices stop getting this family's notifications.
 *
 * Tokens used to live on the family doc (readable by every member and the
 * TV, never removed). That field is ignored and deleted here.
 */
async function loadMemberTokens(familyId: string): Promise<Map<string, string>> {
  const db = getFirestore();
  const familyRef = db.collection("families").doc(familyId);
  const familyDoc = await familyRef.get();
  if (familyDoc.get("fcmTokens") !== undefined) {
    await familyRef.update({ fcmTokens: FieldValue.delete() });
  }

  const roles = (familyDoc.get("roles") as Record<string, string> | undefined) ?? {};
  const uids = Object.keys(roles);
  const owners = new Map<string, string>();
  if (uids.length === 0) return owners;

  const userDocs = await db.getAll(...uids.map((uid) => db.collection("users").doc(uid)));
  const current = userDocs.filter((d) => d.get("familyId") === familyId).map((d) => d.id);
  if (current.length === 0) return owners;

  const pushDocs = await db.getAll(...current.map(pushDoc));
  for (const snap of pushDocs) {
    const uid = snap.ref.parent.parent!.id;
    for (const token of (snap.get("tokens") as string[] | undefined) ?? []) {
      if (typeof token === "string" && token) owners.set(token, uid);
    }
  }
  return owners;
}

/** Removes tokens FCM reported as permanently invalid from their owners' push docs. */
async function pruneTokens(owners: Map<string, string>, dead: string[]): Promise<void> {
  if (dead.length === 0) return;
  const byUid = new Map<string, string[]>();
  for (const token of dead) {
    const uid = owners.get(token);
    if (uid) byUid.set(uid, [...(byUid.get(uid) ?? []), token]);
  }
  const batch = getFirestore().batch();
  byUid.forEach((tokens, uid) => {
    batch.set(pushDoc(uid), { tokens: FieldValue.arrayRemove(...tokens) }, { merge: true });
  });
  await batch.commit();
  logger.info(`Pruned ${dead.length} dead FCM token(s).`);
}

// ---------------------------------------------------------------------------
// Callable helpers
// ---------------------------------------------------------------------------

function requireAuth(req: CallableRequest): string {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
  return uid;
}

/**
 * Allocates a collision-free code from [generate] in [collection]. A doc whose
 * expiresAt has passed counts as free and is overwritten — otherwise expired
 * pairings (only deleted when claimed) would gradually fill the code space.
 */
async function allocateCode(
  collection: string,
  generate: () => string,
  build: (code: string) => Record<string, unknown>,
): Promise<string> {
  const db = getFirestore();
  for (let i = 0; i < MAX_CODE_ATTEMPTS; i++) {
    const code = generate();
    const ref = db.collection(collection).doc(code);
    const created = await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      const expiresAt = snap.get("expiresAt") as Timestamp | undefined;
      const live = snap.exists && (!expiresAt || expiresAt.toMillis() >= Date.now());
      if (live) return false;
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
  const familyId = requireString(req, "familyId", FAMILY_ID_RE);

  const db = getFirestore();
  const fam = await db.collection("families").doc(familyId).get();
  if (!fam.exists) {
    throw new HttpsError("not-found", "Family not found.", { reason: "family_not_found" });
  }
  const roles = (fam.get("roles") as Record<string, string> | undefined) ?? {};
  if (roles[uid] !== "admin") {
    throw new HttpsError("permission-denied", "Only admins can invite members.", {
      reason: "not_admin",
    });
  }

  const code = await allocateCode("invites", inviteCode, () => ({
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
  requireNonAnonymous(req);
  const raw = requireString(req, "code", /^[\sA-Za-z0-9-]{1,32}$/);
  await consume(`join_${uid}`, JOIN_LIMIT.max, JOIN_LIMIT.windowMs);

  // A well-formed-but-wrong guess and a malformed one get the same answer,
  // so the response never hints at the code format.
  const code = normalizeInviteCode(raw);
  if (!isInviteCode(code)) {
    throw new HttpsError("not-found", "Invalid code.", { reason: "code_invalid" });
  }

  const db = getFirestore();
  const inviteRef = db.collection("invites").doc(code);

  const familyId = await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists) {
      throw new HttpsError("not-found", "Invalid code.", { reason: "code_invalid" });
    }
    if (invite.get("used") === true) {
      throw new HttpsError("not-found", "This code has already been used.", {
        reason: "code_used",
      });
    }
    const expiresAt = invite.get("expiresAt") as Timestamp | undefined;
    if (expiresAt && expiresAt.toMillis() < Date.now()) {
      throw new HttpsError("not-found", "This code has expired.", { reason: "code_expired" });
    }
    const fid = invite.get("familyId") as string;
    const familyRef = db.collection("families").doc(fid);
    const fam = await tx.get(familyRef);
    if (!fam.exists) {
      throw new HttpsError("not-found", "Family no longer exists.", {
        reason: "family_not_found",
      });
    }

    // Already a member (e.g. an admin opening their own invite): keep their
    // role — writing "user" here would demote them, and the admin SDK skips
    // the rule that keeps the owner an admin. Leave the invite unused.
    const roles = (fam.get("roles") as Record<string, string> | undefined) ?? {};
    if (roles[uid] !== undefined) {
      tx.set(db.collection("users").doc(uid), { familyId: fid }, { merge: true });
      return fid;
    }

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
  await consume(`pairCreate_${deviceId}`, CREATE_PAIRING_LIMIT.max, CREATE_PAIRING_LIMIT.windowMs);
  const code = await allocateCode("pairings", () => numericCode(6), () => ({
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
  requireNonAnonymous(req);
  const code = requireString(req, "code", SIX_DIGIT_RE);
  const familyId = requireString(req, "familyId", FAMILY_ID_RE);
  await consume(`pairClaim_${uid}`, CLAIM_LIMIT.max, CLAIM_LIMIT.windowMs);

  const db = getFirestore();
  const fam = await db.collection("families").doc(familyId).get();
  if (!fam.exists) {
    throw new HttpsError("not-found", "Family not found.", { reason: "family_not_found" });
  }
  // Only the main admin (owner) may pair/control the TV.
  if (fam.get("ownerUid") !== uid) {
    throw new HttpsError("permission-denied", "Only the family owner can pair a TV.", {
      reason: "not_owner",
    });
  }

  const pairingRef = db.collection("pairings").doc(code);
  // Returning true from the transaction means the code was expired; we throw
  // AFTER the transaction so the cleanup delete actually commits (throwing
  // inside a transaction callback aborts it and rolls back all writes).
  const isExpired = await db.runTransaction(async (tx) => {
    const pairing = await tx.get(pairingRef);
    if (!pairing.exists) {
      throw new HttpsError("not-found", "Invalid code.", { reason: "code_invalid" });
    }
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
        { reason: "tv_already_paired" },
      );
    }

    tx.set(deviceRef, { familyId, name: DEFAULT_DEVICE_NAME });
    tx.update(familyRef, { devices: FieldValue.arrayUnion(deviceId) });
    tx.delete(pairingRef);
    return false;
  });

  if (isExpired) {
    throw new HttpsError("not-found", "This code has expired.", { reason: "code_expired" });
  }
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
  const familyId = requireString(req, "familyId", FAMILY_ID_RE);
  const code = requireString(req, "code", SIX_DIGIT_RE);

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
      throw new HttpsError("not-found", "Family not found.", { reason: "family_not_found" });
    }
    const devices = (famSnap.get("devices") as string[] | undefined) ?? [];
    if (!devices.includes(deviceId)) {
      throw new HttpsError("permission-denied", "Device not paired to this family.", {
        reason: "device_not_paired",
      });
    }

    // (2) Lockout — refuse before consuming the code. A timer lock whose
    // lockedUntil has passed is over by server time, whether or not the TV
    // has cleared it yet; any write below also clears it. A lock with no
    // lockedUntil (parent mode, or escalated) only ends when a parent
    // unlocks.
    const lockoutSnap = await tx.get(lockoutRef);
    const lockedUntil = lockoutSnap.get("lockedUntil") as Timestamp | undefined;
    const timerLockExpired =
      lockoutSnap.get("mode") !== "parent" &&
      lockedUntil !== undefined &&
      lockedUntil.toMillis() <= Date.now();
    const isLocked = lockoutSnap.get("locked") === true && !timerLockExpired;
    if (isLocked) {
      return { kind: "locked" };
    }
    const clearExpired: Record<string, unknown> =
      lockoutSnap.get("locked") === true
        ? { locked: false, lockedUntil: FieldValue.delete() }
        : {};

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

        // Escalation: the Nth timer lockout within 24h becomes a
        // parent-unlock lock, capping unattended guessing at roughly
        // MAX_WRONG_CODE_ATTEMPTS × (ESCALATE_AFTER_LOCKOUTS) tries per day.
        const lockWindowStart = (lockoutSnap.get("lockoutWindowStart") as Timestamp | undefined)?.toMillis();
        const lockWindowFresh =
          lockWindowStart === undefined || now - lockWindowStart > LOCKOUT_ESCALATION_WINDOW_MS;
        const lockoutCount = lockWindowFresh
          ? 1
          : ((lockoutSnap.get("lockoutCount") as number | undefined) ?? 0) + 1;
        const needsParent =
          lockoutSnap.get("mode") === "parent" || lockoutCount >= ESCALATE_AFTER_LOCKOUTS;

        tx.set(
          lockoutRef,
          {
            locked: true,
            failureCount: 0,
            failureWindowStart: FieldValue.delete(),
            lockedAt: FieldValue.serverTimestamp(),
            lockoutCount,
            lockoutWindowStart: lockWindowFresh
              ? Timestamp.fromMillis(now)
              : Timestamp.fromMillis(lockWindowStart!),
            // Always written: a parent lock must not inherit a stale
            // lockedUntil from an earlier timer lock (that would read as
            // already expired).
            lockedUntil: needsParent
              ? FieldValue.delete()
              : Timestamp.fromMillis(now + durationMinutes * 60_000),
          },
          { merge: true },
        );
      } else {
        const windowStart: Timestamp = windowExpired
          ? Timestamp.now()
          : (rawWindowStart ?? Timestamp.now());
        tx.set(
          lockoutRef,
          { ...clearExpired, failureCount: newCount, failureWindowStart: windowStart },
          { merge: true },
        );
      }
      return { kind: "wrong" };
    }

    // Success path — consume code and reset failure counter atomically.
    tx.delete(codeRef);
    tx.set(
      lockoutRef,
      { ...clearExpired, failureCount: 0, failureWindowStart: FieldValue.delete() },
      { merge: true },
    );
    return { kind: "ok", extraMinutes: minutes };
  });

  switch (outcome.kind) {
    case "locked":
      throw new HttpsError("failed-precondition", "Code entry is locked.", {
        reason: "locked_out",
      });
    case "wrong":
      throw new HttpsError("not-found", "Invalid or expired code.", {
        reason: "code_invalid_or_expired",
      });
    case "ok":
      return { extraMinutes: outcome.extraMinutes };
  }
});
