/**
 * Web port of shared/src/main/java/com/screentime/shared/firestore/FirestoreRepository.kt.
 *
 * Collection paths and field names below are copied verbatim from the Kotlin
 * source. The still-running TV app reads/writes the exact same documents —
 * do not rename a field here without updating FirestoreRepository.kt too.
 *
 * Layout:
 *   families/{familyId}/limits/overall          (single doc)
 *   families/{familyId}/limits/perApp/apps/{pkg}
 *   families/{familyId}/usage/{yyyy-MM-dd}      (single doc per day)
 */
import {
  arrayRemove,
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  onSnapshot,
  orderBy,
  query,
  limit as queryLimit,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  writeBatch,
  type DocumentData,
  type QueryDocumentSnapshot,
  type Unsubscribe,
} from 'firebase/firestore';
import { FirebaseError } from 'firebase/app';
import { httpsCallable } from 'firebase/functions';
import { db, functions } from './app';
import type { AppLimit, Limits } from '../models/Limits';
import { defaultLimits, DEFAULT_OVERALL_MINUTES } from '../models/Limits';
import type { DayOfWeek, TimeFrameSchedule, TimeFrameWindow } from '../models/TimeFrameSchedule';
import { DEFAULT_TIME_FRAME_SCHEDULE } from '../models/TimeFrameSchedule';
import type { LockoutMode, LockoutSettings } from '../models/LockoutSettings';
import { defaultLockoutSettings } from '../models/LockoutSettings';
import type { UsageSnapshot } from '../models/UsageSnapshot';
import type { OneTimeCode } from '../models/OneTimeCode';
import type { TimeRequest, TimeRequestStatus } from '../models/TimeRequest';
import type { Family, FamilyRole } from '../models/Family';
import type { InstalledApp } from '../models/InstalledApp';
import type { PairedDevice } from '../models/PairedDevice';
import { DEFAULT_DEVICE_NAME } from '../models/PairedDevice';

// ── field/path constants (verbatim from FirestoreRepository.kt's companion object) ──
const FIELD_MINUTES = 'dailyLimitMinutes';
const FIELD_OVERALL_MINUTES = 'overallDailyMinutes';
const FIELD_LABEL = 'label';
const FIELD_PER_APP_MILLIS = 'perAppMillis';
const FIELD_DEVICES = 'devices';
const FIELD_DEVICE_NAME = 'name';
const ROLE_ADMIN = 'admin';
const ROLE_USER = 'user';
const CODE_TTL_SECONDS = 5 * 60;
const MAX_CODE_ATTEMPTS = 10;
const FIELD_TF_ENABLED = 'enabled';
const FIELD_TF_WINDOWS_BY_DAY = 'windowsByDay';
const FIELD_TF_START = 'start';
const FIELD_TF_END = 'end';
const FIELD_ALLDAY_DATE = 'date';
const FIELD_INSTANT_LOCKED = 'locked';

function familyRef(familyId: string) {
  return doc(db, 'families', familyId);
}

// ── limits ──────────────────────────────────────────────────────────────────

export function subscribeOverallLimit(
  familyId: string,
  cb: (minutes: number) => void,
): Unsubscribe {
  const ref = doc(db, 'families', familyId, 'limits', 'overall');
  return onSnapshot(
    ref,
    (snap) => cb((snap.data()?.[FIELD_OVERALL_MINUTES] as number | undefined) ?? DEFAULT_OVERALL_MINUTES),
    () => cb(DEFAULT_OVERALL_MINUTES),
  );
}

export function subscribePerAppLimits(
  familyId: string,
  cb: (perApp: Record<string, AppLimit>) => void,
): Unsubscribe {
  const ref = collection(db, 'families', familyId, 'limits', 'perApp', 'apps');
  return onSnapshot(
    ref,
    (snap) => {
      const entries: Record<string, AppLimit> = {};
      snap.docs.forEach((d) => {
        const minutes = d.data()[FIELD_MINUTES] as number | undefined;
        if (minutes === undefined) return;
        entries[d.id] = { packageName: d.id, dailyLimitMinutes: minutes };
      });
      cb(entries);
    },
    () => cb({}),
  );
}

