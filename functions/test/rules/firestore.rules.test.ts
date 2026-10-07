/**
 * Exercises firestore.rules against the running Firestore emulator using
 * @firebase/rules-unit-testing. Each test case asserts either allow or deny
 * for a specific identity + path + operation.
 *
 * Run with: npm run test:rules (boots only firestore emulator).
 */

import * as fs from "fs";
import * as path from "path";
import {
  initializeTestEnvironment,
  RulesTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import {
  setDoc,
  getDoc,
  updateDoc,
  deleteDoc,
  doc,
  getDocs,
  collection,
  serverTimestamp,
  deleteField,
  writeBatch,
  arrayRemove,
  Timestamp,
} from "firebase/firestore";

const RULES = fs.readFileSync(
  path.resolve(__dirname, "../../../firestore.rules"),
  "utf8",
);

let env: RulesTestEnvironment;

const FAM = "fam1";
const OWNER = "owner-uid";
const ADMIN2 = "admin2-uid";
const USER = "user-uid";
const STRANGER = "stranger-uid";
const TV = "tv-device-uid";

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-rules",
    firestore: {
      rules: RULES,
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

afterAll(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "families", FAM), {
      ownerUid: OWNER,
      roles: { [OWNER]: "admin", [ADMIN2]: "admin", [USER]: "user" },
      devices: [TV],
    });
    await setDoc(doc(db, "devices", TV), { familyId: FAM, name: "TV" });
  });
});

/** A code payload exactly as the web/Android apps write it. */
const validCode = () => ({
  extraMinutes: 30,
  createdAt: serverTimestamp(),
  expiresAt: Timestamp.fromMillis(Date.now() + 5 * 60_000),
});

const ctx = (uid: string | null) =>
  uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();

describe("/users/{uid}", () => {
  it("owner can read + write their doc", async () => {
    await assertSucceeds(setDoc(doc(ctx(USER), "users", USER), { familyId: FAM }));
    await assertSucceeds(getDoc(doc(ctx(USER), "users", USER)));
  });
  it("others cannot read or write someone else's doc", async () => {
    await assertFails(getDoc(doc(ctx(STRANGER), "users", USER)));
    await assertFails(
      setDoc(doc(ctx(STRANGER), "users", USER), { familyId: FAM }),
    );
  });
  it("a fellow family member can read (but not write) your doc; a stranger still cannot", async () => {
    await env.withSecurityRulesDisabled(async (adminCtx) => {
      await setDoc(doc(adminCtx.firestore(), "users", USER), {
        familyId: FAM,
        displayName: "Jamie",
      });
    });
    await assertSucceeds(getDoc(doc(ctx(ADMIN2), "users", USER)));
    await assertFails(
      setDoc(doc(ctx(ADMIN2), "users", USER), { familyId: FAM, displayName: "Hijacked" }),
    );
    await assertFails(getDoc(doc(ctx(STRANGER), "users", USER)));
  });
});

describe("/users/{uid}/private/push", () => {
  it("the user can save and read their own push tokens", async () => {
    await assertSucceeds(setDoc(doc(ctx(USER), "users", USER, "private", "push"), { tokens: ["t"] }));
    await assertSucceeds(getDoc(doc(ctx(USER), "users", USER, "private", "push")));
  });
  it("fellow family members, the TV and strangers can neither read nor write them", async () => {
    await env.withSecurityRulesDisabled(async (adminCtx) => {
      await setDoc(doc(adminCtx.firestore(), "users", USER), { familyId: FAM });
      await setDoc(doc(adminCtx.firestore(), "users", USER, "private", "push"), { tokens: ["t"] });
    });
    for (const other of [OWNER, ADMIN2, TV, STRANGER]) {
      await assertFails(getDoc(doc(ctx(other), "users", USER, "private", "push")));
      await assertFails(setDoc(doc(ctx(other), "users", USER, "private", "push"), { tokens: ["evil"] }));
    }
  });
});

