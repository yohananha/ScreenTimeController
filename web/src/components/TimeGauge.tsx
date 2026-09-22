import { useTranslation } from 'react-i18next';
import { peachPlumColor, peachPlumSizes } from '../theme/tokens';

const { width, height, radius, stroke, cx, cy } = peachPlumSizes.arc;
const ARC_LENGTH = Math.PI * radius;
const TRACK_PATH = `M${cx - radius} ${cy} A${radius} ${radius} 0 0 1 ${cx + radius} ${cy}`;

/**
 * Half-arc time gauge (design/i6c-peach-plum tokens.json#sizes.arc). The fill
 * represents time USED (not remaining) — it grows toward `over` as the day's
 * total is consumed, even though the centre label shows time left.
 *
 * RTL: only the fill path mirrors (translate+scale around the arc centre,
 * matching screens/Today-HE.html); the track, and the h:mm/word text, never do.
 */
export function TimeGauge({
  centerLabel,
  word,
  progress,
  overLimit = false,
}: {
  /** "h:mm" remaining, or "—" when there's no daily limit. Never mirrored. */
  centerLabel: string;
  /** One short word (≤6 chars), e.g. "left" / "over" / "נשארו" / "נגמר". */
  word: string;
  /** 0–1 fraction of the daily limit already used. */
  progress: number;
  overLimit?: boolean;
}) {
  const { i18n } = useTranslation();
  const isRtl = i18n.dir() === 'rtl';
  const clamped = Math.min(1, Math.max(0, progress));
  const filled = clamped * ARC_LENGTH;

  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
      <defs>
        <linearGradient id="pp-arc-grad" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor={peachPlumColor.peach} />
          <stop offset="1" stopColor={peachPlumColor.rose} />
        </linearGradient>
      </defs>
      <path d={TRACK_PATH} fill="none" stroke={peachPlumColor.hairline} strokeWidth={stroke} strokeLinecap="round" />
      {clamped > 0 && (
        <path
          d={TRACK_PATH}
          fill="none"
          stroke={overLimit ? peachPlumColor.over : 'url(#pp-arc-grad)'}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={`${filled} ${ARC_LENGTH * 2}`}
          style={{ transition: 'stroke-dasharray 600ms ease-out' }}
          transform={isRtl ? `translate(${width} 0) scale(-1 1)` : undefined}
        />
      )}
      <text
        x={cx}
        y={122}
        textAnchor="middle"
        fontFamily="Rubik, system-ui, sans-serif"
        fontWeight={600}
        fontSize={56}
        letterSpacing="-0.03em"
        fill={peachPlumColor.ink}
      >
        {centerLabel}
      </text>
      <text x={cx} y={150} textAnchor="middle" fontFamily="Rubik, system-ui, sans-serif" fontWeight={500} fontSize={16} fill={peachPlumColor.muted}>
        {word}
      </text>
    </svg>
  );
}
