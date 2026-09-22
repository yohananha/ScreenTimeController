# ScreenTime redesign — handoff (“Peach & plum”, direction I6c)

This folder is the source of truth for the 2026 UI/UX redesign of ScreenTime. It **supersedes** `Direction #3 sprout design system (4)/` entirely: Sprout's cream/plum/coral palette, Fredoka + Nunito Sans, cards-with-badges layout and the 4-tab navigation are all retired.

The live, editable canvas is a Claude Design artifact (“ScreenTime Redesign”, page **I6c · Final**). The files here are a static export of that page.

```
design-handoff-i6c/
├── README.md              ← this file: what changed, tokens, every screen, RTL rules, implementation order
├── CLAUDE_CODE_PROMPT.md  ← paste-ready brief for the implementation session
├── tokens.json            ← machine-readable colours, type, spacing, radii, shadows, motion, RTL rules
└── screens/               ← 35 standalone HTML references (open in any browser, no build needed)
    ├── Today.html · Today-HE.html · Rules(-HE) · Unlock(-HE) · Unlock-Ready(-HE) · Family(-HE)      phone 390×844
    ├── TV-Permission … TV-Locked (+ -HE)   10 states × 2 languages                                 TV 1920×1080
    ├── Web-Today(-HE) · Web-Rules(-HE)                                                            desktop 1280×860
    └── Tokens.html                        visual token + component sheet
```

The HTML references are **pixel-precise design specs, not production code**. Recreate them in the target stacks (Jetpack Compose for TV / `androidx.tv`, React + Vite for web, Compose + Material 3 for the legacy mobile app only if it is still being shipped) using each codebase's own patterns. Copy exact values — paddings, radii, sizes, weights — from the HTML; do not round to a framework grid.

---

## 1. What changed and why

**Problem:** the old Limits home carried everything at once — a mode switcher, a hero card, allowed hours, code lockout, an app list, a request banner and a FAB — and every element was a card with a badge. It read as busy, and the Hebrew version used a serif face that did not match the Latin one.

**UX changes (apply to web and mobile alike):**

| Before | After |
|---|---|
| 4 tabs: Limits · Requests · Codes · Settings | **3 tabs: Today · Rules · Family** + one **Unlock** action (round peach button at the end of the nav pill / foot of the web rail) |
| Limits home = everything | **Today** = 5 blocks only: date + avatar → time gauge + live TV pill → Lock/Limits/Allow tiles → pending request strip (only when one exists) → usage by app |
| Allowed hours, overall limit, code lockout, app limits spread across Limits | All of them live on **Rules** (two white cards: general rules, app limits) |
| Requests tab | Pending requests appear **inline on Today** (and in the right column on web). A Requests tab is not needed; approved/denied history can live under Rules → History later if wanted |
| Codes tab | **Unlock** bottom sheet (choose → code ready), opened from the nav |
| Settings tab | **Family**: parents, TVs, language, notifications |
| Status badges everywhere | Status carried by **colour on the bar/label only**: ink = normal, `ok` green = live/on track, `over` red = time's up. No pill badges on rows |

Tone on the TV stays supportive: green “done” circle, never a red X; “That's enough for today.” not “Blocked”.

---

## 2. Design system

Full values are in `tokens.json`; the visual sheet is `screens/Tokens.html`.

### Typeface
**Rubik** 400 / 500 / 600 for everything, Latin and Hebrew, phone / TV / web. One family, no display face, no serif. Google Fonts URL is in `tokens.json`; bundle the same weights in the Android apps.

### Colour roles

