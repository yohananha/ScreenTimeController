import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatCountdown } from '../../i18n/format';
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

export function CodesScreen({ familyId }: { familyId: string }) {
  const { t } = useTranslation();
  const { state, generate, dismiss } = useCodes(familyId);
  const [selectedMinutes, setSelectedMinutes] = useState(30);
  const hPad = useResponsivePadding();

  return (
    <div style={{ minHeight: '100%', background: colors.background, display: 'flex', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `0 ${hPad}px 24px`, display: 'flex', flexDirection: 'column', gap: 14 }}>
        <TopHeader familyName={t('topHeader.family')} parentInitial="P" />
        <div style={{ paddingTop: 4, paddingBottom: 6 }}>
          <h1 style={{ ...typography.display, color: colors.ink, margin: 0 }}>{t('codes.title')}</h1>
          <p style={{ ...typography.caption, color: colors.inkMuted, marginTop: 5 }}>{t('codes.subtitle')}</p>
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
  const { t } = useTranslation();
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
          <span style={{ ...typography.label, color: colors.darkMutedText }}>{t('codes.unlockCode')}</span>
          <span style={{ background: colors.accent, color: colors.ink, borderRadius: radius.pill, padding: '4px 10px', ...typography.caption }}>
            {t('codes.singleUse')}
          </span>
        </div>
        <CodeTilesRow code={code} />
        <span style={{ ...typography.bodyStrong, color: colors.background }}>
          {t('codes.unlocksFor', { minutes: extraMinutes })}
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, background: colors.darkSurface, borderRadius: radius.pill, padding: '7px 13px', width: 'fit-content' }}>
          <span>⏱</span>
          <span style={{ ...typography.label, color: colors.background }}>
            {remaining > 0 ? t('codes.expiresIn', { time: formatCountdown(remaining) }) : t('codes.expired')}
          </span>
        </div>
        <SproutPrimaryButton onClick={onDismiss}>{t('codes.done')}</SproutPrimaryButton>
      </div>
    </HeroCard>
  );
}

function NoCodeHero() {
  const { t } = useTranslation();
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
      <span style={{ ...typography.body, color: colors.inkMuted }}>{t('codes.generateBelow')}</span>
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
  const { t } = useTranslation();
  return (
    <div style={{ background: colors.surface, borderRadius: radius.card, padding: 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
      <span style={{ ...typography.headline, color: colors.ink }}>{t('codes.whatItUnlocks')}</span>
      <span style={{ ...typography.label, color: colors.inkFaint }}>{t('codes.howMuchTime')}</span>
      <ChipGroup
        options={[15, 30, 60, -1]}
        selected={selectedMinutes}
        onSelect={onSelectMinutes}
        label={(m) => (m === -1 ? t('codes.restOfDay') : t('codes.minutesShort', { count: m }))}
      />
      <SproutPrimaryButton onClick={onGenerate} disabled={isGenerating} style={{ width: '100%' }}>
        {isGenerating ? t('codes.generating') : t('codes.generateCode')}
      </SproutPrimaryButton>
    </div>
  );
}
