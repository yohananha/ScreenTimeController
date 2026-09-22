import { peachPlumColor } from '../theme/tokens';

/** "By app" row on Today (design/i6c-peach-plum README §3 "Usage row"). */
export function UsageRow({
  name,
  swatchColor,
  valueLabel,
  progress,
  barColor,
  timesUp = false,
  showBottomBorder = false,
}: {
  name: string;
  swatchColor: string;
  /** Shown next to the bar, e.g. "55m / 1h 30m". Ignored when `timesUp` is set. */
  valueLabel: string;
  /** 0–1 fraction of the app's limit used. */
  progress: number;
  barColor: string;
  timesUp?: boolean;
  showBottomBorder?: boolean;
}) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: 48,
        borderTop: `1px solid ${peachPlumColor.hairline}`,
        borderBottom: showBottomBorder ? `1px solid ${peachPlumColor.hairline}` : undefined,
      }}
    >
      <span style={{ display: 'flex', alignItems: 'center', gap: 10, fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 14, fontWeight: 600, color: peachPlumColor.ink }}>
        <span style={{ width: 10, height: 10, borderRadius: 3, background: swatchColor, display: 'inline-block', flexShrink: 0 }} />
        {name}
      </span>
      {timesUp ? (
        <span style={{ fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 13, fontWeight: 600, color: peachPlumColor.over }}>{valueLabel}</span>
      ) : (
        <span style={{ display: 'flex', alignItems: 'center', gap: 10, fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>
          <span style={{ display: 'block', width: 72, height: 4, borderRadius: 999, background: peachPlumColor.hairline }}>
            <span style={{ display: 'block', width: `${Math.round(Math.min(1, Math.max(0, progress)) * 100)}%`, height: '100%', borderRadius: 999, background: barColor }} />
          </span>
          {valueLabel}
        </span>
      )}
    </div>
  );
}
