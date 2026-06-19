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
import { setDoc, getDoc, updateDoc, deleteDoc, doc, getDocs, collection } from "firebase/firestore";

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
        totalMs: 1000,
      }),
    );
    await assertFails(
      setDoc(doc(ctx(STRANGER), "families", FAM, "usage", "2026-06-18"), {
        totalMs: 1000,
      }),
    );
  });
  it("admin can write; member can read", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(ADMIN2), "families", FAM, "usage", "2026-06-18"), {
        totalMs: 1000,
      }),
    );
    await assertSucceeds(getDoc(doc(ctx(USER), "families", FAM, "usage", "2026-06-18")));
  });
});

describe("/families/{id}/limits/*", () => {
  it("only members can write", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(USER), "families", FAM, "limits", "default"), {
        dailyMinutes: 60,
      }),
    );
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "limits", "default"), {
        dailyMinutes: 60,
      }),
    );
    await assertFails(
      setDoc(doc(ctx(STRANGER), "families", FAM, "limits", "default"), {
        dailyMinutes: 60,
      }),
    );
  });
});

describe("/families/{id}/codes/*", () => {
  it("members can create + delete; TV cannot", async () => {
    await assertSucceeds(
      setDoc(doc(ctx(USER), "families", FAM, "codes", "111111"), {
        extraMinutes: 10,
      }),
    );
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "codes", "222222"), {
        extraMinutes: 10,
      }),
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
      }),
    );
  });
  it("rejects out-of-bounds minutes", async () => {
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "requests", "r2"), {
        appPackage: "com.x",
        requestedMinutes: 9999,
        status: "pending",
      }),
    );
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "requests", "r3"), {
        appPackage: "com.x",
        requestedMinutes: 0,
        status: "pending",
      }),
    );
  });
  it("requires status='pending' on create", async () => {
    await assertFails(
      setDoc(doc(ctx(TV), "families", FAM, "requests", "r4"), {
        appPackage: "com.x",
        requestedMinutes: 10,
        status: "approved",
      }),
    );
  });
});

describe("/families/{id}/settings/lockout", () => {
  it("TV may only clear the lock (locked=false); cannot SET locked=true", async () => {
    await env.withSecurityRulesDisabled(async (c) => {
      await setDoc(
        doc(c.firestore(), "families", FAM, "settings", "lockout"),
        { locked: true, lockedUntil: new Date(Date.now() + 60000) },
      );
    });
    await assertSucceeds(
      updateDoc(doc(ctx(TV), "families", FAM, "settings", "lockout"), {
        locked: false,
      }),
    );
    await assertFails(
      updateDoc(doc(ctx(TV), "families", FAM, "settings", "lockout"), {
        locked: true,
      }),
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
