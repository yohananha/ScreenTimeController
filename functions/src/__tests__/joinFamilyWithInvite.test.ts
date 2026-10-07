import { Timestamp } from "firebase-admin/firestore";
import { initFFT, loadFunctions, db, seedFamily, parentAuth } from "./helpers";

let wrapped: (req: { data: unknown; auth: unknown }) => Promise<unknown>;

beforeAll(() => {
  const fft = initFFT();
  const fns = loadFunctions();
  wrapped = fft.wrap(fns.joinFamilyWithInvite) as typeof wrapped;
});

const FAM = "fam-join";
const ADMIN = "admin-uid";
const JOINER = "joiner-uid";

async function seedInvite(code: string, extra: Record<string, unknown> = {}) {
  await db().collection("invites").doc(code).set({
    familyId: FAM,
    createdBy: ADMIN,
    createdAt: Timestamp.now(),
    expiresAt: Timestamp.fromMillis(Date.now() + 3600_000),
    used: false,
    ...extra,
  });
}

describe("joinFamilyWithInvite", () => {
  beforeEach(async () => {
    await seedFamily({ familyId: FAM, ownerUid: ADMIN, roles: { [ADMIN]: "admin" } });
  });

  it("adds the user to roles, sets familyId on user doc, marks invite used", async () => {
    await seedInvite("ABCD2345");

    const result = (await wrapped({
      data: { code: "ABCD2345" },
      auth: parentAuth(JOINER),
    })) as { familyId: string };

    expect(result.familyId).toBe(FAM);

    const fam = await db().collection("families").doc(FAM).get();
    expect(fam.get(`roles.${JOINER}`)).toBe("user");

    const user = await db().collection("users").doc(JOINER).get();
    expect(user.get("familyId")).toBe(FAM);

    const invite = await db().collection("invites").doc("ABCD2345").get();
    expect(invite.get("used")).toBe(true);
    expect(invite.get("usedBy")).toBe(JOINER);
  });

  it("rejects an unknown code", async () => {
    await expect(
      wrapped({ data: { code: "ZZZZ9999" }, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects an already-used code", async () => {
    await seedInvite("ABCD2345", { used: true });
    await expect(
      wrapped({ data: { code: "ABCD2345" }, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects an expired code", async () => {
    await db().collection("invites").doc("ABCD2345").set({
      familyId: FAM,
      createdBy: ADMIN,
      createdAt: Timestamp.now(),
      expiresAt: Timestamp.fromMillis(Date.now() - 1000),
      used: false,
    });
    await expect(
      wrapped({ data: { code: "ABCD2345" }, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects when the referenced family is gone", async () => {
    await seedInvite("ABCD2345");
    await db().collection("families").doc(FAM).delete();
    await expect(
      wrapped({ data: { code: "ABCD2345" }, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("requires authentication", async () => {
    await expect(
      wrapped({ data: { code: "ABCD2345" }, auth: undefined as never }),
    ).rejects.toThrow(/unauthenticated|Sign-in/i);
  });

  it("requires a code", async () => {
    await expect(
      wrapped({ data: {}, auth: parentAuth(JOINER) }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });

  it("under concurrent joins of the same code, only one wins", async () => {
    await seedInvite("WXYZ7777");
    const results = await Promise.allSettled([
      wrapped({ data: { code: "WXYZ7777" }, auth: parentAuth("u1") }),
      wrapped({ data: { code: "WXYZ7777" }, auth: parentAuth("u2") }),
    ]);
    const winners = results.filter((r) => r.status === "fulfilled");
    expect(winners).toHaveLength(1);
  });
});
