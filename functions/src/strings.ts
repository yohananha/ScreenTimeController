/**
 * Server-rendered push notification copy. Kept data-only (not a `notification`
 * block sent as-is) has a real cost — see onNewTimeRequest in index.ts for
 * why we render here rather than going data-only and letting the client
 * format: data-only messages are dropped under Doze/App Standby, which
 * would blunt a parental-approval flow whose entire value is timeliness.
 *
 * A single request can only be rendered for one language today — the family
 * doesn't (yet) track per-parent locale, only one family-wide
 * settings/language value (see the Hebrew-localization plan's "known
 * divergence" note on per-token locale as a v2 follow-up).
 */

export type Lang = "en" | "he";

interface TimeRequestCopy {
  title: string;
  body: (minutes: number, appLabel: string) => string;
}

export const PUSH_TIME_REQUEST: Record<Lang, TimeRequestCopy> = {
  en: {
    title: "Time request",
    body: (minutes, appLabel) => `+${minutes} min for ${appLabel}`,
  },
  he: {
    title: "בקשת זמן",
    body: (minutes, appLabel) => `‎+${minutes} דק' עבור ${appLabel}`,
  },
};

/** Anything other than the exact "he" tag falls back to English. */
export function resolveLang(code: string | null | undefined): Lang {
  return code === "he" ? "he" : "en";
}