| Role | Hex | Use |
|---|---|---|
| ground | `#FFF6EE` | app background (phone + web) |
| ink | `#2A1E2E` | text, **primary buttons, active tiles, request strip, nav pill, web rail** |
| muted | `#6E5F66` | secondary text (4.5:1 on ground) |
| hairline | `#F0E3D8` | dividers, gauge track, tile borders |
| tint | `#FFEDE0` | soft fills (avatars, icon tiles) |
| surface | `#FFFFFF` | cards, mode tiles, chips |
| peach | `#FFB088` | **accent**: gauge gradient start, Unlock button fill, active-tile icon, TV focus ring, app-icon swatch |
| rose | `#F79AC0` | gauge gradient end, background glows only |
| ok | `#12A87A` | live dot (pulses), on-track bars, approved/unlocked badge on TV |
| over | `#B3261E` | “Time's up” label + bar. The only red in the system |
| navInactive | `#B9A9B5` | inactive labels on ink surfaces |
| tv.ground | `#1E1622` | TV background |
| tv.ink | `#FFF6EE` | TV text, TV primary button fill |
| tv.muted | `#B9A9B5` | TV secondary text, footer |

Rule of thumb: **ink does the work, peach/rose decorate.** Buttons the user presses are ink (white text) or peach (ink text); never white text on peach.

### Backgrounds (“alive” layer)
Two soft radial glows sit behind the top of every phone and web screen: peach `rgba(255,176,136,.24)` top-start, rose `rgba(247,154,192,.20)` top-end (~520–560 px circles, half off-canvas). TV uses the same at ~1200 px and lower alpha (`.16` / `.14`). They are decorative: draw them with `Brush.radialGradient` / CSS `radial-gradient`, never as images.

### Spacing, radius, shadow
- Spacing scale `4 8 12 16 22 26 36`; phone gutter 22, card padding 24, phone top inset 56 (below the real status bar — never draw a fake one).
- Radius: dot 3 · icon tile 12 · tile 14 · input 18 · request strip 20 · card 22 · sheet 28 · pill 999.
- Shadows: card `0 1px 2px rgba(42,30,46,.04), 0 10px 30px rgba(42,30,46,.06)`; nav pill `0 14px 32px rgba(42,30,46,.25)`; sheet `0 -12px 40px rgba(42,30,46,.22)`.

### Motion
- **Live dot**: 2 s expanding-ring pulse (`box-shadow 0→8px`, ease-out, infinite). Used on the TV-online pill and the TV card.
- **Waiting / pairing pulse**: scale 1→1.12, opacity .7→1, 1.8 s ease-in-out.
- **TV focus**: 6 px ground-coloured gap + 6 px peach ring + scale 1.04, 150 ms.
- **Gauge**: animate the arc fill on first draw (~600 ms ease-out).

---

## 3. Components (phone + web)