export async function setLimit(
  familyId: string,
  packageName: string,
  dailyLimitMinutes: number,
): Promise<void> {
  await setDoc(doc(db, 'families', familyId, 'limits', 'perApp', 'apps', packageName), {
    [FIELD_MINUTES]: dailyLimitMinutes,
  });
}

/**
 * Sets the cross-app daily cap: once today's total usage across all apps
 * reaches this many minutes, the TV blocks regardless of per-app limits.
 */
export async function setOverallLimit(familyId: string, minutes: number): Promise<void> {
  await setDoc(doc(db, 'families', familyId, 'limits', 'overall'), {
    [FIELD_OVERALL_MINUTES]: minutes,
  });
}

export async function removeLimit(familyId: string, packageName: string): Promise<void> {
  await deleteDoc(doc(db, 'families', familyId, 'limits', 'perApp', 'apps', packageName));
}

export function subscribeTimeFrame(
  familyId: string,
  cb: (schedule: TimeFrameSchedule) => void,
): Unsubscribe {
  const ref = doc(db, 'families', familyId, 'limits', 'timeFrame');
  return onSnapshot(
    ref,
    (snap) => {
      if (!snap.exists()) {
        cb(DEFAULT_TIME_FRAME_SCHEDULE);
        return;
      }
      const data = snap.data();
      const enabled = (data[FIELD_TF_ENABLED] as boolean | undefined) ?? false;
      const rawDays = (data[FIELD_TF_WINDOWS_BY_DAY] as Record<string, { start: number; end: number }[]> | undefined) ?? {};
      const windowsByDay: TimeFrameSchedule['windowsByDay'] = {};
      for (const [dayName, windows] of Object.entries(rawDays)) {
        const parsed: TimeFrameWindow[] = windows
          .filter((w) => typeof w[FIELD_TF_START] === 'number' && typeof w[FIELD_TF_END] === 'number')
          .map((w) => ({ startMinute: w[FIELD_TF_START], endMinute: w[FIELD_TF_END] }));
        windowsByDay[dayName as DayOfWeek] = parsed;
      }
      cb({ enabled, windowsByDay });
    },
    () => cb(DEFAULT_TIME_FRAME_SCHEDULE),
  );
}

export async function setTimeFrame(familyId: string, schedule: TimeFrameSchedule): Promise<void> {
  const windowsByDay: Record<string, { start: number; end: number }[]> = {};
  for (const [day, windows] of Object.entries(schedule.windowsByDay)) {
    windowsByDay[day] = (windows ?? []).map((w) => ({
      [FIELD_TF_START]: w.startMinute,
      [FIELD_TF_END]: w.endMinute,
    })) as { start: number; end: number }[];
  }
  await setDoc(doc(db, 'families', familyId, 'limits', 'timeFrame'), {
    [FIELD_TF_ENABLED]: schedule.enabled,
    [FIELD_TF_WINDOWS_BY_DAY]: windowsByDay,
  });
}

export function subscribeAllowAllDay(
  familyId: string,
  cb: (date: string | null) => void,
): Unsubscribe {
  const ref = doc(db, 'families', familyId, 'limits', 'allDay');
  return onSnapshot(
    ref,
    (snap) => cb((snap.data()?.[FIELD_ALLDAY_DATE] as string | undefined) ?? null),
    () => cb(null),
  );
}

export async function setAllowAllDay(familyId: string, date: string | null): Promise<void> {
  const ref = doc(db, 'families', familyId, 'limits', 'allDay');
  if (date === null) {
    await setDoc(ref, { [FIELD_ALLDAY_DATE]: deleteField() }, { merge: true });
  } else {
    await setDoc(ref, { [FIELD_ALLDAY_DATE]: date });
  }
}

