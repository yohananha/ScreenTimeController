/**
 * Triggered-by-Firestore test: stubs getMessaging() so we don't need a real FCM
 * channel, then writes a /requests/{id} document and confirms the trigger fires
 * with the expected payload.
 */

import { Timestamp } from "firebase-admin/firestore";
import { initFFT, loadFunctions, db, seedFamily, seedUser, readPushTokens } from "./helpers";

jest.mock("firebase-admin/messaging", () => {
  const sendEachForMulticast = jest.fn().mockResolvedValue({
    successCount: 0,
    failureCount: 0,
    responses: [],
  });
  return {
    getMessaging: () => ({ sendEachForMulticast }),
    __mock__: { sendEachForMulticast },
  };
});

const messagingMock = jest.requireMock("firebase-admin/messaging") as {
  __mock__: { sendEachForMulticast: jest.Mock };
};

let onNewTimeRequest: ReturnType<ReturnType<typeof initFFT>["wrap"]>;

beforeAll(() => {
  const fft = initFFT();
  const fns = loadFunctions();
  onNewTimeRequest = fft.wrap(fns.onNewTimeRequest);
});

beforeEach(() => {
  messagingMock.__mock__.sendEachForMulticast.mockClear();
  messagingMock.__mock__.sendEachForMulticast.mockResolvedValue({
    successCount: 1,
    failureCount: 0,
    responses: [{}],
  });
});

const FAM = "fam-trigger";

// Writes the document to the emulator and returns a real DocumentSnapshot so
// firebase-functions-test's encodeHelper doesn't encounter function-valued
// properties (which it can't serialize to a Firestore Value).
async function makeEvent(data: Record<string, unknown>, requestId = "req-1") {
  const ref = db()
    .collection("families")
    .doc(FAM)
    .collection("requests")
    .doc(requestId);
  await ref.set(data);
  const snap = await ref.get();
  return {
    params: { familyId: FAM, requestId },
    data: snap,
  };
}

describe("onNewTimeRequest", () => {
  it("fans out an FCM multicast to every member's tokens", async () => {
    await seedFamily({
      familyId: FAM,
      ownerUid: "parent",
      roles: { parent: "admin", coparent: "user" },
    });
    await seedUser("parent", FAM, ["tok-parent-1", "tok-parent-2"]);
    await seedUser("coparent", FAM, ["tok-coparent"]);

    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 15 }),
    );

    expect(messagingMock.__mock__.sendEachForMulticast).toHaveBeenCalledTimes(1);
    const call = messagingMock.__mock__.sendEachForMulticast.mock.calls[0][0];
    expect(new Set(call.tokens)).toEqual(
      new Set(["tok-parent-1", "tok-parent-2", "tok-coparent"]),
    );
    expect(call.notification.body).toContain("15");
    expect(call.data.requestedMinutes).toBe("15");
  });

  it("ignores and deletes legacy fcmTokens on the family doc", async () => {
    await seedFamily({
      familyId: FAM,
      ownerUid: "parent",
      fcmTokens: ["legacy-1", "legacy-2"],
    });
    await seedUser("parent", FAM, ["tok-parent"]);

    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10 }),
    );

    const call = messagingMock.__mock__.sendEachForMulticast.mock.calls[0][0];
    expect(call.tokens).toEqual(["tok-parent"]);
    const fam = await db().collection("families").doc(FAM).get();
    expect(fam.get("fcmTokens")).toBeUndefined();
  });

  it("does not notify a removed member (no longer in roles)", async () => {
    await seedFamily({ familyId: FAM, ownerUid: "parent", roles: { parent: "admin" } });
    await seedUser("parent", FAM, ["tok-parent"]);
    // Removed from roles, but their user doc still points at the family.
    await seedUser("removed", FAM, ["tok-removed"]);

    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10 }),
    );

    const call = messagingMock.__mock__.sendEachForMulticast.mock.calls[0][0];
    expect(call.tokens).toEqual(["tok-parent"]);
  });

  it("does not notify a member whose user doc now points at another family", async () => {
    await seedFamily({
      familyId: FAM,
      ownerUid: "parent",
      roles: { parent: "admin", switched: "user" },
    });
    await seedUser("parent", FAM, ["tok-parent"]);
    await seedUser("switched", "some-other-family", ["tok-switched"]);

    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10 }),
    );

    const call = messagingMock.__mock__.sendEachForMulticast.mock.calls[0][0];
    expect(call.tokens).toEqual(["tok-parent"]);
  });

  it("prunes tokens FCM reports as permanently invalid, keeping the rest", async () => {
    await seedFamily({ familyId: FAM, ownerUid: "parent" });
    await seedUser("parent", FAM, ["tok-good", "tok-dead", "tok-flaky"]);
    messagingMock.__mock__.sendEachForMulticast.mockImplementation(
      async ({ tokens }: { tokens: string[] }) => ({
        successCount: 1,
        failureCount: 2,
        responses: tokens.map((t) =>
          t === "tok-dead"
            ? { success: false, error: { code: "messaging/registration-token-not-registered" } }
            : t === "tok-flaky"
              ? { success: false, error: { code: "messaging/internal-error" } }
              : { success: true },
        ),
      }),
    );

    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10 }),
    );

    expect(new Set(await readPushTokens("parent"))).toEqual(new Set(["tok-good", "tok-flaky"]));
  });

  it("is a no-op when there are no tokens", async () => {
    await seedFamily({ familyId: FAM, ownerUid: "parent" });
    await seedUser("parent", FAM, []);
    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10 }),
    );
    expect(messagingMock.__mock__.sendEachForMulticast).not.toHaveBeenCalled();
  });

  it("denies and marks request when requestedMinutes is out of range (too high)", async () => {
    await seedFamily({ familyId: FAM, ownerUid: "parent" });
    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 9999 }),
    );
    const after = await db()
      .collection("families")
      .doc(FAM)
      .collection("requests")
      .doc("req-1")
      .get();
    expect(after.get("status")).toBe("denied");
    expect(after.get("deniedReason")).toBe("invalid_minutes");
    expect(messagingMock.__mock__.sendEachForMulticast).not.toHaveBeenCalled();
  });

  it("denies on non-numeric minutes", async () => {
    await seedFamily({ familyId: FAM, ownerUid: "parent" });
    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: "abc" as never }),
    );
    const after = await db()
      .collection("families")
      .doc(FAM)
      .collection("requests")
      .doc("req-1")
      .get();
    expect(after.get("status")).toBe("denied");
  });
});

