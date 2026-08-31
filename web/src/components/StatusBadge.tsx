import type { CSSProperties } from 'react';
import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

export type Status = 'OnTrack' | 'AlmostUp' | 'TimesUp' | 'Paused';

const VISUALS: Record<Status, { bg: string; fg: string; dot: string; label: string }> = {
  OnTrack: { bg: colors.positiveContainer, fg: colors.positiveText, dot: colors.positiveDisplay, label: 'On track' },
  AlmostUp: { bg: colors.warningContainer, fg: colors.warningText, dot: colors.warningDisplay, label: 'Almost up' },
  TimesUp: { bg: colors.overContainer, fg: colors.overText, dot: colors.overDisplay, label: "Time's up" },
  Paused: { bg: colors.accentContainer, fg: colors.ink, dot: colors.accent, label: 'Paused' },
};

export function StatusBadge({ status, style }: { status: Status; style?: CSSProperties }) {
  const v = VISUALS[status];
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        background: v.bg,
        color: v.fg,
        borderRadius: 999,
        padding: '5px 11px',
        ...typography.caption,
        fontWeight: 800,
        ...style,
      }}
    >
      {status !== 'Paused' && (
        <span style={{ width: 7, height: 7, borderRadius: 999, background: v.dot, display: 'inline-block' }} />
      )}
      {v.label}
    </span>
  );
}

export function statusColorFor(status: Status): string {
  switch (status) {
    case 'OnTrack':
      return colors.positiveDisplay;
    case 'AlmostUp':
      return colors.warningDisplay;
    case 'TimesUp':
      return colors.overDisplay;
    case 'Paused':
      return colors.accent;
  }
}
