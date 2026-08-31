import { useCallback, useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import { describeFunctionsError } from '../i18n/functionsErrors';
import i18n from '../i18n/i18n';
import type { PairedDevice } from '../models/PairedDevice';

export interface PairTvUiState {
  busy: boolean;
  success: boolean;
  message: string | null;
}

const initialState: PairTvUiState = { busy: false, success: false, message: null };

export function usePairedDevices(familyId: string | null) {
  const [devices, setDevices] = useState<PairedDevice[]>([]);
  const [state, setState] = useState<PairTvUiState>(initialState);

  useEffect(() => {
    if (!familyId) {
      setDevices([]);
      return;
    }
    return repo.subscribePairedDevices(familyId, setDevices);
  }, [familyId]);

  const claim = useCallback((code: string, familyIdArg: string) => {
    setState({ busy: true, success: false, message: null });
    repo
      .claimPairing(code, familyIdArg)
      .then((ok) =>
        setState({
          busy: false,
          success: ok,
          message: ok ? i18n.t('errors.tvPaired') : i18n.t('errors.invalidOrExpiredCode'),
        }),
      )
      .catch((e: unknown) => setState({ busy: false, success: false, message: describeFunctionsError(e) }));
  }, []);

  const reset = useCallback(() => setState(initialState), []);

  const rename = useCallback((deviceId: string, name: string) => {
    void repo.renameDevice(deviceId, name);
  }, []);

  const unpair = useCallback((familyIdArg: string, deviceId: string) => {
    void repo.unpairDevice(familyIdArg, deviceId);
  }, []);

  return { devices, state, claim, reset, rename, unpair };
}
