import { useState } from 'react';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { appAccentFor } from '../../theme/appAccent';
import { TopHeader } from '../../components/TopHeader';
import { DailyTotalHero } from '../../components/HeroCard';
import { AppLimitRow } from '../../components/AppLimitRow';
import { SproutPrimaryButton, SproutGhostButton, SproutDangerButton } from '../../components/SproutButton';
import { Modal } from '../../components/Modal';
import type { Status } from '../../components/StatusBadge';
import { useLimits } from '../../hooks/useLimits';
import { UNLIMITED } from '../../models/Limits';
import type { InstalledApp } from '../../models/InstalledApp';
import type { LockoutMode, LockoutSettings } from '../../models/LockoutSettings';
import type { TimeFrameSchedule } from '../../models/TimeFrameSchedule';
import { formatLimitLabel, summarizeSchedule } from './format';

type LimitMode = 'Lock' | 'Default' | 'Allow';

export function LimitsScreen({
  familyId,
  onOpenHistory,
  onOpenTimeFrame,
}: {
  familyId: string;
  onOpenHistory: () => void;
  onOpenTimeFrame: () => void;
}) {
  const {
    state,
    writeError,
    clearWriteError,
    setLimit,
    removeLimit,
    setOverallLimit,
    setLockoutConfig,
    unlockNow,
    selectInstantLock,
    selectAllowAllDay,
    selectDefaultLimits,
  } = useLimits(familyId);
  const [editing, setEditing] = useState<{ packageName: string; displayName: string; defaultMinutes: number } | null>(null);
  const [picking, setPicking] = useState(false);
  const [editingOverall, setEditingOverall] = useState(false);
  const [editingLockout, setEditingLockout] = useState(false);
  const hPad = useResponsivePadding();

  const mode: LimitMode = state.instantLocked ? 'Lock' : state.allowAllDayActive ? 'Allow' : 'Default';
  const totalUsedMs = state.totalUsageMillis;
  const overallMs = state.overallDailyMinutes * 60_000;
  const overallProgress = overallMs > 0 ? Math.min(1, Math.max(0, totalUsedMs / overallMs)) : 0;
  const overallStatus: Status =
    overallMs > 0 && totalUsedMs >= overallMs
      ? 'TimesUp'
      : overallMs > 0 && totalUsedMs >= overallMs * 0.8
        ? 'AlmostUp'
        : 'OnTrack';
  const usedMin = Math.floor(totalUsedMs / 60_000);
  const leftMin = Math.max(0, state.overallDailyMinutes - usedMin);

  return (
    <div style={{ minHeight: '100%', background: colors.background, display: 'flex', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `0 ${hPad}px 132px`, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <TopHeader
          familyName="Family"
          parentInitial="P"
          trailing={
            <button
              onClick={onOpenHistory}
              aria-label="History"
              style={{
                width: 38,
                height: 38,
                borderRadius: '50%',
                background: colors.surface,
                border: 'none',
              }}
            >
              🕐
            </button>
          }
        />
        <div style={{ paddingTop: 4, paddingBottom: 6 }}>
          <h1 style={{ ...typography.display, fontSize: 30, color: colors.ink, margin: 0 }}>Limits</h1>
          <div style={{ ...typography.caption, color: colors.inkMuted, marginTop: 5 }}>
            {new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })}
          </div>
        </div>

        {writeError && (
          <div
            onClick={clearWriteError}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: colors.overContainer,
              borderRadius: radius.input,
              padding: '10px 14px',
              cursor: 'pointer',
            }}
          >
            <span style={{ color: colors.overText, ...typography.caption }}>{writeError}</span>
            <span style={{ color: colors.overText }}>✕</span>
          </div>
        )}

        <LimitModeCard
          mode={mode}
          onSelect={(selected) => {
            if (selected === 'Lock') selectInstantLock();
            else if (selected === 'Default') selectDefaultLimits();
            else selectAllowAllDay();
          }}
        />

        <DailyTotalHero
          usedLabel={formatLimitLabel(usedMin)}
          ofLabel={`of ${formatLimitLabel(state.overallDailyMinutes)} daily`}
          progress={overallProgress}
          timeLeft={`${formatLimitLabel(leftMin)} left`}
          resetLabel="Resets at midnight"
          status={overallStatus}
          onClick={() => setEditingOverall(true)}
        />

        <AllowedHoursRow schedule={state.timeFrame} onClick={onOpenTimeFrame} />

        <LockoutCard lockout={state.lockout} onClick={() => setEditingLockout(true)} onUnlockNow={unlockNow} />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 2px 4px' }}>
          <span style={{ ...typography.title, color: colors.ink }}>App limits</span>
          <span style={{ ...typography.caption, color: colors.inkMuted }}>
            {state.limits.length} {state.limits.length === 1 ? 'app' : 'apps'}
          </span>
        </div>

        {state.limits.length === 0 ? (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 8,
              background: colors.surface,
              borderRadius: radius.card,
              padding: 28,
            }}
          >
            <span style={{ ...typography.headline, color: colors.ink }}>No app limits yet</span>
            <span style={{ ...typography.body, color: colors.inkMuted }}>Tap the + to add one.</span>
          </div>
        ) : (
          state.limits.map((limit) => {
            const displayName =
              state.availableApps.find((a) => a.packageName === limit.packageName)?.label ??
              limit.packageName.split('.').pop() ??
              limit.packageName;
            const usedMs = state.usagePerApp[limit.packageName] ?? 0;
            const limitMs = limit.dailyLimitMinutes * 60_000;
            const rowProgress =
              limit.dailyLimitMinutes <= 0 || limit.dailyLimitMinutes === UNLIMITED
                ? 0
                : Math.min(1, Math.max(0, usedMs / limitMs));
            const rowStatus: Status =
              limit.dailyLimitMinutes === 0
                ? 'TimesUp'
                : limit.dailyLimitMinutes === UNLIMITED
                  ? 'Paused'
                  : limitMs > 0 && usedMs >= limitMs
                    ? 'TimesUp'
                    : limitMs > 0 && usedMs >= limitMs * 0.8
                      ? 'AlmostUp'
                      : 'OnTrack';
            const usedMinutes = Math.floor(usedMs / 60_000);
            return (
              <AppLimitRow
                key={limit.packageName}
                appName={displayName}
                initial={displayName.charAt(0).toUpperCase() || '?'}
                accent={appAccentFor(limit.packageName)}
                usedLabel={`${formatLimitLabel(usedMinutes)} / ${formatLimitLabel(limit.dailyLimitMinutes)}`}
                progress={rowProgress}
                status={rowStatus}
                paused={limit.dailyLimitMinutes === UNLIMITED}
                onClick={() =>
                  setEditing({ packageName: limit.packageName, displayName, defaultMinutes: limit.dailyLimitMinutes })
                }
              />
            );
          })
        )}
      </div>

      <SproutPrimaryButton
        onClick={() => setPicking(true)}
        shadow
        style={{ position: 'fixed', bottom: 96, right: 18, borderRadius: radius.large }}
      >
        + Add limit
      </SproutPrimaryButton>

      {picking && (
        <PickAppDialog
          available={state.availableApps.filter((app) => !state.limits.some((l) => l.packageName === app.packageName))}
          tvHasNoApps={state.availableApps.length === 0}
          onDismiss={() => setPicking(false)}
          onPick={(app) => {
            setPicking(false);
            setEditing({ packageName: app.packageName, displayName: app.label, defaultMinutes: 60 });
          }}
        />
      )}

      {editing && (
        <EditLimitDialog
          target={editing}
          onDismiss={() => setEditing(null)}
          onSave={(minutes) => {
            setLimit(editing.packageName, minutes);
            setEditing(null);
          }}
          onRemove={() => {
            removeLimit(editing.packageName);
            setEditing(null);
          }}
        />
      )}

      {editingOverall && (
        <EditOverallLimitDialog
          currentMinutes={state.overallDailyMinutes}
          onDismiss={() => setEditingOverall(false)}
          onSave={(minutes) => {
            setOverallLimit(minutes);
            setEditingOverall(false);
          }}
        />
      )}

      {editingLockout && (
        <EditLockoutDialog
          current={state.lockout}
          onDismiss={() => setEditingLockout(false)}
          onSave={(minutes, mode2) => {
            setLockoutConfig(minutes, mode2);
            setEditingLockout(false);
          }}
        />
      )}
    </div>
  );
}

