# Screen Time Controller

A two-app Android system for managing screen time on an Android TV from a
parent's phone. Plan and design notes live in
`~/.claude/plans/screen-time-controller-fluffy-dragon.md`.

## Status

Phases 1–8 complete plus the daily-reset slice of Phase 9. The system is
end-to-end functional once Firebase credentials are dropped in.

- **Phase 1 — TV usage tracking**: `UsageStatsManager` event sampling +
  Room cache.
- **Phase 2 — Enforcement**: Accessibility Service + `WindowManager` block
  overlay.
- **Phase 3 — Mobile limits UI**: Compose nav shell, LimitsScreen with
  add/edit/remove.
- **Phase 4 — Firebase sync**: `FirestoreRepository` is the source of
  truth for `/families/{id}/limits` and `/families/{id}/usage/{date}`.
- **Phase 5 — One-time codes**: Mobile generates, TV redeems via the block
  overlay's number pad; `BonusStore` extends today's allowance.
- **Phase 6 — Time requests + FCM**: Child taps "Request more time" → TV
  writes a request → Cloud Function fans out an FCM push → parent
  approves/denies → TV unblocks via `BonusStore`.
- **Phase 7 — Auth + family**: Firebase Auth (email/password), family
  creation, 6-digit invite codes for the second parent.
- **Phase 8 — TV pairing**: TV signs in anonymously, displays a pairing
  code, parent claims it from the mobile Family tab.
- **Phase 9 (partial) — Daily reset**: `DailyResetWorker` clears the bonus
  store at midnight. Remaining polish items (history charts, audit log,
  Compose snapshot tests, Crashlytics, offline indicator) are open.

**The build requires `google-services.json` in each app module.** See the
Firebase setup section below.

## Repo layout

```
ScreenTimeController/
├── settings.gradle.kts            # :shared, :mobile, :tv
├── build.gradle.kts
├── gradle/libs.versions.toml      # version catalog
├── shared/                        # Android library — models, Room, Firestore wrapper
├── mobile/                        # phone app (Phase 3+)
├── tv/                            # Android TV app (Phase 1+)
└── functions/                     # Cloud Function (Phase 6) — not yet created
```

## Firebase setup checklist

Do this before Phase 4 (the Firebase sync layer) is wired up.

1. Create Firebase project named `screen-time-controller` in the Firebase
   console.
2. Register two Android apps under the same project:
   - Mobile: `com.screentime.mobile`
   - TV:     `com.screentime.tv`
   Download each `google-services.json` and drop them at:
   - `mobile/google-services.json`
   - `tv/google-services.json`

   **The Gradle build will fail without these files** — the
   `google-services` plugin requires them. Both files are in `.gitignore`
   so they will not be committed.
3. Enable Authentication → Sign-in methods → Email/Password + Google.
4. Enable Cloud Firestore in production mode. Pick the nearest region.
5. Apply the starter security rules from the plan doc.
6. Enable Cloud Messaging (no console config needed).
7. Upgrade to the Blaze plan and enable Cloud Functions (needed for the
   FCM-on-request trigger in Phase 6).
8. Install Firebase CLI: `npm i -g firebase-tools`, then `firebase login`.

## Running Phase 1 on an Android TV emulator

1. Open the project in Android Studio (Giraffe or newer).
2. Create an AVD: **Tools → Device Manager → Create Device → TV → 1080p
   Android TV**. Use an API 34 image.
3. Select the `:tv` run configuration and Run.
4. On the device, open **Settings → Device Preferences → Apps → Special app
   access → Usage access**, and grant **Screen Time TV**. (On the
   emulator's first launch, the in-app button will deep-link here.)
5. Open YouTube (preinstalled on the TV image) and leave it foregrounded.
6. Watch Logcat for `UsageWorker` log lines:
   `D/UsageWorker: Usage sample 2026-06-14: {com.google.android.youtube.tv=60000}`.
7. The first sample may take up to 15 minutes — that is WorkManager's minimum
   periodic interval. For quicker dev feedback, enqueue a one-shot job from
   Logcat or temporarily call `UsageWorker.schedule` with a shorter interval
   in a dev build.

## End-to-end smoke test

Run a Pixel + Android TV emulator side-by-side:

1. **Sign up on mobile**: create an account → Welcome screen → "Create a
   new family". You land in the Limits tab.
2. **Pair the TV**: launch TV app, grant the three permissions
   (usage / overlay / accessibility). The TV shows a 6-digit pairing code.
   On mobile, Family tab → Pair a TV → enter the code. TV transitions to
   "operational" state.
3. **Set a tight limit**: Limits tab on mobile → +, pick YouTube, slide
   to 2 minutes, Save.
4. **Use YouTube on TV**: open YouTube and let it run. Within ~15 min
   (one `UsageWorker` cycle) the overlay will appear once accumulated
   foreground time crosses 2 minutes.
5. **Try a code**: mobile Codes tab → Generate (30 min). Read the 6-digit
   code, type it on the TV's block overlay → unblocked.
6. **Try a request**: when blocked again, tap "Request more time" → pick
   15 min. Mobile gets an FCM push → tap to open → approve. TV unblocks.
7. **Invite the other parent**: Family tab → Generate code. On a second
   device, sign up and choose "Join with an invite code".

For faster iteration during development, lower `UsageWorker.schedule`
interval in a debug build.

## Cloud Function deploy

After Firebase is set up:

```bash
cd functions
npm install
firebase use <your-firebase-project-id>
npm run deploy
```

The function listens for new `/families/{id}/requests/{requestId}` docs
and pushes to admin FCM tokens.

## Open Phase 9 polish items

- Usage history bar chart (mobile).
- Audit log (`/families/{id}/auditLog`) of every code redemption and
  request approval.
- Offline indicator on TV when the Firestore listener errors.
- Compose snapshot tests for the LimitsScreen and BlockOverlay.
- Firebase emulator integration tests for `FirestoreRepository`.
- Crashlytics on both apps.
- Accessibility-service watchdog (re-prompt user when the OS disables it).

See `~/.claude/plans/screen-time-controller-fluffy-dragon.md` for full
context.
