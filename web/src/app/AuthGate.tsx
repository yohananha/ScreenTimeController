import { useTranslation } from 'react-i18next';
import { colors } from '../theme/colors';
import { useAuthState } from '../hooks/useAuthState';
import { SignInScreen } from '../screens/auth/SignInScreen';
import { FamilyOnboardingScreen } from '../screens/onboarding/FamilyOnboardingScreen';
import { AppShell } from './AppShell';

export function AuthGate() {
  const { t } = useTranslation();
  const state = useAuthState();

  switch (state.status) {
    case 'loading':
      return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
          <div
            aria-label={t('common.loading')}
            style={{
              width: 40,
              height: 40,
              borderRadius: '50%',
              border: `4px solid ${colors.outline}`,
              borderTopColor: colors.primary,
              animation: 'spin 0.8s linear infinite',
            }}
          />
          <style>{'@keyframes spin { to { transform: rotate(360deg); } }'}</style>
        </div>
      );
    case 'needs-sign-in':
      return <SignInScreen />;
    case 'needs-family':
      return <FamilyOnboardingScreen />;
    case 'authenticated':
      return <AppShell uid={state.uid} familyId={state.familyId} />;
  }
}