describe("/families/{id}", () => {
  it("members can read; strangers cannot", async () => {
    await assertSucceeds(getDoc(doc(ctx(USER), "families", FAM)));
    await assertFails(getDoc(doc(ctx(STRANGER), "families", FAM)));
  });

  it("paired TV can read", async () => {
    await assertSucceeds(getDoc(doc(ctx(TV), "families", FAM)));
  });

  it("create requires ownerUid == auth.uid + sole-admin + empty devices", async () => {
    await assertSucceeds(
      setDoc(doc(ctx("new-owner"), "families", "fam2"), {
        ownerUid: "new-owner",
        roles: { "new-owner": "admin" },
        devices: [],
      }),
    );
    // Wrong ownerUid:
    await assertFails(
      setDoc(doc(ctx("new-owner"), "families", "fam3"), {
        ownerUid: "someone-else",
        roles: { "new-owner": "admin" },
        devices: [],
      }),
    );
    // Pre-populated roles:
    await assertFails(
      setDoc(doc(ctx("new-owner"), "families", "fam4"), {
        ownerUid: "new-owner",
        roles: { "new-owner": "admin", evil: "admin" },
        devices: [],
      }),
    );
  });

  it("only owner can delete", async () => {
    await assertFails(deleteDoc(doc(ctx(ADMIN2), "families", FAM)));
    await assertFails(deleteDoc(doc(ctx(USER), "families", FAM)));
    await assertSucceeds(deleteDoc(doc(ctx(OWNER), "families", FAM)));
  });

  it("update: admins can change roles but cannot change ownerUid", async () => {
    await assertFails(
      updateDoc(doc(ctx(ADMIN2), "families", FAM), { ownerUid: ADMIN2 }),
    );
  });

  it("update: non-owner cannot change devices array", async () => {
    await assertFails(
      updateDoc(doc(ctx(ADMIN2), "families", FAM), { devices: [] }),
    );
    await assertSucceeds(
      updateDoc(doc(ctx(OWNER), "families", FAM), { devices: [TV] }),
    );
  });

  it("regular user cannot demote owner", async () => {
    await assertFails(
      updateDoc(doc(ctx(USER), "families", FAM), {
        [`roles.${OWNER}`]: "user",
      }),
    );
  });
});

describe("/families/{id}/usage/{date}", () => {
  it("TV (device) can write; stranger cannot", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(TV), "families", FAM, "usage", "2026-06-18"), {
        perAppMillis: { "com.x": 1000 },
        updatedAt: serverTimestamp(),
      }),
    );
    await assertFails(
      setDoc(doc(ctx(STRANGER), "families", FAM, "usage", "2026-06-18"), {
        perAppMillis: { "com.x": 1000 },
      }),
    );
  });
  it("admin can write; member can read", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(ADMIN2), "families", FAM, "usage", "2026-06-18"), {
        perAppMillis: {},
      }),
    );
    await assertSucceeds(getDoc(doc(ctx(USER), "families", FAM, "usage", "2026-06-18")));
  });
});

describe("/families/{id}/limits/*", () => {
  it("only members can write", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(USER), "families", FAM, "limits", "overall"), {
        overallDailyMinutes: 60,
      }),
    );
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "limits", "overall"), {
        overallDailyMinutes: 60,
      }),
    );
    await assertFails(
      setDoc(doc(ctx(STRANGER), "families", FAM, "limits", "overall"), {
        overallDailyMinutes: 60,
      }),
    );
  });
});

describe("/families/{id}/codes/*", () => {
  it("members can create + delete; TV cannot", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(USER), "families", FAM, "codes", "111111"), validCode()),
    );
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "codes", "222222"), validCode()),
    );
  });
  it("no client may UPDATE a code (server-only consume via Cloud Function)", async () => {
    await env.withSecurityRulesDisabled(async (c) => {
      await setDoc(doc(c.firestore(), "families", FAM, "codes", "333333"), {
        extraMinutes: 10,
      });
    });
    await assertFails(
      updateDoc(doc(ctx(USER), "families", FAM, "codes", "333333"), {
        extraMinutes: 99,
      }),
    );
  });
});

describe("/families/{id}/requests/*", () => {
  it("TV may create with valid minutes", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(TV), "families", FAM, "requests", "r1"), {
        appPackage: "com.x",
        requestedMinutes: 15,
        status: "pending",
        createdAt: serverTimestamp(),
      }),
    );
  });
  it("rejects out-of-bounds minutes", async () => {
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "requests", "r2"), {
        appPackage: "com.x",
        requestedMinutes: 9999,
        status: "pending",
        createdAt: serverTimestamp(),
      }),
    );
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "requests", "r3"), {
        appPackage: "com.x",
        requestedMinutes: 0,
        status: "pending",
        createdAt: serverTimestamp(),
      }),
    );
  });
  it("requires status='pending' on create", async () => {
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "requests", "r4"), {
        appPackage: "com.x",
        requestedMinutes: 10,
        status: "approved",
        createdAt: serverTimestamp(),
      }),
    );
  });
});

