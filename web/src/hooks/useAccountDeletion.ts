import { useCallback, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import { deleteAccountAndSignOut } from '../firebase/authRepository';
import { describeFunctionsError } from '../i18n/functionsErrors';

/**
 * Account / family deletion (Cloud Functions deleteAccount / deleteFamily).
 * On success there is nothing to navigate to: deleting the account signs
 * out (AuthGate shows sign-in), and deleting the family clears this user's
 * familyId (AuthGate shows onboarding).
 */
export function useAccountDeletion(familyId: string) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(describeFunctionsError(e));
    } finally {
      setBusy(false);
    }
  }, []);

  const deleteAccount = useCallback(() => run(deleteAccountAndSignOut), [run]);
  const deleteFamily = useCallback(() => run(() => repo.deleteFamily(familyId)), [run, familyId]);

  return { busy, error, deleteAccount, deleteFamily };
}
