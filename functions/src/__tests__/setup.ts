// Shared after-each hook: clear Firestore + Auth between tests so each
// case starts from a clean emulator state. Functions tests import this
// transitively via jest.config setupFilesAfterEach.

import { execSync } from "child_process";

const PROJECT_ID = process.env.GCLOUD_PROJECT ?? "demo-screentime";
const FIRESTORE_HOST = process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080";
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST ?? "127.0.0.1:9099";

async function clearEmulator(url: string) {
  try {
    await fetch(`http://${url}`, { method: "DELETE" });
  } catch {
    // Emulator may not be running for rules-only tests; ignore.
  }
}

afterEach(async () => {
  await clearEmulator(
    `${FIRESTORE_HOST}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`,
  );
  await clearEmulator(`${AUTH_HOST}/emulator/v1/projects/${PROJECT_ID}/accounts`);
});

export { PROJECT_ID, FIRESTORE_HOST, AUTH_HOST };
