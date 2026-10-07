import { describe, expect, it } from 'vitest';
import { defaultLockoutSettings, needsParentUnlock } from './LockoutSettings';

describe('LockoutSettings', () => {
  it('defaults match documented values', () => {
    const s = defaultLockoutSettings();
    expect(s.durationMinutes).toBe(15);
    expect(s.mode).toBe('TIMER');
    expect(s.locked).toBe(false);
    expect(s.lockedUntil).toBeNull();
  });

  it('needsParentUnlock covers parent mode and escalated (no lockedUntil) timer locks', () => {
    const base = defaultLockoutSettings();
    expect(needsParentUnlock({ ...base, locked: true, mode: 'PARENT_UNLOCK' })).toBe(true);
    expect(needsParentUnlock({ ...base, locked: true, mode: 'TIMER', lockedUntil: null })).toBe(true);
    expect(needsParentUnlock({ ...base, locked: true, mode: 'TIMER', lockedUntil: new Date() })).toBe(false);
    expect(needsParentUnlock({ ...base, locked: false, mode: 'PARENT_UNLOCK' })).toBe(false);
  });
});
