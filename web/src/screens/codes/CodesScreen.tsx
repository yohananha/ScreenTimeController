import { useEffect, useState } from 'react';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { TopHeader } from '../../components/TopHeader';
import { HeroCard } from '../../components/HeroCard';
import { CodeTilesRow } from '../../components/CodeTile';
import { ChipGroup } from '../../components/Chips';
import { SproutPrimaryButton } from '../../components/SproutButton';
import { useCodes } from '../../hooks/useCodes';

function secondsUntil(date: Date): number {
  return Math.max(0, Math.floor((date.getTime() - Date.now()) / 1000));
}

function formatRemaining(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export function CodesScreen({ familyId }: { familyId: string }) {
  const { state, generate, dismiss } = useCodes(familyId);
  const [selectedMinutes, setSelectedMinutes] = useState(30);
  const hPad = useResponsivePadding();

  return (
    <div style={{ minHeight: '100%', background: colors.background, display: 'flex', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `0 ${hPad}px 24px`, display: 'flex', flexDirection: 'column', gap: 14 }}>
        <TopHeader familyName="Family" parentInitial="P" />
        <div style={{ paddingTop: 4, paddingBottom: 6 }}>
          <h1 style={{ ...typography.display, color: colors.ink, margin: 0 }}>Unlock codes</h1>
          <p style={{ ...typography.caption, color: colors.inkMuted, marginTop: 5 }}>
            Single-use codes your child types on the TV.
          </p>
        </div>

        {state.active ? (
          <ActiveCodeHero
            code={state.active.code}
            extraMinutes={state.active.extraMinutes}
            expiresAt={state.active.expiresAt}
            onDismiss={dismiss}
          />
        ) : (
          <>
            <NoCodeHero />
            <SettingsCard
              selectedMinutes={selectedMinutes}
              onSelectMinutes={setSelectedMinutes}
              isGenerating={state.isGenerating}
              onGenerate={() => generate(selectedMinutes)}
            />
          </>
        )}

        {state.error && <p style={{ ...typography.bodyStrong, color: colors.overText }}>{state.error}</p>}
      </div>
    </div>
  );
}

function ActiveCodeHero({
  code,
  extraMinutes,
  expiresAt,
  onDismiss,
}: {
  code: string;
  extraMinutes: number;
  expiresAt: Date;
  onDismiss: () => void;
}) {
  const [remaining, setRemaining] = useState(() => secondsUntil(expiresAt));
  useEffect(() => {
    setRemaining(secondsUntil(expiresAt));
    const id = setInterval(() => setRemaining(secondsUntil(expiresAt)), 1000);
    return () => clearInterval(id);
  }, [expiresAt]);

  return (
    <HeroCard>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ ...typography.label, color: colors.darkMutedText }}>UNLOCK CODE</span>
          <span style={{ background: colors.accent, color: colors.ink, borderRadius: radius.pill, padding: '4px 10px', ...typography.caption }}>
            Single-use
          </span>
        </div>
        <CodeTilesRow code={code} />
        <span style={{ ...typography.bodyStrong, color: colors.background }}>
          Unlocks Everything for {extraMinutes} min
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, background: colors.darkSurface, borderRadius: radius.pill, padding: '7px 13px', width: 'fit-content' }}>
          <span>⏱</span>
          <span style={{ ...typography.label, color: colors.background }}>
            {remaining > 0 ? `Expires in ${formatRemaining(remaining)}` : 'Expired'}
          </span>
        </div>
        <SproutPrimaryButton onClick={onDismiss}>Done</SproutPrimaryButton>
      </div>
    </HeroCard>
  );
}

function NoCodeHero() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14, background: colors.surface, borderRadius: radius.large, padding: 22 }}>
      <div style={{ display: 'flex', gap: 11, width: '100%' }}>
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            style={{
              flex: 1,
              background: colors.surfaceSunken,
              borderRadius: 18,
              padding: '26px 0',
              textAlign: 'center',
              fontFamily: 'Fredoka, sans-serif',
              fontWeight: 600,
              fontSize: 40,
              color: colors.inkFaint,
            }}
          >
            –
          </div>
        ))}
      </div>
      <span style={{ ...typography.body, color: colors.inkMuted }}>Generate a code below.</span>
    </div>
  );
}

function SettingsCard({
  selectedMinutes,
  onSelectMinutes,
  isGenerating,
  onGenerate,
}: {
  selectedMinutes: number;
  onSelectMinutes: (v: number) => void;
  isGenerating: boolean;
  onGenerate: () => void;
}) {
  return (
    <div style={{ background: colors.surface, borderRadius: radius.card, padding: 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
      <span style={{ ...typography.headline, color: colors.ink }}>What it unlocks</span>
      <span style={{ ...typography.label, color: colors.inkFaint }}>HOW MUCH TIME</span>
      <ChipGroup
        options={[15, 30, 60, -1]}
        selected={selectedMinutes}
        onSelect={onSelectMinutes}
        label={(m) => (m === -1 ? 'Rest of day' : `${m} min`)}
      />
      <SproutPrimaryButton onClick={onGenerate} disabled={isGenerating} style={{ width: '100%' }}>
        {isGenerating ? 'Generating…' : 'Generate code'}
      </SproutPrimaryButton>
    </div>
  );
}
