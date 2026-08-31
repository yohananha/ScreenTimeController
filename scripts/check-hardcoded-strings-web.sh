#!/usr/bin/env bash
# Web-app twin of check-hardcoded-strings.sh, guarding web/src against new
# hardcoded, translatable-looking English text landing in JSX after the
# Hebrew-localization pass. Same philosophy: a crude line-count regression
# check, not a real parser — it catches the common case (a new PR adding
# `<span>Save</span>` instead of `{t('common.save')}`) for near-zero cost.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

# Matches a JSX text node starting with a capital letter ("Save", "Cancel"),
# or a hardcoded placeholder/aria-label attribute. Excludes *.test.tsx —
# tests legitimately pass literal content into components under test.
pattern=">[A-Z][a-zA-Z]*[a-zA-Z .,!?…'-]*<|placeholder=\"[A-Za-z]|aria-label=\"[A-Za-z]"

# Known-intentional exceptions, left hardcoded on purpose: none today. If
# this count goes up, something new was hardcoded that probably shouldn't
# have been. If a genuinely non-translatable string needs an exception (a
# brand name, a single glyph/placeholder), raise BASELINE to match and say
# why inline — don't leave slack in the gate.
BASELINE=0

matches="$(grep -rEno "$pattern" web/src --include="*.tsx" 2>/dev/null | grep -v '\.test\.tsx' || true)"
count=0
if [ -n "$matches" ]; then
  count=$(printf '%s\n' "$matches" | wc -l | tr -d ' ')
fi

echo "Hardcoded-string literal count (web): $count (baseline: $BASELINE)"

if [ "$count" -gt "$BASELINE" ]; then
  echo ""
  echo "New hardcoded string literal(s) found in web/src JSX. If this is real" >&2
  echo "user-facing copy, move it to src/i18n/locales/{en,he}.json and use" >&2
  echo "t('...') (or <Trans> for styled/interpolated text) instead. If it's" >&2
  echo "genuinely non-translatable (a brand name, a single glyph/placeholder)," >&2
  echo "raise BASELINE in scripts/check-hardcoded-strings-web.sh to match and" >&2
  echo "say why inline." >&2
  echo "" >&2
  echo "Full match list:" >&2
  printf '%s\n' "$matches" >&2
  exit 1
fi

echo "OK."