| Component | Spec | Reference |
|---|---|---|
| **Time gauge** | 300×170 half-arc, r 120, stroke 16, round caps; track = hairline, fill = linear gradient peach→rose. Inside: `h:mm` (56/600, tracking −0.03em) and **one word ≤6 chars** (16/500 muted). Any longer text goes in a 13/500 caption **under** the SVG. In RTL the fill is mirrored (`scale(-1,1)` about the arc centre); digits are not. | Today, Web-Today |
| **Live TV pill** | 36 h, white, pill radius, pill shadow, 8 px `ok` dot with `.live` pulse, 13/500 | Today |
| **Mode tiles** (Lock · Limits · Allow) | 3-column grid, gap 10, 84 h, radius 22, padding 14, icon top-start (22 px, muted) + label bottom-start (14/600). Active: ink fill, white text, icon stroke peach. Inactive: white, 1.5 px hairline border | Today |
| **Request strip** | ink fill, radius 20, padding 12/12/12/16; title 15/600 white, sub 12/500 white@78%; two 44 px round buttons: ✓ white fill/ink icon (approve at the requested amount), ✕ transparent, 1.5 px white@50% border | Today, Web-Today |
| **Usage row** | 48 h, hairline top (last also bottom); 10 px rounded square swatch + name 14/600; right side: 72×4 bar + value 13/500 muted, or “Time's up” 13/600 `over` when at 100 % | Today, Web-Today |
| **Settings row** | min-h 60, label 16/600 + value 13/500 muted, chevron (mirrored in RTL) | Rules, Family |
| **App row** | 52 h, swatch + name 15/600, value 14/500 + chevron | Rules |
| **White card** | radius 22, card shadow, rows inside with hairlines, 16 px side padding | Rules, Family |
| **Chip** | 44 h, padding 0 18, pill; selected = ink fill white text, else white + hairline border | Unlock |
| **Primary / secondary button** | 46–52 h pill; primary ink/white, secondary transparent + 1.5 px ink border | Unlock, Family |
| **Bottom nav pill** | margin 0 22 24, padding 6, pill, ink fill, nav shadow; 3 items 46 h (active = white@12% fill), + 46 px round **peach Unlock button** (ink icon) | all phone |
| **Bottom sheet** | radius 28 top, ground fill, 40×4 hairline handle, sheet shadow, 34 px bottom inset; scrim ink@35% and the page behind at 40 % opacity | Unlock, Unlock-Ready |
| **Code tiles** | 84 h, radius 18, white, hairline border, 40/600; always LTR | Unlock-Ready |
| **TV card (Family)** | ink fill, radius 22; TV icon in white@12% tile; status line in `#9FE9CE` with live dot; Rename / Unpair ghost pills (Unpair text `#FFD3CD`) | Family |
| **Web rail** | 96 w, ink, logo square (peach, 40, r 14), 3 items 76×68 r 18 (active white@12%), Unlock button 56 px round peach at the foot. Sits on the **right** in RTL | Web-* |

Icons: 2 px stroke, round caps (the inline SVGs in the HTML). Never emoji. On Android use Material Symbols Outlined at weight 400 where a matching glyph exists; keep the custom lock/unlock/tv/sun/clock shapes as vector drawables otherwise.

---

## 4. Screens

### Phone (390×844; English row and Hebrew row in `screens/`)

1. **Today** — date (14/500 muted) + 36 px ink avatar → gauge + live pill → mode tiles → request strip (conditional) → “By app” header with a *Rules* link → 3 usage rows → nav pill (Today active).
   States to implement beyond the reference: no TV paired (gauge track only, pill says “No TV paired”, tiles disabled), TV off/offline (grey dot, no pulse), Lock active (Lock tile ink, gauge dimmed), Allow active (Allow tile ink, caption “All limits paused until midnight”), no limits set (gauge shows `—`, caption “No daily limit”), over the daily total (arc full, `over` colour, word “over”).
2. **Rules** — title 30/600 + subtitle → white card: Overall daily limit · Allowed hours · Code lockout (settings rows) → “App limits” header + *+ Add* → white card: app rows (YouTube, Netflix, Minecraft, Disney+ “Always allowed”) → nav (Rules active). Tapping a row opens the existing edit dialogs / TimeFrame screen restyled with these tokens.
3. **Unlock — choose** — sheet 420 h over Today: title 24/600 + sub → “How much time” chips 15 min / **30 min** / 1 hour / Rest of day → “Applies to” chips **Everything** / YouTube only → Generate (52 h ink pill).
4. **Unlock — code ready** — sheet 372 h: title + “Read it out, or show the phone.” → 4 code tiles (LTR) → scope 14/600 ink · expiry with clock icon → Copy (ink) + New code (secondary).
5. **Family** — “Parents” card: You (peach avatar, “Owner · you”), second parent (tint avatar, chevron), dashed-circle “Invite a parent” row → “TVs” ink card + “+ Pair another TV” text button → white card: Language · Notifications → nav (Family active).

### TV (1920×1080, D-pad only; 10 states × EN/HE)

Shared: `tv.ground` + two glows; “ScreenTime” 28/600 muted top-left at 96/56; centred column max 1280 wide, gap 40; footer 26/500 muted at 64 px from the bottom — left: context (step, reset time), right: remote hint. **Primary** button = `tv.ink` fill, `tv.ground` text, 92 h, padding 0 56, 32/600. **Ghost** = white@8% fill, 3 px white@16% border, 32/500. Focused element gets the peach ring + scale 1.04; every screen shows focus on its default button. Minimum text size on TV: 24 px. Headline 120/600 (Block 140, Ask 132, Keypad 88); **in Hebrew never above 120**.

