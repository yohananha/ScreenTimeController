# Plan: Align All Screens to Final Sprout Designs (v4 fonts)

## Context

The Sprout design system has been finalised with a font change delivered in version 4 of the design files. The new font stack is:

| Role | v3 (old) | v4 (final) | Status in project |
|---|---|---|---|
| Display / Headlines | Fredoka | **Rubik** | ✅ already present (`rubik_variablefont_wght.ttf`) |
| Body / UI text | Nunito Sans | **Varela Round** | ✅ already present (`varela_round_regular.ttf`) |

The Rubik switch was driven by Hebrew support — Rubik covers Latin + Hebrew glyphs with the same rounded, warm personality as Fredoka, enabling localisation. Varela Round (the existing placeholder) is now the correct body font.

This means:
- **No font files need to be downloaded.** Both fonts are already committed.
- The `NunitoFont` FontFamily (which already uses Varela Round) is **correct as-is** — just needs its misleading comment removed.
- The `FredokaFont` FontFamily needs to be **replaced with Rubik** using the variable font file.

---

## Phase 1 — Swap Headline Font: Fredoka → Rubik

### `mobile/src/main/java/com/screentime/mobile/ui/theme/SproutTypography.kt`

Replace the `FredokaFont` FontFamily with `RubikFont` backed by the variable font; rename consistently:

```kotlin
// REMOVE
val FredokaFont = FontFamily(
    Font(R.font.fredoka_regular, FontWeight.Normal),
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

// ADD — variable font covers all weights in a single file
val RubikFont = FontFamily(
    Font(R.font.rubik_variablefont_wght, FontWeight.Normal),
    Font(R.font.rubik_variablefont_wght, FontWeight.Medium),
    Font(R.font.rubik_variablefont_wght, FontWeight.SemiBold),
    Font(R.font.rubik_variablefont_wght, FontWeight.Bold),
)
```

Update every `TextStyle` in `SproutTypeScale` and `rememberSproutTypeScale()` to use `RubikFont` instead of `FredokaFont`.

Remove the misleading comment on `NunitoFont` ("Valera Round replaces Nunito Sans") — Varela Round is the real body font now.

### `tv/src/main/java/com/screentime/tv/ui/theme/SproutTypography.kt`

Identical changes — the TV module has its own copy of the font family and typography file.

---

## Phase 2 — Screen-by-Screen Design Verification & Fixes

Colors, spacing, and overall layout already match the design spec. Specific gaps to fix:

### Mobile screens

