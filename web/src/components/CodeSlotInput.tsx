import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { colors } from '../theme/colors';

/** 6-digit invite/pairing code entry. Hidden text input accepts input; visible slots render code. */
export function CodeSlotInput({
  value,
  onValueChange,
  slots = 6,
}: {
  value: string;
  onValueChange: (value: string) => void;
  slots?: number;
}) {
  const { t } = useTranslation();
  const inputRef = useRef<HTMLInputElement>(null);
  return (
    <div style={{ position: 'relative', width: '100%' }}>
      {/* dir="ltr" keeps digit order stable under RTL — a Row would otherwise reverse it. */}
      <div dir="ltr" style={{ display: 'flex', gap: 8, cursor: 'text' }} onClick={() => inputRef.current?.focus()}>
        {Array.from({ length: slots }).map((_, i) => {
          const filled = i < value.length;
          const active = i === value.length;
          return (
            <div
              key={i}
              style={{
                flex: 1,
                height: 62,
                borderRadius: 14,
                background: filled || active ? colors.background : colors.surfaceSunken,
                border: `${active ? 2 : 1.5}px solid ${active ? colors.primary : colors.outline}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: 'Fredoka, sans-serif',
                fontWeight: 600,
                fontSize: 32,
                color: colors.ink,
              }}
            >
              {value[i] ?? ''}
            </div>
          );
        })}
      </div>
      <input
        ref={inputRef}
        value={value}
        onChange={(e) => onValueChange(e.target.value.replace(/\D/g, '').slice(0, slots))}
        inputMode="numeric"
        aria-label={t('common.code')}
        style={{
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
          opacity: 0,
          border: 'none',
          padding: 0,
        }}
      />
    </div>
  );
}
