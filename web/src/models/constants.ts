/**
 * Cross-repo canary constants. Must stay numerically identical to
 * functions/src/index.ts and shared/src/main/java/.../model/LockoutSettings.kt.
 * Covered by constants.test.ts as the web-side half of that contract.
 */
export const DEFAULT_LOCKOUT_DURATION_MINUTES = 15;
export const MAX_LOCKOUT_ATTEMPTS = 5;
export const LOCKOUT_ATTEMPT_WINDOW_SECONDS = 60;
export const MAX_REQUESTED_MINUTES = 240;
