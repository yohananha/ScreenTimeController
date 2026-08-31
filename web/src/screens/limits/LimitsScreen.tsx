import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
import type { TimeFrameSchedule } from '../../models/TimeFrameSchedule';
import { Chevron } from '../../components/Chevron';
import { formatLimitLabel, summarizeSchedule } from '../../i18n/format';

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
  const { t, i18n } = useTranslation();
  const {
    state,
    writeError,
    clearWriteError,
    setLimit,
    removeLimit,
    setOverallLimit,
    selectInstantLock,
    selectAllowAllDay,
    selectDefaultLimits,
  } = useLimits(familyId);
  const [editing, setEditing] = useState<{ packageName: string; displayName: string; defaultMinutes: number } | null>(null);
  const [picking, setPicking] = useState(false);
  const [editingOverall, setEditingOverall] = useState(false);
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
          familyName={t('topHeader.family')}
          parentInitial="P"
          trailing={
            <button
              onClick={onOpenHistory}
              aria-label={t('limits.historyAria')}
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
          <h1 style={{ ...typography.display, fontSize: 30, color: colors.ink, margin: 0 }}>{t('limits.title')}</h1>
          <div style={{ ...typography.caption, color: colors.inkMuted, marginTop: 5 }}>
            {new Date().toLocaleDateString(i18n.language, { weekday: 'long', month: 'short', day: 'numeric' })}
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
          ofLabel={t('limits.ofDaily', { limit: formatLimitLabel(state.overallDailyMinutes) })}
          progress={overallProgress}
          timeLeft={t('limits.leftLabel', { time: formatLimitLabel(leftMin) })}
          resetLabel={t('limits.resetsAtMidnight')}
          status={overallStatus}
          onClick={() => setEditingOverall(true)}
        />

        <AllowedHoursRow schedule={state.timeFrame} onClick={onOpenTimeFrame} />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 2px 4px' }}>
          <span style={{ ...typography.title, color: colors.ink }}>{t('limits.appLimits')}</span>
          <span style={{ ...typography.caption, color: colors.inkMuted }}>{t('limits.appsCount', { count: state.limits.length })}</span>
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
            <span style={{ ...typography.headline, color: colors.ink }}>{t('limits.noAppLimitsTitle')}</span>
            <span style={{ ...typography.body, color: colors.inkMuted }}>{t('limits.noAppLimitsSubtitle')}</span>
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
        style={{ position: 'fixed', bottom: 96, insetInlineEnd: 18, borderRadius: radius.large }}
      >
        {t('limits.addLimit')}
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

    </div>
  );
}

function AllowedHoursRow({ schedule, onClick }: { schedule: TimeFrameSchedule; onClick: () => void }) {
  const { t } = useTranslation();
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
          <div style={{ ...typography.headline, color: colors.ink }}>{t('limits.allowedHours')}</div>
          <div style={{ ...typography.caption, color: colors.inkMuted }}>{summarizeSchedule(schedule)}</div>
        </div>
      </div>
      <Chevron style={{ color: colors.inkMuted }} />
    </div>
  );
}

function useModeVisuals(): Record<LimitMode, { bg: string; titleColor: string; icon: string; title: string; caption: string; segLabel: string }> {
  const { t } = useTranslation();
  return {
    Lock: { bg: colors.overContainer, titleColor: colors.overText, icon: '🔒', title: t('limits.modeLockTitle'), caption: t('limits.modeLockCaption'), segLabel: t('limits.modeLockLabel') },
    Default: { bg: colors.surface, titleColor: colors.ink, icon: '✓', title: t('limits.modeDefaultTitle'), caption: t('limits.modeDefaultCaption'), segLabel: t('limits.modeDefaultLabel') },
    Allow: { bg: colors.positiveContainer, titleColor: colors.positiveText, icon: '☀', title: t('limits.modeAllowTitle'), caption: t('limits.modeAllowCaption'), segLabel: t('limits.modeAllowLabel') },
  };
}

function LimitModeCard({ mode, onSelect }: { mode: LimitMode; onSelect: (mode: LimitMode) => void }) {
  const visuals = useModeVisuals();
  const v = visuals[mode];
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
              <span style={{ ...typography.caption }}>{visuals[m].segLabel}</span>
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
  const { t } = useTranslation();
  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 12px' }}>{t('limits.addAppLimitTitle')}</h2>
      {available.length === 0 ? (
        <p style={{ ...typography.body, color: colors.inkMuted }}>
          {tvHasNoApps ? t('limits.noAppsFound') : t('limits.allAppsHaveLimits')}
        </p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 320, overflowY: 'auto' }}>
          {available.map((app) => (
            <button
              key={app.packageName}
              onClick={() => onPick(app)}
              style={{ textAlign: 'start', padding: '10px 8px', background: 'none', border: 'none', borderRadius: radius.input }}
            >
              {app.label}
            </button>
          ))}
        </div>
      )}
      <div style={{ marginTop: 16 }}>
        <SproutGhostButton onClick={onDismiss}>{t('common.cancel')}</SproutGhostButton>
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
  const { t } = useTranslation();
  const [unlimited, setUnlimited] = useState(target.defaultMinutes === UNLIMITED);
  const [minutes, setMinutes] = useState(Math.max(0, target.defaultMinutes));

  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 8px' }}>{t('limits.editLimitTitle')}</h2>
      <p style={{ ...typography.caption, color: colors.inkMuted, margin: '0 0 8px' }}>{target.displayName}</p>
      <p style={{ ...typography.title, color: colors.ink, margin: '0 0 8px' }}>
        {unlimited ? t('limits.alwaysAllowed') : formatLimitLabel(minutes)}
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
          {t('limits.block')}
        </SproutGhostButton>
        <SproutGhostButton onClick={() => setUnlimited(true)}>{t('limits.alwaysAllow')}</SproutGhostButton>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <SproutDangerButton onClick={onRemove}>{t('common.remove')}</SproutDangerButton>
          <SproutGhostButton onClick={onDismiss}>{t('common.cancel')}</SproutGhostButton>
        </div>
        <SproutPrimaryButton onClick={() => onSave(unlimited ? UNLIMITED : minutes)}>{t('common.save')}</SproutPrimaryButton>
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
  const { t } = useTranslation();
  const [unlimited, setUnlimited] = useState(currentMinutes === UNLIMITED);
  const [minutes, setMinutes] = useState(Math.max(0, currentMinutes));

  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 8px' }}>{t('limits.overallLimitTitle')}</h2>
      <p style={{ ...typography.caption, color: colors.inkMuted, margin: '0 0 8px' }}>{t('limits.overallLimitBody')}</p>
      <p style={{ ...typography.title, color: colors.ink, margin: '0 0 8px' }}>
        {unlimited ? t('limits.noOverallLimit') : formatLimitLabel(minutes)}
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
          {t('limits.blockAll')}
        </SproutGhostButton>
        <SproutGhostButton onClick={() => setUnlimited(true)}>{t('limits.noLimitAction')}</SproutGhostButton>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
        <SproutGhostButton onClick={onDismiss}>{t('common.cancel')}</SproutGhostButton>
        <SproutPrimaryButton onClick={() => onSave(unlimited ? UNLIMITED : minutes)}>{t('common.save')}</SproutPrimaryButton>
      </div>
    </Modal>
  );
}
