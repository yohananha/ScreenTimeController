/**
 * Boots firebase-functions-test in online mode (talks to the running emulators),
 * lazy-loads the production index.ts so per-test env vars (e.g.
 * FIRESTORE_EMULATOR_HOST) are picked up by initializeApp(), and exposes
 * wrapped callable/firestore-trigger entry points.
 *
 * Usage:
 *   const fft = initFFT();
 *   const fns = loadFunctions();
 *   const wrapped = fft.wrap(fns.redeemCode);
 *   await wrapped({ data: { familyId, code }, auth: { uid: deviceId } });
 */

import functionsTestInit from "firebase-functions-test";
import type { FeaturesList } from "firebase-functions-test/lib/features";
import { getFirestore } from "firebase-admin/firestore";

let _fft: FeaturesList | null = null;
let _fns: typeof import("../index") | null = null;

export function initFFT(): FeaturesList {
  if (_fft) return _fft;
  _fft = functionsTestInit({
    projectId: process.env.GCLOUD_PROJECT ?? "demo-screentime",
  });
  return _fft;
}

export function loadFunctions(): typeof import("../index") {
  if (_fns) return _fns;
  // Require *after* env vars are set so admin.initializeApp picks up emulator.
  _fns = require("../index");
  return _fns!;
}

/** Callable auth for a Google-signed-in parent (vs. the TV's anonymous uid). */
export function parentAuth(uid: string) {
  return { uid, token: { firebase: { sign_in_provider: "google.com" } } };
}

/** Callable auth for an anonymous account, as the TV uses. */
export function anonAuth(uid: string) {
  return { uid, token: { firebase: { sign_in_provider: "anonymous" } } };
}

export function db() {
  return getFirestore();
}

export async function seedFamily(opts: {
  familyId: string;
  ownerUid: string;
  roles?: Record<string, string>;
  devices?: string[];
  fcmTokens?: string[];
}) {
  const roles = opts.roles ?? { [opts.ownerUid]: "admin" };
  await db().collection("families").doc(opts.familyId).set({
    ownerUid: opts.ownerUid,
    roles,
    devices: opts.devices ?? [],
    ...(opts.fcmTokens ? { fcmTokens: opts.fcmTokens } : {}),
  });
}

/** A user whose users/{uid}.familyId is [familyId], with FCM [tokens] in their private push doc. */
export async function seedUser(uid: string, familyId: string, tokens: string[] = []) {
  const userRef = db().collection("users").doc(uid);
  await userRef.set({ familyId });
  await userRef.collection("private").doc("push").set({ tokens });
}

export async function readPushTokens(uid: string): Promise<string[] | undefined> {
  const snap = await db().collection("users").doc(uid).collection("private").doc("push").get();
  return snap.get("tokens") as string[] | undefined;
}

export async function seedCode(
  familyId: string,
  code: string,
  extraMinutes: number,
  expiresAtMs?: number,
) {
  const data: Record<string, unknown> = { extraMinutes };
  if (expiresAtMs !== undefined) {
    const { Timestamp } = await import("firebase-admin/firestore");
    data.expiresAt = Timestamp.fromMillis(expiresAtMs);
  }
  await db()
    .collection("families")
    .doc(familyId)
    .collection("codes")
    .doc(code)
    .set(data);
}

export async function readLockout(familyId: string) {
  const snap = await db()
    .collection("families")
    .doc(familyId)
    .collection("settings")
    .doc("lockout")
    .get();
  return snap.exists ? snap.data() : undefined;
}

export async function seedLockout(
  familyId: string,
  data: Record<string, unknown>,
) {
  await db()
    .collection("families")
    .doc(familyId)
    .collection("settings")
    .doc("lockout")
    .set(data, { merge: true });
}

beforeAll(() => {
  // Ensure emulator env vars are set BEFORE the index module is loaded.
  process.env.GCLOUD_PROJECT ??= "demo-screentime";
  process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";
  process.env.FIREBASE_AUTH_EMULATOR_HOST ??= "127.0.0.1:9099";
});

afterEach(async () => {
  // Clear Firestore and Auth between tests so each case starts from a
  // clean emulator. Lives here (not in setup.ts) because Jest globals are
  // only defined for files imported during test execution.
  const projectId = process.env.GCLOUD_PROJECT ?? "demo-screentime";
  const firestoreHost = process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080";
  const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? "127.0.0.1:9099";
  await Promise.allSettled([
    fetch(
      `http://${firestoreHost}/emulator/v1/projects/${projectId}/databases/(default)/documents`,
      { method: "DELETE" },
    ),
    fetch(`http://${authHost}/emulator/v1/projects/${projectId}/accounts`, {
      method: "DELETE",
    }),
  ]);
});

afterAll(() => {
  _fft?.cleanup();
});
