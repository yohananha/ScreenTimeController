# Handoff: ScreenTime — Family Screen-Time App

## Overview

ScreenTime is a two-platform family screen-time system:

- **Phone app** (Android, Material 3) — parents set app limits, approve time requests, generate unlock codes, and manage the family.
- **TV app** (Android TV, 10-foot UI, D-pad only) — kids see usage tracking and a full-screen block when time runs out. Kids ask for more time or type an unlock code on the TV.

One family has one **owner** (main admin) and any number of **co-parents** (users). There is exactly **one TV per family**; only the owner can pair or unpair it. There are no child accounts — requests come from the TV device, not from named kids.

---

## About the Design Files

The files in this bundle are **high-fidelity design references built in HTML** — pixel-precise prototypes with final colors, typography, spacing, and working interactions. They are **not** production code to copy directly. The task is to **recreate these designs in the target codebase** (Jetpack Compose for the phone app; Compose for TV / Leanback for the TV app) using its established patterns and libraries, matching the visual output pixel-for-pixel.

---

## Fidelity

**High-fidelity.** These are pixel-perfect mockups with final colors, typography, spacing, and interactions. Recreate the UI exactly, adapting only what the target framework requires (e.g. ripple feedback, system bars, TV focus system).

---

## Design System — Sprout

### Color Roles

| Role | Hex | Usage |
|------|-----|-------|
| Background | `#FCF6F0` | App background (Cream) |
| Surface | `#FFFFFF` | Cards, sheets |
| Surface sunken | `#FAF5EF` | Row backgrounds, tinted areas |
| Ink | `#3A2A4D` | Primary text (Plum) |
| Ink muted | `#8A7C96` | Secondary text, labels |
| Ink faint | `#B6ABBF` | Placeholder, tertiary |
| Primary | `#FF6B5E` | Buttons, accent fills (Coral) |
| On-primary | `#3A2A4D` | Text on coral buttons — **contrast 4.65:1 AA** |
| Primary pressed | `#F0584B` | Button press state |
| Accent | `#B9A8F0` | Lilac — avatar backgrounds, accent containers |
| Accent container | `#ECE5FB` | Lilac tint backgrounds |
| Positive display | `#4FCFA1` | Progress bars, success rings (Mint) |
| Positive text | `#15795A` | Text that says "On track" |
| Positive container | `#DFF6EC` | "On track" badge background |
| Warning display | `#F2A93B` | "Almost up" progress (Amber) |
| Warning text | `#9A6313` | "Almost up" badge text |
| Warning container | `#FCEFD7` | "Almost up" badge background |
| Over limit display | `#E5483A` | Progress bar at 100%, "Time's up" |
| Over text | `#B5281C` | "Time's up" badge text |
| Over container | `#FBE2DF` | "Time's up" badge background |
| Outline | `#EADFD4` | Borders, dividers |
| Outline strong | `#D9CABD` | Stronger dividers |
| TV background | `#2E2140` | Deep plum for TV screens |
| TV surface | `#52436A` | Cards/tiles on TV |
| TV muted text | `#C9BBD6` | Secondary text on TV |

### Typography

| Token | Font | Size | Weight | Line-height | Usage |
|-------|------|------|--------|-------------|-------|
| Display | Fredoka | 34px (phone) / 72–90px (TV) | 600 | 1.05 | Hero headlines |
| Title | Fredoka | 24px | 600 | 1.15 | Screen titles |
| Headline | Fredoka | 20px | 500 | 1.3 | Card titles, app names |
| Body L | Nunito Sans | 16px | 600 | 1.5 | Main body copy |
| Body | Nunito Sans | 15px | 400/600 | 1.5 | Supporting body |
| Label | Nunito Sans | 13px | 800 | 1 | Buttons, chips — uppercase |
| Caption | Nunito Sans | 12px | 600 | 1.4 | Meta, timestamps |

Google Fonts import URL:
```
https://fonts.googleapis.com/css2?family=Fredoka:wght@400;500;600;700&family=Nunito+Sans:wght@400;600;700;800;900&display=swap
```

### Spacing Scale (4 px base)

| Token | Value | Use |
|-------|-------|-----|
| xs | 4px | Hairline gaps |
| sm | 8px | Icon ↔ label |
| md | 12px | Inside chips |
| base | 16px | Card padding, gutters |
| lg | 24px | Between cards |
| xl | 32px | Section breaks |
| 2xl | 40px | Screen padding |

