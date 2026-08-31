import { useEffect, useState } from 'react';
import { subscribeSession, type Session } from '../firebase/authRepository';
import { subscribeFamilyId } from '../firebase/familyIdProvider';

export type AuthState =
  | { status: 'loading' }
  | { status: 'needs-sign-in' }
  | { status: 'needs-family' }
  | { status: 'authenticated'; uid: string; familyId: string };

export function useAuthState(): AuthState {
  const [session, setSession] = useState<Session | null | undefined>(undefined);
  const [familyId, setFamilyId] = useState<string | null | undefined>(undefined);

  useEffect(() => subscribeSession(setSession), []);

  useEffect(() => {
    if (!session) {
      setFamilyId(session === null ? null : undefined);
      return;
    }
    return subscribeFamilyId(session.uid, setFamilyId);
  }, [session]);

  if (session === undefined) return { status: 'loading' };
  if (session === null) return { status: 'needs-sign-in' };
  if (familyId === undefined) return { status: 'loading' };
  if (familyId === null) return { status: 'needs-family' };
  return { status: 'authenticated', uid: session.uid, familyId };
}
