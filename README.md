# Screen Time Controller

A parental control system built from two clients that talk to each other through Firebase:

- **TV app** — runs on the family's Android TV, tracks per-app usage, and blocks apps the moment their daily limit is hit.
- **Web app** (`web/`) — runs in any browser (including iOS/desktop); parents set limits, approve time requests, generate one-time unlock codes, and manage the family. Replaces the original Android-only mobile app so parents aren't tied to a particular phone platform.
- **Mobile app** (`mobile/`, being phased out) — the original Android parent app. Kept alongside the web app until the web app reaches full feature parity; see [Repository layout](#repository-layout).

---

## Beta download

> Debug builds — enable **Install unknown apps** on your device before installing the TV APK.

| App | Download |
|---|---|
| Web (parent, any device) | **[screen-time-controller-a653e.web.app](https://screen-time-controller-a653e.web.app)** — sign in with Google |
| Mobile (parent phone, legacy) | [ScreenTime-Mobile-debug.apk](https://github.com/yohananha/ScreenTimeController/releases/download/v0.1.1-beta/mobile-debug.apk) |
| TV | [ScreenTime-TV-debug.apk](https://github.com/yohananha/ScreenTimeController/releases/download/v0.1.1-beta/tv-debug.apk) |

All releases: [github.com/yohananha/ScreenTimeController/releases](https://github.com/yohananha/ScreenTimeController/releases)

---

## How it works

### First-time setup

1. **Parent A** signs in at the web app (or the legacy mobile app) → taps **Create a new family**. They become the family owner (the only account that can pair or unpair the TV).
2. **Pair the TV**: launch the TV app and grant the three permissions it needs (Usage access, Draw over other apps, Accessibility service). The TV shows a **6-digit pairing code**. On the web app, open the **Family** tab → *Pair a TV* → enter the code.
3. **Invite a co-parent**: in the **Family** tab → *Invite a parent*. The second parent signs in, picks *Join a family*, and enters the 6-digit code. They join as a co-parent by default; the owner can promote them to admin from the same tab.

### Daily flow

| What happens | Where |
|---|---|
| Set a YouTube limit of 60 min/day | Web app → Limits tab → + |
| TV blocks YouTube once 60 min is reached | TV — automatically |
| Child taps "Request more time" on the block screen | TV |
| Parent gets a push notification | Web app |
| Parent taps Approve (or Deny) | Web app → Requests tab |
| TV unblocks and adds the approved bonus time | TV — automatically |
| Parent generates a 6-digit one-time code | Web app → Codes tab |
| Child types the code on the TV block screen | TV |
| TV unblocks for the extra minutes in the code | TV — automatically |

Limits and approval decisions sync in real time (< 2 seconds) via Firestore.

### Role system

| Role | Who | Can do |
|---|---|---|
| Owner | Family creator (first parent) | Everything — including pairing/unpairing the TV and deleting the family |
| Admin | Promoted by the owner | Set limits, approve requests, invite members, promote/remove other members |
| Member | Anyone who joins with an invite | View limits and approve requests |

Each TV can only belong to one family at a time, but a family can pair **multiple TVs**. Only the family owner can pair or unpair a TV.

---

## Repository layout

```
ScreenTimeController/
├── shared/          # Android library: Firestore, Room, models — no UI (used by tv/)
├── web/             # Parent web app (React + Vite + TypeScript) — replaces mobile/
├── mobile/          # Parent phone app (Jetpack Compose + Material 3) — legacy, being phased out
├── tv/              # Android TV app (Jetpack Compose for TV)
├── functions/       # Firebase Cloud Functions (TypeScript/Node 20)
├── firestore.rules  # Firestore security rules
└── firebase.json    # Firebase project config (Firestore, Functions, Hosting)
```

`:tv` and (for now) `:mobile` depend on `:shared`; `web/` is a standalone npm project, not a Gradle module. Both `web/` and `mobile/` talk to the exact same Firestore collections and Cloud Functions — `web/src/firebase/firestoreRepository.ts` is a line-for-line TypeScript port of `shared/src/main/java/com/screentime/shared/firestore/FirestoreRepository.kt`, so a normal signed-in Firebase Auth user gets identical read/write access regardless of which client they use. Privilege-escalating operations (invite redemption, TV pairing, code redemption) are callable Cloud Functions so the Firestore rules can stay fully locked.

`mobile/` will be removed once `web/` has been verified at full feature parity end-to-end against a real Firebase project (see `TESTING.md`); `tv/`, `shared/`, `functions/`, and `firestore.rules` are unaffected by that migration either way.

---

## Setting up from source

### Prerequisites

- Android Studio Giraffe or newer (for `mobile/` and `tv/`)
- Node 20+ and `npm` (for `web/` and Cloud Functions)
- A Firebase project on the **Blaze** (pay-as-you-go) plan

### Firebase project

1. In the [Firebase Console](https://console.firebase.google.com), create a project.
2. Register a **Web app** → copy the `firebaseConfig` values into `web/.env.local` (copy `web/.env.example` first — see [Web app](#web-app) below).
3. Register two Android apps:
   - Package `com.screentime.mobile` → download `google-services.json` → place at `mobile/google-services.json`
   - Package `com.screentime.tv` → download `google-services.json` → place at `tv/google-services.json`

   > All of `google-services.json` and `web/.env.local` are in `.gitignore`. The respective builds fail without them.

4. Enable **Authentication** → Google sign-in.
5. Enable **Cloud Firestore** in production mode.
6. Enable **Cloud Messaging** → generate a **Web Push certificate** (VAPID key) for `web/.env.local`'s `VITE_FIREBASE_VAPID_KEY`.
7. Deploy rules, functions, and hosting (see below).

### Deploy rules, Cloud Functions, and Hosting

```bash
npm install -g firebase-tools
firebase login
firebase use <your-project-id>

# from repo root
cd functions && npm install && npm run build && cd ..
cd web && npm install && npm run build && cd ..
firebase deploy --only firestore:rules,functions,hosting
```

### Web app

```bash
cd web
npm install
cp .env.example .env.local   # fill in from Firebase Console -> Project Settings -> General -> Your apps -> Web app
npm run dev
```

### Run the Android apps

Open the project in Android Studio. Select the `:mobile` or `:tv` run configuration and press Run. The `:tv` target requires an Android TV emulator or a physical Android TV device.

**TV emulator tip:** create an AVD with *TV → 1080p Android TV*, API 34 image, and set the GPU to **Software** rendering if you encounter display driver crashes.

---

## Permissions the TV app needs

The TV app asks for three permissions on first launch; all three are required for enforcement to work:

| Permission | Why |
|---|---|
| Usage access (`PACKAGE_USAGE_STATS`) | Reads foreground time per app every 30 seconds |
| Draw over other apps (`SYSTEM_ALERT_WINDOW`) | Displays the block overlay on top of everything |
| Accessibility service | Detects when a blocked app comes to the foreground |

---

## Cloud Functions

| Function | Caller | What it does |
|---|---|---|
| `onNewTimeRequest` | Firestore trigger | Sends FCM push to all family members when the TV creates a time request |
| `createFamilyInvite` | Web/mobile (admin) | Generates a 6-digit, 48-hour invite code |
| `joinFamilyWithInvite` | Web/mobile (new user) | Validates invite and adds the caller as a Member |
| `createTvPairing` | TV | Generates a 6-digit, 10-minute pairing code |
| `claimTvPairing` | Web/mobile (owner only) | Validates pairing code, binds TV to family (a family may have many TVs; each TV belongs to one family) |
| `redeemCode` | TV | Validates a 6-digit unlock code server-side; enforces lockout after 5 wrong attempts in 60 seconds |

---

## Tech stack

| Concern | Choice |
|---|---|
| Language (Android) | Kotlin 2.x |
| UI (web, parent) | React 18 + Vite + TypeScript |
| Web testing | Vitest + React Testing Library |
| UI (mobile, legacy) | Jetpack Compose + Material 3 |
| UI (TV) | Jetpack Compose for TV (`androidx.tv`) |
| DI (Android) | Hilt |
| Local cache (TV) | Room |
| Async (Android) | Kotlin Coroutines + Flow |
| Backend | Firebase (Firestore, Auth, FCM, Cloud Functions, Hosting) |
| Crash reporting (mobile, legacy) | Firebase Crashlytics |
| Cloud Functions runtime | Node 20 / TypeScript |