describe("/families/{id}/settings/lockout", () => {
  async function seedLock(data: Record<string, unknown>) {
    await env.withSecurityRulesDisabled(async (c) => {
      await setDoc(doc(c.firestore(), "families", FAM, "settings", "lockout"), data);
    });
  }
  const tvClear = () =>
    updateDoc(doc(ctx(TV), "families", FAM, "settings", "lockout"), {
      locked: false,
      lockedUntil: deleteField(),
    });

  it("TV may clear a timer lock once lockedUntil has passed", async () => {
    await seedLock({ mode: "timer", locked: true, lockedUntil: new Date(Date.now() - 1000) });
    await assertSucceeds(tvClear());
  });
  it("TV may NOT clear a timer lock early", async () => {
    await seedLock({ mode: "timer", locked: true, lockedUntil: new Date(Date.now() + 60000) });
    await assertFails(tvClear());
  });
  it("TV may NOT clear a parent-unlock lock (no lockedUntil, or parent mode)", async () => {
    await seedLock({ mode: "timer", locked: true });
    await assertFails(tvClear());
    await seedLock({ mode: "parent", locked: true, lockedUntil: new Date(Date.now() - 1000) });
    await assertFails(tvClear());
  });
  it("TV may never SET locked=true or touch the config", async () => {
    await seedLock({ mode: "timer", locked: false });
    await assertFails(
      updateDoc(doc(ctx(TV), "families", FAM, "settings", "lockout"), { locked: true }),
    );
    await assertFails(
      updateDoc(doc(ctx(TV), "families", FAM, "settings", "lockout"), { mode: "timer", durationMinutes: 1 }),
    );
  });
  it("parents may set the config and unlock; bad values are rejected", async () => {
    await seedLock({ mode: "parent", locked: true, failureCount: 0 });
    await assertSucceeds(
      setDoc(doc(ctx(USER), "families", FAM, "settings", "lockout"), { durationMinutes: 30, mode: "timer" }, { merge: true }),
    );
    await assertSucceeds(
      setDoc(doc(ctx(USER), "families", FAM, "settings", "lockout"), { locked: false, lockedUntil: deleteField() }, { merge: true }),
    );
    await assertFails(
      setDoc(doc(ctx(USER), "families", FAM, "settings", "lockout"), { mode: "forever" }, { merge: true }),
    );
    await assertFails(
      setDoc(doc(ctx(USER), "families", FAM, "settings", "lockout"), { durationMinutes: "15" }, { merge: true }),
    );
    await assertFails(
      setDoc(doc(ctx(USER), "families", FAM, "settings", "lockout"), { failureCount: 99 }, { merge: true }),
    );
  });
});

describe("/families/{id}/<unknown-subcollection>", () => {
  it("members can read unknown subcollections but cannot write them", async () => {
    await env.withSecurityRulesDisabled(async (c) => {
      await setDoc(doc(c.firestore(), "families", FAM, "unknown", "doc1"), { x: 1 });
    });
    await assertSucceeds(getDoc(doc(ctx(USER), "families", FAM, "unknown", "doc1")));
    await assertFails(
      setDoc(doc(ctx(USER), "families", FAM, "unknown", "doc2"), { x: 2 }),
    );
    await assertFails(
      setDoc(doc(ctx(ADMIN2), "families", FAM, "unknown", "doc2"), { x: 2 }),
    );
  });
  it("paired TV can read unknown subcollections but cannot write them", async () => {
    await env.withSecurityRulesDisabled(async (c) => {
      await setDoc(doc(c.firestore(), "families", FAM, "unknown", "doc1"), { x: 1 });
    });
    await assertSucceeds(getDoc(doc(ctx(TV), "families", FAM, "unknown", "doc1")));
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "unknown", "doc2"), { x: 2 }),
    );
  });
});

describe("/invites and /pairings", () => {
  it("client reads and writes are denied", async () => {
    await assertFails(getDoc(doc(ctx(USER), "invites", "123456")));
    await assertFails(
      setDoc(doc(ctx(USER), "invites", "123456"), { familyId: FAM }),
    );
    await assertFails(getDoc(doc(ctx(USER), "pairings", "123456")));
  });
});

