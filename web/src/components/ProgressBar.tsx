import { radius } from '../theme/radius';

export function ProgressBar({
  progress,
  fill,
  track,
  height = 12,
}: {
  progress: number;
  fill: string;
  track: string;
  height?: number;
}) {
  const clamped = Math.min(1, Math.max(0, progress));
  return (
    <div
      role="progressbar"
      aria-valuenow={Math.round(clamped * 100)}
      aria-valuemin={0}
      aria-valuemax={100}
      style={{ width: '100%', height, borderRadius: radius.pill, background: track, overflow: 'hidden' }}
    >
      <div style={{ width: `${clamped * 100}%`, height: '100%', borderRadius: radius.pill, background: fill }} />
    </div>
  );
}
