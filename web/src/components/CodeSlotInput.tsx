import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../theme/tokens';

/** Invite characters: no 0/O/1/I look-alikes — must match INVITE_ALPHABET in functions/src/codes.ts. */
const NOT_INVITE_CHAR = /[^A-HJ-NP-Z2-9]/g;

/**
 * Slot-style code entry. Hidden text input accepts input; visible slots render code.
 * Default: 6-digit pairing code. `alphanumeric`: 8-char invite code (uppercased).
 */
export function CodeSlotInput({
  value,
  onValueChange,
  slots = 6,
  alphanumeric = false,
}: {
  value: string;
  onValueChange: (value: string) => void;
  slots?: number;
  alphanumeric?: boolean;
}) {
  const { t } = useTranslation();
  const inputRef = useRef<HTMLInputElement>(null);
  return (
    <div style={{ position: 'relative', width: '100%' }}>
      {/* dir="ltr" keeps digit order stable under RTL — a Row would otherwise reverse it. */}
      <div dir="ltr" style={{ display: 'flex', gap: slots > 6 ? 6 : 8, cursor: 'text' }} onClick={() => inputRef.current?.focus()}>
        {Array.from({ length: slots }).map((_, i) => {
          const active = i === value.length;
          return (
            <div
              key={i}
              style={{
                flex: 1,
                height: 62,
                borderRadius: 18,
                background: peachPlumColor.surface,
                border: `${active ? 2 : 1.5}px solid ${active ? peachPlumColor.ink : peachPlumColor.hairline}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: 'Rubik, system-ui, sans-serif',
                fontWeight: 600,
                fontSize: slots > 6 ? 24 : 32,
                minWidth: 0,
                color: peachPlumColor.ink,
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
        onChange={(e) => {
          const raw = e.target.value;
          const cleaned = alphanumeric ? raw.toUpperCase().replace(NOT_INVITE_CHAR, '') : raw.replace(/\D/g, '');
          onValueChange(cleaned.slice(0, slots));
        }}
        inputMode={alphanumeric ? 'text' : 'numeric'}
        autoCapitalize={alphanumeric ? 'characters' : undefined}
        autoComplete="off"
        autoCorrect="off"
        spellCheck={false}
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
