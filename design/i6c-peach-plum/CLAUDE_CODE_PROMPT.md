# Brief for Claude Code — implement the ScreenTime redesign

Paste the block below as the first message of a Claude Code session opened at the repo root.

---

Implement the UI redesign described in `design/i6c-peach-plum/README.md`. Read that file first, then `design/i6c-peach-plum/tokens.json`, then open the HTML references under `design/i6c-peach-plum/screens/` for each screen as you build it (they are static specs — copy exact sizes, radii, weights and colours from them; do not round to a grid).

Ground rules:

- UI only. Do not change `shared/`, `functions/`, `firestore.rules`, the Firestore repository ports, models, hooks or ViewModels — every screen keeps its existing data contract.
- Start with the **web app** (`web/`), then the **TV app** (`tv/`). Leave `mobile/` untouched unless I say otherwise.
- Follow the implementation order in README §7. After each numbered step, stop and show me: what changed, and a side-by-side check of the English and Hebrew screens against the matching `screens/*.html` and `screens/*-HE.html` files (run the Vite dev server and describe or screenshot both locales).
- Hebrew is a first-class target, not a follow-up. Apply README §5 (RTL rules) inside each step, not at the end: mirror layout and the gauge fill; never mirror digits, code tiles or the keypad; keep Rubik for both scripts; keep the mode labels on one line; the gauge shows only `h:mm` + one short word.
- Generate the token files (`web/src/theme/*.ts`, `tv/.../ui/theme/*.kt`) from `tokens.json` rather than hand-typing hex values, so the two stacks cannot drift.
- Navigation becomes Today · Rules · Family + an Unlock action. Fold Requests into Today and Codes into the Unlock sheet; remove those routes and their nav entries, keeping `useRequests` / `useCodes` as the data source.
- Keep existing tests green and update the ones that assert on the old nav (`BottomNavBar`, `Chips`, `StatusBadge`, `LimitsScreen`, `CodesScreen` tests); add tests for `TimeGauge` (h:mm formatting, RTL mirror), `ModeTiles` and `RequestStrip`.
- Before deleting `Direction #3 sprout design system (4)/`, grep for any remaining references to Sprout tokens.

Begin with step 1 (tokens + font) and report back.
