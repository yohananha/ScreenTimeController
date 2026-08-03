import { useState } from 'react';
import { colors } from '../../theme/colors';
import { typography } from '../../theme/typography';
import { SproutPrimaryButton } from '../../components/SproutButton';
import { signInWithGoogle } from '../../firebase/authRepository';

export function SignInScreen() {
  const [error, setError] = useState<string | null>(null);

  const handleSignIn = () => {
    setError(null);
    signInWithGoogle().catch((e: unknown) => {
      // Popup-closed-by-user is a normal dismissal, not an error.
      if (e instanceof Error && e.message.includes('popup-closed-by-user')) return;
      setError(e instanceof Error ? e.message : 'Google sign-in failed');
    });
  };

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        background: colors.background,
      }}
    >
      <div style={{ width: '100%', maxWidth: 400, padding: '0 32px', textAlign: 'center' }}>
        <div
          style={{
            width: 96,
            height: 96,
            margin: '0 auto',
            borderRadius: 24,
            background: colors.primary,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div style={{ width: 40, height: 40, borderRadius: '50%', background: colors.ink }} />
        </div>
        <h1 style={{ ...typography.display, color: colors.ink, marginTop: 26, marginBottom: 10 }}>
          Welcome to ScreenTime
        </h1>
        <p style={{ ...typography.bodyL, color: colors.inkMuted, margin: 0 }}>
          Sign in to manage your family's screen time
        </p>
        {error && (
          <p style={{ ...typography.bodyStrong, color: colors.overText, marginTop: 16 }}>{error}</p>
        )}
        <div style={{ marginTop: 32 }}>
          <SproutPrimaryButton
            onClick={handleSignIn}
            style={{ width: '100%' }}
            leading={
              <span
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: 8,
                  background: '#fff',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#4285F4',
                  fontWeight: 700,
                }}
              >
                G
              </span>
            }
          >
            Continue with Google
          </SproutPrimaryButton>
        </div>
        <p style={{ ...typography.caption, color: colors.inkFaint, marginTop: 20 }}>
          By continuing you agree to our Privacy Policy and Terms of Service
        </p>
      </div>
    </div>
  );
}
