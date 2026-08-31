import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { TopHeader } from '../../components/TopHeader';
import { EmptyState } from '../../components/EmptyState';
import { ProgressBar } from '../../components/ProgressBar';
import { useHistory } from '../../hooks/useHistory';
import { totalMillis, type UsageSnapshot } from '../../models/UsageSnapshot';

function totalMinutes(snapshot: UsageSnapshot): number {
  return Math.floor(totalMillis(snapshot) / 60_000);
}

function statusColorFor(minutes: number): string {
  if (minutes > 240) return colors.overDisplay;
  if (minutes > 180) return colors.warningDisplay;
  return colors.positiveDisplay;
}

function appLabelFor(pkg: string): string {
  const last = pkg.split('.').pop() ?? pkg;
  return last.charAt(0).toUpperCase() + last.slice(1);
}

export function HistoryScreen({ familyId }: { familyId: string }) {
  const snapshots = useHistory(familyId);
  const nonEmpty = snapshots.filter((s) => Object.keys(s.perAppMillis).length > 0);
  const hPad = useResponsivePadding();

  return (
    <div style={{ minHeight: '100%', background: colors.background, display: 'flex', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `0 ${hPad}px 24px`, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <TopHeader familyName="Family" parentInitial="P" />
        <div style={{ paddingTop: 4, paddingBottom: 6 }}>
          <h1 style={{ ...typography.display, color: colors.ink, margin: 0 }}>Usage</h1>
          <p style={{ ...typography.caption, color: colors.inkMuted, marginTop: 5 }}>Last 7 days</p>
        </div>

        {nonEmpty.length === 0 ? (
          <EmptyState
            icon={<span style={{ fontSize: 36, color: colors.positiveText }}>✓</span>}
            iconBg={colors.positiveContainer}
            title="No usage yet"
            subtitle="Your family hasn't watched anything today. Enjoy the quiet!"
          />
        ) : (
          <>
            <WeeklyBarChartCard snapshots={snapshots} />
            {nonEmpty.map((snapshot) => (
              <DayCard key={snapshot.date} snapshot={snapshot} />
            ))}
          </>
        )}
      </div>
    </div>
  );
}

function WeeklyBarChartCard({ snapshots }: { snapshots: UsageSnapshot[] }) {
  const maxMinutes = Math.max(1, ...snapshots.map(totalMinutes));
  const todayIso = new Date().toISOString().slice(0, 10);

  return (
    <div style={{ background: colors.surface, borderRadius: radius.input, padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', gap: 6 }}>
        {[...snapshots].reverse().map((snapshot) => {
          const isToday = snapshot.date === todayIso;
          const mins = totalMinutes(snapshot);
          const fraction = Math.min(1, Math.max(0.05, mins / maxMinutes));
          const color = statusColorFor(mins);
          const dayLabel = new Date(`${snapshot.date}T00:00:00`).toLocaleDateString(undefined, { weekday: 'narrow' });
          return (
            <div key={snapshot.date} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ width: '100%', height: 94, display: 'flex', alignItems: 'flex-end' }}>
                <div style={{ width: '100%', height: `${fraction * 100}%`, borderRadius: '8px 8px 3px 3px', background: color }} />
              </div>
              <span style={{ ...typography.label, color: isToday ? colors.ink : colors.inkFaint, marginTop: 6 }}>
                {dayLabel}
              </span>
            </div>
          );
        })}
      </div>
      <div style={{ display: 'flex', gap: 14 }}>
        <LegendDot color={colors.positiveDisplay} label="On track" />
        <LegendDot color={colors.warningDisplay} label="Almost up" />
        <LegendDot color={colors.overDisplay} label="Over limit" />
      </div>
    </div>
  );
}

function LegendDot({ color, label }: { color: string; label: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
      <span style={{ width: 8, height: 8, borderRadius: '50%', background: color, display: 'inline-block' }} />
      <span style={{ ...typography.caption, color: colors.inkMuted }}>{label}</span>
    </div>
  );
}

function DayCard({ snapshot }: { snapshot: UsageSnapshot }) {
  const date = new Date(`${snapshot.date}T00:00:00`);
  const today = new Date();
  const isToday = date.toDateString() === today.toDateString();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  const isYesterday = date.toDateString() === yesterday.toDateString();
  const dayLabel = isToday ? 'Today' : isYesterday ? 'Yesterday' : date.toLocaleDateString(undefined, { weekday: 'long' });
  const dateLabel = date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  const total = totalMinutes(snapshot);
  const totalSum = Math.max(1, Object.values(snapshot.perAppMillis).reduce((s, v) => s + v, 0));
  const topApps = Object.entries(snapshot.perAppMillis)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5);

  return (
    <div style={{ background: colors.surface, borderRadius: radius.card, padding: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ ...typography.headline, color: colors.ink }}>{dayLabel}</div>
          <div style={{ ...typography.caption, color: colors.inkMuted }}>{dateLabel}</div>
        </div>
        <span style={{ ...typography.headline, color: colors.ink }}>{total}m</span>
      </div>
      {topApps.map(([pkg, millis]) => (
        <div key={pkg} style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ ...typography.body, color: colors.ink }}>{appLabelFor(pkg)}</span>
            <span style={{ ...typography.bodyStrong, color: colors.inkMuted }}>{Math.floor(millis / 60_000)}m</span>
          </div>
          <ProgressBar progress={millis / totalSum} fill={colors.primary} track={colors.outline} height={6} />
        </div>
      ))}
    </div>
  );
}
