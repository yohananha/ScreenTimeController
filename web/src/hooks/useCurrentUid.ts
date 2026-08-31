import { useEffect, useState } from 'react';
import { subscribeSession } from '../firebase/authRepository';

/**
 * Standalone auth-session subscription for screens rendered outside a resolved
 * familyId (e.g. onboarding), mirroring how FamilyViewModel.kt independently
 * reads auth.currentSession.first() rather than being handed a uid prop.
 */
export function useCurrentUid(): string | null {
  const [uid, setUid] = useState<string | null>(null);
  useEffect(() => subscribeSession((session) => setUid(session?.uid ?? null)), []);
  return uid;
}
