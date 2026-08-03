import { describe, expect, it } from 'vitest';
import { defaultLockoutSettings } from './LockoutSettings';

describe('LockoutSettings', () => {
  it('defaults match documented values', () => {
    const s = defaultLockoutSettings();
    expect(s.durationMinutes).toBe(15);
    expect(s.mode).toBe('TIMER');
    expect(s.locked).toBe(false);
    expect(s.lockedUntil).toBeNull();
  });
});