describe("/rateLimits", () => {
  it("clients can neither read nor reset their own counters", async () => {
    await assertFails(getDoc(doc(ctx(USER), "rateLimits", `join_${USER}`)));
    await assertFails(setDoc(doc(ctx(USER), "rateLimits", `join_${USER}`), { count: 0 }));
    await assertFails(deleteDoc(doc(ctx(USER), "rateLimits", `join_${USER}`)));
  });
});

describe("/devices/{id}", () => {
  it("TV can read its own device doc; stranger cannot", async () => {
    await assertSucceeds(getDoc(doc(ctx(TV), "devices", TV)));
    await assertFails(getDoc(doc(ctx(STRANGER), "devices", TV)));
  });
  it("client cannot CREATE device docs (server-only)", async () => {
    await assertFails(
      setDoc(doc(ctx(USER), "devices", "new-device"), { familyId: FAM }),
    );
  });
  it("client cannot change the family pointer on a device", async () => {
    await assertFails(
      updateDoc(doc(ctx(TV), "devices", TV), { familyId: "other-fam" }),
    );
  });
  it("only owner can delete (unpair) a device", async () => {
    await assertFails(deleteDoc(doc(ctx(ADMIN2), "devices", TV)));
    await assertFails(deleteDoc(doc(ctx(USER), "devices", TV)));
    await assertSucceeds(deleteDoc(doc(ctx(OWNER), "devices", TV)));
  });
  it("LIST is disallowed", async () => {
    await assertFails(getDocs(collection(ctx(OWNER), "devices")));
  });
});

// ---------------------------------------------------------------------------
// Schema validation (security plan Phase 4). Each "ok" case is the payload the
// web/Android apps actually write, so a rules change that breaks a real client
// write fails here.
// ---------------------------------------------------------------------------

const anonCtx = (uid: string) =>
  env.authenticatedContext(uid, { firebase: { sign_in_provider: "anonymous" } }).firestore();
const parentCtx = (uid: string) =>
  env.authenticatedContext(uid, { firebase: { sign_in_provider: "google.com" } }).firestore();

describe("schema: family doc", () => {
  it("a Google account can create its own family (app payload, in the createFamily batch)", async () => {
    const db = parentCtx(STRANGER);
    const batch = writeBatch(db);
    batch.set(doc(db, "families", "newfam"), { ownerUid: STRANGER, roles: { [STRANGER]: "admin" }, devices: [] });
    batch.set(doc(db, "users", STRANGER), { familyId: "newfam" }, { merge: true });
    await assertSucceeds(batch.commit());
  });
  it("an anonymous (TV) account cannot create a family", async () => {
    await assertFails(
      setDoc(doc(anonCtx("anon-1"), "families", "f2"), { ownerUid: "anon-1", roles: { "anon-1": "admin" }, devices: [] }),
    );
  });
  it("create rejects extra fields", async () => {
    await assertFails(
      setDoc(doc(parentCtx(STRANGER), "families", "f3"), {
        ownerUid: STRANGER,
        roles: { [STRANGER]: "admin" },
        devices: [],
        fcmTokens: ["x"],
      }),
    );
  });
  it("admins can change roles but not write stray fields or invent roles", async () => {
    await assertSucceeds(updateDoc(doc(ctx(ADMIN2), "families", FAM), { [`roles.${USER}`]: "admin" }));
    await assertFails(updateDoc(doc(ctx(ADMIN2), "families", FAM), { fcmTokens: ["t"] }));
    await assertFails(updateDoc(doc(ctx(ADMIN2), "families", FAM), { [`roles.${USER}`]: "superadmin" }));
  });
  it("the owner can unpair a TV atomically, but cannot add a device directly", async () => {
    const db = ctx(OWNER);
    const batch = writeBatch(db);
    batch.update(doc(db, "families", FAM), { devices: arrayRemove(TV) });
    batch.delete(doc(db, "devices", TV));
    await assertSucceeds(batch.commit());
    await assertFails(updateDoc(doc(ctx(OWNER), "families", FAM), { devices: ["some-other-uid"] }));
  });
});

