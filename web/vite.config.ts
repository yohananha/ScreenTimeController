/// <reference types="vitest/config" />
import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';

// One id per build. It is baked into the bundle and also written to
// /version.json, so a tab running an older bundle can tell a newer deploy exists.
const BUILD_ID = Date.now().toString(36);

function versionFile(): Plugin {
  return {
    name: 'emit-version-json',
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify({ buildId: BUILD_ID }) });
    },
  };
}

export default defineConfig({
  plugins: [react(), versionFile()],
  define: { __BUILD_ID__: JSON.stringify(BUILD_ID) },
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
