/**
 * One-off: stamps `expireAt` on usage and request docs written before
 * retention was added, so the Firestore TTL policies (firestore.indexes.json)
 * can delete them. New docs get it from the TV app.
 *
 *   usage/{yyyy-MM-dd}  → that date + 90 days
 *   requests/{id}       → createdAt + 90 days (now + 90 days if missing)
 *
 * Dry run by default; pass --apply to write. Needs admin credentials:
 *   gcloud auth application-default login
 *   GCLOUD_PROJECT=<project-id> node scripts/backfillExpireAt.js [--apply]
 *
 * Not deployed (firebase.json ignores functions/scripts).
 */
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");

const RETENTION_MS = 90 * 24 * 60 * 60 * 1000;
const apply = process.argv.includes("--apply");

initializeApp();
const db = getFirestore();

async function backfill(group, expiryFor) {
  const snap = await db.collectionGroup(group).get();
  const writer = db.bulkWriter();
  let pending = 0;
  for (const doc of snap.docs) {
    // Only the family subcollections, and only docs not stamped yet.
    if (doc.ref.parent.parent?.parent.id !== "families" || doc.get("expireAt")) continue;
    const expireAt = expiryFor(doc);
    if (!expireAt) continue;
    pending++;
    if (apply) writer.update(doc.ref, { expireAt });
  }
  await writer.close();
  console.log(`${group}: ${pending} doc(s) ${apply ? "updated" : "would be updated (dry run)"}`);
}

(async () => {
  await backfill("usage", (doc) => {
    const day = Date.parse(`${doc.id}T00:00:00Z`);
    return Number.isNaN(day) ? null : Timestamp.fromMillis(day + RETENTION_MS);
  });
  await backfill("requests", (doc) => {
    const created = doc.get("createdAt");
    const from = created && typeof created.toMillis === "function" ? created.toMillis() : Date.now();
    return Timestamp.fromMillis(from + RETENTION_MS);
  });
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
