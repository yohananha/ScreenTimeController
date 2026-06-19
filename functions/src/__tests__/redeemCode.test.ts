import { Timestamp } from "firebase-admin/firestore";
import {
  initFFT,
  loadFunctions,
  db,
  seedFamily,
  seedCode,
  seedLockout,
  readLockout,
} from "./helpers";

const FAMILY = "fam-redeem";
const DEVICE = "tv-device-1";
const OTHER_FAMILY = "fam-other";

let wrapped: (req: { data: unknown; auth: { uid: string } }) => Promise<unknown>;

beforeAll(() => {
  const fft = initFFT();
  const fns = loadFunctions();
  wrapped = fft.wrap(fns.redeemCode) as typeof wrapped;
});

async function call(data: unknown, uid: string = DEVICE) {
  return wrapped({ data, auth: { uid } });
}

describe("redeemCode", () => {
  describe("happy path", () => {
    it("returns extraMinutes, deletes the code, and clears the failure counter", async () => {
      await seedFamily({ familyId: FAMILY, ownerUid: "parent", devices: [DEVICE] });
      await seedCode(FAMILY, "111111", 30);
      await seedLockout(FAMILY, {
        failureCount: 2,
        failureWindowStart: Timestamp.now(),
      });

      const result = (await call({ familyId: FAMILY, code: "111111" })) as {
        extraMinutes: number;
      };

      expect(result.extraMinutes).toBe(30);

      const codeSnap = await db()
        .collection("families").doc(FAMILY)
        .collection("codes").doc("111111").get();
      expect(codeSnap.exists).toBe(false);

      const lockout = await readLockout(FAMILY);
      expect(lockout?.failureCount).toBe(0);
      expect(lockout?.failureWindowStart).toBeUndefined();
    });
  });

  describe("auth + pairing", () => {
    it("rejects unauthenticated callers", async () => {
      await expect(
        wrapped({ data: { familyId: FAMILY, code: "111111" }, auth: undefined as never }),
      ).rejects.toThrow(/unauthenticated|Sign-in/i);
    });

    it("rejects missing arguments", async () => {
      await expect(call({})).rejects.toMatchObject({ code: "invalid-argument" });
      await expect(call({ familyId: FAMILY })).rejects.toMatchObject({
        code: "invalid-argument",
      });
    });

    it("rejects when family does not exist", async () => {
      await expect(call({ familyId: "ghost", code: "111111" })).rejects.toMatchObject({
        code: "not-found",
      });
    });

    it("rejects when device is not paired to the family", async () => {
      await seedFamily({ familyId: FAMILY, ownerUid: "parent", devices: ["other-tv"] });
      await expect(call({ familyId: FAMILY, code: "111111" })).rejects.toMatchObject({
        code: "permission-denied",
      });
    });
  });

  describe("lockout", () => {
    it("refuses redemption while locked", async () => {
      await seedFamily({ familyId: FAMILY, ownerUid: "parent", devices: [DEVICE] });
      await seedCode(FAMILY, "111111", 30);
      await seedLockout(FAMILY, { locked: true });

      await expect(call({ familyId: FAMILY, code: "111111" })).rejects.toMatchObject({
        code: "failed-precondition",
      });

      // Code must NOT be consumed while locked.
      const codeSnap = await db()
        .collection("families").doc(FAMILY)
        .collection("codes").doc("111111").get();
      expect(codeSnap.exists).toBe(true);
    });
  });

  describe("wrong code + failure counter", () => {
    beforeEach(async () => {
      await seedFamily({ familyId: FAMILY, ownerUid: "parent", devices: [DEVICE] });
    });

    it("increments the counter on a wrong code", async () => {
      await expect(call({ familyId: FAMILY, code: "999999" })).rejects.toMatchObject({
        code: "not-found",
      });
      const lockout = await readLockout(FAMILY);
      expect(lockout?.failureCount).toBe(1);
      expect(lockout?.failureWindowStart).toBeDefined();
    });

    it("treats an expired code as wrong AND deletes it", async () => {
      await seedCode(FAMILY, "111111", 30, Date.now() - 1000);
      await expect(call({ familyId: FAMILY, code: "111111" })).rejects.toMatchObject({
        code: "not-found",
      });
      const codeSnap = await db()
        .collection("families").doc(FAMILY)
        .collection("codes").doc("111111").get();
      expect(codeSnap.exists).toBe(false);
      const lockout = await readLockout(FAMILY);
      expect(lockout?.failureCount).toBe(1);
    });

    it("resets the counter to 1 when the previous window has expired", async () => {
      await seedLockout(FAMILY, {
        failureCount: 4,
        failureWindowStart: Timestamp.fromMillis(Date.now() - 5 * 60_000),
      });
      await expect(call({ familyId: FAMILY, code: "999999" })).rejects.toMatchObject({
        code: "not-found",
      });
      const lockout = await readLockout(FAMILY);
      expect(lockout?.failureCount).toBe(1);
    });

    it("locks (no lockedUntil) in parent mode after MAX failures", async () => {
      await seedLockout(FAMILY, {
        failureCount: 4,
        failureWindowStart: Timestamp.now(),
        mode: "parent",
        durationMinutes: 15,
      });
      await expect(call({ familyId: FAMILY, code: "999999" })).rejects.toMatchObject({
        code: "not-found",
      });
      const lockout = await readLockout(FAMILY);
      expect(lockout?.locked).toBe(true);
      expect(lockout?.failureCount).toBe(0);
      expect(lockout?.lockedUntil).toBeUndefined();
      expect(lockout?.lockedAt).toBeDefined();
    });

    it("locks WITH lockedUntil in time mode after MAX failures", async () => {
      await seedLockout(FAMILY, {
        failureCount: 4,
        failureWindowStart: Timestamp.now(),
        durationMinutes: 20,
      });
      const before = Date.now();
      await expect(call({ familyId: FAMILY, code: "999999" })).rejects.toMatchObject({
        code: "not-found",
      });
      const lockout = await readLockout(FAMILY);
      expect(lockout?.locked).toBe(true);
      const lockedUntilMs = (lockout?.lockedUntil as Timestamp).toMillis();
      expect(lockedUntilMs).toBeGreaterThanOrEqual(before + 20 * 60_000 - 1000);
      expect(lockedUntilMs).toBeLessThanOrEqual(before + 20 * 60_000 + 5000);
    });
  });

  describe("transaction atomicity", () => {
    it("only one of two concurrent redeems succeeds", async () => {
      await seedFamily({ familyId: FAMILY, ownerUid: "parent", devices: [DEVICE] });
      await seedCode(FAMILY, "222222", 15);

      const results = await Promise.allSettled([
        call({ familyId: FAMILY, code: "222222" }),
        call({ familyId: FAMILY, code: "222222" }),
      ]);
      const fulfilled = results.filter((r) => r.status === "fulfilled");
      expect(fulfilled).toHaveLength(1);
    });
  });

  describe("cross-family safety", () => {
    it("a device paired to family A cannot redeem against family B's codes", async () => {
      await seedFamily({ familyId: FAMILY, ownerUid: "p1", devices: [DEVICE] });
      await seedFamily({
        familyId: OTHER_FAMILY,
        ownerUid: "p2",
        devices: ["other-tv"],
      });
      await seedCode(OTHER_FAMILY, "111111", 30);

      await expect(
        call({ familyId: OTHER_FAMILY, code: "111111" }),
      ).rejects.toMatchObject({ code: "permission-denied" });
    });
  });
});
