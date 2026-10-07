import { getAuth } from "firebase-admin/auth";
import { getFirestore, FieldValue, DocumentSnapshot } from "firebase-admin/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { FAMILY_ID_RE, requireNonAnonymous, requireString } from "./codes";

/**
 * Account and family deletion. Data about a child (daily app usage, installed
 * apps, request history) must not outlive the family that collected it, and
 * a parent must be able to remove their account and what it points at.
 */

/**
 * Deletes everything that belongs to one family: the family doc and every
 * subcollection under it (usage, limits, codes, requests, settings, tvApps),
 * its paired-device docs, its invites, and each member's pointer to it.
 */
async function deleteFamilyData(family: DocumentSnapshot): Promise<void> {
  const db = getFirestore();
  const familyId = family.id;
  const roles = (family.get("roles") as Record<string, string> | undefined) ?? {};

  // Members' users/{uid}.familyId → cleared, so their apps fall back to
  // onboarding instead of pointing at a family that no longer exists.
  const memberRefs = Object.keys(roles).map((uid) => db.collection("users").doc(uid));
  const memberDocs = memberRefs.length > 0 ? await db.getAll(...memberRefs) : [];
  const [devices, invites] = await Promise.all([
    db.collection("devices").where("familyId", "==", familyId).get(),
    db.collection("invites").where("familyId", "==", familyId).get(),
  ]);

  const writer = db.bulkWriter();
  for (const doc of memberDocs) {
    if (doc.exists && doc.get("familyId") === familyId) {
      writer.update(doc.ref, { familyId: FieldValue.delete() });
    }
  }
  devices.docs.forEach((d) => writer.delete(d.ref));
  invites.docs.forEach((d) => writer.delete(d.ref));
  await writer.close();

  await db.recursiveDelete(family.ref);
  logger.info(
    `Deleted family ${familyId}: ${devices.size} device(s), ${invites.size} invite(s), ${memberDocs.length} member pointer(s).`,
  );
}

/** The family OWNER deletes the family and all of its data, for every member. */
export const deleteFamily = onCall(async (req) => {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
  requireNonAnonymous(req);
  const familyId = requireString(req, "familyId", FAMILY_ID_RE);

  const family = await getFirestore().collection("families").doc(familyId).get();
  if (!family.exists) {
    throw new HttpsError("not-found", "Family not found.", { reason: "family_not_found" });
  }
  if (family.get("ownerUid") !== uid) {
    throw new HttpsError("permission-denied", "Only the family owner can delete the family.", {
      reason: "not_owner",
    });
  }
  await deleteFamilyData(family);
  return { success: true };
});

/**
 * Deletes the caller's account:
 *  - every family they OWN is deleted outright (for all members — the UI
 *    says so before confirming); in families they merely belong to, they are
 *    removed from `roles`;
 *  - unused invites they created are revoked;
 *  - users/{uid} and its private subcollection (push tokens) are deleted;
 *  - the Firebase Auth user is deleted.
 */
export const deleteAccount = onCall(async (req) => {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
  requireNonAnonymous(req);

  const db = getFirestore();
  const memberships = await db
    .collection("families")
    .where(`roles.${uid}`, "in", ["admin", "user"])
    .get();
  for (const family of memberships.docs) {
    if (family.get("ownerUid") === uid) {
      await deleteFamilyData(family);
    } else {
      await family.ref.update({ [`roles.${uid}`]: FieldValue.delete() });
    }
  }

  const invites = await db.collection("invites").where("createdBy", "==", uid).get();
  const writer = db.bulkWriter();
  invites.docs.filter((d) => d.get("used") !== true).forEach((d) => writer.delete(d.ref));
  await writer.close();

  await db.recursiveDelete(db.collection("users").doc(uid));

  try {
    await getAuth().deleteUser(uid);
  } catch (e) {
    if ((e as { code?: string }).code !== "auth/user-not-found") throw e;
  }
  logger.info(`Deleted account ${uid} (${memberships.size} family membership(s)).`);
  return { success: true };
});
