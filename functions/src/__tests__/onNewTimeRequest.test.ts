/**
 * Triggered-by-Firestore test: stubs getMessaging() so we don't need a real FCM
 * channel, then writes a /requests/{id} document and confirms the trigger fires
 * with the expected payload.
 */

import { initFFT, loadFunctions, db, seedFamily, seedUser } from "./helpers";

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
    await seedUser("parent", ["tok-parent-1", "tok-parent-2"]);
    await seedUser("coparent", ["tok-coparent"]);

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

  it("falls back to legacy fcmTokens on the family doc", async () => {
    await seedFamily({
      familyId: FAM,
      ownerUid: "p",
      fcmTokens: ["legacy-1", "legacy-2"],
    });

    await onNewTimeRequest(
      await makeEvent({ appPackage: "com.x", requestedMinutes: 10 }),
    );

    const call = messagingMock.__mock__.sendEachForMulticast.mock.calls[0][0];
    expect(new Set(call.tokens)).toEqual(new Set(["legacy-1", "legacy-2"]));
  });

  it("is a no-op when there are no tokens", async () => {
    await seedFamily({ familyId: FAM, ownerUid: "parent" });
    await seedUser("parent", []);
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