function LockoutCard({
  lockout,
  onClick,
  onUnlockNow,
}: {
  lockout: LockoutSettings;
  onClick: () => void;
  onUnlockNow: () => void;
}) {
  const statusText = lockout.mode === 'TIMER' ? `${lockout.durationMinutes} min lock` : 'Parent unlock';
  return (
    <div style={{ background: colors.surface, borderRadius: radius.card, padding: '14px 15px' }}>
      <div onClick={onClick} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}>
        <div>
          <div style={{ ...typography.headline, color: colors.ink }}>Code lockout</div>
          <div style={{ ...typography.caption, color: colors.inkMuted }}>After 5 wrong codes in 1 minute</div>
        </div>
        <span style={{ ...typography.bodyStrong, color: colors.ink }}>{statusText}</span>
      </div>
      {lockout.locked && (
        <>
          <div style={{ ...typography.caption, color: colors.overText, marginTop: 6 }}>
            TV code entry is currently locked.
          </div>
          {lockout.mode === 'PARENT_UNLOCK' && (
            <div style={{ marginTop: 8 }}>
              <SproutPrimaryButton onClick={onUnlockNow}>Unlock now</SproutPrimaryButton>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function AllowedHoursRow({ schedule, onClick }: { schedule: TimeFrameSchedule; onClick: () => void }) {
  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        background: colors.surface,
        borderRadius: radius.input,
        padding: '14px 15px',
        cursor: 'pointer',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div
          style={{
            width: 36,
            height: 36,
            borderRadius: radius.icon,
            background: colors.accentContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          🕐
        </div>
        <div>
          <div style={{ ...typography.headline, color: colors.ink }}>Allowed hours</div>
          <div style={{ ...typography.caption, color: colors.inkMuted }}>{summarizeSchedule(schedule)}</div>
        </div>
      </div>
      <span style={{ color: colors.inkMuted }}>{'›'}</span>
    </div>
  );
}

const MODE_VISUALS: Record<LimitMode, { bg: string; titleColor: string; icon: string; title: string; caption: string }> = {
  Lock: { bg: colors.overContainer, titleColor: colors.overText, icon: '🔒', title: 'Instant lock', caption: 'TV is locked — tap Default to release' },
  Default: { bg: colors.surface, titleColor: colors.ink, icon: '✓', title: 'Limits active', caption: "Today's schedule and app limits apply" },
  Allow: { bg: colors.positiveContainer, titleColor: colors.positiveText, icon: '☀', title: 'Allow all day', caption: 'All limits paused until midnight' },
};

function LimitModeCard({ mode, onSelect }: { mode: LimitMode; onSelect: (mode: LimitMode) => void }) {
  const v = MODE_VISUALS[mode];
  return (
    <div style={{ background: v.bg, borderRadius: radius.card, padding: '14px 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 36, height: 36, borderRadius: radius.icon, background: 'rgba(0,0,0,0.06)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {v.icon}
        </div>
        <div>
          <div style={{ ...typography.headline, color: v.titleColor }}>{v.title}</div>
          <div style={{ ...typography.caption, color: colors.inkMuted }}>{v.caption}</div>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 4, background: colors.surfaceSunken, borderRadius: radius.pill, padding: 4 }}>
        {(['Lock', 'Default', 'Allow'] as LimitMode[]).map((m) => {
          const selected = mode === m;
          const selectedBg = m === 'Lock' ? colors.overDisplay : m === 'Allow' ? colors.positiveDisplay : colors.surface;
          const selectedFg = m === 'Default' ? colors.ink : colors.surface;
          return (
            <button
              key={m}
              onClick={() => onSelect(m)}
              style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 2,
                background: selected ? selectedBg : 'transparent',
                border: 'none',
                borderRadius: radius.pill,
                padding: '8px 4px',
                color: selected ? selectedFg : colors.inkMuted,
              }}
            >
              <span>{m === 'Lock' ? '🔒' : m === 'Default' ? '✓' : '☀'}</span>
              <span style={{ ...typography.caption }}>{m}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function PickAppDialog({
  available,
  tvHasNoApps,
  onDismiss,
  onPick,
}: {
  available: InstalledApp[];
  tvHasNoApps: boolean;
  onDismiss: () => void;
  onPick: (app: InstalledApp) => void;
}) {
  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 12px' }}>Add app limit</h2>
      {available.length === 0 ? (
        <p style={{ ...typography.body, color: colors.inkMuted }}>
          {tvHasNoApps ? 'No apps found on the TV yet. Make sure the TV is paired and online.' : 'All apps already have limits.'}
        </p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 320, overflowY: 'auto' }}>
          {available.map((app) => (
            <button
              key={app.packageName}
              onClick={() => onPick(app)}
              style={{ textAlign: 'left', padding: '10px 8px', background: 'none', border: 'none', borderRadius: radius.input }}
            >
              {app.label}
            </button>
          ))}
        </div>
      )}
      <div style={{ marginTop: 16 }}>
        <SproutGhostButton onClick={onDismiss}>Cancel</SproutGhostButton>
      </div>
    </Modal>
  );
}

function EditLimitDialog({
  target,
  onDismiss,
  onSave,
  onRemove,
}: {
  target: { packageName: string; displayName: string; defaultMinutes: number };
  onDismiss: () => void;
  onSave: (minutes: number) => void;
  onRemove: () => void;
}) {
  const [unlimited, setUnlimited] = useState(target.defaultMinutes === UNLIMITED);
  const [minutes, setMinutes] = useState(Math.max(0, target.defaultMinutes));

  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 8px' }}>Edit limit</h2>
      <p style={{ ...typography.caption, color: colors.inkMuted, margin: '0 0 8px' }}>{target.displayName}</p>
      <p style={{ ...typography.title, color: colors.ink, margin: '0 0 8px' }}>
        {unlimited ? 'Always allowed' : formatLimitLabel(minutes)}
      </p>
      {!unlimited && (
        <>
          <input
            type="range"
            min={0}
            max={240}
            value={minutes}
            onChange={(e) => setMinutes(Number(e.target.value))}
            style={{ width: '100%' }}
          />
          <input
            type="number"
            value={minutes}
            onChange={(e) => setMinutes(Math.max(0, Number(e.target.value.replace(/\D/g, '')) || 0))}
            style={{ width: '100%', padding: 10, marginTop: 8, borderRadius: radius.input, border: `1px solid ${colors.outline}` }}
          />
        </>
      )}
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <SproutGhostButton
          onClick={() => {
            setUnlimited(false);
            setMinutes(0);
          }}
        >
          Block
        </SproutGhostButton>
        <SproutGhostButton onClick={() => setUnlimited(true)}>Always allow</SproutGhostButton>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <SproutDangerButton onClick={onRemove}>Remove</SproutDangerButton>
          <SproutGhostButton onClick={onDismiss}>Cancel</SproutGhostButton>
        </div>
        <SproutPrimaryButton onClick={() => onSave(unlimited ? UNLIMITED : minutes)}>Save</SproutPrimaryButton>
      </div>
    </Modal>
  );
}

function EditOverallLimitDialog({
  currentMinutes,
  onDismiss,
  onSave,
}: {
  currentMinutes: number;
  onDismiss: () => void;
  onSave: (minutes: number) => void;
}) {
  const [unlimited, setUnlimited] = useState(currentMinutes === UNLIMITED);
  const [minutes, setMinutes] = useState(Math.max(0, currentMinutes));

  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 8px' }}>Overall daily limit</h2>
      <p style={{ ...typography.caption, color: colors.inkMuted, margin: '0 0 8px' }}>
        Once today's total usage across all apps reaches this, the TV blocks no matter which app is open.
      </p>
      <p style={{ ...typography.title, color: colors.ink, margin: '0 0 8px' }}>
        {unlimited ? 'No overall limit' : formatLimitLabel(minutes)}
      </p>
      {!unlimited && (
        <>
          <input
            type="range"
            min={0}
            max={480}
            value={minutes}
            onChange={(e) => setMinutes(Number(e.target.value))}
            style={{ width: '100%' }}
          />
          <input
            type="number"
            value={minutes}
            onChange={(e) => setMinutes(Math.max(0, Number(e.target.value.replace(/\D/g, '')) || 0))}
            style={{ width: '100%', padding: 10, marginTop: 8, borderRadius: radius.input, border: `1px solid ${colors.outline}` }}
          />
        </>
      )}
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <SproutGhostButton
          onClick={() => {
            setUnlimited(false);
            setMinutes(0);
          }}
        >
          Block all
        </SproutGhostButton>
        <SproutGhostButton onClick={() => setUnlimited(true)}>No limit</SproutGhostButton>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
        <SproutGhostButton onClick={onDismiss}>Cancel</SproutGhostButton>
        <SproutPrimaryButton onClick={() => onSave(unlimited ? UNLIMITED : minutes)}>Save</SproutPrimaryButton>
      </div>
    </Modal>
  );
}

function EditLockoutDialog({
  current,
  onDismiss,
  onSave,
}: {
  current: LockoutSettings;
  onDismiss: () => void;
  onSave: (minutes: number, mode: LockoutMode) => void;
}) {
  const [minutes, setMinutes] = useState(current.durationMinutes);
  const [mode, setMode] = useState<LockoutMode>(current.mode);

  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 8px' }}>Code lockout</h2>
      <p style={{ ...typography.caption, color: colors.inkMuted }}>
        After 5 incorrect codes within 1 minute, the TV's code entry will:
      </p>
      <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <input type="radio" checked={mode === 'TIMER'} onChange={() => setMode('TIMER')} />
        Lock for a set time
      </label>
      <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <input type="radio" checked={mode === 'PARENT_UNLOCK'} onChange={() => setMode('PARENT_UNLOCK')} />
        Require a parent to unlock
      </label>
      {mode === 'TIMER' && (
        <>
          <p style={{ ...typography.title, color: colors.ink }}>{formatLimitLabel(minutes)}</p>
          <input
            type="range"
            min={5}
            max={60}
            value={minutes}
            onChange={(e) => setMinutes(Number(e.target.value))}
            style={{ width: '100%' }}
          />
        </>
      )}
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
        <SproutGhostButton onClick={onDismiss}>Cancel</SproutGhostButton>
        <SproutPrimaryButton onClick={() => onSave(minutes, mode)}>Save</SproutPrimaryButton>
      </div>
    </Modal>
  );
}
