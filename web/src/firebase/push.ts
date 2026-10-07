import { deleteToken, getToken, onMessage, type Messaging } from 'firebase/messaging';
import type { Unsubscribe } from 'firebase/firestore';
import { doc, setDoc, arrayRemove, arrayUnion } from 'firebase/firestore';
import { getMessagingIfSupported } from './messaging';
import { db } from './app';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

async function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (!('serviceWorker' in navigator)) return null;
  const query = new URLSearchParams(firebaseConfig as Record<string, string>).toString();
  return navigator.serviceWorker.register(`/firebase-messaging-sw.js?${query}`);
}

/**
 * users/{uid}/private/push — the only place FCM tokens are stored. Readable
 * and writable by that user alone (see firestore.rules), so other family
 * members and the TV never see them; onNewTimeRequest reads it server-side.
 */
function pushDoc(uid: string) {
  return doc(db, 'users', uid, 'private', 'push');
}

async function currentToken(messaging: Messaging): Promise<string | null> {
  const registration = await registerServiceWorker();
  const token = await getToken(messaging, {
    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
    serviceWorkerRegistration: registration ?? undefined,
  });
  return token || null;
}

/**
 * Requests notification permission (a user gesture is required on web,
 * unlike Android's install-time grant) and, on success, saves this
 * browser's FCM token for [uid].
 */
export async function enableNotifications(uid: string): Promise<boolean> {
  const messaging = await getMessagingIfSupported();
  if (!messaging) return false;

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return false;

  const token = await currentToken(messaging);
  if (!token) return false;

  await setDoc(pushDoc(uid), { tokens: arrayUnion(token) }, { merge: true });
  return true;
}

/**
 * Re-saves this browser's token on app load when permission was already
 * granted, so a rotated token (or one saved before tokens moved to the
 * private doc) is picked up without the user re-enabling. Never prompts.
 */
export async function syncNotificationToken(uid: string): Promise<void> {
  if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return;
  const messaging = await getMessagingIfSupported();
  if (!messaging) return;
  const token = await currentToken(messaging);
  if (token) await setDoc(pushDoc(uid), { tokens: arrayUnion(token) }, { merge: true });
}

/**
 * On sign-out: removes this browser's token from [uid]'s push doc and
 * invalidates it with FCM, so this browser stops getting the family's
 * notifications once someone else (or no one) is signed in. Must run
 * before Firebase sign-out — the write needs [uid]'s credentials.
 */
export async function disableNotifications(uid: string): Promise<void> {
  if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return;
  const messaging = await getMessagingIfSupported();
  if (!messaging) return;
  const token = await currentToken(messaging);
  if (!token) return;
  await setDoc(pushDoc(uid), { tokens: arrayRemove(token) }, { merge: true });
  await deleteToken(messaging);
}

/** Foreground messages don't auto-show a system notification on web; caller renders an in-app toast. */
export async function subscribeForegroundMessages(
  cb: (payload: { title: string; body: string }) => void,
): Promise<Unsubscribe | null> {
  const messaging = await getMessagingIfSupported();
  if (!messaging) return null;
  return onMessage(messaging, (payload) => {
    const title = payload.notification?.title ?? payload.data?.title ?? 'Time request';
    const body = payload.notification?.body ?? payload.data?.body ?? '';
    cb({ title, body });
  });
}