describe("schema: users/{uid}", () => {
  it("accepts the app's writes (displayName incl. null, language, own familyId)", async () => {
    await assertSucceeds(setDoc(doc(ctx(USER), "users", USER), { displayName: "Jamie" }, { merge: true }));
    await assertSucceeds(setDoc(doc(ctx(USER), "users", USER), { displayName: null }, { merge: true }));
    await assertSucceeds(setDoc(doc(ctx(USER), "users", USER), { language: "he" }, { merge: true }));
    await assertSucceeds(setDoc(doc(ctx(USER), "users", USER), { familyId: FAM }, { merge: true }));
  });
  it("cannot point familyId at a family you are not in", async () => {
    await assertFails(setDoc(doc(ctx(STRANGER), "users", STRANGER), { familyId: FAM }, { merge: true }));
  });
  it("rejects stray fields and bad values", async () => {
    await assertFails(setDoc(doc(ctx(USER), "users", USER), { fcmTokens: ["t"] }, { merge: true }));
    await assertFails(setDoc(doc(ctx(USER), "users", USER), { role: "admin" }, { merge: true }));
    await assertFails(setDoc(doc(ctx(USER), "users", USER), { displayName: "x".repeat(101) }, { merge: true }));
    await assertFails(setDoc(doc(ctx(USER), "users", USER), { language: 7 }, { merge: true }));
  });
  it("a legacy doc with retired fields stays writable for allowed fields", async () => {
    await env.withSecurityRulesDisabled(async (c) => {
      await setDoc(doc(c.firestore(), "users", USER), { familyId: FAM, fcmTokens: ["old"] });
    });
    await assertSucceeds(setDoc(doc(ctx(USER), "users", USER), { displayName: "Jamie" }, { merge: true }));
  });
});

describe("schema: limits", () => {
  const lim = (uid: string, ...path: string[]) =>
    doc(ctx(uid), "families", FAM, "limits", ...(path as [string, ...string[]]));

  it("accepts app payloads", async () => {
    await assertSucceeds(setDoc(lim(USER, "overall"), { overallDailyMinutes: -1 }));
    await assertSucceeds(setDoc(lim(USER, "perApp", "apps", "com.google.android.youtube"), { dailyLimitMinutes: 45 }));
    await assertSucceeds(
      setDoc(lim(USER, "timeFrame"), { enabled: true, windowsByDay: { MONDAY: [{ start: 480, end: 1200 }] } }),
    );
    await assertSucceeds(setDoc(lim(USER, "allDay"), { date: "2026-10-07", indefinite: false }));
    await assertSucceeds(setDoc(lim(USER, "allDay"), { date: null, indefinite: true }));
    await assertSucceeds(setDoc(lim(USER, "allDay"), { date: deleteField(), indefinite: deleteField() }, { merge: true }));
    await assertSucceeds(setDoc(lim(USER, "instantLock"), { locked: true, date: "2026-10-07" }));
    await assertSucceeds(setDoc(lim(USER, "instantLock"), { locked: false }));
    await assertSucceeds(deleteDoc(lim(USER, "perApp", "apps", "com.google.android.youtube")));
  });
  it("rejects wrong types and out-of-range values (these used to crash the TV)", async () => {
    await assertFails(setDoc(lim(USER, "overall"), { overallDailyMinutes: "60" }));
    await assertFails(setDoc(lim(USER, "overall"), { overallDailyMinutes: 99999 }));
    await assertFails(setDoc(lim(USER, "perApp", "apps", "com.x"), { dailyLimitMinutes: "lots" }));
    await assertFails(setDoc(lim(USER, "perApp", "apps", "com.x"), { dailyLimitMinutes: 30, extra: 1 }));
    await assertFails(setDoc(lim(USER, "timeFrame"), { enabled: "yes", windowsByDay: {} }));
    await assertFails(setDoc(lim(USER, "allDay"), { date: "tomorrow" }));
    await assertFails(setDoc(lim(USER, "instantLock"), { locked: 1 }));
    await assertFails(setDoc(lim(USER, "somethingElse"), { x: 1 }));
  });
  it("the TV still cannot write any limit", async () => {
    await assertFails(setDoc(lim(TV, "instantLock"), { locked: false }));
  });
});

describe("schema: codes", () => {
  const code = (id: string) => doc(ctx(USER), "families", FAM, "codes", id);
  it("rejects huge grants, far-future expiry, bad ids and extra fields", async () => {
    await assertFails(setDoc(code("111111"), { ...validCode(), extraMinutes: 99999 }));
    await assertFails(
      setDoc(code("111111"), { ...validCode(), expiresAt: Timestamp.fromMillis(Date.now() + 7 * 86_400_000) }),
    );
    await assertFails(setDoc(code("12ab56"), validCode()));
    await assertFails(setDoc(code("111111"), { ...validCode(), note: "x" }));
    await assertFails(setDoc(code("111111"), { extraMinutes: 30, expiresAt: Timestamp.fromMillis(Date.now()) }));
  });
});

