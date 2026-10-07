/**
 * `YYYY-MM-DD` for [date] in the browser's LOCAL time zone — the same
 * calendar day the TV's `LocalDate.now()` produces for its usage docs,
 * instant lock and allow-all-day.
 *
 * Not `toISOString().slice(0, 10)`: that is the UTC date, which east of UTC
 * (e.g. Israel, UTC+2/+3) is still *yesterday* for the first hours after
 * local midnight — so a lock or allow written then never matched "today" on
 * the TV.
 */
export function localIsoDate(date: Date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/** The local `YYYY-MM-DD` [daysAgo] days before today. */
export function localIsoDateDaysAgo(daysAgo: number, now: Date = new Date()): string {
  const d = new Date(now);
  d.setDate(d.getDate() - daysAgo);
  return localIsoDate(d);
}
