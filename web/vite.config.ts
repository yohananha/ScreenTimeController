/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    css: false,
    // Dummy but well-formed values so firebase/app's client-side validation
    // (e.g. the API key shape check) doesn't throw during module import in
    // tests that mock firestoreRepository/hooks but still transitively load
    // src/firebase/app.ts. Never real credentials.
    env: {
      VITE_FIREBASE_API_KEY: 'AIzaSyDUMMYKEYFAKE1234567890ABCDEFGH',
      VITE_FIREBASE_AUTH_DOMAIN: 'test.firebaseapp.com',
      VITE_FIREBASE_PROJECT_ID: 'test-project',
      VITE_FIREBASE_STORAGE_BUCKET: 'test-project.appspot.com',
      VITE_FIREBASE_MESSAGING_SENDER_ID: '000000000000',
      VITE_FIREBASE_APP_ID: '1:000000000000:web:0000000000000000000000',
      VITE_FIREBASE_VAPID_KEY: 'test-vapid-key',
    },
  },
});
