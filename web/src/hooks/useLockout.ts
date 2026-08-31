import { useCallback, useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import { describeFunctionsError } from '../i18n/functionsErrors';
import { defaultLockoutSettings, type LockoutMode, type LockoutSettings } from '../models/LockoutSettings';

/**
 * Lean lockout-only hook for the Settings screen — useLimits() also subscribes
 * to app limits, installed apps, time frame and usage, which Settings doesn't
 * need.
 */
export function useLockout(familyId: string | null) {
  const [lockout, setLockout] = useState<LockoutSettings>(defaultLockoutSettings());
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!familyId) {
      setLockout(defaultLockoutSettings());
      return;
    }
    return repo.subscribeLockout(familyId, setLockout);
  }, [familyId]);

  const setLockoutConfig = useCallback(
    (durationMinutes: number, mode: LockoutMode) => {
      if (!familyId) return;
      repo.setLockoutConfig(familyId, durationMinutes, mode).catch((e: unknown) => setError(describeFunctionsError(e)));
    },
    [familyId],
  );

  const unlockNow = useCallback(() => {
    if (!familyId) return;
    repo.clearLockout(familyId).catch((e: unknown) => setError(describeFunctionsError(e)));
  }, [familyId]);

  return { lockout, error, setLockoutConfig, unlockNow };
}
