# Testing

Layered test suite covering Android logic, Compose UI, Firebase Cloud Functions,
and Firestore security rules. Designed so the fast PR gate runs in ~5–10 minutes
on GitHub Actions, while slower instrumented UI tests run on release tags.

## Layers

| Layer                       | Tool                                   | Runs on              | Module(s)                   |
| --------------------------- | -------------------------------------- | -------------------- | --------------------------- |
| JVM unit tests + Robolectric | JUnit4, MockK, Turbine, Truth         | Every PR             | `mobile`, `tv`, `shared`    |
| Coverage gate (≥80%)        | Kotlinx Kover                          | Every PR             | aggregate                   |
| Cloud Functions             | Jest + firebase-functions-test + Emulator | Every PR          | `functions`                 |
| Firestore rules             | `@firebase/rules-unit-testing` + Emulator | Every PR          | `firestore.rules`           |
| Android lint                | AGP                                    | Every PR             | `mobile`, `tv`              |
| Compose UI (instrumented)   | androidx.compose.ui.test               | Release tags + local | `mobile`, `tv`              |

## Running locally

### Prerequisites

- JDK 17 (Temurin recommended)
- Node 20 + `npm i -g firebase-tools` (for Functions and rules tests)
- An Android SDK install for AndroidTest runs (optional unless you want UI)

### One-shot

```powershell
./scripts/test-all.ps1
```

### Per-layer

```powershell
# Android JVM tests + coverage check (the PR gate)
./gradlew testDebugUnitTest koverXmlReport koverVerify
# HTML report: build/reports/kover/html/index.html

# Cloud Functions (boots Firestore + Auth emulators, then runs Jest with coverage)
cd functions
npm install
npm test

# Firestore rules only
npm run test:rules

# Compose UI on a connected device or emulator (Pixel for mobile, ATV for tv)
./gradlew :mobile:connectedDebugAndroidTest
./gradlew :tv:connectedDebugAndroidTest
```

## What's covered

### Cloud Functions (`functions/src/__tests__`)
- `redeemCode` — happy path, locked, wrong code → counter, window reset,
  parent-mode lockout, time-mode lockout (with `lockedUntil`), expired code
  deletion, transaction atomicity, cross-family safety, auth/pairing checks.
- `createFamilyInvite` — admin allowed, non-admin denied, family-not-found,
  TTL window, auth required.
- `joinFamilyWithInvite` — used/expired/invalid codes, role+user-doc updates,
  invite marked used, concurrent-join atomicity.
- `createTvPairing` / `claimTvPairing` — TTL, owner-only claim, one-to-one
  invariant (family↔TV both directions), expired pairing cleanup.
- `onNewTimeRequest` — multicast fanout, legacy `fcmTokens` fallback, no-token
  no-op, out-of-range minute rejection (with request marked `denied`).

### Firestore rules (`functions/test/rules/firestore.rules.test.ts`)
- `/users/{uid}` owner-only access.
- `/families/{id}` create/read/update/delete role matrix; ownerUid immutability;
  devices array writable only by owner; owner-cannot-be-demoted.
- `/families/{id}/usage`, `/limits`, `/codes`, `/requests`, `/settings` access
  matrix including the TV's narrow "clear lockout only" exception.
- `/invites` / `/pairings` fully server-managed (client read/write denied).
- `/devices/{id}` one-to-one binding, no client create, no family-pointer change,
  owner-only delete, no listing.

### Android (`shared` / `tv` / `mobile` src/test)
- `LockoutSettings` constants (canary that protects the parity with
  `functions/src/index.ts`).
- `BonusStore` — Room-backed extension, stacking, clear.
- `CodeRedeemer` — server success / wrong / locked / no-family branches;
  `clearExpiredLockout` mode + state guards.
- `RequestController` — submit, pending → approved bonus-before-status ordering,
  denied flow.
- `DailyResetWorker.runIfOverdue` — fresh stamp, idempotent re-run, overdue
  catch-up, corrupt-stamp tolerance.
- `CodesViewModel` — generating, success, error, no-family-id, dismiss.
- `RequestsViewModel` — approve/deny/override; no-family no-op.
- `TimeRequest` grant-window math.

### Compose UI (instrumented; release-tag CI)
Existing tests in `mobile/src/androidTest`:
`CodesScreenTest`, `LimitsScreenTest`, `ComponentsTest`, `ResponsiveLayoutTest`,
`SproutThemeTest`. Extend the same pattern for new screens.

## Coverage threshold

The PR gate runs `./gradlew koverVerify` which fails when aggregated line
coverage drops below **80%**. Cloud Functions Jest config enforces the same
80% line / 75% branch threshold per `functions/jest.config.js`.

To inspect what's not covered:

```powershell
./gradlew koverHtmlReport
start build/reports/kover/html/index.html
```

## Troubleshooting

- **Emulator already running** — kill leftover `firebase emulators:exec` with
  `taskkill /im node.exe /f` (PowerShell) and re-run.
- **Native sqlcipher in unit tests** — the Robolectric tests build Room
  in-memory without SQLCipher; production code path is exercised by
  `androidTest` only.
- **`google-services.json` missing** — CI provides a stub
  (`.github/workflows/google-services.stub.json`). Locally, drop the real file
  into `mobile/` and `tv/`.
- **`koverVerify` fails locally but passes in CI** — make sure you ran
  `testDebugUnitTest` first; verify only reads the IC dumps from prior runs.

## Adding a new test

1. Pick the layer (JVM unit, Compose UI, Functions Jest, rules).
2. Mirror an existing file as a starting point (same directory conventions).
3. Run that single layer locally before pushing.
4. If you add a new public branch to Cloud Functions / rules / a ViewModel,
   add a test in the same PR — the coverage gate will refuse otherwise.
