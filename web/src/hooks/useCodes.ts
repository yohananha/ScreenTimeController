import { useCallback, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import { describeFunctionsError } from '../i18n/functionsErrors';
import i18n from '../i18n/i18n';
import type { OneTimeCode } from '../models/OneTimeCode';

export interface CodesUiState {
  isGenerating: boolean;
  active: OneTimeCode | null;
  error: string | null;
}

const initialState: CodesUiState = { isGenerating: false, active: null, error: null };

export function useCodes(familyId: string | null) {
  const [state, setState] = useState<CodesUiState>(initialState);

  const generate = useCallback(
    (extraMinutes: number) => {
      if (!familyId) {
        setState((prev) => ({ ...prev, error: i18n.t('codes.noFamilyLinked') }));
        return;
      }
      setState((prev) => ({ ...prev, isGenerating: true, error: null }));
      repo
        .createCode(familyId, extraMinutes)
        .then((code) => setState({ isGenerating: false, active: code, error: null }))
        .catch((e: unknown) => setState({ isGenerating: false, active: null, error: describeFunctionsError(e) }));
    },
    [familyId],
  );

  const dismiss = useCallback(() => setState(initialState), []);

  return { state, generate, dismiss };
}