describe("onNewTimeRequest input hygiene and rate limiting", () => {
  const reqDoc = (id: string) =>
    db().collection("families").doc(FAM).collection("requests").doc(id);

  beforeEach(async () => {
    await seedFamily({ familyId: FAM, ownerUid: "parent" });
    await seedUser("parent", FAM, ["tok-parent"]);
  });

  it("denies a request whose appPackage is not a package name (it is used in a doc path)", async () => {
    await onNewTimeRequest(await makeEvent({ appPackage: "x/../../y", requestedMinutes: 10 }));
    expect((await reqDoc("req-1").get()).get("deniedReason")).toBe("invalid_package");
    expect(messagingMock.__mock__.sendEachForMulticast).not.toHaveBeenCalled();
  });

  it("denies a second request within 60s without pushing again", async () => {
    await reqDoc("earlier").set({
      appPackage: "com.x", requestedMinutes: 10, status: "pending",
      createdAt: Timestamp.fromMillis(Date.now() - 10_000),
    });
    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10, createdAt: Timestamp.now() }),
    );
    expect((await reqDoc("req-1").get()).get("deniedReason")).toBe("rate_limited");
    expect(messagingMock.__mock__.sendEachForMulticast).not.toHaveBeenCalled();
  });

  it("an older request does not block a new one", async () => {
    await reqDoc("old").set({
      appPackage: "com.x", requestedMinutes: 10, status: "pending",
      createdAt: Timestamp.fromMillis(Date.now() - 5 * 60_000),
    });
    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10, createdAt: Timestamp.now() }),
    );
    expect(messagingMock.__mock__.sendEachForMulticast).toHaveBeenCalledTimes(1);
  });

  it("the TV-supplied label is sanitized before it reaches the notification", async () => {
    await db().collection("families").doc(FAM).collection("tvApps").doc("com.x")
      .set({ label: "‮evil\nlabel" });
    await onNewTimeRequest(await makeEvent({ appPackage: "com.x", requestedMinutes: 10 }));
    const call = messagingMock.__mock__.sendEachForMulticast.mock.calls[0][0];
    expect(call.data.appLabel).toBe("evil label");
    expect(call.notification.body).not.toMatch(/[‪-‮\n]/);
  });
});

describe("sanitizeLabel", () => {
  it("strips controls and bidi overrides, collapses whitespace, caps length", () => {
    const { sanitizeLabel } = loadFunctions();
    expect(sanitizeLabel("  You⁦Tube⁩\t ")).toBe("YouTube");
    expect(sanitizeLabel("‮\u0007")).toBeNull();
    expect(sanitizeLabel(42)).toBeNull();
    const long = sanitizeLabel("x".repeat(200))!;
    expect(long.length).toBe(60);
    expect(long.endsWith("…")).toBe(true);
  });
});
