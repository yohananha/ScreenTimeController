import { GoogleAuthProvider, onAuthStateChanged, signInWithPopup, signOut as fbSignOut } from 'firebase/auth';
import type { Unsubscribe } from 'firebase/auth';
import { auth } from './app';

export interface Session {
  uid: string;
  email: string | null;
}

export function subscribeSession(cb: (session: Session | null) => void): Unsubscribe {
  return onAuthStateChanged(auth, (user) => {
    cb(user ? { uid: user.uid, email: user.email } : null);
  });
}

export async function signInWithGoogle(): Promise<void> {
  await signInWithPopup(auth, new GoogleAuthProvider());
}

export function signOut(): Promise<void> {
  return fbSignOut(auth);
}
