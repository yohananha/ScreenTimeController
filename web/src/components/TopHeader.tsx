import type { ReactNode } from 'react';
import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

export function InitialCircle({
  initial,
  size,
  bg,
  onClick,
}: {
  initial: string;
  size: number;
  bg: string;
  onClick?: () => void;
}) {
  return (
    <div
      onClick={onClick}
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: bg,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: onClick ? 'pointer' : undefined,
        flexShrink: 0,
        ...typography.headline,
        color: colors.ink,
      }}
    >
      {initial}
    </div>
  );
}

export function TopHeader({
  familyName,
  parentInitial,
  onFamilyClick,
  onParentClick,
  trailing,
}: {
  familyName: string;
  parentInitial: string;
  onFamilyClick?: () => void;
  onParentClick?: () => void;
  trailing?: ReactNode;
}) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px' }}>
      <div
        onClick={onFamilyClick}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 9,
          background: colors.surface,
          border: `1px solid ${colors.outline}`,
          borderRadius: 999,
          paddingBlock: 5,
          paddingInlineStart: 5,
          paddingInlineEnd: 12,
          cursor: onFamilyClick ? 'pointer' : undefined,
        }}
      >
        <InitialCircle initial={familyName.charAt(0).toUpperCase() || '?'} size={30} bg={colors.accent} />
        <span style={{ ...typography.label, color: colors.ink }}>{familyName}</span>
        <span style={{ color: colors.inkMuted, fontSize: 12 }}>{'▾'}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {trailing}
        <InitialCircle initial={parentInitial} size={38} bg={colors.primary} onClick={onParentClick} />
      </div>
    </div>
  );
}
