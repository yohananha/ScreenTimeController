import { useCallback, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
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
        setState((prev) => ({ ...prev, error: 'No family linked yet.' }));
        return;
      }
      setState((prev) => ({ ...prev, isGenerating: true, error: null }));
      repo
        .createCode(familyId, extraMinutes)
        .then((code) => setState({ isGenerating: false, active: code, error: null }))
        .catch((e: unknown) =>
          setState({ isGenerating: false, active: null, error: e instanceof Error ? e.message : 'Failed.' }),
        );
    },
    [familyId],
  );

  const dismiss = useCallback(() => setState(initialState), []);

  return { state, generate, dismiss };
}
