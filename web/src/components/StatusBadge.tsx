import type { CSSProperties } from 'react';
import { useTranslation } from 'react-i18next';
import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

export type Status = 'OnTrack' | 'AlmostUp' | 'TimesUp' | 'Paused';

const VISUALS: Record<Status, { bg: string; fg: string; dot: string; labelKey: string }> = {
  OnTrack: { bg: colors.positiveContainer, fg: colors.positiveText, dot: colors.positiveDisplay, labelKey: 'history.onTrack' },
  AlmostUp: { bg: colors.warningContainer, fg: colors.warningText, dot: colors.warningDisplay, labelKey: 'history.almostUp' },
  TimesUp: { bg: colors.overContainer, fg: colors.overText, dot: colors.overDisplay, labelKey: 'limits.timesUp' },
  Paused: { bg: colors.accentContainer, fg: colors.ink, dot: colors.accent, labelKey: 'limits.paused' },
};

export function StatusBadge({ status, style }: { status: Status; style?: CSSProperties }) {
  const { t } = useTranslation();
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
      {t(v.labelKey)}
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