/**
 * Fail-secure: on a listener error, re-emit the last known value, or default
 * to locked if none was ever received. Defaulting to unlocked would allow a
 * child to bypass an active instant-lock by forcing listener errors.
 */
export function subscribeInstantLock(
  familyId: string,
  cb: (locked: boolean) => void,
): Unsubscribe {
  const ref = doc(db, 'families', familyId, 'limits', 'instantLock');
  let lastKnown: boolean | null = null;
  return onSnapshot(
    ref,
    (snap) => {
      const value = (snap.data()?.[FIELD_INSTANT_LOCKED] as boolean | undefined) ?? false;
      lastKnown = value;
      cb(value);
    },
    () => cb(lastKnown ?? true),
  );
}

export async function setInstantLock(familyId: string, locked: boolean): Promise<void> {
  await setDoc(doc(db, 'families', familyId, 'limits', 'instantLock'), {
    [FIELD_INSTANT_LOCKED]: locked,
  });
}

/** Combines the 5 limits sub-docs into one Limits object, mirroring FirestoreRepository.limitsFlow. */
export function subscribeLimits(familyId: string, cb: (limits: Limits) => void): Unsubscribe {
  const state: Limits = defaultLimits();
  const received = { overall: false, perApp: false, timeFrame: false, allDay: false, instant: false };
  const emit = () => {
    if (Object.values(received).every(Boolean)) cb({ ...state });
  };
  const unsubs = [
    subscribeOverallLimit(familyId, (v) => {
      state.overallDailyMinutes = v;
      received.overall = true;
      emit();
    }),
    subscribePerAppLimits(familyId, (v) => {
      state.perApp = v;
      received.perApp = true;
      emit();
    }),
    subscribeTimeFrame(familyId, (v) => {
      state.timeFrame = v;
      received.timeFrame = true;
      emit();
    }),
    subscribeAllowAllDay(familyId, (v) => {
      state.allowAllDayDate = v;
      received.allDay = true;
      emit();
    }),
    subscribeInstantLock(familyId, (v) => {
      state.instantLocked = v;
      received.instant = true;
      emit();
    }),
  ];
  return () => unsubs.forEach((u) => u());
}

// ── installed apps (TV-synced app catalog) ────────────────────────────────
// syncInstalledApps is TV-only (writes tvApps) — not ported; web never syncs local apps.

export function subscribeInstalledApps(
  familyId: string,
  cb: (apps: InstalledApp[]) => void,
): Unsubscribe {
  const ref = collection(db, 'families', familyId, 'tvApps');
  return onSnapshot(
    ref,
    (snap) => {
      const apps = snap.docs
        .map((d) => {
          const label = d.data()[FIELD_LABEL] as string | undefined;
          return label ? { packageName: d.id, label } : null;
        })
        .filter((a): a is InstalledApp => a !== null)
        .sort((a, b) => a.label.localeCompare(b.label));
      cb(apps);
    },
    () => cb([]),
  );
}

// ── lockout ─────────────────────────────────────────────────────────────────

export function subscribeLockout(
  familyId: string,
  cb: (settings: LockoutSettings) => void,
): Unsubscribe {
  const ref = doc(db, 'families', familyId, 'settings', 'lockout');
  return onSnapshot(
    ref,
    (snap) => {
      const data = snap.data();
      const durationMinutes = (data?.durationMinutes as number | undefined) ?? defaultLockoutSettings().durationMinutes;
      const mode: LockoutMode = data?.mode === 'parent' ? 'PARENT_UNLOCK' : 'TIMER';
      const locked = (data?.locked as boolean | undefined) ?? false;
      const lockedUntilTs = data?.lockedUntil as Timestamp | undefined;
      cb({ durationMinutes, mode, locked, lockedUntil: lockedUntilTs ? lockedUntilTs.toDate() : null });
    },
    () => cb(defaultLockoutSettings()),
  );
}

