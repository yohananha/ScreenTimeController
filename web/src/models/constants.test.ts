import { describe, expect, it } from 'vitest';
import {
  DEFAULT_LOCKOUT_DURATION_MINUTES,
  LOCKOUT_ATTEMPT_WINDOW_SECONDS,
  MAX_LOCKOUT_ATTEMPTS,
  MAX_REQUESTED_MINUTES,
} from './constants';

/**
 * Canary: mirrors shared/src/test/java/.../LockoutSettingsTest.kt's
 * "companion constants are in sync with functions/index.ts" test. If any of
 * these change, functions/src/index.ts's server-side enforcement AND the
 * Kotlin LockoutSettings companion must be updated in lockstep — this test
 * (together with its Kotlin counterpart) is the canary that protects that
 * cross-repo contract.
 */
describe('cross-repo canary constants', () => {
  it('match functions/src/index.ts and LockoutSettings.kt', () => {
    expect(DEFAULT_LOCKOUT_DURATION_MINUTES).toBe(15);
    expect(MAX_LOCKOUT_ATTEMPTS).toBe(5);
    expect(LOCKOUT_ATTEMPT_WINDOW_SECONDS).toBe(60);
    expect(MAX_REQUESTED_MINUTES).toBe(240);
  });
});
