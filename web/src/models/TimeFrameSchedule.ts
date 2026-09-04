/** Mirrors java.time.DayOfWeek.name — Firestore stores these exact uppercase English strings. */
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

/** Date.getDay() is 0=Sunday..6=Saturday; DayOfWeek.values() is 0=Monday..6=Sunday. */
const JS_DAY_INDEX_TO_DAY_OF_WEEK: DayOfWeek[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

export function dayOfWeekFromDate(date: Date): DayOfWeek {
  return JS_DAY_INDEX_TO_DAY_OF_WEEK[date.getDay()]!;
}

export interface TimeFrameWindow {
  /** Minutes from midnight, 0..1439. */
  startMinute: number;
  /** Exclusive, 1..1440. */
  endMinute: number;
}

/** A single window spanning the entire day — the "allow all day" shortcut. */
export const ALL_DAY_WINDOW: TimeFrameWindow = { startMinute: 0, endMinute: 1440 };

/** True if two windows share any minute. Adjacent windows (one ends where the other starts) don't overlap. */
export function windowsOverlap(a: TimeFrameWindow, b: TimeFrameWindow): boolean {
  return a.startMinute < b.endMinute && b.startMinute < a.endMinute;
}

export interface TimeFrameSchedule {
  enabled: boolean;
  windowsByDay: Partial<Record<DayOfWeek, TimeFrameWindow[]>>;
}

export const DEFAULT_TIME_FRAME_SCHEDULE: TimeFrameSchedule = {
  enabled: false,
  windowsByDay: {},
};

export function isAllowedAt(schedule: TimeFrameSchedule, local: Date): boolean {
  if (!schedule.enabled) return true;
  const minuteOfDay = local.getHours() * 60 + local.getMinutes();
  const windows = schedule.windowsByDay[dayOfWeekFromDate(local)];
  if (!windows) return false;
  return windows.some((w) => w.startMinute <= minuteOfDay && minuteOfDay < w.endMinute);
}

/** Scans up to 7 days forward for the next moment a window opens. Mirrors Kotlin's nextAllowedMinute. */
export function nextAllowedMinute(schedule: TimeFrameSchedule, from: Date): Date | null {
  if (!schedule.enabled) return null;
  for (let dayOffset = 0; dayOffset <= 6; dayOffset++) {
    const day = new Date(from);
    day.setDate(day.getDate() + dayOffset);
    const startOfDay = new Date(day);
    startOfDay.setHours(0, 0, 0, 0);
    const minuteOfDay = dayOffset === 0 ? day.getHours() * 60 + day.getMinutes() : 0;
    const windows = schedule.windowsByDay[dayOfWeekFromDate(day)] ?? [];
    const next = [...windows]
      .sort((a, b) => a.startMinute - b.startMinute)
      .find((w) => w.startMinute > minuteOfDay);
    if (next) {
      const result = new Date(startOfDay);
      result.setMinutes(result.getMinutes() + next.startMinute);
      return result;
    }
  }
  return null;
}
