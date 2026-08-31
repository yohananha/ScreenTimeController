import { describe, expect, it } from 'vitest';
import { dayOfWeekFromDate, isAllowedAt, nextAllowedMinute, type TimeFrameSchedule } from './TimeFrameSchedule';

describe('dayOfWeekFromDate', () => {
  it('maps JS Date.getDay() (0=Sunday) to the Kotlin DayOfWeek name Firestore stores', () => {
    // 2026-01-04 is a Sunday, 2026-01-05 is a Monday (verified against a real calendar).
    expect(dayOfWeekFromDate(new Date('2026-01-04T12:00:00'))).toBe('SUNDAY');
    expect(dayOfWeekFromDate(new Date('2026-01-05T12:00:00'))).toBe('MONDAY');
    expect(dayOfWeekFromDate(new Date('2026-01-10T12:00:00'))).toBe('SATURDAY');
  });
});

describe('isAllowedAt', () => {
  const schedule: TimeFrameSchedule = {
    enabled: true,
    windowsByDay: { MONDAY: [{ startMinute: 480, endMinute: 600 }] }, // 8:00-10:00
  };

  it('disabled schedule always allows', () => {
    expect(isAllowedAt({ enabled: false, windowsByDay: {} }, new Date('2026-01-05T03:00:00'))).toBe(true);
  });

  it('allows within a configured window', () => {
    expect(isAllowedAt(schedule, new Date('2026-01-05T09:00:00'))).toBe(true);
  });

  it('blocks outside a configured window', () => {
    expect(isAllowedAt(schedule, new Date('2026-01-05T11:00:00'))).toBe(false);
  });

  it('blocks on a day with no windows at all', () => {
    expect(isAllowedAt(schedule, new Date('2026-01-06T09:00:00'))).toBe(false); // Tuesday
  });
});

describe('nextAllowedMinute', () => {
  it('returns null when disabled', () => {
    expect(nextAllowedMinute({ enabled: false, windowsByDay: {} }, new Date())).toBeNull();
  });

  it('finds the next window later the same day', () => {
    const schedule: TimeFrameSchedule = {
      enabled: true,
      windowsByDay: { MONDAY: [{ startMinute: 600, endMinute: 660 }] }, // 10:00-11:00
    };
    const from = new Date('2026-01-05T09:00:00'); // Monday 9am
    const next = nextAllowedMinute(schedule, from);
    expect(next).toEqual(new Date('2026-01-05T10:00:00'));
  });

  it('scans forward to the next day when today has no more windows', () => {
    const schedule: TimeFrameSchedule = {
      enabled: true,
      windowsByDay: { TUESDAY: [{ startMinute: 480, endMinute: 600 }] }, // 8:00-10:00
    };
    const from = new Date('2026-01-05T09:00:00'); // Monday 9am
    const next = nextAllowedMinute(schedule, from);
    expect(next).toEqual(new Date('2026-01-06T08:00:00'));
  });
});
