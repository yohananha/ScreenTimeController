import { useState } from 'react';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { SproutPrimaryButton } from '../../components/SproutButton';
import { CodeSlotInput } from '../../components/CodeSlotInput';
import { useFamily } from '../../hooks/useFamily';
import { useCurrentUid } from '../../hooks/useCurrentUid';

export function FamilyOnboardingScreen() {
  const { state, createFamily, joinByCode } = useFamily(null);
  const uid = useCurrentUid();
  const [joinExpanded, setJoinExpanded] = useState(false);
  const [code, setCode] = useState('');
  const hPad = useResponsivePadding();

  return (
    <div style={{ minHeight: '100vh', background: colors.background, display: 'flex', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `12px ${hPad}px 24px` }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
            <div
              style={{
                width: 30,
                height: 30,
                borderRadius: 10,
                background: colors.primary,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <div style={{ width: 12, height: 12, borderRadius: '50%', background: colors.ink }} />
            </div>
            <span style={{ ...typography.headline, color: colors.ink }}>ScreenTime</span>
          </div>
          <div
            style={{
              width: 38,
              height: 38,
              borderRadius: '50%',
              background: colors.primary,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              ...typography.label,
              color: colors.ink,
            }}
          >
            P
          </div>
        </div>

        <h1 style={{ ...typography.display, color: colors.ink, marginTop: 24, marginBottom: 7 }}>
          Set up your family
        </h1>
        <p style={{ ...typography.bodyStrong, color: colors.inkMuted, margin: 0 }}>
          Create a new family or join one a co-parent set up.
        </p>

        <div
          onClick={() => uid && createFamily(uid)}
          style={{
            marginTop: 20,
            display: 'flex',
            alignItems: 'center',
            gap: 15,
            background: colors.surface,
            borderRadius: radius.card,
            padding: 20,
            cursor: 'pointer',
          }}
        >
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: '50%',
              background: colors.accentContainer,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 22,
              color: '#5B4D8C',
            }}
          >
            +
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ ...typography.headline, color: colors.ink }}>Create a family</div>
            <div style={{ ...typography.body, color: colors.inkMuted }}>Start fresh. You'll be the owner.</div>
          </div>
          <span style={{ color: colors.primary }}>{'›'}</span>
        </div>

        <div style={{ marginTop: 14, background: colors.surface, borderRadius: radius.card, padding: 20 }}>
          <div
            onClick={() => {
              setJoinExpanded((v) => !v);
              if (joinExpanded) setCode('');
            }}
            style={{ display: 'flex', alignItems: 'center', gap: 15, cursor: 'pointer' }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: '50%',
                background: colors.primary,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 22,
              }}
            >
              🔑
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ ...typography.headline, color: colors.ink }}>Join a family</div>
              <div style={{ ...typography.body, color: colors.inkMuted }}>
                Use the invite code from your co-parent.
              </div>
            </div>
            <span style={{ color: colors.primary }}>{'›'}</span>
          </div>
          {joinExpanded && (
            <div style={{ marginTop: 18 }}>
              <div style={{ ...typography.label, color: colors.inkFaint }}>ENTER THE 6-DIGIT INVITE CODE</div>
              <div style={{ marginTop: 10 }}>
                <CodeSlotInput value={code} onValueChange={setCode} />
              </div>
              <div style={{ marginTop: 14 }}>
                <SproutPrimaryButton
                  onClick={() => joinByCode(code)}
                  disabled={code.length !== 6 || state.joining}
                  style={{ width: '100%' }}
                >
                  {state.joining ? 'Joining…' : 'Continue'}
                </SproutPrimaryButton>
              </div>
            </div>
          )}
        </div>

        {state.error && (
          <p style={{ ...typography.bodyStrong, color: colors.overText, marginTop: 12 }}>{state.error}</p>
        )}
      </div>
    </div>
  );
}
