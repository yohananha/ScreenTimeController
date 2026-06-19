import { Timestamp } from "firebase-admin/firestore";
import { initFFT, loadFunctions, db, seedFamily } from "./helpers";

let createTv: (req: { data: unknown; auth: { uid: string } }) => Promise<unknown>;
let claimTv: (req: { data: unknown; auth: { uid: string } }) => Promise<unknown>;

beforeAll(() => {
  const fft = initFFT();
  const fns = loadFunctions();
  createTv = fft.wrap(fns.createTvPairing) as typeof createTv;
  claimTv = fft.wrap(fns.claimTvPairing) as typeof claimTv;
});

const FAM = "fam-pair";
const OWNER = "owner-uid";
const NOT_OWNER = "admin2-uid";
const TV = "tv-device-uid";

async function seedPairing(code: string, extra: Record<string, unknown> = {}) {
  await db().collection("pairings").doc(code).set({
    deviceId: TV,
    createdAt: Timestamp.now(),
    expiresAt: Timestamp.fromMillis(Date.now() + 5 * 60_000),
    ...extra,
  });
}

describe("createTvPairing", () => {
  it("creates a 6-digit pairing doc with 10m TTL", async () => {
    const before = Date.now();
    const result = (await createTv({ data: {}, auth: { uid: TV } })) as {
      code: string;
    };
    expect(result.code).toMatch(/^\d{6}$/);
    const snap = await db().collection("pairings").doc(result.code).get();
    expect(snap.get("deviceId")).toBe(TV);
    const expires = (snap.get("expiresAt") as Timestamp).toMillis();
    expect(expires).toBeGreaterThanOrEqual(before + 9 * 60_000);
    expect(expires).toBeLessThanOrEqual(before + 11 * 60_000);
  });

  it("requires authentication", async () => {
    await expect(
      createTv({ data: {}, auth: undefined as never }),
    ).rejects.toThrow(/unauthenticated|Sign-in/i);
  });
});

describe("claimTvPairing", () => {
  beforeEach(async () => {
    await seedFamily({
      familyId: FAM,
      ownerUid: OWNER,
      roles: { [OWNER]: "admin", [NOT_OWNER]: "admin" },
    });
  });

  it("happy path: writes device doc, appends to family.devices, deletes pairing", async () => {
    await seedPairing("424242");

    const result = (await claimTv({
      data: { code: "424242", familyId: FAM },
      auth: { uid: OWNER },
    })) as { success: boolean };
    expect(result.success).toBe(true);

    const device = await db().collection("devices").doc(TV).get();
    expect(device.get("familyId")).toBe(FAM);
    expect(device.get("name")).toBeDefined();

    const fam = await db().collection("families").doc(FAM).get();
    expect(fam.get("devices")).toContain(TV);

    const pairing = await db().collection("pairings").doc("424242").get();
    expect(pairing.exists).toBe(false);
  });

  it("denies non-owners (even other admins)", async () => {
    await seedPairing("424242");
    await expect(
      claimTv({ data: { code: "424242", familyId: FAM }, auth: { uid: NOT_OWNER } }),
    ).rejects.toMatchObject({ code: "permission-denied" });
  });

  it("rejects an unknown code", async () => {
    await expect(
      claimTv({ data: { code: "999999", familyId: FAM }, auth: { uid: OWNER } }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects (and deletes) an expired code", async () => {
    await seedPairing("424242", {
      expiresAt: Timestamp.fromMillis(Date.now() - 1000),
    });
    await expect(
      claimTv({ data: { code: "424242", familyId: FAM }, auth: { uid: OWNER } }),
    ).rejects.toMatchObject({ code: "not-found" });
    const after = await db().collection("pairings").doc("424242").get();
    expect(after.exists).toBe(false);
  });

  it("allows pairing a second TV to the same family", async () => {
    await db()
      .collection("families")
      .doc(FAM)
      .set({ devices: ["older-tv"] }, { merge: true });
    await seedPairing("424242");
    const result = (await claimTv({
      data: { code: "424242", familyId: FAM },
      auth: { uid: OWNER },
    })) as { success: boolean };
    expect(result.success).toBe(true);
    const fam = await db().collection("families").doc(FAM).get();
    expect(fam.get("devices")).toContain("older-tv");
    expect(fam.get("devices")).toContain(TV);
  });

  it("is idempotent when the same TV re-pairs to the same family", async () => {
    await db()
      .collection("families")
      .doc(FAM)
      .set({ devices: [TV] }, { merge: true });
    await db().collection("devices").doc(TV).set({ familyId: FAM });
    await seedPairing("424242");
    const result = (await claimTv({
      data: { code: "424242", familyId: FAM },
      auth: { uid: OWNER },
    })) as { success: boolean };
    expect(result.success).toBe(true);
    const fam = await db().collection("families").doc(FAM).get();
    expect((fam.get("devices") as string[]).filter((d: string) => d === TV)).toHaveLength(1);
    const pairing = await db().collection("pairings").doc("424242").get();
    expect(pairing.exists).toBe(false);
  });

  it("rejects when TV already belongs to another family", async () => {
    await seedFamily({ familyId: "other-fam", ownerUid: "stranger" });
    await db().collection("devices").doc(TV).set({ familyId: "other-fam" });
    await seedPairing("424242");
    await expect(
      claimTv({ data: { code: "424242", familyId: FAM }, auth: { uid: OWNER } }),
    ).rejects.toMatchObject({ code: "failed-precondition" });
  });

  it("requires both code and familyId", async () => {
    await expect(
      claimTv({ data: { code: "424242" }, auth: { uid: OWNER } }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
    await expect(
      claimTv({ data: { familyId: FAM }, auth: { uid: OWNER } }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });
});
