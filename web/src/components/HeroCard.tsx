import type { CSSProperties, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { colors } from '../theme/colors';
import { radius } from '../theme/radius';
import { typography } from '../theme/typography';
import { StatusBadge, statusColorFor, type Status } from './StatusBadge';
import { ProgressBar } from './ProgressBar';

/** Dark plum hero card used for the daily total on Limits and the active unlock code on Codes. */
export function HeroCard({ children, style }: { children: ReactNode; style?: CSSProperties }) {
  return (
    <div
      style={{
        position: 'relative',
        overflow: 'hidden',
        width: '100%',
        borderRadius: radius.large,
        background: colors.ink,
        ...style,
      }}
    >
      <div
        style={{
          position: 'absolute',
          top: -30,
          right: -30,
          width: 120,
          height: 120,
          borderRadius: '50%',
          background: colors.darkSurface,
          opacity: 0.5,
        }}
      />
      <div style={{ position: 'relative', padding: 22 }}>{children}</div>
    </div>
  );
}

export function DailyTotalHero({
  usedLabel,
  ofLabel,
  progress,
  timeLeft,
  resetLabel,
  status,
  onClick,
}: {
  usedLabel: string;
  ofLabel: string;
  progress: number;
  timeLeft: string;
  resetLabel: string;
  status: Status;
  onClick?: () => void;
}) {
  const { t } = useTranslation();
  return (
    <HeroCard style={onClick ? { cursor: 'pointer' } : undefined}>
      <div onClick={onClick} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ ...typography.label, color: colors.darkMutedText }}>{t('limits.todaysScreenTime')}</span>
          <StatusBadge status={status} />
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8 }}>
          <span className="num" style={{ ...typography.display, fontSize: 46, fontWeight: 600, color: colors.background }}>
            {usedLabel}
          </span>
          <span style={{ ...typography.bodyL, color: colors.darkMutedText, paddingBottom: 8 }}>{ofLabel}</span>
        </div>
        <ProgressBar progress={progress} fill={statusColorFor(status)} track={colors.darkSurface} />
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span style={{ ...typography.caption, color: colors.darkMutedText }}>{timeLeft}</span>
          <span style={{ ...typography.caption, color: colors.darkMutedText }}>{resetLabel}</span>
        </div>
      </div>
    </HeroCard>
  );
}
