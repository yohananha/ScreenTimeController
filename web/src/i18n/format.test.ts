import { afterEach, describe, expect, it } from 'vitest';
import i18n from './i18n';
import { formatDurationLabel, formatLimitLabel, formatRelativeTime, summarizeSchedule } from './format';
import { UNLIMITED } from '../models/Limits';
import { DEFAULT_TIME_FRAME_SCHEDULE } from '../models/TimeFrameSchedule';

describe('format (English)', () => {
  afterEach(() => void i18n.changeLanguage('en'));

  it('formats minutes-only and hours-only durations', () => {
    expect(formatDurationLabel(45)).toBe('45m');
    expect(formatDurationLabel(60)).toBe('1h');
    expect(formatDurationLabel(90)).toBe('1h 30m');
  });

  it('formats the unlimited and zero sentinels', () => {
    expect(formatLimitLabel(UNLIMITED)).toBe('No limit');
    expect(formatLimitLabel(0)).toBe('0m');
  });

  it('reports no schedule set when disabled', () => {
    expect(summarizeSchedule(DEFAULT_TIME_FRAME_SCHEDULE)).toBe('No schedule set');
  });
});

describe('format (Hebrew)', () => {
  afterEach(() => void i18n.changeLanguage('en'));

  it('formats durations with Hebrew unit abbreviations', async () => {
    await i18n.changeLanguage('he');
    expect(formatDurationLabel(45)).toBe('45 דק׳');
    expect(formatDurationLabel(60)).toBe('1 שע׳');
    expect(formatDurationLabel(90)).toBe('1 שע׳ 30 דק׳');
  });

  it('uses the Hebrew dual form for exactly two more days', async () => {
    await i18n.changeLanguage('he');
    const schedule = {
      enabled: true,
      windowsByDay: {
        MONDAY: [{ startMinute: 480, endMinute: 1200 }],
        TUESDAY: [{ startMinute: 540, endMinute: 1140 }],
        WEDNESDAY: [{ startMinute: 540, endMinute: 1140 }],
      },
    };
    // Monday's window differs from the other two, so the summary is
    // "Mon: <window> · 2 more days" — dual form in Hebrew.
    expect(summarizeSchedule(schedule as never)).toContain('עוד יומיים');
  });

  it('formats the unlimited sentinel in Hebrew', async () => {
    await i18n.changeLanguage('he');
    expect(formatLimitLabel(UNLIMITED)).toBe('ללא הגבלה');
  });
});

describe('formatRelativeTime', () => {
  afterEach(() => void i18n.changeLanguage('en'));

  it('renders a recent timestamp as "now"', () => {
    expect(formatRelativeTime(new Date())).toMatch(/now/i);
  });

  it('renders a Hebrew relative time for an hour ago', async () => {
    await i18n.changeLanguage('he');
    const anHourAgo = new Date(Date.now() - 3_600_000);
    expect(formatRelativeTime(anHourAgo)).toContain('שעה');
  });
});