describe("schema: requests", () => {
  const req = (uid: string, id: string) => doc(ctx(uid), "families", FAM, "requests", id);
  const pending = () => ({
    appPackage: "com.google.android.youtube",
    requestedMinutes: 15,
    status: "pending",
    createdAt: serverTimestamp(),
  });

  it("TV rejects junk package names and extra fields", async () => {
    await assertFails(setDoc(req(TV, "r1"), { ...pending(), appPackage: "../../x" }));
    await assertFails(setDoc(req(TV, "r1"), { ...pending(), appPackage: 42 }));
    await assertFails(setDoc(req(TV, "r1"), { ...pending(), approvedMinutes: 240 }));
  });
  it("parents approve/deny with the app payload; bad updates are rejected", async () => {
    await env.withSecurityRulesDisabled(async (c) => {
      await setDoc(doc(c.firestore(), "families", FAM, "requests", "r9"), {
        appPackage: "com.x",
        requestedMinutes: 15,
        status: "pending",
      });
    });
    await assertFails(updateDoc(req(USER, "r9"), { status: "approved", approvedMinutes: "lots" }));
    await assertFails(updateDoc(req(USER, "r9"), { status: "pending" }));
    await assertFails(updateDoc(req(USER, "r9"), { requestedMinutes: 240 }));
    await assertSucceeds(
      updateDoc(req(USER, "r9"), { status: "approved", respondedAt: serverTimestamp(), approvedMinutes: 30 }),
    );
    await assertFails(updateDoc(req(TV, "r9"), { status: "approved" }));
  });
});

describe("schema: tvApps, language, devices", () => {
  it("tvApps: label must be a short string on a package-name id", async () => {
    const app = (id: string) => doc(ctx(TV), "families", FAM, "tvApps", id);
    await assertSucceeds(setDoc(app("com.google.android.youtube"), { label: "YouTube" }));
    await assertFails(setDoc(app("com.x"), { label: "x".repeat(101) }));
    await assertFails(setDoc(app("com.x"), { label: 5 }));
    await assertFails(setDoc(app("com.x"), { label: "X", extra: true }));
  });
  it("language: parents set a language code; the TV cannot", async () => {
    const lang = (uid: string) => doc(ctx(uid), "families", FAM, "settings", "language");
    await assertSucceeds(setDoc(lang(USER), { code: "he" }));
    await assertSucceeds(setDoc(lang(USER), { code: "fr" }));
    await assertFails(setDoc(lang(USER), { code: "not a language" }));
    await assertFails(setDoc(lang(TV), { code: "en" }));
  });
  it("devices: parents rename (bounded); the TV may only stamp lastSeen", async () => {
    await assertSucceeds(setDoc(doc(ctx(USER), "devices", TV), { name: "Living room" }, { merge: true }));
    await assertFails(setDoc(doc(ctx(USER), "devices", TV), { name: "x".repeat(61) }, { merge: true }));
    await assertFails(setDoc(doc(ctx(USER), "devices", TV), { name: "" }, { merge: true }));
    await assertFails(setDoc(doc(ctx(TV), "devices", TV), { name: "Mine now" }, { merge: true }));
    await assertSucceeds(setDoc(doc(ctx(TV), "devices", TV), { lastSeen: serverTimestamp() }, { merge: true }));
  });
});

describe("schema: settings/timezone", () => {
  const tz = (uid: string) => doc(ctx(uid), "families", FAM, "settings", "timezone");
  it("parents set an IANA zone; the TV and junk values are rejected", async () => {
    await assertSucceeds(setDoc(tz(USER), { zone: "Asia/Jerusalem" }));
    await assertSucceeds(setDoc(tz(USER), { zone: "America/Argentina/Buenos_Aires" }));
    await assertFails(setDoc(tz(USER), { zone: "<script>" }));
    await assertFails(setDoc(tz(USER), { zone: 3 }));
    await assertFails(setDoc(tz(USER), { zone: "UTC", extra: 1 }));
    await assertFails(setDoc(tz(TV), { zone: "Pacific/Kiritimati" }));
    await assertSucceeds(getDoc(tz(TV)));
  });
});
