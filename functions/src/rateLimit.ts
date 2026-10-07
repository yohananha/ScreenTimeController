import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";

/**
 * Fixed-window counter at /rateLimits/{key} (client access denied by rules;
 * expiresAt drives a TTL policy so stale counters clean themselves up).
 *
 * [consume] records one hit and throws resource-exhausted once [max] hits
 * have landed inside the current [windowMs] window. [check] is the read-only
 * variant, used to refuse a caller *before* doing work when only failures
 * are counted (see joinFamilyWithInvite).
 */
export async function consume(key: string, max: number, windowMs: number): Promise<void> {
  const db = getFirestore();
  const ref = db.collection("rateLimits").doc(key);
  const allowed = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const now = Date.now();
    const windowStart = (snap.get("windowStart") as Timestamp | undefined)?.toMillis();
    const fresh = windowStart === undefined || now - windowStart >= windowMs;
    const count = fresh ? 0 : ((snap.get("count") as number | undefined) ?? 0);
    if (count >= max) return false;
    const start = fresh ? now : windowStart!;
    tx.set(ref, {
      count: count + 1,
      windowStart: Timestamp.fromMillis(start),
      expiresAt: Timestamp.fromMillis(start + windowMs),
    });
    return true;
  });
  if (!allowed) throw rateLimited();
}

export async function check(key: string, max: number, windowMs: number): Promise<void> {
  const snap = await getFirestore().collection("rateLimits").doc(key).get();
  const windowStart = (snap.get("windowStart") as Timestamp | undefined)?.toMillis();
  if (windowStart === undefined || Date.now() - windowStart >= windowMs) return;
  if (((snap.get("count") as number | undefined) ?? 0) >= max) throw rateLimited();
}

function rateLimited(): HttpsError {
  return new HttpsError("resource-exhausted", "Too many attempts. Try again later.", {
    reason: "rate_limited",
  });
}