| Screen | Gap | Fix |
|---|---|---|
| **LimitsScreen** | "Limits" screen title uses `display` (34sp). Design says 30sp for this H1. | Use `Sprout.typography.display.copy(fontSize = 30.sp)` for the title. |
| **LimitsScreen / HeroCard** | Decorative offset circle (`#52436A`, 120dp, alpha 0.5, top-right) is missing. | Add a `Box` with absolute alignment inside `HeroCard`. |
| **LimitsScreen** | FAB coral shadow (`0 10px 24px rgba(255,107,94,.45)`) not applied. | Add `Modifier.shadow()` with a coral-tinted elevation or `graphicsLayer` drop shadow. |
| **RequestsScreen** | "Wants **X** more" — the amount portion must be `#E5483A` (overDisplay). | Use `buildAnnotatedString` with a `SpanStyle(color = Sprout.colors.overDisplay)` on the amount. |
| **CodesScreen** | "Single-use" pill: bg should be `Sprout.colors.accent` (#B9A8F0), text `Sprout.colors.ink`. Currently reversed. | Swap fill/text colors on that pill. |
| **All screens** | Horizontal padding is fixed `16.dp`. Tablets (≥ 600dp) need wider padding. | Add `rememberHorizontalPadding(): Dp` utility using `LocalConfiguration.current.screenWidthDp` — compact 16dp / medium 32dp / expanded 64dp — and apply it to all screen `LazyColumn` / `Column` padding. |

### TV screens

| Screen | Gap | Fix |
|---|---|---|
| **BlockOverlayContent** | Copy text and headline sizes need audit vs. design README. | Read the full file and correct any copy differences. |
| **All TV views** | `RubikFont` must replace `FredokaFont` in `SproutTypography.kt`. | Covered by Phase 1. |

---

## Phase 3 — Add UI Test Infrastructure

No `androidTest` source set exists yet.

### 3a. `gradle/libs.versions.toml`

Add Compose UI test entries (versions already compatible with BOM 2024.12.01):
```toml
[libraries]
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

### 3b. `mobile/build.gradle.kts`

```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}
dependencies {
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

### 3c. Test Files

Create `mobile/src/androidTest/java/com/screentime/mobile/ui/`:

**`SproutThemeTest.kt`**
- `headlineFontIsRubik` — render `Text` with `Sprout.typography.display`; assert font family name contains "rubik"
- `bodyFontIsVarelaRound` — same for `Sprout.typography.label`; assert "varela"
- `primaryColorIsCorrect` — assert `Sprout.colors.primary == Color(0xFFFF6B5E)`
- `backgroundIsCreamy` — assert `Sprout.colors.background == Color(0xFFFCF6F0)`

**`LimitsScreenTest.kt`**
- `heroCardIsVisible` — `onNodeWithText("TODAY'S SCREEN TIME")` exists
- `fabIsPresent` — `onNodeWithText("Add limit")` exists
- `progressBarSemantics` — progress bar semantic node present
- `titleIs30sp` — title node fontSize asserted (via `captureToImage` pixel check or semantic tag)

**`ComponentsTest.kt`**
- `statusBadgeOnTrack` — renders green dot + correct text
- `statusBadgeAlmostUp` — amber
- `statusBadgeTimesUp` — red
- `primaryButtonEnabledState` — enabled renders coral background
- `primaryButtonDisabledState` — disabled renders muted
- `chipGroupTogglesSelection` — tap chip → selected state updates

**`CodesScreenTest.kt`**
- `noCodeStatePlaceholders` — 4 dash tiles visible before code generated
- `activeCodeDigitTilesVisible` — digit tiles appear after code set
- `singleUsePillColor` — "Single-use" pill background is accent lilac

**`ResponsiveLayoutTest.kt`**
- `compactWidthUses16dpPadding` — at 360dp screen width, padding == 16dp
- `mediumWidthUses32dpPadding` — at 600dp screen width, padding == 32dp

---

## Phase 4 — App Icon Verification

Read and compare the existing launcher XML files against the design spec:
- Phone: coral `#FF6B5E` bg + plum `#3A2A4D` circle
- TV: inverted — plum bg + coral circle

Update if they differ.

---

## File Change Summary

| File | Change |
|---|---|
| `mobile/.../theme/SproutTypography.kt` | `FredokaFont` → `RubikFont` (variable font); clean up NunitoFont comment |
| `tv/.../theme/SproutTypography.kt` | Same |
| `mobile/.../ui/limits/LimitsScreen.kt` | Title 30sp; responsive horizontal padding |
| `mobile/.../ui/components/HeroCard.kt` | Add decorative offset circle |
| `mobile/.../ui/requests/RequestsScreen.kt` | Amount text highlighted in overDisplay colour |
| `mobile/.../ui/codes/CodesScreen.kt` | Fix "Single-use" pill colours |
| `tv/.../ui/BlockOverlayContent.kt` | Copy/layout audit vs. design spec |
| `gradle/libs.versions.toml` | Add Compose UI test library entries |
| `mobile/build.gradle.kts` | Add `testInstrumentationRunner` + androidTest deps |
| `mobile/src/androidTest/...` | **New** — 5 test files |

> **No font file downloads needed.** `rubik_variablefont_wght.ttf` and `varela_round_regular.ttf` are already committed in both module `res/font/` directories.

---

## Verification

1. **Build**: `./gradlew :mobile:assembleDebug :tv:assembleDebug` — must pass with zero errors.
2. **Visual check**: run the app in an emulator; headlines must render with Rubik's characteristic rounded-but-heavier geometry (vs Fredoka's bubble-style), body in Varela Round.
3. **Hebrew rendering**: type Hebrew text in any `Text` — should render correctly with Rubik's Hebrew glyphs.
4. **UI tests**: `./gradlew :mobile:connectedAndroidTest` — all tests green.
5. **Design diff**: open each `.dc.html` from the v4 folder alongside the running app and compare visually.