/** Parent-configurable lockout duration/mode (does not change live lock state). */
export async function setLockoutConfig(
  familyId: string,
  durationMinutes: number,
  mode: LockoutMode,
): Promise<void> {
  await setDoc(
    doc(db, 'families', familyId, 'settings', 'lockout'),
    { durationMinutes, mode: mode === 'PARENT_UNLOCK' ? 'parent' : 'timer' },
    { merge: true },
  );
}

/** Called by the TV when a timer-based lockout expires, or by a parent unlocking remotely. */
export async function clearLockout(familyId: string): Promise<void> {
  await setDoc(
    doc(db, 'families', familyId, 'settings', 'lockout'),
    { locked: false, lockedUntil: deleteField() },
    { merge: true },
  );
}

// ── language ────────────────────────────────────────────────────────────────
// What the TV (and server-rendered push) render — same families/{id}/settings/language
// doc the mobile app writes. Bare "en"/"he" only; see functions/src/strings.ts.

export function subscribeLanguage(familyId: string, cb: (tag: 'en' | 'he' | null) => void): Unsubscribe {
  return onSnapshot(
    doc(db, 'families', familyId, 'settings', 'language'),
    (snap) => {
      const tag = snap.data()?.code as string | undefined;
      cb(tag === 'en' || tag === 'he' ? tag : null);
    },
    () => cb(null),
  );
}

export async function setLanguage(familyId: string, tag: 'en' | 'he'): Promise<void> {
  await setDoc(doc(db, 'families', familyId, 'settings', 'language'), { code: tag }, { merge: true });
}

/** The signed-in user's own portable language preference; null = follow device. */
export function subscribeUserLanguage(uid: string, cb: (tag: 'en' | 'he' | null) => void): Unsubscribe {
  return onSnapshot(
    doc(db, 'users', uid),
    (snap) => {
      const tag = snap.data()?.language as string | undefined;
      cb(tag === 'en' || tag === 'he' ? tag : null);
    },
    () => cb(null),
  );
}

/** null clears the preference (back to "follow device"); always merges so familyId/fcmTokens survive. */
export async function setUserLanguage(uid: string, tag: 'en' | 'he' | null): Promise<void> {
  await setDoc(doc(db, 'users', uid), { language: tag ?? deleteField() }, { merge: true });
}

// ── usage ───────────────────────────────────────────────────────────────────

export function subscribeUsage(
  familyId: string,
  date: string,
  cb: (snapshot: UsageSnapshot) => void,
): Unsubscribe {
  const ref = doc(db, 'families', familyId, 'usage', date);
  return onSnapshot(
    ref,
    (snap) => {
      const raw = (snap.data()?.[FIELD_PER_APP_MILLIS] as Record<string, number> | undefined) ?? {};
      cb({ date, perAppMillis: raw });
    },
    () => cb({ date, perAppMillis: {} }),
  );
}
// recordUsage is TV-only — not ported.

// ── unlock codes ────────────────────────────────────────────────────────────

/**
 * Generates a random 6-digit code, writes families/{id}/codes/{code}.
 * 1,000,000-code space combined with the server-side lockout (5 wrong / 60s)
 * makes online brute force infeasible. Retries on the (very rare) collision.
 */
export async function createCode(familyId: string, extraMinutes: number): Promise<OneTimeCode> {
  const expiresAt = new Date(Date.now() + CODE_TTL_SECONDS * 1000);
  const codesCollection = collection(db, 'families', familyId, 'codes');
  for (let attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
    const candidate = String(Math.floor(Math.random() * 1_000_000)).padStart(6, '0');
    const docRef = doc(codesCollection, candidate);
    const snap = await getDoc(docRef);
    if (snap.exists()) continue;
    await setDoc(docRef, {
      extraMinutes,
      createdAt: serverTimestamp(),
      expiresAt: Timestamp.fromDate(expiresAt),
    });
    return { code: candidate, extraMinutes, expiresAt };
  }
  throw new Error(`Could not allocate a unique 6-digit code after ${MAX_CODE_ATTEMPTS} attempts.`);
}
// redeemCode/createTvPairing are TV-only callables — not ported.

