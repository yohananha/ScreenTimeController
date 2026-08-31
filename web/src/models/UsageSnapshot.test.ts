import { describe, expect, it } from 'vitest';
import { millisFor, totalMillis } from './UsageSnapshot';

describe('UsageSnapshot', () => {
  const snapshot = { date: '2026-01-01', perAppMillis: { 'com.a': 1000, 'com.b': 2000 } };

  it('millisFor returns 0 for an unknown package', () => {
    expect(millisFor(snapshot, 'com.unknown')).toBe(0);
  });

  it('millisFor returns the stored value for a known package', () => {
    expect(millisFor(snapshot, 'com.a')).toBe(1000);
  });

  it('totalMillis sums all packages', () => {
    expect(totalMillis(snapshot)).toBe(3000);
  });
});
