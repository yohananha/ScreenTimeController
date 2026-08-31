import { useCallback, useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import { describeFunctionsError } from '../i18n/functionsErrors';
import i18n from '../i18n/i18n';
import type { Family, FamilyRole } from '../models/Family';

export interface FamilyUiState {
  inviteCode: string | null;
  joining: boolean;
  error: string | null;
  family: Family | null;
}

const initialState: FamilyUiState = { inviteCode: null, joining: false, error: null, family: null };

export function useFamily(familyId: string | null) {
  const [state, setState] = useState<FamilyUiState>(initialState);

  useEffect(() => {
    if (!familyId) {
      setState(initialState);
      return;
    }
    return repo.subscribeFamily(familyId, (family) => setState((prev) => ({ ...prev, family })));
  }, [familyId]);

  const createFamily = useCallback((uid: string) => {
    repo.createFamily(uid).catch((e: unknown) => setState((prev) => ({ ...prev, error: describeFunctionsError(e) })));
  }, []);

  const generateInvite = useCallback(() => {
    if (!familyId) return;
    repo
      .generateInvite(familyId)
      .then((code) => setState((prev) => ({ ...prev, inviteCode: code, error: null })))
      .catch((e: unknown) => setState((prev) => ({ ...prev, error: describeFunctionsError(e) })));
  }, [familyId]);

  const joinByCode = useCallback((code: string) => {
    setState((prev) => ({ ...prev, joining: true, error: null }));
    repo
      .joinFamilyByInvite(code)
      .then((joined) => {
        setState((prev) => ({
          ...prev,
          joining: false,
          error: joined === null ? i18n.t('errors.invalidOrExpiredCode') : null,
        }));
      })
      .catch((e: unknown) => setState((prev) => ({ ...prev, joining: false, error: describeFunctionsError(e) })));
  }, []);

  const setMemberRole = useCallback(
    (uid: string, role: FamilyRole) => {
      if (!familyId) return;
      repo.setMemberRole(familyId, uid, role).catch((e: unknown) => setState((prev) => ({ ...prev, error: describeFunctionsError(e) })));
    },
    [familyId],
  );

  const removeMember = useCallback(
    (uid: string) => {
      if (!familyId) return;
      repo.removeMember(familyId, uid).catch((e: unknown) => setState((prev) => ({ ...prev, error: describeFunctionsError(e) })));
    },
    [familyId],
  );

  return { state, createFamily, generateInvite, joinByCode, setMemberRole, removeMember };
}
