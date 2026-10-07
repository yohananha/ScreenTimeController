import { Timestamp } from "firebase-admin/firestore";
import {
  initFFT,
  loadFunctions,
  db,
  seedFamily,
  parentAuth,
  anonAuth,
  readLockout,
} from "./helpers";
import * as codes from "../codes";

type Callable = (req: { data: unknown; auth: unknown }) => Promise<unknown>;
let join: Callable;
let createTv: Callable;
let claimTv: Callable;
let redeem: Callable;

beforeAll(() => {
  const fft = initFFT();
  const fns = loadFunctions();
  join = fft.wrap(fns.joinFamilyWithInvite) as Callable;
  createTv = fft.wrap(fns.createTvPairing) as Callable;
  claimTv = fft.wrap(fns.claimTvPairing) as Callable;
  redeem = fft.wrap(fns.redeemCode) as Callable;
});

afterEach(() => jest.restoreAllMocks());

const FAM = "fam-sec";
const OWNER = "owner-uid";
const JOINER = "joiner-uid";
const TV = "tv-uid";

async function seedInvite(code: string) {
  await db().collection("invites").doc(code).set({
    familyId: FAM,
    createdBy: OWNER,
    createdAt: Timestamp.now(),
    expiresAt: Timestamp.fromMillis(Date.now() + 3600_000),
    used: false,
  });
}

describe("code generation", () => {
  it("numericCode is zero-padded digits of the requested length", () => {
    for (let i = 0; i < 200; i++) expect(codes.numericCode(6)).toMatch(/^\d{6}$/);
  });

  it("inviteCode uses the unambiguous 8-char alphabet", () => {
    for (let i = 0; i < 200; i++) {
      const c = codes.inviteCode();
      expect(c).toMatch(/^[A-HJ-NP-Z2-9]{8}$/);
      expect(codes.isInviteCode(c)).toBe(true);
    }
  });

  it("normalizeInviteCode uppercases and strips separators", () => {
    expect(codes.normalizeInviteCode("abcd-2345")).toBe("ABCD2345");
    expect(codes.normalizeInviteCode(" ab cd 23 45 ")).toBe("ABCD2345");
    expect(codes.isInviteCode("ABCD0O1I")).toBe(false);
  });
});

describe("joinFamilyWithInvite hardening", () => {
  beforeEach(async () => {
    await seedFamily({ familyId: FAM, ownerUid: OWNER });
  });

  it("rejects anonymous callers", async () => {
    await seedInvite("ABCD2345");
    await expect(
      join({ data: { code: "ABCD2345" }, auth: anonAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "permission-denied" });
  });

  it("accepts the display form (lowercase, dashed)", async () => {
    await seedInvite("ABCD2345");
    const res = (await join({ data: { code: "abcd-2345" }, auth: parentAuth(JOINER) })) as {
      familyId: string;
    };
    expect(res.familyId).toBe(FAM);
  });

  it("answers a legacy 6-digit or malformed code as not-found", async () => {
    await expect(
      join({ data: { code: "123456" }, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects a non-string code as invalid-argument", async () => {
    await expect(
      join({ data: { code: 12345678 }, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });

  it("rate-limits the 11th attempt within the hour", async () => {
    for (let i = 0; i < 10; i++) {
      await expect(
        join({ data: { code: "ZZZZ9999" }, auth: parentAuth(JOINER) }),
      ).rejects.toMatchObject({ code: "not-found" });
    }
    await seedInvite("ABCD2345");
    await expect(
      join({ data: { code: "ABCD2345" }, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "resource-exhausted" });
  });

  it("an existing admin redeeming their own invite keeps the admin role", async () => {
    await seedInvite("ABCD2345");
    await join({ data: { code: "ABCD2345" }, auth: parentAuth(OWNER) });
    const fam = await db().collection("families").doc(FAM).get();
    expect(fam.get(`roles.${OWNER}`)).toBe("admin");
    const invite = await db().collection("invites").doc("ABCD2345").get();
    expect(invite.get("used")).toBe(false);
  });
});

describe("TV pairing hardening", () => {
  it("rate-limits createTvPairing to 5 per 10 minutes", async () => {
    for (let i = 0; i < 5; i++) await createTv({ data: {}, auth: anonAuth(TV) });
    await expect(createTv({ data: {}, auth: anonAuth(TV) })).rejects.toMatchObject({
      code: "resource-exhausted",
    });
  });

  it("reuses a code whose pairing doc has expired", async () => {
    jest.spyOn(codes, "numericCode").mockReturnValue("123456");
    await db().collection("pairings").doc("123456").set({
      deviceId: "old-tv",
      expiresAt: Timestamp.fromMillis(Date.now() - 1000),
    });
    const res = (await createTv({ data: {}, auth: anonAuth(TV) })) as { code: string };
    expect(res.code).toBe("123456");
    const snap = await db().collection("pairings").doc("123456").get();
    expect(snap.get("deviceId")).toBe(TV);
  });

  it("never overwrites a live pairing doc", async () => {
    jest.spyOn(codes, "numericCode").mockReturnValue("123456");
    await db().collection("pairings").doc("123456").set({
      deviceId: "other-tv",
      expiresAt: Timestamp.fromMillis(Date.now() + 60_000),
    });
    await expect(createTv({ data: {}, auth: anonAuth(TV) })).rejects.toMatchObject({
      code: "resource-exhausted",
    });
    const snap = await db().collection("pairings").doc("123456").get();
    expect(snap.get("deviceId")).toBe("other-tv");
  });

  it("claimTvPairing rejects anonymous callers", async () => {
    await seedFamily({ familyId: FAM, ownerUid: OWNER });
    await expect(
      claimTv({ data: { code: "424242", familyId: FAM }, auth: anonAuth(OWNER) }),
    ).rejects.toMatchObject({ code: "permission-denied" });
  });

  it("claimTvPairing rejects a malformed code without touching Firestore", async () => {
    await seedFamily({ familyId: FAM, ownerUid: OWNER });
    await expect(
      claimTv({ data: { code: "42/242", familyId: FAM }, auth: parentAuth(OWNER) }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });

  it("claimTvPairing rate-limits the 11th attempt", async () => {
    await seedFamily({ familyId: FAM, ownerUid: OWNER });
    for (let i = 0; i < 10; i++) {
      await expect(
        claimTv({ data: { code: "999999", familyId: FAM }, auth: parentAuth(OWNER) }),
      ).rejects.toMatchObject({ code: "not-found" });
    }
    await expect(
      claimTv({ data: { code: "999999", familyId: FAM }, auth: parentAuth(OWNER) }),
    ).rejects.toMatchObject({ code: "resource-exhausted" });
  });
});

describe("redeemCode input validation", () => {
  it("rejects a non-6-digit code as invalid-argument without counting a failure", async () => {
    await seedFamily({ familyId: FAM, ownerUid: OWNER, devices: [TV] });
    await expect(
      redeem({ data: { familyId: FAM, code: { $gt: "" } }, auth: { uid: TV } }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
    expect((await readLockout(FAM))?.failureCount).toBeUndefined();
  });
});