// ── time requests ───────────────────────────────────────────────────────────
// createRequest is TV-only — not ported.

function snapToRequest(snap: QueryDocumentSnapshot<DocumentData>): TimeRequest | null {
  const data = snap.data();
  const appPackage = data.appPackage as string | undefined;
  const requestedMinutes = data.requestedMinutes as number | undefined;
  if (appPackage === undefined || requestedMinutes === undefined) return null;
  const status: TimeRequestStatus =
    data.status === 'approved' ? 'Approved' : data.status === 'denied' ? 'Denied' : 'Pending';
  const approvedMinutes = (data.approvedMinutes as number | undefined) ?? null;
  const createdAt = data.createdAt ? (data.createdAt as Timestamp).toDate() : new Date(0);
  const respondedAt = data.respondedAt ? (data.respondedAt as Timestamp).toDate() : null;
  return { id: snap.id, appPackage, requestedMinutes, status, approvedMinutes, createdAt, respondedAt };
}

export function subscribeRecentRequests(
  familyId: string,
  cb: (requests: TimeRequest[]) => void,
  limitCount = 20,
): Unsubscribe {
  const q = query(
    collection(db, 'families', familyId, 'requests'),
    orderBy('createdAt', 'desc'),
    queryLimit(limitCount),
  );
  return onSnapshot(
    q,
    (snap) => cb(snap.docs.map(snapToRequest).filter((r): r is TimeRequest => r !== null)),
    () => cb([]),
  );
}

export async function respondToRequest(
  familyId: string,
  requestId: string,
  approvedMinutes: number | null,
): Promise<void> {
  const status = approvedMinutes === null ? 'denied' : 'approved';
  const update: Record<string, unknown> = { status, respondedAt: serverTimestamp() };
  if (approvedMinutes !== null) update.approvedMinutes = approvedMinutes;
  await updateDoc(doc(db, 'families', familyId, 'requests', requestId), update);
}

// ── family ──────────────────────────────────────────────────────────────────

function snapToFamily(snap: QueryDocumentSnapshot<DocumentData> | DocumentData): Family | null {
  const data = 'data' in snap && typeof snap.data === 'function' ? snap.data() : (snap as DocumentData);
  const ownerUid = data?.ownerUid as string | undefined;
  if (!ownerUid) return null;
  const rawRoles = (data.roles as Record<string, string> | undefined) ?? {};
  const members: Record<string, FamilyRole> = {};
  for (const [uid, role] of Object.entries(rawRoles)) {
    members[uid] = role === ROLE_ADMIN ? 'ADMIN' : 'USER';
  }
  const devices = (data[FIELD_DEVICES] as string[] | undefined) ?? [];
  const id = 'id' in snap ? (snap.id as string) : '';
  return { id, ownerUid, members, deviceIds: devices };
}

/**
 * Creates families/{newId} with uid as the owner (the single main admin) and
 * sole member, and sets users/{uid}.familyId. Returns the new familyId.
 */
export async function createFamily(uid: string): Promise<string> {
  const newFamilyRef = doc(collection(db, 'families'));
  const userRef = doc(db, 'users', uid);
  const batch = writeBatch(db);
  batch.set(newFamilyRef, { ownerUid: uid, roles: { [uid]: ROLE_ADMIN }, [FIELD_DEVICES]: [] });
  batch.set(userRef, { familyId: newFamilyRef.id }, { merge: true });
  await batch.commit();
  return newFamilyRef.id;
}

export function subscribeFamily(familyId: string, cb: (family: Family | null) => void): Unsubscribe {
  return onSnapshot(
    familyRef(familyId),
    (snap) => cb(snap.exists() ? snapToFamily(snap) : null),
    () => cb(null),
  );
}

