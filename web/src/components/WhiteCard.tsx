import type { CSSProperties, ReactNode } from 'react';
import { peachPlumColor, peachPlumRadius, peachPlumShadow } from '../theme/tokens';

/** The ≥840px web cards use their own padding/radius (26, not the general `card` token) — copied verbatim from Web-Today.html/Web-Rules.html. */
export const wideCardStyle = {
  display: 'flex',
  flexDirection: 'column' as const,
  gap: 16,
  padding: 24,
  borderRadius: 26,
  background: peachPlumColor.surface,
  boxShadow: '0 1px 2px rgba(42,30,46,0.04), 0 10px 30px rgba(42,30,46,0.06)',
};

/** White card container used on Rules and Family (design/i6c-peach-plum README §3 "White card"). */
export function WhiteCard({ children, style }: { children: ReactNode; style?: CSSProperties }) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        padding: '0 16px',
        borderRadius: peachPlumRadius.card,
        background: peachPlumColor.surface,
        boxShadow: peachPlumShadow.card,
        ...style,
      }}
    >
      {children}
    </div>
  );
}
