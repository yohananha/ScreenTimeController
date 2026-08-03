import type { ReactNode } from 'react';
import { colors } from '../theme/colors';
import { radius } from '../theme/radius';
import { typography } from '../theme/typography';

export function EmptyState({
  icon,
  iconBg,
  title,
  subtitle,
}: {
  icon: ReactNode;
  iconBg: string;
  title: string;
  subtitle: string;
}) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 12,
        width: '100%',
        background: colors.surface,
        borderRadius: radius.card,
        padding: 32,
        textAlign: 'center',
      }}
    >
      <div
        style={{
          width: 88,
          height: 88,
          borderRadius: '50%',
          background: iconBg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {icon}
      </div>
      <span style={{ ...typography.title, color: colors.ink }}>{title}</span>
      <span style={{ ...typography.bodyStrong, color: colors.inkMuted }}>{subtitle}</span>
    </div>
  );
}