### Corner Radius

| Token | Value | Use |
|-------|-------|-----|
| icon | 10px | App icon tiles |
| input | 18px | Inputs, small containers |
| card | 24px | Cards, panels |
| large | 28px | Hero cards |
| pill | 999px | Buttons, badges, chips |

### Shadows

- Card: `0 1px 2px rgba(0,0,0,.04), 0 10px 34px rgba(0,0,0,.05)`
- Button primary: `0 8px 18px rgba(255,107,94,.30)`
- TV card: none — contrast via background color difference

---

## Screens — Phone App

### Shared Components

**Top header** (all phone screens):
- Left: family-switcher pill — avatar circle 30px (accent bg, Fredoka initial), family name Nunito 800 14px, chevron. Background white, border `#EADFD4`, border-radius pill, padding `5px 12px 5px 5px`.
- Right: parent avatar circle 38px (primary coral bg, Fredoka initial white 15px).

**Bottom navigation bar**:
- Background white, 1px top border `#F2E7DB`, padding `8px 6px 10px`.
- 4 tabs: Limits, Requests, Codes, Family.
- Active tab: 60×32 pill background `#ECE5FB`, icon stroke `#3A2A4D`, label Nunito 800 11px `#3A2A4D`.
- Inactive: icon stroke `#8A7C96`, label Nunito 700 11px `#8A7C96`.
- Requests tab shows a red dot badge (`#E5483A`) with count when pending.

**Status badges** (chips):
- On track: bg `#DFF6EC`, text `#15795A`, dot `#2E9E78`
- Almost up: bg `#FCEFD7`, text `#9A6313`, dot `#F2A93B`
- Time's up: bg `#FBE2DF`, text `#B5281C`, dot `#E5483A`
- Paused: bg `#ECE5FB`, text `#3A2A4D`
- All: Nunito 800 12px, border-radius pill, padding `5px 11px`, leading dot 7px circle.

---

### 1 · Limits (home screen)

**Purpose**: Parent's primary view — total daily screen time + per-app limits.

**Layout**: Scroll view, 16px horizontal padding, 132px bottom padding (FAB + nav).

**Header**: see Shared Components. Right side also includes a "TV on" status pill: bg `#DFF6EC`, text `#15795A` Nunito 800 12px, green dot 7px.

**Title block**:
- H1 "Limits" Fredoka 600 30px, color Ink.
- Subtitle date string Nunito 600 13px, color Ink muted. Margin-top 5px.

