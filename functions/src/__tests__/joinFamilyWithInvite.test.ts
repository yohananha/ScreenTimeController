import { Timestamp } from "firebase-admin/firestore";
import { initFFT, loadFunctions, db, seedFamily } from "./helpers";

let wrapped: (req: { data: unknown; auth: { uid: string } }) => Promise<unknown>;

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
    await seedInvite("123456");

    const result = (await wrapped({
      data: { code: "123456" },
      auth: { uid: JOINER },
    })) as { familyId: string };

    expect(result.familyId).toBe(FAM);

    const fam = await db().collection("families").doc(FAM).get();
    expect(fam.get(`roles.${JOINER}`)).toBe("user");

    const user = await db().collection("users").doc(JOINER).get();
    expect(user.get("familyId")).toBe(FAM);

    const invite = await db().collection("invites").doc("123456").get();
    expect(invite.get("used")).toBe(true);
    expect(invite.get("usedBy")).toBe(JOINER);
  });

  it("rejects an unknown code", async () => {
    await expect(
      wrapped({ data: { code: "999999" }, auth: { uid: JOINER } }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects an already-used code", async () => {
    await seedInvite("123456", { used: true });
    await expect(
      wrapped({ data: { code: "123456" }, auth: { uid: JOINER } }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects an expired code", async () => {
    await db().collection("invites").doc("123456").set({
      familyId: FAM,
      createdBy: ADMIN,
      createdAt: Timestamp.now(),
      expiresAt: Timestamp.fromMillis(Date.now() - 1000),
      used: false,
    });
    await expect(
      wrapped({ data: { code: "123456" }, auth: { uid: JOINER } }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("rejects when the referenced family is gone", async () => {
    await seedInvite("123456");
    await db().collection("families").doc(FAM).delete();
    await expect(
      wrapped({ data: { code: "123456" }, auth: { uid: JOINER } }),
    ).rejects.toMatchObject({ code: "not-found" });
  });

  it("requires authentication", async () => {
    await expect(
      wrapped({ data: { code: "123456" }, auth: undefined as never }),
    ).rejects.toThrow(/unauthenticated|Sign-in/i);
  });

  it("requires a code", async () => {
    await expect(
      wrapped({ data: {}, auth: { uid: JOINER } }),
    ).rejects.toMatchObject({ code: "invalid-argument" });
  });

  it("under concurrent joins of the same code, only one wins", async () => {
    await seedInvite("777777");
    const results = await Promise.allSettled([
      wrapped({ data: { code: "777777" }, auth: { uid: "u1" } }),
      wrapped({ data: { code: "777777" }, auth: { uid: "u2" } }),
    ]);
    const winners = results.filter((r) => r.status === "fulfilled");
    expect(winners).toHaveLength(1);
  });
});
