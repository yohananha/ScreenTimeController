import { doc, onSnapshot } from 'firebase/firestore';
import type { Unsubscribe } from 'firebase/firestore';
import { db } from './app';

/** Resolves familyId from users/{uid}.familyId. */
export function subscribeFamilyId(uid: string, cb: (familyId: string | null) => void): Unsubscribe {
  return onSnapshot(
    doc(db, 'users', uid),
    (snap) => cb((snap.data()?.familyId as string | undefined) ?? null),
    () => cb(null),
  );
}