**Hero card** (today's total):
- Background `#3A2A4D` (Ink), border-radius 26px, padding 22px.
- Decorative circle: `#52436A` 120px, border-radius 50%, position absolute right -30px top -30px, opacity 0.5.
- Label "TODAY'S SCREEN TIME" Nunito 800 13px uppercase letter-spacing 0.04em, color `#C9BBD6`.
- Right: "On track" badge bg `#4FCFA1`, text Nunito 800 12px `#0F1F18`.
- Big number: "2h 35m" Fredoka 600 46px cream, white-space nowrap.
- "of 4h daily" Nunito 700 15px `#C9BBD6`, baseline-aligned.
- Progress bar: height 12px, bg `#52436A`, radius pill, filled portion `#4FCFA1`.
- Footer: "1h 25m left" and "Resets at midnight" Nunito 700 13px `#C9BBD6`.

**Request banner** (shown when pending):
- Background `#ECE5FB`, border-radius 20px, padding `12px 14px`, margin-top 14px.
- TV tile icon 38×38px, radius 12px, bg `#3A2A4D`, TV SVG icon `#FCF6F0`.
- Text: "TV wants 15 more minutes" Nunito 800 14px; "on YouTube · just now" Nunito 600 12px `#6E5C7E`.
- Right: "Review" pill — bg white, Nunito 800 13px `#3A2A4D`, padding `9px 15px`, radius pill.

**Section header**: "App limits" Fredoka 600 19px left, "5 apps" Nunito 700 13px `#8A7C96` right. Margin `24px 2px 12px`.

**App limit row**:
- Container: bg white, border 1px `#F2E7DB`, border-radius 20px, padding `14px 15px`.
- App icon tile: 46×46px, radius 14px, app-brand color bg, Fredoka 600 20px white initial.
- App name: Fredoka 500 17px Ink.
- Usage: Nunito 800 14px Ink + muted `#B6ABBF` total.
- Progress bar: height 8px, bg `#F2E7DB`, colored fill.
- Status badge (bottom-left) + chevron SVG `#B6ABBF` (bottom-right).
- For "paused" apps: opacity 0.78; no progress bar; status badge "Paused" lilac; no time label.

**FAB**: position absolute, bottom 96px, right 18px.
- Background `#FF6B5E`, radius 20px, padding `15px 20px 15px 17px`.
- Shadow `0 10px 24px rgba(255,107,94,.45)`.
- "+" SVG stroke `#3A2A4D` 22px + label "Add limit" Nunito 800 15px `#3A2A4D`.

---

### 2 · Requests

**Purpose**: Parent approves or denies time requests sent by the TV.

**Empty state**: when no pending requests — green checkmark circle (bg `#DFF6EC`), "You're all caught up", subtext Nunito 700 15px `#5B4D69`.

**Pending request card**:
- Container: bg white, border 1px `#F2E7DB`, border-radius 24px, padding 18px. Shadow `0 6px 20px rgba(58,42,77,.05)`.
- **Top row**: TV tile 44×44px radius 14px bg `#3A2A4D`; TV SVG `#FCF6F0`; device name "Living Room TV" Fredoka 500 18px; timestamp Nunito 700 12px muted. App pill right (app color 20px tile + name Nunito 800 12px).
- **Ask line**: "Wants [X] more" Fredoka 500 21px — the amount in `#E5483A`. Margin-top 15px.
- **Context**: "Used [X] of [Y] today" Nunito 700 13px `#8A7C96`. Margin-top 4px.
- **Note bubble** (optional, if note was typed on TV): bg `#ECE5FB`, radius 16px (top-left 6px), padding `11px 13px`, Nunito 700 14px Ink in quotes.
- **Amount chooser**: label "GRANT HOW LONG?" Nunito 800 12px uppercase muted, margin-top 16px. Row of 3 chips: 15m / 30m / 60m. Selected: bg `#3A2A4D` text cream border same. Unselected: bg white, text Ink, border `#EADFD4`. All: Nunito 800 14px, padding `11px 0`, flex 1, radius 14px.
- **Actions**: Approve pill (coral, Nunito 800 15px, label = "Approve [selected amount]", shadow `0 6px 16px rgba(255,107,94,.32)`) + Deny ghost pill.

**Resolved rows**:
- Container: bg `#FAF5EF`, border `#F2E7DB`, radius 18px, padding `11px 14px`.
- TV tile 36×36px radius 11px bg `#EDE6E0`; TV SVG stroke `#8A7C96`.
- Text: Nunito 800 14px action description; subtitle "Living Room TV · by [parent] · [time]" Nunito 600 12px muted.
- Status badge right: Approved (positive) or Denied (muted `#F2EAE2` text `#8A7C96`).

**Interaction**:
- Tapping an amount chip updates the Approve button label.
- Approve → card animates out → appears in Resolved section as "Approved [X] on [app]".
- Deny → same dismissal → "Denied [X] on [app]".
- When all cards resolved → empty state appears.

---

### 3 · Unlock Codes

**Purpose**: Parent generates a single-use 4-digit code; the kid types it into the TV to unlock time.

**Active code card**:
- Background `#3A2A4D`, radius 26px, padding 22px. Decorative circle absolute bottom-right.
- "UNLOCK CODE" label Nunito 800 13px uppercase `#C9BBD6`. "Single-use" pill bg `#B9A8F0` text Nunito 800 11px `#3A2A4D`.
- 4 digit tiles: each flex 1, height 78px, radius 18px, bg `#FCF6F0`, Fredoka 600 44px `#3A2A4D`. Gap 11px.
- Headline e.g. "Unlocks Everything for 30 min" Nunito 700 15px `#EFE7F3`.
- Countdown pill: bg `#52436A`, radius pill, padding `7px 13px`. Clock SVG `#FFB7AF` + "Expires in [m:ss]" Nunito 800 13px `#FFD9D4`.
- Actions: "Copy code" coral pill + "New code" ghost. "Copy code" changes to "Copied!" for 1.5s.

**No-code state**: Dashed card border `#DDCFC2`, 4 placeholder tiles (`#FAF5EF` bg, `–` Fredoka 600 40px `#C9BCD0`). Instruction text muted.

**Settings card**:
- Title "What it unlocks" Fredoka 600 17px.
- "HOW MUCH TIME" — 2×2 grid of chips: 15 min / 30 min / 1 hour / Rest of day. Same chip style as Requests.
- "APPLIES TO" — 2 chips: Everything / Just YouTube.
- Generate button: full-width coral pill, Nunito 800 16px. Shadow `0 8px 18px rgba(255,107,94,.30)`. Label "Generate code" or "Replace with a new code".

**Recent codes list**:
- Row: `#FAF5EF` bg, radius 18px, padding `11px 14px`.
- Code string Fredoka 600 18px letter-spacing 0.08em, color `#B6ABBF`.
- Grant description + timestamp.
- Status badge: "Used" (positive) or "Expired" (muted).

**Interaction**:
- Tapping chips updates code settings live.
- Generate → random 4-digit code, 600s (10 min) countdown begins.
- Countdown reaches 0 → code auto-expires to Recent with "Expired" status.
- Copy → clipboard write + 1.5s "Copied!" state.
- New code → same as Generate.

---

### 4 · Family & Devices

**Purpose**: Owner manages parents and the one TV.

**Parents section**:
- Owner row: coral avatar "D", name + "Owner · you" badge (bg `#B9A8F0`, Nunito 800 11px `#3A2A4D`), email muted.
- Co-parent row: same structure, badge "Co-parent" (bg `#ECE5FB`, text `#5B4D69`). Kebab ⋮ button 36×36px bg `#FAF5EF`, radius 50%.
  - Kebab opens inline action row: "Remove from family" danger (bg `#FBE2DF`, text `#B5281C`) + Cancel.
  - Remove → row disappears, parent count updates.
- Invite row: dashed border `#DDCFC2`, plus icon in lilac circle, "Invite a parent" Fredoka 500 17px + "They join as a co-parent" muted.
  - Tap → invite panel expands (bg `#ECE5FB`, radius 20px). Shows invite URL in white pill, Copy button (coral). Note: owner-only, expires 24h.

**Devices section**:
- "TV" header Fredoka 600 19px + "1 of 1 paired" muted right.
- **Paired TV card**: bg `#3A2A4D`, radius 24px, padding 20px. Decorative circle top-right.
  - TV tile 48×48px radius 14px bg `#52436A`.
  - "Living Room TV" Fredoka 500 19px cream.
  - Online indicator: `#4FCFA1` dot + "On now · YouTube" 13px `#9FE9CE`.
  - Two stat tiles (Today / Paired) — bg `#52436A`, radius 14px, Fredoka 600 18px cream.
  - Unpair flow: tap "Unpair TV" → inline confirm card (bg `#4A2230`, border `#7A3A48`) with warning + Unpair (danger, `#E5483A`) + Cancel.
  - Confirm Unpair → card replaced with empty state.
- **Empty TV card**: dashed border, faint TV icon, "No TV paired", instructions text, "Pair a TV" coral pill.
- Owner-only footer note with info icon.

**Interaction**:
- All destructive actions (Remove co-parent, Unpair TV) have an inline confirm step.
- Invite panel is a toggle.

---

## Screens — TV App (10-foot UI)

### Design Principles (TV-specific)

- **No touch, no mouse** — everything navigable with D-pad (← → ↑ ↓) and OK/Enter.
- **Focus ring**: `box-shadow: 0 0 0 6px #FCF6F0` on primary buttons; `border-color: #FF6B5E; box-shadow: 0 0 0 6px rgba(255,107,94,.5)` on ghost buttons. Always `transform: scale(1.06)` on focused element.
- **Font sizes**: never below 24px for readable UI; headlines 72–90px.
- **WCAG AA**: cream (`#FCF6F0`) text on `#2E2140` bg = very high contrast. Coral (`#FF6B5E`) text on `#3A2A4D` = 4.65:1 AA.
- Resolution: **1920 × 1080**.

### TV Canvas

- Background: `#2E2140` full-bleed.
- Decorative radial gradients: top-right (`#4A356A`) and bottom-left (`#3E2C57`) — subtle depth.
- Brand chip: top-left, logo tile 38×38px radius 12px coral + text "ScreenTime" Fredoka 600 24px `#EFE7F3`.
- Remote hint: bottom-center, low-opacity muted. "◀ ▶ ▲ ▼ + OK".

### Button Styles (TV)

| Kind | Background | Text | Border | Focused add |
|------|-----------|------|--------|-------------|
| Primary | `#FF6B5E` | `#3A2A4D` | `3px solid #FF6B5E` | `box-shadow:0 0 0 6px #FCF6F0; scale(1.06)` |
| Ghost | `rgba(255,255,255,0.08)` | `#FCF6F0` | `3px solid rgba(255,255,255,0.24)` | `border-color:#FF6B5E; bg rgba(255,107,94,.16); shadow coral` |

All TV buttons: Nunito Sans 800 30px, padding `24px 46px`, border-radius pill.

---

### TV Screen 1 · Permission Setup

**When shown**: first launch, before Android Usage Access permission is granted.

**Layout**: Full-screen centered column. 200px horizontal padding.

- Eye SVG illustration circle: `#ECE5FB` bg 168px, stroke `#5B4D8C`, centered. Margin-bottom 44px.
- Headline: "One quick setup" Fredoka 600 78px cream.
- Body: 32px Nunito 600 `#C6B9D6`, max-width 1180px: "ScreenTime needs **Usage Access** so it can see which app is open…"
- Step dots: filled coral (`#FF6B5E`) 44×12px + empty dark (`#5A4A72`) 14×12px. Margin `40px 0 56px`.
- Buttons row: [Open Android settings (primary)] [Why do you need this? (ghost)].
- Focus default: index 0 (Open settings). Arrow left/right cycles. Enter activates.
- "Open Android settings" → launches Android Settings intent for PACKAGE_USAGE_STATS permission.

---

### TV Screen 2 · Pairing

**When shown**: after permission, or when owner initiates pairing from the phone.

**Layout**: Full-screen centered column.

- Headline: "Pair with a parent's phone" Fredoka 600 72px.
- Body: 30px `#C6B9D6` instructions to open Family & devices on phone.
- 6-digit code tiles: each 118×148px, radius 24px, bg `#FCF6F0`, Fredoka 600 84px `#3A2A4D`. Gap 18px. Code is randomly generated and displayed here.
- Waiting indicator: `#4FCFA1` dot (animated blink) + "Waiting for the phone…" Nunito 700 26px `#9FE9CE`.
- Buttons: [Get a new code (ghost)] [How to pair (ghost)].
- "Get a new code" → generates a new random 6-digit code via the ScreenTime backend.
- Pairing completes when the phone owner enters this code → TV transitions to Block (or permission-granted normal state).

---

### TV Screen 3 · Block (Time's Up) — EMOTIONAL CORE

**When shown**: whenever the current foreground app hits its daily limit. Full-screen overlay over whatever app was running.

**Tone**: warm, supportive, not punishing. "Nice watching" not "stop". Green done-badge not red X.

**Layout**: Full-screen centered column. 180px horizontal padding.

**App context chip** (above headline):
- Bg `rgba(255,255,255,0.08)`, border `rgba(255,255,255,0.16)`, radius pill, padding `10px 22px`.
- App tile 26×26px radius 8px (app brand color) + app name + "· [X] watched today" Nunito 800 24px `#EFE7F3`.

**Mint badge**: 210×210px circle bg `#4FCFA1`, box-shadow `0 0 0 16px rgba(79,207,161,.18)`. Checkmark SVG stroke `#0F1F18` 2.4px. Margin-bottom 40px.

**Headline**: "That's a wrap for today!" Fredoka 600 90px cream.

**Body**: 34px Nunito 600 `#C6B9D6`, max-width 1180px. "Nice watching. You've used all your [app] time — see you tomorrow, or ask for a little more." Adapt app name dynamically.

**Reset chip**: clock SVG + "Resets at midnight · [time remaining]" Nunito 700 22px `#9785AC`. Bg `rgba(255,255,255,0.07)` radius pill. Margin `30px 0 52px`.

**Buttons** (gap 28px): [Ask a parent for more time (primary)] [Enter an unlock code (ghost)].

Focus default: index 0. Enter → ask flow. Arrow cycles.

---

### TV Screen 4 · Ask for Time

**When shown**: after pressing "Ask a parent for more time".

- Headline: "How much more?" Fredoka 600 84px.
- Sub: "We'll send a quick request to a parent's phone." 32px `#C6B9D6`.
- Buttons: [15 more minutes (primary)] [30 more minutes (primary)] [Maybe later (ghost)].
- Focus: 0 (15 min). Arrow navigates 3 items.
- Pressing 15 or 30 min → saves selection, transitions to Waiting screen.

---

### TV Screen 5 · Waiting for Parent

**When shown**: after kid submits a request.

- Animated pulsing lilac circle 200×200px (keyframe: scale 1→1.18, opacity 0.55→1, 1.8s loop). Phone/notification SVG stroke `#3A2A4D`.
- Headline: "Asked your parent!" Fredoka 600 84px.
- Sub: "They just got a notification. Hang tight — or punch in an unlock code if they gave you one." 32px.
- Buttons: [Enter a code instead (ghost)] [Cancel (ghost)].
- **Auto-resolve**: if parent approves (via push/socket), transition to Approved. If denied → Denied. Timeout (if no response in e.g. 5 min) → stay on waiting, show "Still waiting…" copy update.

---

### TV Screen 6 · Approved

**When shown**: parent approves the request.

- Mint badge 210px (same as Block) — checkmark.
- Headline: "You got [X] more!" Fredoka 600 88px. X = the amount the parent approved.
- Sub: "Make it count — your timer's running again." 34px.
- Button: [Keep watching (primary)]. Enter → dismiss overlay, return to content.

---

### TV Screen 7 · Unlocked (Code Entry Success)

**When shown**: kid entered the correct unlock code.

- Mint badge, open-padlock SVG.
- Headline: "Code worked! +[X] min" Fredoka 600 88px.
- Sub: "Limit's paused for a bit. Enjoy!" 34px.
- Button: [Keep watching (primary)].

---

### TV Screen 8 · Denied

**When shown**: parent denies the request.

**Tone**: gentle, not harsh. Lilac badge, no red.

- Lilac circle 200px, bg `#ECE5FB`, sad-face SVG stroke `#5B4D8C`.
- Headline: "Not right now" Fredoka 600 84px.
- Sub: "Your parent said maybe later — and that's okay. There's always tomorrow. Maybe something offline for now?" 32px.
- Buttons: [Enter an unlock code (ghost)] [Okay (primary)]. "Okay" → returns to block screen.

---

### TV Screen 9 · Enter Unlock Code (Keypad)

**When shown**: kid chooses "Enter an unlock code" from Block or Denied screens.

**Layout**: Two-column (side by side, gap 120px), centered vertically.

**Left column** (prompt + code slots):
- Headline: "Enter the unlock code" Fredoka 600 72px.
- Sub: "Ask a parent to read it to you, or check their phone." 28px `#C6B9D6`.
- 4 code slots: each 120×150px, radius 24px. Filled: bg `#FCF6F0`, Fredoka 600 84px `#3A2A4D`. Active (current): bg `rgba(252,246,240,0.12)`, border `4px solid #FF6B5E`, box-shadow `0 0 0 6px rgba(255,107,94,.3)`. Empty: bg `rgba(252,246,240,0.10)`, border `rgba(255,255,255,0.2)`.
- Error state: bg `rgba(229,72,58,.18)` border `#E5483A` rounded card, warning SVG + "That code didn't work — [N] tries left" Nunito 800 26px `#FFD9D4`.

**Right column** (keypad):
- 3×4 grid, gap 22px, each key 150px wide × 150px tall.
- Key layout: rows 1–3 = digits 1–9; row 4 = [C (clear), 0, ⌫ (backspace)].
- Unfocused: bg `rgba(255,255,255,0.07)`, Fredoka 600 56px `#FCF6F0`, border `rgba(255,255,255,0.14)`, radius 26px.
- Focused: bg `#FF6B5E`, text `#3A2A4D`, box-shadow `0 0 0 6px #FCF6F0`, scale(1.06).
- C and ⌫ keys: text color `#B9A8F0` (lilac) when unfocused.

**D-pad navigation**: row/column grid. Arrows move within grid. Enter presses focused key. Backspace key on remote → same as ⌫ key. Escape → return to Block.

**Validation**: on 4th digit entered, validate against server-issued code. Correct → Unlocked screen. Wrong → error message, clear slots, decrement tries. 0 tries → Locked out screen.

---

### TV Screen 10 · Locked Out

**When shown**: too many wrong code attempts.

**Tone**: calm break, not punishment.

- Amber circle 200px, bg `#F2C879`, hint-bulb SVG stroke `#5A3E12`.
- Headline: "Let's take a short break" Fredoka 600 84px.
- Sub: "That's a few wrong codes. The TV unlocks again on its own — grab a snack or stretch!" 32px.
- **Countdown**: Fredoka 600 120px `#F2C879`, format mm:ss, counts from 5:00. At 0 → auto-returns to Block.
- Button: [Ask a parent instead (ghost)] → Ask for Time flow.

---

## State Management (TV App)

```
TVState
  ├── permission: 'needed' | 'granted'
  ├── pairing: { status: 'waiting' | 'paired', code: string }
  ├── session:
  │     ├── activeApp: { name, packageName, usedSeconds, limitSeconds } | null
  │     ├── scene: 'content' | 'block' | 'ask' | 'waiting' | 'approved'
  │     │          | 'denied' | 'keypad' | 'unlocked' | 'lockedout'
  │     ├── requestId: string | null
  │     ├── askAmount: 15 | 30 | null
  │     ├── codeInput: string (max 4 chars)
  │     ├── codeTries: number (start 3)
  │     └── lockSeconds: number (start 300)
  └── focus: number (index within current scene's focusable items)
```

Push/socket events from the backend:
- `request.approved { grantedMinutes }` → transition to `approved`
- `request.denied` → transition to `denied`
- `code.valid { grantedMinutes }` → transition to `unlocked`
- `limits.updated` → refresh session.activeApp

---

## State Management (Phone App)

```
PhoneState
  ├── auth: { userId, role: 'owner' | 'coparent', familyId }
  ├── family: { name, members: [{ id, name, role, email }] }
  ├── tv: { paired: boolean, name: string, lastSeen: Date }
  ├── limits: [{ appId, name, dailyLimitSeconds, usedSeconds, status, paused }]
  ├── requests: { pending: [Request], resolved: [Request] }
  │   Request: { id, app, requestedMinutes, selectedMinutes, note, timestamp }
  └── codes: { active: { code, grantMinutes, scope, expiresAt } | null,
               recent: [{ code, grant, status, timestamp }] }
```

---

## Assets

### ScreenTime App Icon

Both variants are in this folder as SVG — scale to any required density bucket.

**Phone icon** (`app-icon-phone.svg`) — for Android launcher + Play Store:
- Shape: 1024×1024, `rx/ry 230` (standard Google Play rounded rect)
- Background: `#FF6B5E` (Coral)
- Foreground: `#3A2A4D` (Plum) circle, `r 216`, centered at 512,512
- Android adaptive icon: use a solid `#FF6B5E` background layer + the plum circle as the foreground layer (centered within the 66% safe zone)

**TV icon** (`app-icon-tv.svg`) — for Android TV launcher (dark shelf):
- Identical shape, colors **inverted**: `#3A2A4D` background + `#FF6B5E` circle
- This reads cleanly on the dark TV launcher row

The mark (rounded square + centered circle) appears at every scale throughout the UI — brand chip on the TV screen (38px), avatar fallback, loading states.

### Other icons
- **App limit tiles**: each third-party app's official launcher icon. Designs use colored letter-tile placeholders.
- **System icons** (bell, clock, people, dialpad, TV, chevron): use Material Icons Outlined, 2dp stroke weight, to match the Sprout stroke style.
- No custom illustration assets — all decorative shapes are pure CSS/SVG geometry.

---

## Design Reference Files

All files are interactive HTML prototypes viewable in any browser:

| File | Description |
|------|-------------|
| `ScreenTime Design System.dc.html` | Color roles, type scale, spacing, radius, all core components |
| `Limits Home — Phone.dc.html` | Phone · Limits screen |
| `Requests — Phone.dc.html` | Phone · Requests screen (interactive approve/deny) |
| `Unlock Codes — Phone.dc.html` | Phone · Unlock codes with live countdown |
| `Family & Devices — Phone.dc.html` | Phone · Family members + TV management |
| `TV App.dc.html` | TV · All 10 scenes, D-pad interactive (click screen, use arrow keys + Enter) |

Open the `.dc.html` files directly in Chrome or Firefox.
For `TV App.dc.html`: use the **scene jump chips** above the TV to navigate between states; use **arrow keys + Enter** to drive focus like a remote. Demo unlock code: **1234**.
