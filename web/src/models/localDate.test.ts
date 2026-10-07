import { afterEach, describe, expect, it, vi } from 'vitest';
import { localIsoDate, localIsoDateDaysAgo } from './localDate';

describe('localIsoDate', () => {
  afterEach(() => vi.useRealTimers());

  it('formats the local calendar day, zero-padded', () => {
    expect(localIsoDate(new Date(2026, 0, 5, 12, 0))).toBe('2026-01-05');
  });

  it('uses the local day just after local midnight (where the UTC day may still be yesterday)', () => {
    // 00:30 local on Oct 7. In any zone east of UTC, toISOString() would say Oct 6.
    expect(localIsoDate(new Date(2026, 9, 7, 0, 30))).toBe('2026-10-07');
  });

  it('defaults to now', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 7, 1, 15));
    expect(localIsoDate()).toBe('2026-10-07');
  });

  it('counts days back across a month boundary', () => {
    expect(localIsoDateDaysAgo(0, new Date(2026, 9, 1, 0, 30))).toBe('2026-10-01');
    expect(localIsoDateDaysAgo(1, new Date(2026, 9, 1, 0, 30))).toBe('2026-09-30');
  });
});
