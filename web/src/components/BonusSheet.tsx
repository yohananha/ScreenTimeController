import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../theme/tokens';
import { BottomSheet } from './BottomSheet';
import { ChipGroup } from './Chips';
import { PrimaryButton } from './Button';

const CUSTOM = -1;
const MAX_CUSTOM_MINUTES = 240;

/**
 * "Add bonus time" sheet: pick 15 / 30 / 60 minutes or a custom amount. The
 * grant applies to today only and reaches the TV immediately — no approval step.
 */
export function BonusSheet({ onClose, onAdd }: { onClose: () => void; onAdd: (minutes: number) => void }) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState(30);
  const [customText, setCustomText] = useState('');
  const minutes = selected === CUSTOM ? Number(customText) || 0 : selected;
  const valid = Number.isInteger(minutes) && minutes >= 1 && minutes <= MAX_CUSTOM_MINUTES;

  return (
    <BottomSheet onDismiss={onClose} ariaLabel={t('bonus.title')}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <span style={{ fontWeight: 600, fontSize: 24, lineHeight: 1.1, letterSpacing: '-0.02em', color: peachPlumColor.ink }}>{t('bonus.title')}</span>
        <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('bonus.subtitle')}</span>
      </div>
      <ChipGroup
        options={[15, 30, 60, CUSTOM]}
        selected={selected}
        onSelect={setSelected}
        label={(m) => (m === CUSTOM ? t('bonus.custom') : t('codes.minutesShort', { count: m }))}
      />
      {selected === CUSTOM && (
        <label style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: 13, fontWeight: 600, color: peachPlumColor.muted }}>
          {t('bonus.customField', { max: MAX_CUSTOM_MINUTES })}
          <input
            inputMode="numeric"
            value={customText}
            onChange={(e) => setCustomText(e.target.value.replace(/\D/g, '').slice(0, 3))}
            style={{
              height: 48,
              padding: '0 16px',
              borderRadius: 14,
              border: `1.5px solid ${peachPlumColor.hairline}`,
              background: peachPlumColor.surface,
              color: peachPlumColor.ink,
              fontFamily: 'Rubik, system-ui, sans-serif',
              fontSize: 16,
            }}
          />
        </label>
      )}
      <PrimaryButton
        disabled={!valid}
        onClick={() => {
          onAdd(minutes);
          onClose();
        }}
      >
        {valid ? t('bonus.addAction', { count: minutes }) : t('bonus.add')}
      </PrimaryButton>
    </BottomSheet>
  );
}
