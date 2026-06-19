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

export async function seedUser(uid: string, fcmTokens: string[] = []) {
  await db().collection("users").doc(uid).set({ familyId: "", fcmTokens });
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

afterAll(() => {
  _fft?.cleanup();
});
