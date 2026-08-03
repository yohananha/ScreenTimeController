import { UNLIMITED } from '../../models/Limits';
import type { TimeFrameSchedule } from '../../models/TimeFrameSchedule';
import { DAYS_OF_WEEK } from '../../models/TimeFrameSchedule';

export function formatDurationLabel(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hours === 0) return `${mins}m`;
  if (mins === 0) return `${hours}h`;
  return `${hours}h ${mins}m`;
}

export function formatLimitLabel(minutes: number): string {
  if (minutes === UNLIMITED) return 'No limit';
  if (minutes <= 0) return '0m';
  return formatDurationLabel(minutes);
}

export function minutesToAmPm(minute: number): string {
  const h = Math.floor(minute / 60);
  const m = minute % 60;
  const period = h < 12 ? 'AM' : 'PM';
  const displayHour = h === 0 ? 12 : h <= 12 ? h : h - 12;
  return m === 0 ? `${displayHour} ${period}` : `${displayHour}:${String(m).padStart(2, '0')} ${period}`;
}

export function toTimeLabel(minute: number): string {
  const h = Math.floor(minute / 60);
  const m = minute % 60;
  const amPm = h < 12 ? 'AM' : 'PM';
  const hour12 = h === 0 ? 12 : h <= 12 ? h : h - 12;
  return `${hour12}:${String(m).padStart(2, '0')} ${amPm}`;
}

function dayShortLabel(day: string): string {
  return day.charAt(0) + day.slice(1).toLowerCase().slice(0, 2);
}

export function summarizeSchedule(schedule: TimeFrameSchedule): string {
  if (!schedule.enabled) return 'No schedule set';
  const sorted = DAYS_OF_WEEK.map((day) => [day, schedule.windowsByDay[day] ?? []] as const).filter(
    ([, windows]) => windows.length > 0,
  );
  if (sorted.length === 0) return 'Schedule on — no windows set';
  const [, firstWindows] = sorted[0]!;
  const first = firstWindows[0]!;
  const windowStr = `${minutesToAmPm(first.startMinute)}–${minutesToAmPm(first.endMinute)}`;
  const allSame = sorted.every(
    ([, windows]) => windows.length === 1 && windows[0]!.startMinute === first.startMinute && windows[0]!.endMinute === first.endMinute,
  );
  if (allSame) {
    const days = sorted.map(([day]) => dayShortLabel(day));
    return `${days[0]}–${days[days.length - 1]}, ${windowStr}`;
  }
  const dayShort = dayShortLabel(sorted[0]![0]);
  const more = sorted.length - 1;
  return `${dayShort}: ${windowStr}` + (more > 0 ? ` · ${more} more ${more === 1 ? 'day' : 'days'}` : '');
}

export function windowsSummary(windows: { startMinute: number; endMinute: number }[]): string {
  if (windows.length === 0) return 'Blocked all day';
  if (windows.length === 1 && windows[0]!.startMinute === 0 && windows[0]!.endMinute >= 1440) return 'All day';
  return windows.map((w) => `${toTimeLabel(w.startMinute)}–${toTimeLabel(w.endMinute)}`).join(' · ');
}

export function isAllDayWindow(windows: { startMinute: number; endMinute: number }[]): boolean {
  return windows.length === 1 && windows[0]!.startMinute === 0 && windows[0]!.endMinute >= 1440;
}
