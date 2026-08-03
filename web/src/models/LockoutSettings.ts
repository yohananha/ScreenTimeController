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

export function defaultLockoutSettings(): LockoutSettings {
  return {
    durationMinutes: DEFAULT_LOCKOUT_DURATION_MINUTES,
    mode: 'TIMER',
    locked: false,
    lockedUntil: null,
  };
}
