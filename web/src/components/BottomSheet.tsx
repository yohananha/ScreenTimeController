import type { ReactNode } from 'react';
import { peachPlumColor, peachPlumRadius, peachPlumShadow } from '../theme/tokens';

/** Bottom sheet with scrim and drag handle (design/i6c-peach-plum README §3 "Bottom sheet"). */
export function BottomSheet({
  onDismiss,
  ariaLabel,
  children,
}: {
  onDismiss: () => void;
  ariaLabel: string;
  children: ReactNode;
}) {
  return (
    <div style={{ position: 'fixed', inset: 0, zIndex: 200 }}>
      <div data-testid="sheet-scrim" onClick={onDismiss} style={{ position: 'absolute', inset: 0, background: 'rgba(42,30,46,0.35)' }} />
      <div
        role="dialog"
        aria-label={ariaLabel}
        style={{
          position: 'absolute',
          insetInline: 0,
          bottom: 0,
          boxSizing: 'border-box',
          maxWidth: 430,
          margin: '0 auto',
          padding: '14px 24px 34px',
          borderRadius: `${peachPlumRadius.sheet}px ${peachPlumRadius.sheet}px 0 0`,
          background: peachPlumColor.ground,
          display: 'flex',
          flexDirection: 'column',
          gap: 20,
          boxShadow: peachPlumShadow.sheet,
        }}
      >
        <div style={{ width: 40, height: 4, borderRadius: 999, background: peachPlumColor.hairline, alignSelf: 'center' }} />
        {children}
      </div>
    </div>
  );
}
