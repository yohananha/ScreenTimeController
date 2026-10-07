import { getAuth } from "firebase-admin/auth";
import { Timestamp } from "firebase-admin/firestore";
import { initFFT, loadFunctions, db, seedFamily, seedUser, parentAuth, anonAuth } from "./helpers";

type Callable = (req: { data: unknown; auth: unknown }) => Promise<unknown>;
let deleteFamily: Callable;
let deleteAccount: Callable;

beforeAll(() => {
  const fft = initFFT();
  const fns = loadFunctions();
  deleteFamily = fft.wrap(fns.deleteFamily) as Callable;
  deleteAccount = fft.wrap(fns.deleteAccount) as Callable;
});

const FAM = "fam-del";
const OTHER = "fam-other";
const OWNER = "owner-uid";
const COPARENT = "coparent-uid";
const TV = "tv-uid";

/** A family with real data in every subcollection, a paired TV, an invite and two members. */
async function seedFullFamily() {
  await seedFamily({
    familyId: FAM,
    ownerUid: OWNER,
    roles: { [OWNER]: "admin", [COPARENT]: "user" },
    devices: [TV],
  });
  await seedUser(OWNER, FAM, ["tok-owner"]);
  await seedUser(COPARENT, FAM, ["tok-coparent"]);
  const fam = db().collection("families").doc(FAM);
  await Promise.all([
    fam.collection("usage").doc("2026-10-07").set({ perAppMillis: { "com.x": 1000 } }),
    fam.collection("limits").doc("overall").set({ overallDailyMinutes: 60 }),
    fam.collection("limits").doc("perApp").collection("apps").doc("com.x").set({ dailyLimitMinutes: 30 }),
    fam.collection("requests").doc("r1").set({ appPackage: "com.x", requestedMinutes: 15, status: "pending" }),
    fam.collection("tvApps").doc("com.x").set({ label: "X" }),
    fam.collection("settings").doc("lockout").set({ mode: "timer" }),
    db().collection("devices").doc(TV).set({ familyId: FAM, name: "TV" }),
    db().collection("invites").doc("ABCD2345").set({
      familyId: FAM,
      createdBy: OWNER,
      used: false,
      expiresAt: Timestamp.fromMillis(Date.now() + 3600_000),
    }),
  ]);
}

async function familyIsGone() {
  const fam = db().collection("families").doc(FAM);
  const [doc, usage, requests, perApp, device, invite] = await Promise.all([
    fam.get(),
    fam.collection("usage").get(),
    fam.collection("requests").get(),
    fam.collection("limits").doc("perApp").collection("apps").get(),
    db().collection("devices").doc(TV).get(),
    db().collection("invites").doc("ABCD2345").get(),
  ]);
  return !doc.exists && usage.empty && requests.empty && perApp.empty && !device.exists && !invite.exists;
}

describe("deleteFamily", () => {
  it("the owner deletes the family, all subcollections, its TVs and invites, and members' pointers", async () => {
    await seedFullFamily();
    await deleteFamily({ data: { familyId: FAM }, auth: parentAuth(OWNER) });

    expect(await familyIsGone()).toBe(true);
    const coparent = await db().collection("users").doc(COPARENT).get();
    expect(coparent.exists).toBe(true);
    expect(coparent.get("familyId")).toBeUndefined();
  });

  it("refuses non-owners (even members) and anonymous callers", async () => {
    await seedFullFamily();
    await expect(
      deleteFamily({ data: { familyId: FAM }, auth: parentAuth(COPARENT) }),
    ).rejects.toMatchObject({ code: "permission-denied" });
    await expect(
      deleteFamily({ data: { familyId: FAM }, auth: anonAuth(TV) }),
    ).rejects.toMatchObject({ code: "permission-denied" });
    expect((await db().collection("families").doc(FAM).get()).exists).toBe(true);
  });

  it("rejects a malformed familyId", async () => {
    await expect(
      deleteFamily({ data: { familyId: "a/b" }, auth: parentAuth(OWNER) }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });
});

describe("deleteAccount", () => {
  it("an owner's account deletion removes their family, user doc, push tokens and Auth user", async () => {
    await seedFullFamily();
    await getAuth().createUser({ uid: OWNER });

    await deleteAccount({ data: {}, auth: parentAuth(OWNER) });

    expect(await familyIsGone()).toBe(true);
    expect((await db().collection("users").doc(OWNER).get()).exists).toBe(false);
    expect((await db().collection("users").doc(OWNER).collection("private").doc("push").get()).exists).toBe(false);
    await expect(getAuth().getUser(OWNER)).rejects.toMatchObject({ code: "auth/user-not-found" });
  });

  it("a co-parent leaves the family (which survives) and their unused invites are revoked", async () => {
    await seedFullFamily();
    await seedFamily({ familyId: OTHER, ownerUid: "someone", roles: { someone: "admin", [COPARENT]: "admin" } });
    await db().collection("invites").doc("WXYZ7777").set({ familyId: OTHER, createdBy: COPARENT, used: false });

    await deleteAccount({ data: {}, auth: parentAuth(COPARENT) });

    const fam = await db().collection("families").doc(FAM).get();
    expect(fam.exists).toBe(true);
    expect(fam.get(`roles.${COPARENT}`)).toBeUndefined();
    expect(fam.get(`roles.${OWNER}`)).toBe("admin");
    const other = await db().collection("families").doc(OTHER).get();
    expect(other.get(`roles.${COPARENT}`)).toBeUndefined();
    expect((await db().collection("invites").doc("WXYZ7777").get()).exists).toBe(false);
    expect((await db().collection("users").doc(COPARENT).get()).exists).toBe(false);
  });

  it("refuses anonymous (TV) callers", async () => {
    await expect(deleteAccount({ data: {}, auth: anonAuth(TV) })).rejects.toMatchObject({
      code: "permission-denied",
    });
  });
});
