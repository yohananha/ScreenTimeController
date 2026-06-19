import { Timestamp } from "firebase-admin/firestore";
import { initFFT, loadFunctions, db, seedFamily } from "./helpers";

let wrapped: (req: { data: unknown; auth: { uid: string } }) => Promise<unknown>;

beforeAll(() => {
  const fft = initFFT();
  const fns = loadFunctions();
  wrapped = fft.wrap(fns.createFamilyInvite) as typeof wrapped;
});

const FAM = "fam-invite";
const ADMIN = "admin-uid";
const USER = "user-uid";

describe("createFamilyInvite", () => {
  it("allows an admin to create an invite with 48h TTL", async () => {
    await seedFamily({
      familyId: FAM,
      ownerUid: ADMIN,
      roles: { [ADMIN]: "admin" },
    });

    const before = Date.now();
    const result = (await wrapped({
      data: { familyId: FAM },
      auth: { uid: ADMIN },
    })) as { code: string };

    expect(result.code).toMatch(/^\d{6}$/);

    const inviteSnap = await db().collection("invites").doc(result.code).get();
    expect(inviteSnap.exists).toBe(true);
    expect(inviteSnap.get("familyId")).toBe(FAM);
    expect(inviteSnap.get("createdBy")).toBe(ADMIN);
    expect(inviteSnap.get("used")).toBe(false);
    const expiresAt = inviteSnap.get("expiresAt") as Timestamp;
    const ms = expiresAt.toMillis();
    expect(ms).toBeGreaterThanOrEqual(before + 47 * 3600_000);
    expect(ms).toBeLessThanOrEqual(before + 49 * 3600_000);
  });

  it("denies non-admins", async () => {
    await seedFamily({
      familyId: FAM,
      ownerUid: ADMIN,
      roles: { [ADMIN]: "admin", [USER]: "user" },
    });
    await expect(
      wrapped({ data: { familyId: FAM }, auth: { uid: USER } }),
    ).rejects.toMatchObject({ code: "permission-denied" });
  });

  it("requires authentication", async () => {
    await expect(
      wrapped({ data: { familyId: FAM }, auth: undefined as never }),
    ).rejects.toThrow(/unauthenticated|Sign-in/i);
  });

  it("requires a familyId", async () => {
    await expect(
      wrapped({ data: {}, auth: { uid: ADMIN } }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });

  it("returns not-found when the family does not exist", async () => {
    await expect(
      wrapped({ data: { familyId: "ghost" }, auth: { uid: ADMIN } }),
    ).rejects.toMatchObject({ code: "not-found" });
  });
});
