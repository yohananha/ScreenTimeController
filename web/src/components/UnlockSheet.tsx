import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../theme/tokens';
import { BottomSheet } from './BottomSheet';
import { ChipGroup } from './Chips';
import { CodeTilesRow } from './CodeTile';
import { PrimaryButton, SecondaryButton } from './Button';
import { useCodes } from '../hooks/useCodes';
import { formatCountdown, formatDurationLabel } from '../i18n/format';

function secondsUntil(date: Date): number {
  return Math.max(0, Math.floor((date.getTime() - Date.now()) / 1000));
}

/**
 * Unlock bottom sheet — choose duration then show the generated code
 * (design/i6c-peach-plum README §4 "Unlock — choose" / "Unlock — code ready").
 * Wired to the existing useCodes hook; folds in what was CodesScreen.
 *
 * The design's "Applies to: Everything / {{app}} only" chips assume per-app
 * scoped codes, but OneTimeCode/createCode only ever unlock everything — there
 * is no scope field to honor a per-app choice. Rather than show a chip that
 * silently does nothing, this only offers "Everything".
 */
export function UnlockSheet({ familyId, onClose }: { familyId: string; onClose: () => void }) {
  const { t } = useTranslation();
  const { state, generate, dismiss } = useCodes(familyId);
  const [selectedMinutes, setSelectedMinutes] = useState(30);

  return (
    <BottomSheet onDismiss={onClose} ariaLabel={t('today.unlockAria')}>
      {state.active ? (
        <ReadyState code={state.active.code} extraMinutes={state.active.extraMinutes} expiresAt={state.active.expiresAt} onNewCode={dismiss} />
      ) : (
        <ChooseState
          selectedMinutes={selectedMinutes}
          onSelectMinutes={setSelectedMinutes}
          isGenerating={state.isGenerating}
          onGenerate={() => generate(selectedMinutes)}
        />
      )}
      {state.error && <span style={{ fontSize: 13, fontWeight: 600, color: peachPlumColor.over }}>{state.error}</span>}
    </BottomSheet>
  );
}

function SheetHeading({ subtitle }: { subtitle: string }) {
  const { t } = useTranslation();
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <span style={{ fontWeight: 600, fontSize: 24, lineHeight: 1.1, letterSpacing: '-0.02em', color: peachPlumColor.ink }}>{t('today.unlockAria')}</span>
      <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{subtitle}</span>
    </div>
  );
}

function durationChipLabel(t: (key: string, opts?: Record<string, unknown>) => string, minutes: number): string {
  if (minutes === -1) return t('codes.restOfDay');
  if (minutes === 60) return t('unlock.oneHour');
  return t('codes.minutesShort', { count: minutes });
}

function ChooseState({
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
    <>
      <SheetHeading subtitle={t('unlock.chooseSubtitle')} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <span style={{ fontSize: 13, fontWeight: 600, color: peachPlumColor.muted }}>{t('unlock.howMuchTime')}</span>
        <ChipGroup options={[15, 30, 60, -1]} selected={selectedMinutes} onSelect={onSelectMinutes} label={(m) => durationChipLabel(t, m)} />
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <span style={{ fontSize: 13, fontWeight: 600, color: peachPlumColor.muted }}>{t('unlock.appliesTo')}</span>
        <button
          aria-pressed="true"
          disabled
          style={{
            height: 44,
            padding: '0 18px',
            width: 'fit-content',
            borderRadius: 999,
            border: `1.5px solid ${peachPlumColor.ink}`,
            background: peachPlumColor.ink,
            color: peachPlumColor.surface,
            fontFamily: 'Rubik, system-ui, sans-serif',
            fontSize: 14,
            fontWeight: 600,
          }}
        >
          {t('unlock.everything')}
        </button>
      </div>
      <PrimaryButton onClick={onGenerate} disabled={isGenerating} style={{ marginTop: 4 }}>
        {isGenerating ? t('codes.generating') : t('codes.generateCode')}
      </PrimaryButton>
    </>
  );
}

function ReadyState({
  code,
  extraMinutes,
  expiresAt,
  onNewCode,
}: {
  code: string;
  extraMinutes: number;
  expiresAt: Date;
  onNewCode: () => void;
}) {
  const { t } = useTranslation();
  const [remaining, setRemaining] = useState(() => secondsUntil(expiresAt));
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    setRemaining(secondsUntil(expiresAt));
    const id = setInterval(() => setRemaining(secondsUntil(expiresAt)), 1000);
    return () => clearInterval(id);
  }, [expiresAt]);

  return (
    <>
      <SheetHeading subtitle={t('unlock.readySubtitle')} />
      <CodeTilesRow code={code} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 14, fontWeight: 500, color: peachPlumColor.muted }}>
        <span style={{ color: peachPlumColor.ink, fontWeight: 600 }}>{t('unlock.scopeLine', { duration: formatDurationLabel(extraMinutes) })}</span>
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={peachPlumColor.muted} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7v5l3 2" />
          </svg>
          {remaining > 0 ? t('codes.expiresIn', { time: formatCountdown(remaining) }) : t('codes.expired')}
        </span>
      </div>
      <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
        <PrimaryButton
          style={{ flexGrow: 1 }}
          onClick={() => {
            void navigator.clipboard.writeText(code);
            setCopied(true);
          }}
        >
          {copied ? t('settings.copied') : t('settings.copy')}
        </PrimaryButton>
        <SecondaryButton onClick={onNewCode}>{t('unlock.newCode')}</SecondaryButton>
      </div>
    </>
  );
}
