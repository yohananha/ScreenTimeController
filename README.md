# Screen Time Controller

An Android TV parental control system built with two apps that talk to each other through Firebase:

- **TV app** — runs on the family's Android TV, tracks per-app usage, and blocks apps the moment their daily limit is hit.
- **Mobile app** — runs on the parent's phone; sets limits, approves time requests, generates one-time unlock codes, and manages the family.

---

## How it works

### First-time setup

1. **Parent A** signs up in the mobile app → taps **Create a new family**. They become the family owner (the only account that can pair or unpair the TV).
2. **Pair the TV**: launch the TV app and grant the three permissions it needs (Usage access, Draw over other apps, Accessibility service). The TV shows a **6-digit pairing code**. On mobile, open the **Family** tab → *Pair a TV* → enter the code.
3. **Invite a co-parent**: in the **Family** tab → *Generate code*. The second parent signs up on their phone, picks *Join with an invite code*, and enters the 6-digit code. They join as a Member by default; the owner can promote them to Admin from the same tab.

### Daily flow

| What happens | Where |
|---|---|
| Set a YouTube limit of 60 min/day | Mobile → Limits tab → + |
| TV blocks YouTube once 60 min is reached | TV — automatically |
| Child taps "Request more time" on the block screen | TV |
| Parent gets a push notification | Mobile |
| Parent taps Approve (or Deny) | Mobile → Requests tab |
| TV unblocks and adds the approved bonus time | TV — automatically |
| Parent generates a 4-digit one-time code | Mobile → Codes tab |
| Child types the code on the TV block screen | TV |
| TV unblocks for the extra minutes in the code | TV — automatically |

Limits and approval decisions sync in real time (< 2 seconds) via Firestore.

### Role system

| Role | Who | Can do |
|---|---|---|
| Owner | Family creator (first parent) | Everything — including pairing/unpairing the TV and deleting the family |
| Admin | Promoted by the owner | Set limits, approve requests, invite members, promote/remove other members |
| Member | Anyone who joins with an invite | View limits and approve requests |

The TV is bound one-to-one to the family; it cannot be paired with a second family, and a family cannot claim two TVs at the same time.

---

## Repository layout

```
ScreenTimeController/
├── shared/          # Android library: Firestore, Room, models — no UI
├── mobile/          # Parent phone app (Jetpack Compose + Material 3)
├── tv/              # Android TV app (Jetpack Compose for TV)
├── functions/       # Firebase Cloud Functions (TypeScript/Node 20)
├── firestore.rules  # Firestore security rules
└── firebase.json    # Firebase project config
```

Both `:mobile` and `:tv` depend on `:shared`. All Firestore reads/writes go through `FirestoreRepository` in `:shared`. Privilege-escalating operations (invite redemption, TV pairing, code redemption) are callable Cloud Functions so the Firestore rules can stay fully locked.

---

## Setting up from source

### Prerequisites

- Android Studio Giraffe or newer
- Node 22+ and `npm` (for Cloud Functions)
- A Firebase project on the **Blaze** (pay-as-you-go) plan

### Firebase project

1. In the [Firebase Console](https://console.firebase.google.com), create a project.
2. Register two Android apps:
   - Package `com.screentime.mobile` → download `google-services.json` → place at `mobile/google-services.json`
   - Package `com.screentime.tv` → download `google-services.json` → place at `tv/google-services.json`

   > Both files are in `.gitignore`. The build will fail without them.

3. Enable **Authentication** → Email/Password + Google sign-in.
4. Enable **Cloud Firestore** in production mode.
5. Enable **Cloud Messaging** (no extra console config needed).
6. Deploy rules and functions (see below).

### Deploy rules and Cloud Functions

```bash
npm install -g firebase-tools
firebase login
firebase use <your-project-id>

# from repo root
cd functions && npm install && npm run build && cd ..
firebase deploy --only firestore:rules,functions
```

### Run the apps

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
| `createFamilyInvite` | Mobile (admin) | Generates a 6-digit, 48-hour invite code |
| `joinFamilyWithInvite` | Mobile (new user) | Validates invite and adds the caller as a Member |
| `createTvPairing` | TV | Generates a 6-digit, 10-minute pairing code |
| `claimTvPairing` | Mobile (owner only) | Validates pairing code, binds TV to family (one-to-one) |
| `redeemCode` | TV | Validates a 4-digit unlock code server-side; enforces lockout after 5 wrong attempts in 60 seconds |

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.x |
| UI (mobile) | Jetpack Compose + Material 3 |
| UI (TV) | Jetpack Compose for TV (`androidx.tv`) |
| DI | Hilt |
| Local cache | Room |
| Async | Kotlin Coroutines + Flow |
| Backend | Firebase (Firestore, Auth, FCM, Cloud Functions) |
| Crash reporting | Firebase Crashlytics |
| Cloud Functions runtime | Node 20 / TypeScript |
