import type { ReactNode } from 'react';
import { colors } from '../theme/colors';
import { radius } from '../theme/radius';

export function Modal({ onClose, children }: { onClose: () => void; children: ReactNode }) {
  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(58,42,77,0.35)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 16,
        zIndex: 100,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: colors.surface,
          borderRadius: radius.card,
          padding: 24,
          width: '100%',
          maxWidth: 420,
          maxHeight: '85vh',
          overflowY: 'auto',
        }}
      >
        {children}
      </div>
    </div>
  );
}
