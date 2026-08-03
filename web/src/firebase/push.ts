import { getToken, onMessage } from 'firebase/messaging';
import type { Unsubscribe } from 'firebase/firestore';
import { doc, setDoc, arrayUnion } from 'firebase/firestore';
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
 * Requests notification permission (a user gesture is required on web,
 * unlike Android's install-time grant) and, on success, registers the FCM
 * token to families/{familyId}.fcmTokens — the exact field the
 * onNewTimeRequest Cloud Function already checks first. Mirrors
 * PushService.onNewToken's write path; no backend change needed.
 */
export async function enableNotifications(familyId: string): Promise<boolean> {
  const messaging = await getMessagingIfSupported();
  if (!messaging) return false;

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return false;

  const registration = await registerServiceWorker();
  const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;
  const token = await getToken(messaging, {
    vapidKey,
    serviceWorkerRegistration: registration ?? undefined,
  });
  if (!token) return false;

  await setDoc(doc(db, 'families', familyId), { fcmTokens: arrayUnion(token) }, { merge: true });
  return true;
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
