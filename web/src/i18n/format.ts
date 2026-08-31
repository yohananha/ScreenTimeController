import i18n from './i18n';
import { UNLIMITED } from '../models/Limits';
import type { TimeFrameSchedule } from '../models/TimeFrameSchedule';
import { DAYS_OF_WEEK, type DayOfWeek } from '../models/TimeFrameSchedule';

/**
 * Locale-aware formatting, consolidating what used to be three near-identical
 * copies (screens/limits/format.ts, RequestsScreen's formatRelativeTime,
 * CodesScreen's formatRemaining) — mirrors :shared/format/ on the Android side.
 */

export function formatDurationLabel(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hours === 0) return i18n.t('format.durationM', { m: mins });
  if (mins === 0) return i18n.t('format.durationH', { h: hours });
  return i18n.t('format.durationHM', { h: hours, m: mins });
}

export function formatLimitLabel(minutes: number): string {
  if (minutes === UNLIMITED) return i18n.t('format.durationNone');
  if (minutes <= 0) return i18n.t('format.durationZero');
  return formatDurationLabel(minutes);
}

/** "2:30 PM" / "14:30" — Intl picks the right hour cycle per locale, no hand-rolled AM/PM math. */
export function formatClockTime(minute: number): string {
  const d = new Date(0, 0, 0, Math.floor(minute / 60), minute % 60);
  return new Intl.DateTimeFormat(i18n.language, { hour: 'numeric', minute: '2-digit' }).format(d);
}

/** Kept for callers that only have a minute-of-day and want the same clock format. */
export const toTimeLabel = formatClockTime;

function dayShortLabel(day: DayOfWeek): string {
  const refDate = new Date(2024, 0, 1 + DAYS_OF_WEEK.indexOf(day)); // a known Mon–Sun week
  return new Intl.DateTimeFormat(i18n.language, { weekday: 'short' }).format(refDate);
}

export function dayDisplayName(day: DayOfWeek): string {
  const refDate = new Date(2024, 0, 1 + DAYS_OF_WEEK.indexOf(day));
  return new Intl.DateTimeFormat(i18n.language, { weekday: 'long' }).format(refDate);
}

export function summarizeSchedule(schedule: TimeFrameSchedule): string {
  if (!schedule.enabled) return i18n.t('limits.noScheduleSet');
  const sorted = DAYS_OF_WEEK.map((day) => [day, schedule.windowsByDay[day] ?? []] as const).filter(
    ([, windows]) => windows.length > 0,
  );
  if (sorted.length === 0) return i18n.t('limits.scheduleOnNoWindows');
  const [, firstWindows] = sorted[0]!;
  const first = firstWindows[0]!;
  const windowStr = `${formatClockTime(first.startMinute)}–${formatClockTime(first.endMinute)}`;
  const allSame = sorted.every(
    ([, windows]) => windows.length === 1 && windows[0]!.startMinute === first.startMinute && windows[0]!.endMinute === first.endMinute,
  );
  if (allSame) {
    const days = sorted.map(([day]) => dayShortLabel(day));
    return `${days[0]}–${days[days.length - 1]}, ${windowStr}`;
  }
  const dayShort = dayShortLabel(sorted[0]![0]);
  const more = sorted.length - 1;
  return `${dayShort}: ${windowStr}` + (more > 0 ? ` · ${i18n.t('format.moreDays', { count: more })}` : '');
}

export function windowsSummary(windows: { startMinute: number; endMinute: number }[]): string {
  if (windows.length === 0) return i18n.t('limits.blockedAllDay');
  if (windows.length === 1 && windows[0]!.startMinute === 0 && windows[0]!.endMinute >= 1440) return i18n.t('limits.allDay');
  return windows.map((w) => `${formatClockTime(w.startMinute)}–${formatClockTime(w.endMinute)}`).join(' · ');
}

export function isAllDayWindow(windows: { startMinute: number; endMinute: number }[]): boolean {
  return windows.length === 1 && windows[0]!.startMinute === 0 && windows[0]!.endMinute >= 1440;
}

/** "just now" / "5m ago" / "2h ago" via Intl.RelativeTimeFormat — replaces the hand-rolled version. */
export function formatRelativeTime(date: Date): string {
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
  const rtf = new Intl.RelativeTimeFormat(i18n.language, { numeric: 'auto' });
  if (seconds < 60) return rtf.format(0, 'second');
  if (seconds < 3600) return rtf.format(-Math.floor(seconds / 60), 'minute');
  return rtf.format(-Math.floor(seconds / 3600), 'hour');
}

/** "4:05" countdown — digits are locale-invariant, no translation needed. */
export function formatCountdown(totalSeconds: number): string {
  const s = Math.max(0, totalSeconds);
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${m}:${String(sec).padStart(2, '0')}`;
}
