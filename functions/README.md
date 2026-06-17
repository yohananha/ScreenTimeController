# Cloud Functions

- `onNewTimeRequest` — triggered when the TV writes a new time-request doc;
  fans out an FCM push to every registered family member.
- `createFamilyInvite` / `joinFamilyWithInvite` — server-side invite issue +
  redemption (joiner is added as a `user`, never a client-side write).
- `createTvPairing` / `claimTvPairing` — server-side TV pairing. `claimTvPairing`
  enforces the one-TV-per-family / one-family-per-TV relationship and only lets
  the family **owner** pair a TV.

These callables exist because the Firestore rules deny clients direct access to
`/invites`, `/pairings`, and cross-member writes to `/families/{id}.roles`.

## Local setup

```bash
cd functions
npm install
firebase login
firebase use <your-firebase-project-id>
```

## Deploy

```bash
npm run deploy
```

## Logs

```bash
npm run logs
```

## Token storage

- **Per-member**: `/users/{uid}/fcmTokens`, with members discovered via the
  `/families/{familyId}.roles` map (every admin + user). This is the normal path.
- **Legacy fallback**: `/families/{familyId}.fcmTokens` array, or the old
  `/families/{familyId}.admins` array, if no role-based tokens are found.
