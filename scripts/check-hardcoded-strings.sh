#!/usr/bin/env bash
# Guards against new hardcoded, translatable-looking string literals landing
# in mobile/tv Compose UI code after the Hebrew-localization pass (see the
# plan's Stage 7). This is deliberately crude — a line-count regression
# check, not a real parser — but it catches the common case (a new PR adding
# `Text("Save")` instead of `Text(stringResource(R.string.action_save))`)
# for near-zero cost, run on every CI build via the `lint` job.
#
# It is NOT a substitute for `MissingTranslation`/`ExtraTranslation` lint
# (see the `lint {}` block in each module's build.gradle.kts) — those catch
# resource-level drift; this catches strings that never became a resource
# in the first place.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

# Matches `Text("Foo` / `text = "Foo` where the literal starts with a letter —
# i.e. plausible human-readable copy, not a symbol, digit, or empty string.
# Deliberately excludes Kotlin test sources: androidTest/test fixtures often
# pass literal content into components under test on purpose (see
# ComponentsTest.kt, CodesScreenTest.kt).
pattern='Text\(\s*"[A-Za-z]|text = "[A-Za-z]'

# Known-intentional exceptions, left hardcoded on purpose:
#   - the "ScreenTime" brand wordmark (brand names aren't translated)
#   - a bare "G" (Google logo letter) and "P" (parent-initial placeholder) —
#     both data/glyphs, not copy
# If this count goes UP, something new was hardcoded that probably shouldn't
# have been. If it goes down (someone extracts one of the exceptions above
# to a resource, e.g. by adding translatable="false"), lower BASELINE to
# match — don't leave slack in the gate.
BASELINE=4

matches="$(grep -rEno "$pattern" mobile/src/main/java tv/src/main/java 2>/dev/null || true)"
count=0
if [ -n "$matches" ]; then
  count=$(printf '%s\n' "$matches" | wc -l | tr -d ' ')
fi

echo "Hardcoded-string literal count: $count (baseline: $BASELINE)"

if [ "$count" -gt "$BASELINE" ]; then
  echo ""
  echo "New hardcoded string literal(s) found in Compose UI code. If this is" >&2
  echo "real user-facing copy, move it to strings.xml (+ values-he) and use" >&2
  echo "stringResource(...) instead. If it's genuinely non-translatable (a" >&2
  echo "brand name, a single glyph/placeholder), raise BASELINE in" >&2
  echo "scripts/check-hardcoded-strings.sh to match and say why inline." >&2
  echo "" >&2
  echo "Full match list:" >&2
  printf '%s\n' "$matches" >&2
  exit 1
fi

echo "OK."