/** Admin-only: promote/demote a member. Rules forbid touching the owner. */
export async function setMemberRole(familyId: string, uid: string, role: FamilyRole): Promise<void> {
  await updateDoc(familyRef(familyId), { [`roles.${uid}`]: role === 'ADMIN' ? ROLE_ADMIN : ROLE_USER });
}

/** Admin-only: remove a member from the family (revokes their access). */
export async function removeMember(familyId: string, uid: string): Promise<void> {
  await updateDoc(familyRef(familyId), { [`roles.${uid}`]: deleteField() });
}

// ── devices / TV pairing ─────────────────────────────────────────────────────
// createPairing (createTvPairing callable) is TV-only — not ported.

export function subscribePairedDevices(
  familyId: string,
  cb: (devices: PairedDevice[]) => void,
): Unsubscribe {
  return onSnapshot(
    familyRef(familyId),
    async (snap) => {
      const ids = ((snap.data()?.[FIELD_DEVICES] as string[] | undefined) ?? []).filter(
        (id): id is string => typeof id === 'string',
      );
      const devices = await Promise.all(
        ids.map(async (id) => {
          try {
            const deviceSnap = await getDoc(doc(db, 'devices', id));
            return { id, name: (deviceSnap.data()?.[FIELD_DEVICE_NAME] as string | undefined) ?? DEFAULT_DEVICE_NAME };
          } catch {
            return { id, name: DEFAULT_DEVICE_NAME };
          }
        }),
      );
      cb(devices);
    },
    () => cb([]),
  );
}

/** Renames a paired TV as shown on the pairing screen. */
export async function renameDevice(deviceId: string, name: string): Promise<void> {
  await setDoc(doc(db, 'devices', deviceId), { [FIELD_DEVICE_NAME]: name }, { merge: true });
}

/** Removes deviceId from the family and deletes its device doc. The TV falls back to the pairing screen. */
export async function unpairDevice(familyId: string, deviceId: string): Promise<void> {
  await updateDoc(familyRef(familyId), { [FIELD_DEVICES]: arrayRemove(deviceId) });
  await deleteDoc(doc(db, 'devices', deviceId));
}

// ── Cloud Functions callables ────────────────────────────────────────────────

/** Cloud Function error codes we treat as "code invalid/expired/used". */
function isInvalidCodeError(e: unknown): boolean {
  return e instanceof FirebaseError && (e.code === 'functions/not-found' || e.code === 'functions/failed-precondition');
}

/** Admins only; the code is written server-side so /invites stays closed to clients. */
export async function generateInvite(familyId: string): Promise<string> {
  const call = httpsCallable<{ familyId: string }, { code?: string }>(functions, 'createFamilyInvite');
  const result = await call({ familyId });
  const code = result.data?.code;
  if (!code) throw new Error('Invite creation failed: no code returned.');
  return code;
}

/** Adds the caller to the family as a "user". Returns the joined familyId, or null if invalid/expired/used. */
export async function joinFamilyByInvite(code: string): Promise<string | null> {
  const call = httpsCallable<{ code: string }, { familyId?: string }>(functions, 'joinFamilyWithInvite');
  try {
    const result = await call({ code });
    return result.data?.familyId ?? null;
  } catch (e) {
    if (isInvalidCodeError(e)) return null;
    throw e;
  }
}

/**
 * The family OWNER claims a pairing code, binding the TV (one-TV-per-family
 * is enforced server-side) and deleting the pairing doc.
 */
export async function claimPairing(code: string, familyId: string): Promise<boolean> {
  const call = httpsCallable<{ code: string; familyId: string }, { success?: boolean }>(functions, 'claimTvPairing');
  try {
    const result = await call({ code, familyId });
    return result.data?.success ?? false;
  } catch (e) {
    if (isInvalidCodeError(e)) return false;
    throw e;
  }
}
