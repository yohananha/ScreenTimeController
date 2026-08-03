import { colors } from '../theme/colors';
import { radius } from '../theme/radius';
import { typography } from '../theme/typography';
import { StatusBadge, statusColorFor, type Status } from './StatusBadge';
import { ProgressBar } from './ProgressBar';

export function AppLimitRow({
  appName,
  initial,
  accent,
  usedLabel,
  progress,
  status,
  paused = false,
  onClick,
}: {
  appName: string;
  initial: string;
  accent: string;
  usedLabel: string;
  progress: number;
  status: Status;
  paused?: boolean;
  onClick?: () => void;
}) {
  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        width: '100%',
        background: colors.surface,
        border: `1px solid ${colors.outline}`,
        borderRadius: radius.input,
        padding: '14px 15px',
        opacity: paused ? 0.78 : 1,
        cursor: onClick ? 'pointer' : undefined,
      }}
    >
      <div
        style={{
          width: 46,
          height: 46,
          flexShrink: 0,
          borderRadius: 14,
          background: accent,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
          ...typography.headline,
        }}
      >
        {initial}
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6, minWidth: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ ...typography.headline, color: colors.ink }}>{appName}</span>
          {!paused && <span style={{ ...typography.bodyStrong, color: colors.ink }}>{usedLabel}</span>}
        </div>
        {!paused && <ProgressBar progress={progress} fill={statusColorFor(status)} track={colors.outline} height={8} />}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <StatusBadge status={paused ? 'Paused' : status} />
          <span style={{ color: colors.inkFaint }}>{'›'}</span>
        </div>
      </div>
    </div>
  );
}
