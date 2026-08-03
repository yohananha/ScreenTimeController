/* eslint-disable no-undef */
// Background FCM handler. Service workers can't read Vite env vars or use
// the modular SDK's app singleton, so the app registers this worker with the
// Firebase config passed as query params (see src/firebase/push.ts) and we
// read them back here via location.search.
importScripts('https://www.gstatic.com/firebasejs/11.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.0.0/firebase-messaging-compat.js');

const params = new URLSearchParams(self.location.search);

firebase.initializeApp({
  apiKey: params.get('apiKey'),
  authDomain: params.get('authDomain'),
  projectId: params.get('projectId'),
  storageBucket: params.get('storageBucket'),
  messagingSenderId: params.get('messagingSenderId'),
  appId: params.get('appId'),
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const title = payload.notification?.title ?? payload.data?.title ?? 'Time request';
  const body = payload.notification?.body ?? payload.data?.body ?? '';
  self.registration.showNotification(title, {
    body,
    icon: '/favicon.svg',
    data: { openTab: 'requests' },
  });
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.postMessage({ type: 'open-requests' });
          return client.focus();
        }
      }
      return self.clients.openWindow('/requests');
    }),
  );
});