| State | Headline | Elements | Default focus |
|---|---|---|---|
| Permission | One quick setup | eye badge (peach@16% circle, peach icon) · body · 3 step dots (peach active 44×12) · [Open Android settings] [Why is this needed?] · footer “Step 1 of 3” | primary |
| Pairing | Pair with a parent's phone | body · 6 cream code tiles 118×148 r 24 (LTR) · pulsing green dot + “Waiting for the phone…” (`#9FE9CE`) · [Get a new code] | ghost |
| Block (Time's up) | That's enough **for today.** (second half in peach) | label “YouTube · 1 hour today” · body · [Ask a parent] [Enter a code] · footer “Resets at midnight · in 3h 12m” | primary |
| Ask | How much more? | body · [15 more minutes] [30 more minutes] [Maybe later] | 15 |
| Waiting | Asked your parent! | pulsing rose circle with phone icon · body · [Enter a code instead] [Cancel] | ghost 1 |
| Approved | You got 15 more! | `ok` circle with check (icon `#062A1E`) · body · [Keep watching] | primary |
| Denied | Not right now | white@10% circle with gentle frown · body · [Enter a code] [Okay] | Okay |
| Keypad | Enter the unlock code | two columns, gap 120: left = headline 88 + body + 4 slots 120×150 (filled cream / active peach 4 px border + glow / empty white@8%) + error chip (red@22% fill, `#D9534F` border, `#FFD3CD` text); right = 3×4 keys 150×150 r 26, gap 22, C and ⌫ in peach; focused key = cream fill + ring | key “3” (any) |
| Unlocked | Code worked! +30 min | `ok` circle with open padlock · body · [Keep watching] | primary |
| Locked out | Let's take a short break | peach@18% circle with pause icon · body · countdown 140/600 peach `4:59` · [Ask a parent instead] | ghost |

Keypad and code tiles are always LTR, even on the Hebrew screens.

### Web (1280 wide; scales down to the phone layout below 840)

- **Today**: rail (Today active) → main padding 36/40: title 34/600 + date, “Unlock code” secondary pill top-right → 2-column grid gap 22. Left: status card (gauge, live pill, mode tiles) + TV card. Right: “Requests” card (request strip) + “Usage by app” card (rows + Rules link).
- **Rules**: rail (Rules active) → title + TV name → 2 columns: general rules card · app limits card (+ Add).
- Family on web = the phone Family screen content in one column at 640 max width (not drawn; follow the phone spec).

---

## 5. Hebrew / RTL — non-negotiable rules

1. `dir="rtl"` / `LayoutDirection.Rtl` on the root; flex/grid, padding-start/end and chevrons mirror automatically. The web rail moves to the right.
2. **Mirror the gauge fill** so it starts from the right: `transform="translate(300 0) scale(-1 1)"` on the fill path (SVG) or draw the arc sweep negative in Compose.
3. **Never mirror**: digits, `h:mm` times, unlock/pairing code tiles, the TV keypad, app names. Wrap those containers in `dir="ltr"` / `LayoutDirection.Ltr`.
4. Rubik covers Hebrew — keep the same family and weights. Do not switch to Heebo/Assistant/Frank Ruhl.
5. The gauge shows only `h:mm` + one short word; long Hebrew phrases (“שעה ו־25 דק׳ נשארו”) go in the caption **under** the arc. This is what fixed the overflow — keep it.
6. Strings come from `web/src/i18n/locales/he.json` where they exist; new strings used in the design (add them to both locales):
   `היום`, `כללים`, `משפחה`, `קוד פתיחה`, `נשארו`, `מתוך {{limit}} · מתאפס ב־00:00`, `מגבלות` (mode), `הכול מותר`, `לאשר {{m}} דקות`, `לדחות`, `לפי אפליקציה`, `לוח זמנים פעיל · {{hours}}`, `יצירת קוד`, `העתקה`, `קוד חדש`, and the TV set listed in `screens/TV-*-HE.html` `<title>`/body text.
7. Mode labels must stay on one line at 390 px: EN Lock / Limits / Allow; HE נעילה / מגבלות / הכול מותר. Do not use “מגבלות פעילות”.

---

## 6. Mapping to the codebase

| Area | Files to change |
|---|---|
| Web tokens | `web/src/theme/colors.ts`, `typography.ts`, `spacing.ts`, `radius.ts`, `index.css` (font import, glows, keyframes) — generate from `tokens.json` |
| Web nav | `web/src/components/BottomNavBar.tsx` → 3 items + Unlock button; replace emoji icons with inline SVG; add a `SideRail` for ≥ 840 px |
| Web screens | `screens/limits/LimitsScreen.tsx` → `TodayScreen` (gauge, mode tiles, request strip, usage rows); `LimitsScreen` content that is rules-only + `TimeFrameScreen` entry + lockout → `RulesScreen`; `codes/CodesScreen.tsx` → `UnlockSheet` (two states); `settings/SettingsScreen.tsx` → `FamilyScreen`; `requests/RequestsScreen.tsx` folds into Today (keep the hook `useRequests`) |
| Web components | `HeroCard` → `TimeGauge`; `AppLimitRow` → `UsageRow` / `AppRow`; `StatusBadge` removed from rows (keep for TV card status text); `Chips` restyled; `CodeSlotInput` / `CodeTile` restyled; new `ModeTiles`, `RequestStrip`, `LivePill`, `NavPill` |
| TV tokens | `tv/.../ui/theme/SproutTokens.kt` + `SproutTypography.kt` + `Theme.kt` → rename to `PeachPlum*` (or keep names, swap values); Rubik in `res/font` |
| TV screens | `ui/BlockOverlayContent.kt` (all block/ask/waiting/approved/denied/keypad/unlocked/locked scenes), `ui/pairing/PairingScreen.kt`, permission setup in `MainActivity`/`PermissionState`; components `TvButtons.kt` (primary/ghost + focus ring), `TvCodeSlot.kt`, `TvKeypad.kt`, `TvSurfaces.kt` (glow background), `IconBadge.kt` |
| Mobile (legacy) | only if still shipped: mirror the web mapping onto `mobile/.../ui/theme/*` and screens; otherwise leave untouched |
| Strings | `web/src/i18n/locales/en.json` + `he.json`; TV `strings.xml` (+ `values-iw`/`values-he`) |

Nothing in `shared/`, `functions/`, Firestore rules or the data models changes. The redesign is UI only; every hook/ViewModel keeps its contract.

---

## 7. Suggested implementation order

1. Tokens + font (web theme files, TV theme files) — nothing visible changes yet.
2. Web: `TimeGauge`, `ModeTiles`, `RequestStrip`, `UsageRow`, `NavPill` components + `TodayScreen`; wire to existing hooks. Verify EN and HE side by side against `screens/Today*.html`.
3. Web: `RulesScreen`, `UnlockSheet`, `FamilyScreen`; remove the Requests and Codes routes; add the side rail breakpoint.
4. TV: theme + `TvButtons` focus ring + glow background; then Block → Ask → Waiting → Approved/Denied → Keypad → Unlocked/Locked → Pairing → Permission, matching `screens/TV-*.html`.
5. Hebrew pass on everything: RTL mirroring, the never-mirror list, one-line mode labels, headline ≤ 120 px on TV.
6. States not drawn (no TV paired, offline, Lock/Allow active, no limits, over total) using the same components.
7. Delete `Direction #3 sprout design system (4)/` and the Sprout token names once nothing references them.
