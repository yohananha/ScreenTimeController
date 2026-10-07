import { GoogleAuthProvider, onAuthStateChanged, signInWithPopup, signOut as fbSignOut } from 'firebase/auth';
import type { Unsubscribe } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore';
import { auth, db } from './app';
import { disableNotifications } from './push';
import { deleteAccount } from './firestoreRepository';

export interface Session {
  uid: string;
  email: string | null;
}

/**
 * onAuthStateChanged fires immediately with whatever session is currently
 * active — including a persisted sign-in restored on page load, not just a
 * fresh interactive sign-in — so syncing the display name here (rather than
 * only inside signInWithGoogle) also backfills it for members who were
 * already signed in before this shipped, the next time they load the app.
 */
export function subscribeSession(cb: (session: Session | null) => void): Unsubscribe {
  return onAuthStateChanged(auth, (user) => {
    if (user) {
      setDoc(doc(db, 'users', user.uid), { displayName: user.displayName }, { merge: true }).catch(() => {});
    }
    cb(user ? { uid: user.uid, email: user.email } : null);
  });
}

export async function signInWithGoogle(): Promise<void> {
  await signInWithPopup(auth, new GoogleAuthProvider());
}

/**
 * Unregisters this browser's push token first (best-effort — sign-out must
 * never be blocked by a messaging/network failure), then signs out.
 */
/**
 * Deletes the account server-side, then signs out. The push token is
 * unregistered first, while the user (and their push doc) still exist.
 */
export async function deleteAccountAndSignOut(): Promise<void> {
  const uid = auth.currentUser?.uid;
  if (uid) await disableNotifications(uid).catch(() => {});
  await deleteAccount();
  await fbSignOut(auth);
}

export async function signOut(): Promise<void> {
  const uid = auth.currentUser?.uid;
  if (uid) await disableNotifications(uid).catch(() => {});
  await fbSignOut(auth);
}
