import { DEFAULT_LOCKOUT_DURATION_MINUTES } from './constants';

export type LockoutMode = 'TIMER' | 'PARENT_UNLOCK';

/**
 * Configures what happens when too many wrong codes are entered on the TV.
 * `locked`/`lockedUntil` reflect the TV's current lockout state; the rest is
 * parent-configurable from the web app.
 */
export interface LockoutSettings {
  durationMinutes: number;
  mode: LockoutMode;
  locked: boolean;
  lockedUntil: Date | null;
}

/**
 * A lock with no `lockedUntil` — parent mode, or a timer lock the server
 * escalated after repeated lockouts — only ends when a parent unlocks.
 * Mirrors LockoutSettings.needsParent in shared/.../model/LockoutSettings.kt.
 */
export function needsParentUnlock(s: LockoutSettings): boolean {
  return s.locked && (s.mode === 'PARENT_UNLOCK' || s.lockedUntil === null);
}

export function defaultLockoutSettings(): LockoutSettings {
  return {
    durationMinutes: DEFAULT_LOCKOUT_DURATION_MINUTES,
    mode: 'TIMER',
    locked: false,
    lockedUntil: null,
  };
}
